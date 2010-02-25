'''
Created on Feb 01, 2010

@author: Tonio Gsell
@author: Mustafa Yuecel
'''

import struct
import os
import logging
import zlib
from collections import deque
from threading import Event, Thread
from pyinotify import WatchManager, ThreadedNotifier, EventsCodes, ProcessEvent

import BackLogMessage
from AbstractPlugin import AbstractPluginClass
from string import join

DEFAULT_DATE_TIME_FORMATE = 'yyyy-MM-dd'

CHUNK_ACK_CHECK_SEC = 5

ACK_PACKET = 0
INIT_PACKET = 1
RESEND_PACKET = 2
CHUNK_PACKET = 3
CRC_PACKET = 4

class BigBinaryPluginClass(AbstractPluginClass):
    '''
    This plugin offers the functionality to send binaries to GSN
    in the size of up to 4GB. You can specify different folders to be watched for
    binaries to be modified. On each modification event (modify binary/create binary)
    the modified binary will be sent to GSN and afterwards deleted. The binaries
    will be sent in chunks. Thus, no significant interrupts of other plugin traffic
    is guaranteed. In case of a connection loss, the download of the actual binary
    will be resumed as soon as GSN reconnects.
    
    The following values have to specified in the plugin configuration file:
        rootdir:        The root folder in which the binaries are located
        
        watch#:         The relative directory based on the root directory to be watched.
                        The second part (after the comma) specifies where it has to be
                        stored on side of GSN. In the database or in the filesystem.
                        If filesystem is chosen, the third part can optionally be set
                        to format the subfolders based on the modification time. Please
                        refer to Java SimpleDateFormat Class for possible formatting information.
                        The different watch options have to be followed by an incrementing number
                        (e.g. watch1 = ..., watch2 = ..., etc.)
                        If no watch is specified, the root directory will be watched for
                        binary modifications (this is the same as setting 'watch1 = ./,filesystem').

    For example:
        rootdir = /media/
        watch1 = webcam1,database
        watch2 = webcam2,filesystem
        watch3 = camera,filesystem,yyyy-MM
        
        In this example the three folders '/media/webcam1', '/media/webcam2' and
        '/media/camera' will be watched for any new binary modifications. Changing binaries
        in '/media/webcam1' will be stored in the database in GSN. Whereas changed ones in the
        folders '/media/webcam2' and '/media/camera' will be stored on disk on side of GSN.
        Binaries from '/media/webcam2' will be separated into standardly named subfolders
        corresponding to the default set by DEFAULT_DATE_TIME_FORMATE. While binaries from
        '/media/camera' will be sorted into monthly based subfolders.



    data/instance attributes:
    _parent
    _notifier
    _stopped
    _filedeque
    _work
    _waitforack
    _rootdir
    _watches
    
    TODO: remove CRC functionality after long time testing. It is not necessary over TCP.
    '''
    
    def __init__(self, parent, options):
        AbstractPluginClass.__init__(self, parent, options)
        self._parent = parent
        
        self._rootdir = self.getOptionValue('rootdir')
        
        watches = self.getOptionValues('watch')

        if self._rootdir is None:
            raise TypeError('no rootdir specified')
        
        if not self._rootdir.endswith('/'):
            self._rootdir += '/'
        
        if not os.path.isdir(self._rootdir):
            raise TypeError('rootdir >' + self._rootdir + '< is not a directory')

        if not watches:
            watches.append('.,filesystem')

        wm = WatchManager()
        self._notifier = ThreadedNotifier(wm, BinaryChangedProcessing(self))
        
        self._work = Event()
        self._filedeque = deque()
        self._msgdeque = deque()
        
        self._watches = []
        filetime = []
        for watch in watches:
            w = watch.split(',')
        
            if not w[0].endswith('/'):
                w[0] += '/'
                
            if len(w) == 1:
                raise TypeError('watch >' + watch + '< in the configuration file is not well formatted')
            elif len(w) == 2 or len(w) == 3:
                w[1] = w[1].lower()
                if w[1] == 'database':
                    w[1] = 1
                    if len(w) == 2:
                        w.append('')
                elif w[1] == 'filesystem':
                    w[1] = 0
                    if len(w) == 2:
                        w.append(DEFAULT_DATE_TIME_FORMATE)
                    elif len(w) == 3:
                        if len(w[2]) > 255:
                            raise TypeError('the date time format in watch >' + watch + '< is longer than 255 characters')
                else:
                    raise TypeError('the second part of watch >' + watch + '< in the configuration file has to be database or filesystem')
            else:
                raise TypeError('watch >' + watch + '< in the configuration file has too many commas')
            
            dir = w[0]
            pathname = os.path.join(self._rootdir, dir)
            pathname = os.path.normpath(pathname)
            if not os.path.isdir(pathname):
                os.makedirs(pathname)
                
            # tell the watch manager which folders to watch for newly written files
            wm.add_watch(pathname, EventsCodes.FLAG_COLLECTIONS['OP_FLAGS']['IN_CLOSE_WRITE'])
            self.info('enable close-after-write notification for path ' + pathname)
            
            files = os.listdir(pathname)
            
            self._watches.append(w)
            
            # check the path for existing files which have to be sent
            for filename in files:
                f = os.path.join(pathname, filename)
                if os.path.isfile(f):
                    time = os.stat(f).st_mtime
                    filetime.append((time, f))
                    
        for watch in self._watches:
            self.debug('watch: ' + watch[0] + '  -  ' + str(watch[1]) + '  -  ' + watch[2])
        
        # sort the existing files by time
        filetime.sort()
        # and put it into the fifo
        for file in filetime:
            self._filedeque.appendleft(file[1])
            self.debug('putting existing file into FIFO: ' + file[1])

        self._lastRecvPacketType = None
        self._waitforack = False
        self._stopped = False


    def getMsgType(self):
        return BackLogMessage.BIG_BINARY_MESSAGE_TYPE
    
    
    def msgReceived(self, msg):
        self.debug('message received')
        self._msgdeque.appendleft(msg)
        self._work.set()
           
        
    def run(self):
        self.info('started')
        
        # start pyinotify to watch the specified folders
        try:
            self._notifier.start()
        except Exception, e:
            if not self._stopped:
                self.exception(e)
                
        filedescriptor = None
        chunkNumber = -1
                
        while not self._stopped:
            # wait for the next file event to happen
            self._work.wait()
            if self._stopped:
                break
                
            try:
                message = self._msgdeque.pop()
                self._waitforack = False
                # the first byte of the message defines its type
                type = struct.unpack('B', message[0])[0]
                self.debug('type: ' + str(type))
                
                if type == ACK_PACKET:
                    setCRC = False
                    chkNr = int(struct.unpack('<I', message[1:5])[0])
                    self.debug('acknowledge packet received for chunk number >' + str(chkNr) + '< sent chunk number was >' + str(chunkNumber) + '<')
                    
                    if self._lastRecvPacketType == ACK_PACKET:
                        if chunkNumber-1 == chkNr:
                            self.debug('acknowledge packet for chunk number >' + str(chkNr) + '< already received')
                            continue
                        elif not chunkNumber == chkNr:
                            self.warning('received chunk number >' + str(chkNr) + '< is not the one we expected >' + str(chunkNumber) + '< (should not happen)')
                            continue
                        elif chkNr == 0:
                            # crc has been accepted by GSN
                            self.debug('crc has been accepted for ' + filename)
                            # remove it from disk
                            os.remove(filename)
                            setCRC = True
                    elif self._lastRecvPacketType == CRC_PACKET and chkNr == 0:
                        self.debug('CRC packet already received')
                        continue
                
                    if setCRC:
                        self._lastRecvPacketType = CRC_PACKET
                    else:
                        self._lastRecvPacketType = ACK_PACKET
                # if the type is INIT_PACKET we have to send a new file
                elif type == INIT_PACKET:
                    if self._lastRecvPacketType == INIT_PACKET:
                        self.debug('init packet already received')
                        continue
                    self.debug('new binary request received')
                    if filedescriptor and not filedescriptor.closed:
                        filename = filedescriptor.name
                        self.warning('new file request, but actual file (' + filename + ') not yet closed -> remove it!')
                        os.chmod(filename, 0744)
                        filedescriptor.close()
                        os.remove(filename)
                    
                    self._lastRecvPacketType = INIT_PACKET
                # if the type is RESEND_PACKET we have to resend a part of a file...
                elif type == RESEND_PACKET:
                    self.debug('binary retransmission request received')
                    
                    # how much of the file has already been downloaded
                    downloaded = int(struct.unpack('<I', message[1:5])[0])
                    # what chunk number are we at
                    chunkNumber = int(struct.unpack('<I', message[5:9])[0])
                    # what is the name of the file to resend
                    filenamenoprefix = struct.unpack(str(len(message)-9) + 's', message[9:len(message)])[0]
                    filename = os.path.join(self._rootdir, filenamenoprefix)
                    self.debug('downloaded size: ' + str(downloaded))
                    self.debug('chunk number: ' + str(chunkNumber))
                    self.debug('file: ' + filename)
                    
                    try:
                        # open the specified file
                        filedescriptor = open(filename, 'rb')
                        os.chmod(filename, 0444)
                        
                        if downloaded > 0:
                            # recalculate the crc
                            sizecalculated = 0
                            crc = None
                            while sizecalculated != downloaded:
                                if downloaded - sizecalculated > 4096:
                                    part = filedescriptor.read(4096)
                                else:
                                    part = filedescriptor.read(downloaded - sizecalculated)
                                    
                                if crc:
                                    crc = zlib.crc32(part, crc)
                                else:
                                    crc = zlib.crc32(part)
                                sizecalculated += len(part)
                                    
                                if part == '':
                                    self.warning('end of file reached while calculating CRC')
                                    break
        
                            self.debug('recalculated crc: ' + str(crc))
                        else:
                            crc = None
                    except IOError, e:
                        self.warning(e)
                    
                    self._lastRecvPacketType = RESEND_PACKET
            except  IndexError:
                pass
        
            try:
                if (not filedescriptor or filedescriptor.closed):
                    # get the next file to send out of the fifo
                    filename = self._filedeque.pop()
                    
                    if os.path.isfile(filename):
                        # open the file
                        filedescriptor = open(filename, 'rb')
                        os.chmod(filename, 0444)
                    else:
                        # if the file does not exist we are continuing in the fifo
                        continue
                    
                    # get the size of the file
                    filelen = os.path.getsize(filename)
                    
                    if filelen == 0:
                        # if the file is empty we ignore it and continue in the fifo
                        self.debug('ignore empty file: ' + filename)
                        os.chmod(filename, 0744)
                        filedescriptor.close()
                        os.remove(filename)
                        continue
                    
                    filenamenoprefix = filename.replace(self._rootdir, '')
                    self.debug('filename without prefix: ' + filenamenoprefix)
                    filenamelen = len(filenamenoprefix)
                    if filenamelen > 255:
                        filenamelen = 255
                    
                    # check witch watch this file belongs to
                    l = -1
                    watch = None
                    for w in self._watches:
                        if (filename.count(w[0]) > 0 or w[0] == './') and len(w[0]) > l:
                            watch = w
                            l = len(w[0])
                    if not watch:
                        self.error('no watch specified for ' + filename + ' (this is very strange!!!) -> close file')
                        os.chmod(filename, 0744)
                        filedescriptor.close()
                        continue
                        
                    chunkNumber = 1
                    packet = struct.pack('<BqIB', INIT_PACKET, os.stat(filename).st_mtime * 1000, filelen, watch[1])
                    packet += struct.pack(str(filenamelen) + 'sx', filenamenoprefix)
                    packet += struct.pack(str(len(watch[2])) + 'sx', watch[2])
                    
                    self.debug('sending initial binary packet for ' + filedescriptor.name + ' from watch directory >' + watch[0] + '<, storage type: >' + str(watch[1]) + '< and time date format >' + watch[2] + '<')
                
                    crc = None
                # or are we already sending chunks of a file?
                else:
                    # read the next chunk out of the opened file
                    chunk = filedescriptor.read(self._parent.gsnpeer.getMTU()-20)
                    
                    if crc:
                        crc = zlib.crc32(chunk, crc)
                    else:
                        crc = zlib.crc32(chunk)
                    
                    if chunk == "":
                        # so we have reached the end of the file...
                        self.debug('binary completely sent')
                        
                        # create the crc packet [type, crc]
                        packet = struct.pack('<Bi', CRC_PACKET, crc)
                        packet += chunk
                        
                        filename = filedescriptor.name
                        os.chmod(filename, 0744)
                        filedescriptor.close()
                        
                        # send it
                        self.debug('sending crc: ' + str(crc))
                        timestamp = self.getTimeStamp()
                        self._lasttimestamp = timestamp
                        first = True
                        chunkNumber = 0
                    else:
                        # increase the chunk number
                        chunkNumber = chunkNumber + 1
                        # create the packet [type, chunk number (4bytes)]
                        packet = struct.pack('<BI', CHUNK_PACKET, chunkNumber)
                        packet += chunk
                        
                        self.debug('sending binary chunk number ' + str(chunkNumber) + ' for ' + filedescriptor.name)
                
                # tell BackLogMain to send the packet to GSN
                self._work.clear()
                first = True
                while not self._work.isSet() and self.isGSNConnected():
                    if not first:
                        self.info('resend message')
                    self._waitforack = True
                    self.processMsg(self.getTimeStamp(), packet, self._backlog)
                    # and resend it if no ack has been received
                    self._work.wait(CHUNK_ACK_CHECK_SEC)
                    first = False
            except IndexError:
                # fifo is empty
                self.debug('file FIFO is empty waiting for next file to arrive')
                packet = struct.pack('<B', ACK_PACKET)
                self.processMsg(self.getTimeStamp(), packet, self._backlog)
                self._work.clear()
            except Exception, e:
                self._waitforack = False
                os.chmod(filename, 0744)
                filedescriptor.close()
                self.error(e.__str__())
            

        self.info('died')
    
    
    def stop(self):
        self._stopped = True
        self._notifier.stop()
        self._work.set()
        self._filedeque.clear()
        if not filedescriptor.closed:
            os.chmod(filename, 0744)
            filedescriptor.close()
        self.info('stopped')




class BinaryChangedProcessing(ProcessEvent):
    
    '''
    data/instance attributes:
    _logger
    _parent
    '''

    def __init__(self, parent):
        self._logger = logging.getLogger(self.__class__.__name__)
        self._parent = parent

    def process_default(self, event):
        self._logger.debug(event.pathname + ' changed')
        
        self._parent._filedeque.appendleft(event.pathname)
        if self._parent.isGSNConnected() and not self._parent._waitforack:
            self._parent._work.set()