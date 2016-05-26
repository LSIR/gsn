# -*- coding: UTF-8 -*-
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"

import struct
import os
import logging
import zlib
import Queue
from collections import deque
from threading import Thread, Timer, Event, Lock
from pyinotify import WatchManager, ThreadedNotifier, EventsCodes, ProcessEvent

import BackLogMessage
from AbstractPlugin import AbstractPluginClass

DEFAULT_DATE_TIME_FORMATE = 'yyyy-MM-dd'

CHUNK_RESEND_TIMEOUT_DEFAULT = 30

CHUNK_SIZE = 64000

ACK_PACKET = 0
START_PACKET = 1
INIT_PACKET = 2
RESEND_PACKET = 3
CHUNK_PACKET = 4
CRC_PACKET = 5

MESSAGE_NUMBER_MOD = 0xFFFFFFF

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
    _binaryWriter
    _notifier
    _plugStop
    _interPlugStop
    _filedequelock
    _filedeque
    _msgqueue
    _isBusy
    _rootdir
    _filedescriptor
    _watches
    _packetCRC
    _inrun
    _readyfornewbinary
    
    TODO: remove CRC functionality after long time testing. It is not necessary over TCP.
    '''
    
    def __init__(self, parent, options):
        AbstractPluginClass.__init__(self, parent, options, False)
        
        self._rootdir = self.getOptionValue('rootdir')
        
        watches = self.getOptionValues('watch')
            
        chunk_resend_timeout = self.getOptionValue('chunk_resend_timeout')
        if chunk_resend_timeout:
            self._chunk_resend_timeout = int(chunk_resend_timeout)
            self.info('chunk_resend_timeout: %d seconds' % (self._chunk_resend_timeout,))
        else:
            self._chunk_resend_timeout = CHUNK_RESEND_TIMEOUT_DEFAULT
            self.info('no chunk_resend_timeout set. Using default: %d seconds' % (self._chunk_resend_timeout,))

        if self._rootdir is None:
            raise TypeError('no rootdir specified')
        
        if not self._rootdir.endswith('/'):
            self._rootdir += '/'
        
        if not os.path.isdir(self._rootdir):
            self.warning('rootdir >%s< is not a directory -> creating it' % (self._rootdir,))
            os.makedirs(self._rootdir)

        if not watches:
            watches.append('.,filesystem,,')

        self._readyfornewbinary = False
        wm = WatchManager()
        self._notifier = ThreadedNotifier(wm, BinaryChangedProcessing(self))
        
        self._binaryWriter = BinaryWriter(self)
        self._filedeque = deque()
        self._filedequelock = Lock()
        self._msgqueue = Queue.Queue()
        
        self._watches = []
        filetime = []
        for watch in watches:
            w = watch.split(',')
            for index, entry in enumerate(w):
                w[index] = entry.strip()
        
            if not w[0].endswith('/'):
                w[0] += '/'
                
            if len(w) != 4:
                raise TypeError('watch >%s< in the configuration file is not well formatted' % (watch,))
            else:
                w[1] = w[1].lower()
                if w[1] == 'database':
                    w[1] = 1
                elif w[1] == 'filesystem':
                    w[1] = 0
                else:
                    raise TypeError('the second part of watch >%s< in the configuration file has to be database or filesystem' %( watch,))
                    
                if not w[2]:
                    w[2] = self.getDeviceId()
                else:
                    if not w[2].isdigit():
                        raise TypeError('the device id in watch >%s< has to be a number' % (watch,))
                    else:
                        w[2] = int(w[2])
                        
                if not w[3]:
                    w[3] = DEFAULT_DATE_TIME_FORMATE
                else:
                    if len(w[3]) > 255:
                        raise TypeError('the date time format in watch >%s< is longer than 255 characters' % (watch,))
            
            dir = w[0]
            pathname = os.path.join(self._rootdir, dir)
            pathname = os.path.normpath(pathname)
            if not os.path.isdir(pathname):
                os.makedirs(pathname)
                
            # tell the watch manager which folders to watch for newly written files
            wm.add_watch(pathname, EventsCodes.FLAG_COLLECTIONS['OP_FLAGS']['IN_CLOSE_WRITE'] | EventsCodes.FLAG_COLLECTIONS['OP_FLAGS']['IN_MOVED_TO'])
            self.info('enable close-after-write notification for path %s' % (pathname,))
            
            files = os.listdir(pathname)
            
            self._watches.append(w)
            
            # check the path for existing files which have to be sent
            for filename in files:
                f = os.path.join(pathname, filename)
                if os.path.isfile(f):
                    time = os.stat(f).st_mtime
                    filetime.append((time, f))
           
        for path, storage, device_id, time_format in self._watches:
            self.debug('watch: %s - %d - %d - %s' % (path, storage, device_id, time_format))
        
        # sort the existing files by time
        filetime.sort()
        # and put it into the fifo
        for file in filetime:
            self._filedequelock.acquire()
            self._filedeque.appendleft([file[1], os.path.getsize(file[1])])
            self._filedequelock.release()
            self.debug('putting existing file into FIFO: %s' % (file[1],))
        if filetime:
            self.info('files to be transmitted: %d' % (len(filetime),))
            
        self._isBusy = False
        if self._filedeque and self.getMaxRuntime():
            self._isBusy = True
                
        self._filedescriptor = None
        self._inrun = Event()
        self._plugStop = False
        self._interPlugStop = False
    
    
    def msgReceived(self, data):
        self.debug('message received')
        if not self._interPlugStop:
            self._msgqueue.put(data)
        
    
    def connectionToGSNestablished(self):
        self.debug('connection established')
        if not self._interPlugStop:
            self._restartSending()
        
    
    def connectionToGSNlost(self):
        self.debug('connection lost')
        self._readyfornewbinary = False
        if not self._inrun.isSet():
            self._stopSending()
        
        
    def recvInterPluginCommand(self, command):
        if command == 'stop':
            if not self._interPlugStop:
                self.info('inter plugin stop command received')
                self._interPlugStop = True
                if not self._inrun.isSet():
                    self._stopSending()
            else:
                self.warning('inter plugin stop command received but Plugin already stopped')
        elif command == 'start':
            if self._interPlugStop:
                self.info('inter plugin start command received')
                self._interPlugStop = False
                if self.isGSNConnected():
                    self._restartSending()
            else:
                self.warning('inter plugin start command received but Plugin already running')
        else:
            self.error('inter plugin command unknown: %s' % (str(command)))
        
    
    def run(self):
        self.name = 'BinaryPlugin-Thread'
        self.info('started')
        
        self._binaryWriter.start()
        
        # start pyinotify to watch the specified folders
        try:
            self._notifier.start()
        except Exception, e:
            if not self._stopped:
                self.exception(e)
                
        while not self._plugStop:
            try:
                data = self._msgqueue.get()
                self._inrun.set()
                if self._plugStop:
                    try:
                        self._msgqueue.task_done()
                    except ValueError, e:
                        self.exception(e)
                    break
                
                if not self._interPlugStop and self.isGSNConnected():
                    # the first entry of the data defines its packet number
                    pktNr = data[0]
                    # the second entry of the data defines its type
                    pktType = data[1]
                    
                    try:
                        if pktNr == self._binaryWriter.getSentMsgNr():
                            self._binaryWriter.stopSending()
                            # if the type is INIT_PACKET we have to send a new file
                            if pktType == INIT_PACKET:
                                self.debug('new binary request received')
                                self._getInitialBinaryPacket()
                            # if the type is RESEND_PACKET we have to resend a part of a file...
                            elif pktType == RESEND_PACKET:
                                self.debug('binary retransmission request received')
                                if self._reopenFile(data[2], struct.unpack('i', struct.pack('I', data[3]))[0], data[4]):
                                    self._getNextChunk()
                                else:
                                    self._getInitialBinaryPacket()
                            elif pktType == ACK_PACKET:
                                ackType = data[2]
                                self.debug('acknowledge received for packet type %d' % (ackType,))
                                    
                                if ackType == INIT_PACKET:
                                    self.debug('acknowledge received for init packet')
                                    self._getNextChunk()
                                elif ackType == CHUNK_PACKET:
                                    self.debug('acknowledge for packet number >%d< received' % (pktNr,))
                                    self._getNextChunk()
                                elif ackType == CRC_PACKET:
                                    filename = self._filedescriptor.name
                                    if os.path.isfile(filename):
                                        os.chmod(filename, 0744)
                                        self._filedescriptor.close()
                                        # crc has been accepted by GSN
                                        self.debug('crc has been accepted for %s => remove it' % (filename,))
                                        # remove it from disk
                                        os.remove(filename)
                                    else:
                                        self.debug('crc acknowledge file does not exist')
                                    self._getInitialBinaryPacket()
                                else:
                                    self.error('unexpected acknowledge received')
                        elif pktNr == self._binaryWriter.getSentMsgNr()-1 or (pktNr == MESSAGE_NUMBER_MOD-1 and self._binaryWriter.getSentMsgNr() == 0):
                            self.debug('packet number already received')
                        else:
                            self.error('packet number out of order (recv=%d, sent=%d)' % (pktNr, self._binaryWriter.getSentMsgNr()))
                    except Exception, e:
                        self._backlogMain.incrementExceptionCounter()
                        self._logger.exception(str(e))
                        if self._filedescriptor and not self._filedescriptor.closed:
                            self._filedescriptor.close()
                        self._getInitialBinaryPacket()
                        
                try:
                    self._msgqueue.task_done()
                except ValueError, e:
                    self.exception(e)
                    
                self._inrun.clear()
                    
                if self._interPlugStop or not self.isGSNConnected():
                    self._stopSending()
            except Exception, e:
                self._backlogMain.incrementExceptionCounter()
                self._logger.exception(str(e))
                
        if self._filedescriptor and not self._filedescriptor.closed:
            os.chmod(self._filedescriptor.name, 0744)
            self._filedescriptor.close()
            
        self._logger.info('died')
        
        
        
    def _stopSending(self):
        self._isBusy = False
        self._binaryWriter.pause()
        if self._filedescriptor:
            if os.path.exists(self._filedescriptor.name):
                self._filedequelock.acquire()
                self._filedeque.append([self._filedescriptor.name, os.path.getsize(self._filedescriptor.name)])
                self._filedequelock.release()
            self._filedescriptor.close()
        self._emptyQueue()
        
        
    def _restartSending(self):
        if (self._filedescriptor and not self._filedescriptor.closed) or self._filedeque:
            self._isBusy = True
        self._binaryWriter.restart()
        self._binaryWriter.resetResendCounter()
        self._binaryWriter.sendMessage([START_PACKET])


    def _emptyQueue(self):
        while not self._msgqueue.empty():
            try:
                self._msgqueue.get_nowait()
            except Queue.Empty:
                self._logger.warning('message queue is empty')
                break
            try:
                self._msgqueue.task_done()
            except ValueError, e:
                self.exception(e)
                
                
    def _getInitialBinaryPacket(self):
        while not self._plugStop:
            try:
                if self._filedescriptor and not self._filedescriptor.closed:
                    self._filedescriptor.seek(0)
                    filename = self._filedescriptor.name
                    if not os.path.isfile(filename):
                        self._filedescriptor.close()
                        continue
                else:
                    try:
                        # get the next file to send out of the fifo
                        fileobj = self._filedeque.pop()
                    except IndexError:
                        self.debug('file FIFO is empty waiting for next file to arrive')
                        self._readyfornewbinary = True
                        self._isBusy = False
                        return
                        
                    filename = fileobj[0]
                
                    if os.path.isfile(filename):
                        # open the file
                        self._filedescriptor = open(filename, 'rb')
                        os.chmod(filename, 0444)
                    else:
                        # if the file does not exist we are continuing in the fifo
                        continue
                
                # get the size of the file
                filelen = os.path.getsize(filename)
                
                if filelen == 0:
                    # if the file is empty we ignore it and continue in the fifo
                    self.debug('ignore empty file: %s' % (filename,))
                    os.chmod(filename, 0744)
                    self._filedescriptor.close()
                    os.remove(filename)
                    continue
                
                filenamenoprefix = filename.replace(self._rootdir, '')
                self.debug('filename without prefix: %s' % (filenamenoprefix,))
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
                    self.error('no watch specified for %s (this is very strange!!!) -> close file' % (filename,))
                    os.chmod(filename, 0744)
                    self._filedescriptor.close()
                    continue
    
                break
            except Exception, e:
                self._backlogMain.incrementExceptionCounter()
                self._logger.exception(str(e))
                if self._filedescriptor and not self._filedescriptor.closed:
                    self._filedescriptor.close()
                
        if not self._plugStop:
            self._binaryWriter.resetResendCounter()
                
            packet = [INIT_PACKET, self._getFileQueueSize(), len(self._filedeque), watch[2], long(os.stat(filename).st_mtime * 1000), filelen, watch[1]]
            packet.append(filenamenoprefix)
            packet.append(watch[3])
            
            self.debug('sending initial binary packet for %s from watch directory >%s<, storage type: >%d<, device id >%d and time date format >%s<' % (self._filedescriptor.name, watch[0], watch[1], watch[2], watch[3]))
        
            self._packetCRC = None
            self._emptyQueue()
            
            self._binaryWriter.sendMessage(packet)
        
        
    def _reopenFile(self, downloaded, gsnCRC, filenamenoprefix):
        filename = os.path.join(self._rootdir, filenamenoprefix)
        self.debug('downloaded size: %d' % (downloaded,))
        self.debug('crc at GSN: %d' % (gsnCRC,))
        self.debug('file: %s' % (filename,))
        
        try:
            if downloaded > 0:
                try:
                    self._filedeque.remove([filename, os.path.getsize(filename)])
                except:
                    pass
                else:
                    self.debug('copy >%s< removed from file queue' % (filename,))
                # open the specified file
                self._filedescriptor = open(filename, 'rb')
                os.chmod(filename, 0444)
                
                # recalculate the crc
                sizecalculated = 0
                self._packetCRC = None
                while sizecalculated != downloaded:
                    if downloaded - sizecalculated > 4096:
                        part = self._filedescriptor.read(4096)
                    else:
                        part = self._filedescriptor.read(downloaded - sizecalculated)
                        
                    if self._packetCRC:
                        self._packetCRC = zlib.crc32(part, self._packetCRC)
                    else:
                        self._packetCRC = zlib.crc32(part)
                    sizecalculated += len(part)
                        
                    if part == '':
                        self.warning('end of file reached while calculating CRC')
                        break
                    
                if self._packetCRC != gsnCRC:
                    self.warning('crc received from gsn >%d< does not match local one >%s< -> resend complete binary' % (gsnCRC, self._packetCRC))
                    os.chmod(filename, 0744)
                    self._filedeque.append([filename, os.path.getsize(filename)])
                    self._filedescriptor.close()
                    return False
                else:
                    self._binaryWriter.resetResendCounter()
                    self.debug('crc received from gsn matches local one -> resend following part of binary')
                    return True
            else:
                try:
                    # resend the whole binary
                    self._filedeque.append([filename, os.path.getsize(filename)])
                except OSError, e:
                    self.warning(str(e))
                return False
        except IOError, e:
            self.error(e)
            return False
        
        
    def _getNextChunk(self):
        # read the next chunk out of the opened file
        chunk = self._filedescriptor.read(CHUNK_SIZE)
        
        if self._packetCRC:
            self._packetCRC = zlib.crc32(chunk, self._packetCRC)
        else:
            self._packetCRC = zlib.crc32(chunk)
        
        if not chunk:
            # so we have reached the end of the file...
            self.debug('binary completely sent')
            
            # create the crc packet [type, crc]
            packet = [CRC_PACKET, self._getFileQueueSize(), len(self._filedeque), struct.unpack('I', struct.pack('i', self._packetCRC))[0]]
            
            # send it
            self.debug('sending crc: %d' % (self._packetCRC,))
        else:
            # create the packet [type, chunk number (4bytes)]
            packet = [CHUNK_PACKET, self._getFileQueueSize(), len(self._filedeque)]
            packet.append(bytearray(chunk))
            
            self.debug('sending binary chunk for %s' % (self._filedescriptor.name,))
            
        self._binaryWriter.sendMessage(packet)
        
        
    def _getFileQueueSize(self):
        counter = 0
        self._filedequelock.acquire()
        for file, size in self._filedeque:
            counter += size
        self._filedequelock.release()
        return counter
        
         
    def isBusy(self):
        return self._isBusy
        
        
    def needsWLAN(self):
        return self._isBusy
        
        
    def beaconCleared(self):
        if self._readyfornewbinary:
            self._isBusy = False
    
    
    def stop(self):
        self._isBusy = False
        self._plugStop = True
        self._notifier.stop()
        self._binaryWriter.stop()
        self._filedequelock.acquire()
        self._filedeque.clear()
        self._filedequelock.release()
        self._msgqueue.put('end')
        self.info('stopped')



class BinaryWriter(Thread):

    '''
    data/instance attributes:
    _logger
    _binaryPluginClass
    _sendqueue
    _messageNr
    _resendcounter
    _stopsending
    _paused
    _binaryWriterStop
    '''

    def __init__(self, parent):
        Thread.__init__(self, name='BinaryWriter-Thread')
        self._logger = logging.getLogger(self.__class__.__name__)
        self._binaryPluginClass = parent
        self._sendqueue = Queue.Queue(2)
        self._stopsending = Event()
        self._messageNr = -1
        self._resendcounter = 0
        self._paused = False
        self._binaryWriterStop = False


    def run(self):
        self._logger.info('started')
        
        while not self._binaryWriterStop:
            msg = self._sendqueue.get()
            self._stopsending.clear()
            if not self._paused:
                self._messageNr = (self._messageNr+1)%MESSAGE_NUMBER_MOD
                
                if not self._binaryWriterStop:
                    self._binaryPluginClass.processMsg(self._binaryPluginClass.getTimeStamp(), [self._messageNr, self._resendcounter] + msg)
                    while not self._stopsending.isSet() and self._binaryPluginClass.isGSNConnected() and not self._binaryWriterStop:
                        self._stopsending.wait(self._binaryPluginClass._chunk_resend_timeout)
                        if not self._stopsending.isSet() and self._binaryPluginClass.isGSNConnected() and not self._binaryWriterStop:
                            self._logger.debug('resend message')
                            self._resendcounter += 1
                            self._binaryPluginClass.processMsg(self._binaryPluginClass.getTimeStamp(), [self._messageNr, self._resendcounter] + msg)
            try:
                self._sendqueue.task_done()
            except Exception, e:
                self._exception(e)
            
        self._logger.info('died')
        
        
    def sendMessage(self, msg):
        if self._binaryPluginClass.isGSNConnected() and not self._binaryWriterStop and not self._paused:
            try:
                self._sendqueue.put_nowait(msg)
            except Queue.Full:
                self._exception('send queue is full')
                
                
    def stopSending(self):
        self._stopsending.set()
        
        
    def pause(self):
        self._paused = True
        self._stopsending.set()
        
    def restart(self):
        self._paused = False
        
        
    def resetResendCounter(self):
        self._resendcounter = 0
        
    def getSentMsgNr(self):
        return self._messageNr


    def stop(self):
        self._binaryWriterStop = True
        self._paused = False
        self._stopsending.set()
        try:
            self._sendqueue.put('stop')
        except:
            pass
        self._logger.info('stopped')


    def _exception(self, error):
        self._binaryPluginClass._backlogMain.incrementExceptionCounter()
        self._logger.exception(str(error))




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
        try:
            if self._logger.isEnabledFor(logging.DEBUG):
                self._logger.debug('%s changed' % (event.pathname,))
    
            self._binaryPlugin._filedequelock.acquire()
            self._binaryPlugin._filedeque.appendleft([event.pathname, os.path.getsize(event.pathname)])
            self._binaryPlugin._filedequelock.release()
            if self._binaryPlugin._readyfornewbinary:
                self._binaryPlugin._readyfornewbinary = False
                self._binaryPlugin._isBusy = True
                self._binaryPlugin._getInitialBinaryPacket()
        except Exception, e:
            self._binaryPlugin._filedequelock.release()
            self._binaryPlugin._backlogMain.incrementExceptionCounter()
            self._logger.exception(str(e))