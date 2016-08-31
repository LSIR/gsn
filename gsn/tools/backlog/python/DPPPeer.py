# -*- coding: UTF-8 -*-
from _ctypes import Array
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"

import logging
import time
import Queue
import struct
import array
from threading import Thread, Lock, Event
from ctypes import c_ushort

import DPPTypes
from smbus import SMBus
from BackLogMessage import PLUGIN_MESSAGE_TYPES
from SpecialAPI import Statistics

# Interval a time synchronization message is sent to DPP in seconds.
TIMESYNC_INTERVAL_SEC = 10.0

DEFAULT_BACKLOG = True

'''
 The I2C commands
'''
I2C_CMD_INVALID = 0
I2C_CMD_READ_MSG = 1
I2C_CMD_TIMESYNC = 2

I2C_HEADER_LENGTH = 2
I2C_MSG_HEADER_LENGTH = 14
I2C_MSG_CRC16_LENGTH = 2

SEND_QUEUE_SIZE = 25

class DPPPeerClass(Thread, Statistics):
    '''
    Offers the functionality to communicate with the MSP432 over i2c on DPP.payload|
    '''

    '''
    data/instance attributes:
    _backlogMain
    _i2cBus
    _i2cAddress
    _dppWriter
    _logger 
    _dppPeerStop
    _workEvent
    _i2cPollingIntervalSec
    '''
    
    def __init__(self, parent, i2cDevice, i2cAddress, i2cPollingIntervalSec):
        Thread.__init__(self, name='DPPPeer-Thread')
        self._logger = logging.getLogger(self.__class__.__name__)
        Statistics.__init__(self)
        
        self._i2cLock = Lock()
        self._i2cAddress = i2cAddress
        self._i2cPollingIntervalSec = i2cPollingIntervalSec
        try:
            self._i2cBus = SMBus(i2cDevice)
        except Exception, e:
            raise TypeError('could not initialize I2C bus: %s' % (e,))
        
        self._msgSentCounterId = self.createCounter()
        self._msgReceivedCounterId = self.createCounter()
        self._ackSentCounterId = self.createCounter()
        
        self._dppWriter = DPPWriter(self)
        
        self._timesynctimer = TimeSyncTimer(TIMESYNC_INTERVAL_SEC, self.timesync)
        
        self._crc16 = CRC16()

        self._backlogMain = parent
        self._dppPeerStop = False
        
        self._workEvent = Event()
            
        
    def run(self):
        try:
            self._logger.info('started')
    
            self._dppWriter.start()
            self._timesynctimer.start()
            
            # speed optimizations
            acquire = self._i2cLock.acquire
            read = self._i2cBus.read_i2c_block_data
            release = self._i2cLock.release
            processDPPMsg = self._backlogMain.processDPPMsg
            isEnabledFor = self._logger.isEnabledFor
            
            i2cPacketCnt=0
            i2cLastSeqNo=-1
            msg = ''
            
            while not self._dppPeerStop:
                # read packet from I2C bus
                try:
                    if isEnabledFor(logging.DEBUG):
                        self._logger.debug('rcv...')
                    acquire()
                    i2cPacket = array.array('B', read(self._i2cAddress, I2C_CMD_READ_MSG)).tostring()
                    timestamp = int(time.time()*1000)
                    release()
                except Exception, e:
                    release()
                    if not self._dppPeerStop:
                        self.exception('could not read from I2C bus: %s' % (e,))
                    self._workEvent.wait(self._i2cPollingIntervalSec)
                    continue
                
                (i2cPktBufSize, i2cSekAndPkts) = struct.unpack('<BB',i2cPacket[:I2C_HEADER_LENGTH])
                i2cSeqNo = (i2cSekAndPkts >> 7) & 0x01
                i2cNrOfPkts = i2cSekAndPkts & 0x7f
                
                # is there a message?
                if i2cPktBufSize == 0:
                    self._workEvent.wait(self._i2cPollingIntervalSec)
                    continue
                
                msg = ''.join([msg,i2cPacket[I2C_HEADER_LENGTH:]])
                
                if i2cLastSeqNo != i2cSeqNo:
                    firstTimestamp = timestamp
                    i2cLastSeqNo = i2cSeqNo
                    if i2cPacketCnt != 0:
                        self.exception("incomplete message received over I2C -> drop corrupted content")
                        msg = i2cPacket[I2C_HEADER_LENGTH:]
                        i2cPacketCnt = 0
                
                i2cPacketCnt = i2cPacketCnt + 1
                
                if i2cPacketCnt == i2cNrOfPkts:
                    #TODO: user MSB from type field for generation_time selection
                    msgHeader = struct.unpack('<HBBHQ',msg[:I2C_MSG_HEADER_LENGTH])
                    payload = msg[I2C_MSG_HEADER_LENGTH:I2C_MSG_HEADER_LENGTH+msgHeader[2]]
                    crc16 = struct.unpack('<H', msg[I2C_MSG_HEADER_LENGTH+msgHeader[2]:I2C_MSG_HEADER_LENGTH+msgHeader[2]+I2C_MSG_CRC16_LENGTH])[0]
                    dppMsg = dict(device_id=msgHeader[0], type=msgHeader[1], payload_len=msgHeader[2], seqnr=msgHeader[3], generation_time=msgHeader[4], payload=bytearray(payload))
                    
                    if isEnabledFor(logging.DEBUG):
                        self._logger.debug('rcv (timestamp=%d, device_id=%d, type=%d, payload_len=%d, seqnr=%d, generation_time=%d, crc16=%d)' % (firstTimestamp, dppMsg['device_id'], dppMsg['type'], dppMsg['payload_len'], dppMsg['seqnr'], dppMsg['generation_time'], crc16))
                        self._logger.debug('payload [' + ','.join(str(int(x)) for x in dppMsg['payload']) + ']')
                    
                    if crc16 == self._crc16.calculate(bytearray(msg[:I2C_MSG_HEADER_LENGTH+dppMsg["payload_len"]])):
                        self.counterAction(self._msgReceivedCounterId)
                        # tell BackLogMain to send the packet to the plugins
                        # TODO: implement proper message acknowledge and make sure no messages can get lost
                        if processDPPMsg(firstTimestamp, dppMsg):
                            self.counterAction(self._ackSentCounterId)
                    else:
                        self.exception("CRC16 not correct for message -> drop corrupted content")
                        
                    msg = ''
                    i2cPacketCnt = 0
                
                if --i2cPktBufSize == 0:
                    # wait for the next poll
                    self._workEvent.wait(self._i2cPollingIntervalSec)
                            
            self._timesynctimer.join()
            self._dppWriter.join()
        except Exception, e:
            self.exception(str(e))
    
        self._logger.info('died')
        
            
    def sendDPPMsg(self, dppMsg):
        if not self.isAlive():
            raise Exception("DPPPeer is not alive")
        return self._dppWriter.addMsg(dppMsg)


    def timesync(self):
        try:
            #TODO: set own device_id?
            packet = dict(device_id=0, type=DPPTypes.MSG_TYPE_TIMESYNC, payload_len=0, seqnr=0, generation_time=0, payload=None)
            self._dppWriter.addMsg(packet)
        except Exception, e:
            self.exception(str(e))
            
            
    def getStatus(self):
        '''
        Returns the status of the DPP peer as list:
        
        @return: status of the DPP peer [received message counter,
                                         sent acknowledge counter,
                                         sent message counter]
        '''
        return [self.getCounterValue(self._msgReceivedCounterId), \
                self.getCounterValue(self._ackSentCounterId), \
                self.getCounterValue(self._msgSentCounterId)]


    def exception(self, exception):
        self._backlogMain.incrementExceptionCounter()
        self._logger.exception(str(exception))

        
    def stop(self):
        self._dppPeerStop = True
        self._timesynctimer.stop()
        self._workEvent.set()
        self._dppWriter.stop()
        self._i2cBus.close()
        self._logger.info('stopped')



