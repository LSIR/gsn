# -*- coding: UTF-8 -*-
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"

import struct
import re
import math
from types import *


### internal message types ###

# The acknowledge message type. This message type is used to
# acknowledge data messages.
ACK_MESSAGE_TYPE = 1

# The ping message type. This message type is used to
# ping the GSN server, thus, requesting a PING_ACK_MESSAGE_TYPE.
PING_MESSAGE_TYPE = 2

# The ping acknowledge message type. This message type is used to
# acknowledge a ping request.
PING_ACK_MESSAGE_TYPE = 3

# The message queue full message type. This message type is used
# to control the message flow.
MESSAGE_QUEUE_LIMIT_MESSAGE_TYPE = 4

# The message queue ready message type. This message type is used
# to control the message flow.
MESSAGE_QUEUE_READY_MESSAGE_TYPE = 5

# The Schedule message type.
SCHEDULE_MESSAGE_TYPE = 50

# The Configuration message type.
CONFIG_MESSAGE_TYPE = 51


### plugin message types ###

PLUGIN_MESSAGE_TYPES = dict(
# The backlog status message type. This message type is used to
# send/receive backlog status messages by the 
# plugins.BackLogStatusPlugin.
BackLogStatusPlugin = 10,

# The CoreStation status message type. This message type is used to
# send CoreStation status messages to GSN.
CoreStationStatusPlugin = 11,

# The syslog-ng message type. This message type is used to
# send syslog-ng log messages to GSN.
SyslogNgPlugin = 12,

# The TOS message type. This message type is used to
# send/receive TOS messages by the plugins.TOSPlugin.
TOSPlugin = 20,
TOS1xPlugin = 21,

# The binary message type. This message type is used to
# send any binary data to GSN.
BinaryPlugin = 30,

# The Vaisala weather sensor (WXT520) message type.
VaisalaWXT520Plugin = 40,

# The GPS message type.
GPSPlugin = 60,

# GPSNAVPlugin
GPSPluginNAV = 61,

#Power Manager
PowerManagerPlugin = 70,

# MiCS-OZ-47 Ozone Sensor 
OZ47Plugin1 = 80,
OZ47Plugin2 = 80,

# ECVQ-EK3 Gas Sensor 
ECVQEK3Plugin = 81,

# STEVAL Accelerometer 
STEVALPlugin = 82,

# STEVAL Accelerometer 
AlphasensePlugin = 83,

# MOTION DETECTION 
MotionDetectionPlugin = 84,

# MINIDISC 
MinidiscPlugin = 85,

# CONO2
CONO2Plugin = 87,

# Wifi
WifiPlugin = 88,

# Gsm
GsmPlugin = 89,

# CamZillaPlugin
CamZillaPlugin = 90,

# Sampler 6712
Sampler6712Plugin = 100,

# DPP Messages
DPPPlugin = 110,

# DPP Firmware
DPPFirmwarePlugin = 111,

TestPlugin = 126
)

# The maximum supported payload size (2^20). This is due to
# the sending mechanism. A message consists of the type field (1 byte),
# the timestamp (8 bytes) and the payload (max. 1MB).
MAX_PAYLOAD_SIZE = 1048576


    
STRUCTMATCH = re.compile('^[cbB?hHiqdfsX]*$')


