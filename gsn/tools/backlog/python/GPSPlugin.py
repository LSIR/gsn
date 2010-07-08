
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import struct
import time
import serial
from threading import Event

import BackLogMessage
from AbstractPlugin import AbstractPluginClass

DEFAULT_BACKLOG = True

class GPSPluginClass(AbstractPluginClass):
    '''
    Reads raw GPS messages from a u-blox device and sends them to GSN
    '''
    
    def __init__(self, parent, config):
        AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG)

        # initialize GPS message headers
        self._gpsHeader = struct.pack('2B', 0xB5, 0x62)
        self._rawMessageId = struct.pack('2B', 0x02, 0x10)
        self._rateMessageId = struct.pack('2B', 0x06, 0x08)
        # TODO Insert new message type headers here
       
        self._runEv = Event()

        # initialize measurment parameters
        self._device = serial.Serial(self.getOptionValue('gps_device'))
        self._device.timeout = 10;
        self._interval = int(self.getOptionValue('poll_interval')) # The measurement interval in seconds
        self._measTime = int(self.getOptionValue('measurement_time')) # The measurement time in seconds

        # configure the receiver to the correct measurement rate
        self._sendGpsMessage(self._rateMessageId, struct.pack('3H', self._interval*1000, 1, 1))
        
        self._stopped = False

    def run(self):

        self.info('started')

        # Prepare for precise timing
        startTime = time.time()
        measurementNo = 1

        # scheduling my death...
        self._endTime = startTime + self._measTime

        while time.time() <= self._endTime and not self._stopped:
            # Wait for next measurement time
            self._runEv.wait((startTime + measurementNo * self._interval) - time.time())
            if self._runEv.isSet():
                break
            
            # Poll that GPS RAW message
            rawMsg = self._pollGpsMessage(self._rawMessageId)

            # Parse the RAW message
            payload = rawMsg[2]
            payloadHeader = struct.unpack('ih2B', payload[0:8])
         
            self.debug('GPS Time: '+str(payloadHeader[0])+":"+str(payloadHeader[1]))

            for i in range(0, payloadHeader[2]):
                startIndex = 8+i*24

                # We have to parse the meas-quality and signal strength fields because java does not support the corresponding data types
                tmpData = struct.unpack('2b', payload[(startIndex+21):(startIndex+23)])                
                dataPackage = payload[0:6] + payload[startIndex:(startIndex+21)] + struct.pack('2H', tmpData[0], tmpData[1]) + payload[(startIndex+23)]

                # Send the message to the BackLog-System
                self.processMsg(self.getTimeStamp(), dataPackage, self._priority, self._backlog)

            measurementNo += 1

        # die...
        self.stop()


    def _pollGpsMessage(self, msgId):
        '''
        Poll a UBX message which means sending a message of this type with empty payload and then receive a message of the same type
        ''' 
        self._sendGpsMessage(msgId)
        return self._readGpsMessage(msgId) 


    def _readGpsMessage(self, msgId):
        '''
        Receive a UBX Command from the GPS Device
        '''
        while not self._stopped:
            # Wait for the Header
            a = 'a'
            while not self._runEv.isSet() and len(a)==1 and ord(a) != 0xB5:
                a = self._device.read(1)

            a = self._device.read(1)
            if not self._runEv.isSet() and len(a)==1 and ord(a) == 0x62:
                # Got a message! :)
                recMsgId = self._device.read(2)

                # Is it the right Msg Type?
                if(recMsgId == msgId):
                    header = struct.unpack('2B', recMsgId)
                    msgClass = header[0]
                    msgId = header[1]
                    rawPayloadLength = self._device.read(2)
                    payloadLength = struct.unpack('H', rawPayloadLength)[0]

                    payload = self._device.read(payloadLength)

                    submitChecksum = struct.unpack('2B', self._device.read(2))

                    calculatedChecksum = self._calculateChecksum(recMsgId + rawPayloadLength + payload)
                
                    if submitChecksum == calculatedChecksum:
                        return (msgClass, msgId, payload)
                    else:
                        raise Exception('The submitted checksum did not match the expected one')


    def _sendGpsMessage(self, msgId, payload=None):
        '''
        Send a UBX Command to the GPS Device
        '''

        if(payload != None):
            msg = msgId + struct.pack('H', len(payload)) + payload
        else:
            msg = msgId + struct.pack('H', 0)

        checksums = self._calculateChecksum(msg)
    
        outBuffer = self._gpsHeader + msg + struct.pack('B', checksums[0]) + struct.pack('B', checksums[1])

        self._device.write(outBuffer)


    def _calculateChecksum(self, msg):
        '''
        Calculate the UBX Checksum for a packet
        '''
        ck_a = 0
        ck_b = 0

        for i in msg:
            ck_a += ord(i)
            ck_b += ck_a

        return (ck_a & 0xFF, ck_b & 0xFF)
    
    def isBusy(self):
        return False

    def getMsgType(self):
        return BackLogMessage.GPS_MESSAGE_TYPE

    def stop(self):
        self._stopped = True
        self._runEv.set()