class DPPWriter(Thread):

    '''
    data/instance attributes:
    _logger
    _dppPeer
    _sendqueue
    _dppWriterStop
    '''

    def __init__(self, parent):
        Thread.__init__(self, name='%s-Thread' % (self.__class__.__name__,))
        self._logger = logging.getLogger(self.__class__.__name__)
        self._dppPeer = parent
        self._sendqueue = Queue.Queue(SEND_QUEUE_SIZE)
        self._dppWriterStop = False


    def run(self):
        self._logger.info('started')
        
        # speed optimizations
        acquire = self._dppPeer._i2cLock.acquire
        write = self._dppPeer._i2cBus.write_i2c_block_data
        release = self._dppPeer._i2cLock.release
        get = self._sendqueue.get
        isEnabledFor = self._logger.isEnabledFor
        
        while not self._dppWriterStop:
            dppMsg = get()
            if self._dppWriterStop:
                break
            
            try:
                #TODO: split packets if bigger than 32bytes
                #TODO: set 2nd i2c Byte correctly
                if dppMsg['type'] == DPPTypes.MSG_TYPE_TIMESYNC:
                    if isEnabledFor(logging.DEBUG):
                        self._logger.debug('write MSG_TYPE_TIMESYNC message (%d,%d,%d,%d,%d)' % (dppMsg['device_id'], dppMsg['type'], dppMsg['payload_len'], dppMsg['seqnr'], int(time.time()*1000000)))
                    packet = array.array('B', struct.pack('<BBHBBHQ', self._sendqueue.qsize(), 0, dppMsg['device_id'], dppMsg['type'], dppMsg['payload_len'], dppMsg['seqnr'], int(time.time()*1000000))).tolist()
                    type = I2C_CMD_TIMESYNC
                else:
                    if isEnabledFor(logging.DEBUG):
                        self._logger.debug('write message with type %d' % dppMsg['type'])
                    packet = array.array('B', struct.pack('<BBHBBHQ%dc' % dppMsg['payload_len'], self._sendqueue.qsize(), 0, dppMsg['device_id'], dppMsg['type'], dppMsg['payload_len'], dppMsg['seqnr'], dppMsg['generation_time'], dppMsg['payload'])).tolist()
                    type = I2C_CMD_READ_MSG
                    
                packet = packet + array.array('B', struct.pack('<H', self._dppPeer._crc16.calculate(bytearray(packet)))).tolist()
                acquire()
                write(self._dppPeer._i2cAddress, type, packet)
                self._dppPeer.counterAction(self._dppPeer._msgSentCounterId)
                if isEnabledFor(logging.DEBUG):
                    self._logger.debug('snd (%d,?,%d)' % (PLUGIN_MESSAGE_TYPES['DPPPlugin'], len(packet)))
            except Exception, e:
                if not self._dppWriterStop:
                    self._logger.warning('could not write message to I2C bus: %s' % (e,))
            finally:
                release()
                self._sendqueue.task_done()
 
        self._logger.info('died')


    def stop(self):
        self._dppWriterStop = True
        try:
            self._sendqueue.put_nowait('end')
        except Queue.Full:
            pass
        except Exception, e:
            self._logger.exception(e)
        self._logger.info('stopped')


    def addMsg(self, packet):
        if not self._dppWriterStop:
            try:
                self._sendqueue.put_nowait(packet)
            except Queue.Full:
                self._logger.warning('DPP send queue is full')
                return False
        return True