class BackLogMessageClass:
    '''
    Defines a backlog message, used by the protocol communicated 
    between the Python backlog program running at the deployment and
    the DeploymentClient on side of the GSN server. It offers the
    functionality to access the message format.
    
    The binary message has the following format:
        |       header       |            payload             |
        |  type  | timestamp |                                |
        | 1 byte |  8 bytes  | maximum MAX_PAYLOAD_SIZE bytes |
    
    There are three predefined message types:
        ACK_MESSAGE_TYPE = 1: The acknowledge message type.
            This message type is used to acknowledge data messages.
        PING_MESSAGE_TYPE = 2: The ping message type. This message
            type is used to ping a deployment, thus, requesting a
            PING_ACK_MESSAGE_TYPE.
        PING_ACK_MESSAGE_TYPE = 3: The ping acknowledge message type.
            This message type is used to acknowledge a ping request.
    
    Each plugin must specify a unique message type, which has to be named on module level
    '''

    '''
    data/instance attributes:
    _type
    _timestamp
    _header
    _payload
    '''

    def __init__(self, type=0, timestamp=0, payload=None):
        '''
        Class constructor specifying the message type, the timestamp
        and the payload.

        @param type: message type
        @param timestamp: timestamp in milliseconds
        @param payload: payload of the message as a byte array or a list.
                         Should not be bigger than MAX_PAYLOAD_SIZE.

        @raise TypeError if firstArg can not be interpreted as a BackLog
            message or if the payload length exceeds MAX_PAYLOAD_SIZE
                 
        @raise ValueError: if something is wrong with the payload.
        '''
        
        if type < 0 or type > 255:
            raise TypeError('BackLog message type has to be in range 0 to 255')
        if payload is not None and len(payload) > MAX_PAYLOAD_SIZE:
            raise TypeError('BackLog message payload has to be smaller then MAX_PAYLOAD_SIZE')
        self._type = type
        self._timestamp = timestamp
        try:
            self._header = struct.pack('<Bq', type, timestamp)
        except Exception, e:
            raise TypeError('cannot pack message: %s' % (e,))

        if payload == None:
            self._payload = None
        else:
            if isinstance(payload, bytearray):
                self._payload = self._packList([payload])
            else:
                self._payload = self._packList(payload)
        
        
    def __lt__(self, other):
        return self._timestamp < other._timestamp
    
    
    def getMessage(self):
        if self._payload:
            return self._header + str(self._payload)
        else:
            return self._header
    
    
    def setMessage(self, bytes):
        '''
        Sets the message from byte array.
        '''
        
        try:
            self._type = struct.unpack('<B', bytes[0])[0]
            if self._type < 0 or self._type > 255:
                raise TypeError('BackLog message type has to be in range 0 to 255')
            self._timestamp = struct.unpack('<q', bytes[1:9])[0]
        except Exception, e:
            raise TypeError('cannot unpack message: %s' % (e,))
       
        self._header = bytes[0:9]
        
        if len(bytes) > 9:
            self._payload = bytes[9:]
        else:
            self._payload = None
    
    
    def getType(self):
        '''
        Get the message type.

        @return: the type of the message
        '''
        return self._type

    
    def getTimestamp(self):
        '''
        Get the timestamp of the message.

        @return: the timestamp of the message
        '''
        return self._timestamp

    
    def getData(self):
        '''
        Get the data of the message as a list.

        @return: the data as a list
        '''
        if self._payload == None:
            return []
        else:
            return self._unpackPayload(self._payload)

    
    def getSize(self):
        '''
        Get size of the payload in bytes

        @return: the size of the payload in bytes
        '''
        if self._payload == None:
            return 0
        else:
            return len(self._payload)
    
    
    def _unpackPayload(self, payload):
        ret = []
        append = ret.append
        unpack_from = struct.unpack_from
        calcsize = struct.calcsize
        
        fmt_len = unpack_from('<i', payload)[0]
        fmt = unpack_from('<%ds' % fmt_len, payload, 4)[0]
        plength = calcsize('<i%ds' % fmt_len)
        
        for c in fmt:
            if c == '0':
                append(None)
            elif c == 'X':
                l = unpack_from('<i', payload, plength)[0]
                append(payload[plength+4:plength+4+l])
                plength += 4+l
            elif c == 's':
                l = unpack_from('<i', payload, plength)[0]
                append(unpack_from('<%ds' % l, payload, plength+4)[0])
                plength += calcsize('<i%ds' % l)
            else:
                append(unpack_from('<'+c, payload, plength)[0])
                plength += calcsize(c)
        return ret

    
    def _packList(self, values):
        pack = struct.pack
        format = ''
        data = ''
        for index, val in enumerate(values):
            t = type(val)
            if val == None:
                format += '0'
            else:
                if t == bool:
                    format += '?'
                    if val:
                        data += pack('<b', 1)
                    else:
                        data += pack('<b', 0)
                elif t == int:
                    if val < 127 and val > -128:
                        format += 'b'
                        data += pack('<b', val)
                    elif val < 32767 and val > -32768:
                        format += 'h'
                        data += pack('<h', val)
                    elif val < 2147483647 and val > -2147483648:
                        format += 'i'
                        data += pack('<i', val)
                    elif val < 9223372036854775807 and val > -9223372036854775808:
                        format += 'q'
                        data += pack('<q', val)
                    else:
                        raise ValueError('the passed integer value is too big to be transfered using 8 bytes')
                elif t == long:
                    if val < 127 and val > -128:
                        format += 'b'
                        data += pack('<b', val)
                    elif val < 32767 and val > -32768:
                        format += 'h'
                        data += pack('<h', val)
                    elif val < 2147483647 and val > -2147483648:
                        format += 'i'
                        data += pack('<i', val)
                    elif val < 9223372036854775807 and val > -9223372036854775808:
                        format += 'q'
                        data += pack('<q', val)
                    else:
                        raise ValueError('the passed integer value is too big to be transfered using 8 bytes')
                elif t == float:
                    format += 'd'
                    data += pack('<d', val)
                elif t == str:
                    format += 's'
                    data += pack('<i%ds' % len(val), len(val), val)
                elif t == bytearray:
                    format += 'X'
                    data += pack('<i', len(val)) + val
                else:
                    raise ValueError('value at index %d in passed list is of unsupported %s' % (index, t))
                    
        return pack('<i', len(format)) + pack('<%ds' % len(format), format) + data
    