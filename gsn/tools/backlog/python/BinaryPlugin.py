# -*- coding: UTF-8 -*-
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import struct
import os
import logging
import zlib
from collections import deque
from threading import Timer, Event
from pyinotify import WatchManager, ThreadedNotifier, EventsCodes, ProcessEvent

import BackLogMessage
from AbstractPlugin import AbstractPluginClass

DEFAULT_DATE_TIME_FORMATE = 'yyyy-MM-dd'

RESEND_INTERVAL_SEC = 30

CHUNK_SIZE = 64000

ACK_PACKET = 0
INIT_PACKET = 1
RESEND_PACKET = 2
CHUNK_PACKET = 3
CRC_PACKET = 4

class BinaryPluginClass(AbstractPluginClass):
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
                        The third part can be used to define a device id this binary data
                        is coming from. If it is not set the device id of this Core Station
                        is used.
                        If filesystem has been chosen, the fourth part can optionally be set
                        to format the subfolders based on the modification time. Please
                        refer to Java SimpleDateFormat Class for possible formatting information.
                        The different watch options have to be followed by an incrementing number
                        (e.g. watch1 = ..., watch2 = ..., etc.)
                        If no watch is specified, the root directory will be watched for
                        binary modifications (this is the same as setting 'watch1 = ./,filesystem,,').

    For example:
        rootdir = /media/
        watch1 = webcam1,database,,
        watch2 = webcam2,filesystem,666,
        watch3 = camera,filesystem,,yyyy-MM
        
        In this example the three folders '/media/webcam1', '/media/webcam2' and
        '/media/camera' will be watched for any new binary modifications. Changing binaries
        in '/media/webcam1' will be stored in the database in GSN. Whereas changed ones in the
        folders '/media/webcam2' and '/media/camera' will be stored on disk on side of GSN.
        Binaries from '/media/webcam2' will be linked to device id 666. While binaries from
        '/media/webcam1' and '/media/camera' will be linked to the device id of this CoreStation.
        Binaries from '/media/webcam2' will be separated into standardly named subfolders
        corresponding to the default set by DEFAULT_DATE_TIME_FORMATE. While binaries from
        '/media/camera' will be sorted into monthly based subfolders.



    data/instance attributes:
    _notifier
    _plugStop
    _filedeque
    _msgqueue
    _isBusy
    _msgEvent
    _workEvent
    _waitforack
    _rootdir
    _filedescriptor
    _watches
    _resendcounter
    _lastRecvPacketType
    _lastSentPacketType
    _sentPacketNr
    
    TODO: remove CRC functionality after long time testing. It is not necessary over TCP.
    '''
    
    def __init__(self, parent, options):
        AbstractPluginClass.__init__(self, parent, options, False)
        
        self._rootdir = self.getOptionValue('rootdir')
        
        watches = self.getOptionValues('watch')
        
        wait_min_for_file = self.getOptionValue('wait_min_for_file')
        if wait_min_for_file:
            self._waitforfile = True
            self._waitforfiletimer = Timer(float(wait_min_for_file) * 60, self.checkForFileTimerAction)
            self._waitforfiletimer.start()
            self.info('waiting at least ' + wait_min_for_file + ' minutes for a file to arrive')
        else:
            self._waitforfile = False

        if self._rootdir is None:
            raise TypeError('no rootdir specified')
        
        if not self._rootdir.endswith('/'):
            self._rootdir += '/'
        
        if not os.path.isdir(self._rootdir):
            self.warning('rootdir >' + self._rootdir + '< is not a directory -> creating it')
            os.makedirs(self._rootdir)

        if not watches:
            watches.append('.,filesystem,,')

        wm = WatchManager()
        self._notifier = ThreadedNotifier(wm, BinaryChangedProcessing(self))
        
        self._fileEvent = Event()
        self._filedeque = deque()
        self._msgqueue = Queue()
        
        self._watches = []
        filetime = []
        for watch in watches:
            w = watch.split(',')
            for index, entry in enumerate(w):
                w[index] = entry.strip()
        
            if not w[0].endswith('/'):
                w[0] += '/'
                
            if len(w) != 4:
                raise TypeError('watch >' + watch + '< in the configuration file is not well formatted')
            else:
                w[1] = w[1].lower()
                if w[1] == 'database':
                    w[1] = 1
                elif w[1] == 'filesystem':
                    w[1] = 0
                else:
                    raise TypeError('the second part of watch >' + watch + '< in the configuration file has to be database or filesystem')
                    
                if not w[2]:
                    w[2] = self.getDeviceId()
                else:
                    if not w[2].isdigit():
                        raise TypeError('the device id in watch >' + watch + '< has to be a number')
                    else:
                        w[2] = int(w[2])
                        
                if not w[3]:
                    w[3] = DEFAULT_DATE_TIME_FORMATE
                else:
                    if len(w[3]) > 255:
                        raise TypeError('the date time format in watch >' + watch + '< is longer than 255 characters')
            
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
                    
        for path, storage, device_id, time_format in self._watches:
            self.debug('watch: ' + path + '  -  ' + str(storage) + '  -  ' + str(device_id) + ' - ' + time_format)
        
        # sort the existing files by time
        filetime.sort()
        # and put it into the fifo
        for file in filetime:
            self._filedeque.appendleft([file[1], os.path.getsize(file[1])])
            self.debug('putting existing file into FIFO: ' + file[1])
        if filetime:
            self.info('files to be transmitted: ' + str(len(filetime)))
            
        if self._filedeque or self.getMaxRuntime():
            self._isBusy = True
        else:
            self._isBusy = False
            
        if self._filedeque:
            self._fileEvent.set()
                
        self._filedescriptor = None
        self._waitforack = True
        self._plugStop = False
        self._resendcounter = 0


    def getMsgType(self):
        return BackLogMessage.BINARY_MESSAGE_TYPE
    
    
    def msgReceived(self, data):
        self.debug('message received')
        self._msgqueue.put(data)
        self._msgEvent.set()
        self._workEvent.set()
        
    
    def connectionToGSNestablished(self):
        self.debug('connection established')
        if self._filedescriptor:
            if os.path.exists(self._filedescriptor.name):
                self._filedeque.append([self._filedescriptor.name, os.path.getsize(self._filedescriptor.name)])
            self._filedescriptor.close()
        self._lastRecvPacketType = None
        self._lastSentPacketType = None
        self._backlogMain._waitforack = False
        
    
    def connectionToGSNlost(self):
        self.debug('connection lost')
        self._workEvent.clear()
           
        
    def run(self):
        self.info('started')
        
        # start pyinotify to watch the specified folders
        try:
            self._notifier.start()
        except Exception, e:
            if not self._stopped:
                self.exception(e)

        packet = None
        self._lastRecvPacketType = None
        self._lastSentPacketType = None
                
        while not self._plugStop:
            if packet == None:
                # wait for the next message to arrive from GSN
                data = self._msgqueue.get()
            else:
                self._sentPacketNr = pktNr
                self.processMsg(self.getTimeStamp(), [self._sentPacketNr] + packet, self._priority, self._backlog)
                while not self._plugStop:
                    try:
                        data = self._msgqueue.get(timeout=RESEND_INTERVAL_SEC)
                    except Queue.Empty:
                        if self._plugStop:
                            break
                        self.debug('resend message')
                        self._resendcounter += 1
                        packet[3] += 1
                        self.processMsg(self.getTimeStamp(), [self._sentPacketNr] + packet, self._priority, self._backlog)
                    else:
                        break
                
            if self._plugStop:
                break
            
            # the first entry of the data defines its packet number
            pktNr = data[0]
            self.debug('packet number: ' + str(pktNr))
            # the second entry of the data defines its type
            pktType = data[1]
            self.debug('packet type: ' + str(pktType))
            
            # send an acknowledge
            self.processMsg(self.getTimeStamp(), [pktNr, ACK_PACKET, pkt_type], self._priority, self._backlog)
            
            # if the type is INIT_PACKET we have to send a new file
            if pktType == INIT_PACKET:
                if self._lastRecvPacketType == INIT_PACKET:
                    self.debug('init packet already received')
                else:
                    self.debug('new binary request received')
                    packet = self._getInitialBinaryPacket()
                    self._lastRecvPacketType = INIT_PACKET
            # if the type is RESEND_PACKET we have to resend a part of a file...
            elif pktType == RESEND_PACKET:
                if self._lastRecvPacketType == RESEND_PACKET:
                    self.debug('binary retransmission request already received')
                else:
                    self.debug('binary retransmission request received')
                    if self._reopenFile(data[2], data[3], struct.unpack('i', struct.pack('I', data[4]))[0], data[5]):
                        packet = self._getNextChunk()
                        self._lastRecvPacketType = RESEND_PACKET
                    else:
                        packet = None
            elif pktType == ACK_PACKET:
                ackType = data[2]
                    
                if pktNr == self._sentPacketNr+1:
                    if ackType == INIT_PACKET and self._lastSentPacketType == INIT_PACKET:
                        self.debug('acknowledge received for init packet')
                        packet = self._getNextChunk()
                    elif ackType == CHUNK_PACKET and self._lastSentPacketType == CHUNK_PACKET:
                        self.debug('acknowledge for packet number >' + str(pktNr) + '< received')
                        packet = self._getNextChunk()
                    elif ackType == CRC_PACKET and self._lastSentPacketType == CRC_PACKET:
                        filename = self._filedescriptor.name
                        os.chmod(filename, 0744)
                        self._filedescriptor.close()
                        if os.path.isfile(filename):
                            # crc has been accepted by GSN
                            self.debug('crc has been accepted for ' + filename)
                            # remove it from disk
                            os.remove(filename)
                            packet = self._getInitialBinaryPacket()
                        else:
                            self.error('crc acknowledge file does not exist')
                            
                    self._lastRecvPacketType = ACK_PACKET
                elif pktNr == self._sentPacketNr:
                    self.debug('acknowledge already received')
                else:
                    self.error('acknowledge out of order')
                
                
    def _getInitialBinaryPacket(self):
        if self._filedescriptor and not self._filedescriptor.closed:
            filename = self._filedescriptor.name
            self.warning('new file request, but actual file (' + filename + ') not yet closed -> remove it!')
            os.chmod(filename, 0744)
            self._filedescriptor.close()
            os.remove(filename)
            
        self._fileEvent.wait()
        if self._plugStop:
            return None
            
        ret = self._openNewFile()
        if not ret:
            return None
            
        self._resendcounter = 0
            
        packet = [INIT_PACKET, self._getFileQueueSize(), len(self._filedeque), self._resendcounter] + ret
        packet.append(filenamenoprefix)
        packet.append(watch[3])
        
        self.debug('sending initial binary packet for ' + self._filedescriptor.name + ' from watch directory >' + watch[0] + '<, storage type: >' + str(watch[1]) + '<, device id >' + str(watch[2]) + ' and time date format >' + watch[3] + '<')
    
        crc = None
        return packet
    
    
    def _openNewFile(self):
        while not self._plugStop:
            try:
                # get the next file to send out of the fifo
                fileobj = self._filedeque.pop()
            except IndexError:
                self._fileEvent.clear()
                self.debug('file FIFO is empty waiting for next file to arrive')
                return None
                
            filename = fileobj[0]
            
            if os.path.isfile(filename):
                # open the file
                self._filedescriptor = open(filename, 'rb')
                os.chmod(filename, 0444)
            else:
                # if the file does not exist we are continuing in the fifo
                continue
            
            # get the size of the file
            filelen = fileobj[1]
            
            if filelen == 0:
                # if the file is empty we ignore it and continue in the fifo
                self.debug('ignore empty file: ' + filename)
                os.chmod(filename, 0744)
                self._filedescriptor.close()
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
                self._filedescriptor.close()
                continue
            
            return [watch[2], long(os.stat(filename).st_mtime * 1000), filelen, watch[1]]
        
        
    def _reopenFile(self, downloaded, gsnCRC, filenamenoprefix):
        filename = os.path.join(self._rootdir, filenamenoprefix)
        self.debug('downloaded size: ' + str(downloaded))
        self.debug('crc at GSN: ' + str(gsnCRC))
        self.debug('file: ' + filename)
        
        try:
            if downloaded > 0:
                try:
                    self._filedeque.remove([filename, os.path.getsize(filename)])
                except:
                    pass
                else:
                    self.debug('copy >' + filename + '< removed from file queue')
                # open the specified file
                self._filedescriptor = open(filename, 'rb')
                os.chmod(filename, 0444)
                
                # recalculate the crc
                sizecalculated = 0
                crc = None
                while sizecalculated != downloaded:
                    if downloaded - sizecalculated > 4096:
                        part = self._filedescriptor.read(4096)
                    else:
                        part = self._filedescriptor.read(downloaded - sizecalculated)
                        
                    if crc:
                        crc = zlib.crc32(part, crc)
                    else:
                        crc = zlib.crc32(part)
                    sizecalculated += len(part)
                        
                    if part == '':
                        self.warning('end of file reached while calculating CRC')
                        break
                    
                if crc != gsnCRC:
                    self.warning('crc received from gsn >' + str(gsnCRC) + '< does not match local one >' + str(crc) + '< -> resend complete binary')
                    os.chmod(filename, 0744)
                    self._filedeque.append([filename, os.path.getsize(filename)])
                    self._filedescriptor.close()
                    return self._openNewFile()
                else:
                    self.debug('crc received from gsn matches local one -> resend following part of binary')
                    return True
            else:
                # resend the whole binary
                self._filedeque.append([filename, os.path.getsize(filename)])
                return self._openNewFile()
        except IOError, e:
            self.error(e)
            return self._openNewFile()
        
        
    def _getNextChunk(self):
        # read the next chunk out of the opened file
        chunk = self._filedescriptor.read(CHUNK_SIZE)
        
        if crc:
            crc = zlib.crc32(chunk, crc)
        else:
            crc = zlib.crc32(chunk)
        
        if not chunk:
            # so we have reached the end of the file...
            self.debug('binary completely sent')
            
            # create the crc packet [type, crc]
            packet = [CRC_PACKET, self._getFileQueueSize(), len(self._filedeque), self._resendcounter, struct.unpack('I', struct.pack('i', crc))[0]]
            
            # send it
            self.debug('sending crc: ' + str(crc))
        else:
            # create the packet [type, chunk number (4bytes)]
            packet = [CHUNK_PACKET, self._getFileQueueSize(), len(self._filedeque), self._resendcounter]
            packet.append(bytearray(chunk))
            
            self.debug('sending binary chunk for ' + self._filedescriptor.name)
        return packet
        
        
    def _getFileQueueSize(self):
        counter = 0
        for file, size in self._filedeque:
            counter += size
        return counter
        
        
    def checkForFileTimerAction(self):
        self._waitforfile = False
        if not self._filedeque and (not self._filedescriptor or self._filedescriptor.closed):
            self._isBusy = False
            self.info('there is no file to be transmitted')
        else:
            self.info('there are still files to be transmitted')
        
         
    def isBusy(self):
        return self._isBusy
        
        
    def needsWLAN(self):
        return self._isBusy
    
    
    def stopIfNotInDutyCycle(self):
        return False
        
        
    def beaconCleared(self):
        if not self._filedeque and (not self._filedescriptor or self._filedescriptor.closed) and not self._waitforfile:
            self._isBusy = False
    
    
    def stop(self):
        self._isBusy = False
        self._plugStop = True
        if self._waitforfile:
            self._waitforfiletimer.cancel()
        self._notifier.stop()
        self._msgEvent.set()
        self._workEvent.set()
        self._filedeque.clear()
        if self._filedescriptor and not self._filedescriptor.closed:
            os.chmod(self._filedescriptor.name, 0744)
            self._filedescriptor.close()
        self.info('stopped')




class BinaryChangedProcessing(ProcessEvent):
    
    '''
    data/instance attributes:
    _logger
    _binaryPlugin
    '''

    def __init__(self, parent):
        self._logger = logging.getLogger(self.__class__.__name__)
        self._binaryPlugin = parent

    def process_default(self, event):
        self._logger.debug(event.pathname + ' changed')
        
        self._binaryPlugin._filedeque.appendleft([event.pathname, os.path.getsize(event.pathname)])
        self._binaryPlugin._fileEvent.set()