class TimeSyncTimer(Thread):
    
    '''
    data/instance attributes:
    _logger
    _action
    _wait
    _timer
    _timeSyncTimerStop
    '''
    
    def __init__(self, interval, action):
        Thread.__init__(self, name='%s-Thread' % (self.__class__.__name__,))
        self._logger = logging.getLogger(self.__class__.__name__)
        self._action = action
        self._wait = interval
        self._timer = Event()
        self._timeSyncTimerStop = False
        
           
    def run(self):
        self._logger.info('started')
        while not self._timeSyncTimerStop:
            self._logger.debug('action')
            self._action()
            self._timer.wait(self._wait)
        self._logger.info('died')
    
    
    def stop(self):
        self._timeSyncTimerStop = True
        self._timer.set()
        self._logger.info('stopped')


class CRC16(object):
    crc16_tab = []

    # The CRC's are computed using polynomials. Here is the most used
    # coefficient for CRC16
    crc16_constant = 0xA001  # 40961

    def __init__(self, modbus_flag=False):
        # initialize the precalculated tables
        if not len(self.crc16_tab):
            self.init_crc16()
        self.mdflag = bool(modbus_flag)

    def calculate(self, input_data=None):
        is_string = isinstance(input_data, str)
        is_bytes = isinstance(input_data, (bytes, bytearray))

        if not is_string and not is_bytes:
            raise Exception("Please provide a string or a byte sequence "
                            "as argument for calculation.")

        crc_value = 0x0000 if not self.mdflag else 0xffff

        for c in input_data:
            d = ord(c) if is_string else c
            tmp = crc_value ^ d
            rotated = crc_value >> 8
            crc_value = rotated ^ self.crc16_tab[(tmp & 0x00ff)]

        return crc_value

    def init_crc16(self):
        """The algorithm uses tables with precalculated values"""
        for i in range(0, 256):
            crc = c_ushort(i).value
            for j in range(0, 8):
                if crc & 0x0001:
                    crc = c_ushort(crc >> 1).value ^ self.crc16_constant
                else:
                    crc = c_ushort(crc >> 1).value
            self.crc16_tab.append(crc)
