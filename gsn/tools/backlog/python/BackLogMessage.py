# -*- coding: UTF-8 -*-
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

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


### plugin message types ###

# The backlog status message type. This message type is used to
# send/receive backlog status messages by the 
# plugins.BackLogStatusPlugin.
BACKLOG_STATUS_MESSAGE_TYPE = 10

# The CoreStation status message type. This message type is used to
# send CoreStation status messages to GSN.
CORESTATION_STATUS_MESSAGE_TYPE = 11

# The TOS message type. This message type is used to
# send/receive TOS messages by the plugins.TOSPlugin.
TOS_MESSAGE_TYPE = 20
TOS1x_MESSAGE_TYPE = 21

# The binary message type. This message type is used to
# send any binary data to GSN.
BINARY_MESSAGE_TYPE = 30

# The Vaisala weather sensor (WXT520) message type.
VAISALA_WXT520_MESSAGE_TYPE = 40

# The Schedule message type.
SCHEDULE_MESSAGE_TYPE = 50

# The GPS message type.
GPS_MESSAGE_TYPE = 60

# The maximum supported payload size (2^32-9bytes). This is due to
# the sending mechanism. A sent message is defined by preceding
# four bytes containing the message size. A message consists of
# the type field (1 byte), the timestamp (8 bytes) and the payload
# (max. ~4GB).
MAX_PAYLOAD_SIZE = 4294967287


    
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
        
        self._type = type
        self._timestamp = timestamp
        try:
            self._header = struct.pack('<Bq', type, timestamp)
        except Exception, e:
            raise TypeError('cannot pack message: ' + e.__str__())

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
            self._type = int(struct.unpack('<B', bytes[0])[0])
            self._timestamp = int(struct.unpack('<q', bytes[1:9])[0])
        except Exception, e:
            raise TypeError('cannot unpack message: ' + e.__str__())
       
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
        fmt_len = struct.unpack_from('<i', payload)[0]
        fmt = struct.unpack_from('<%ds' % fmt_len, payload, 4)[0]
        plength = struct.calcsize('<i%ds' % fmt_len)
        
        ret = []
        for c in fmt:
            if c == '0':
                ret.append(None)
            elif c == 'X':
                l = struct.unpack_from('<i', payload, plength)[0]
                ret.append(payload[plength+4:plength+4+l])
                plength += 4+l
            elif c == 's':
                l = struct.unpack_from('<i', payload, plength)[0]
                ret.append(struct.unpack_from('<%ds' % l, payload, plength+4)[0])
                plength += struct.calcsize('<i%ds' % l)
            else:
                ret.append(struct.unpack_from('<'+c, payload, plength)[0])
                plength += struct.calcsize(c)
        return ret

    
    def _packList(self, values):
        format = ''
        data = ''
        bitstr = ''
        bitlist = []
        for index, val in enumerate(values):
            if val == None:
                format += '0'
            else:
                if type(val) == bool:
                    format += '?'
                    if val:
                        data += struct.pack('<b', 1)
                    else:
                        data += struct.pack('<b', 0)
                elif type(val) == int:
                    if val < 127 and val > -128:
                        format += 'b'
                        data += struct.pack('<b', val)
                    elif val < 32767 and val > -32768:
                        format += 'h'
                        data += struct.pack('<h', val)
                    elif val < 2147483647 and val > -2147483648:
                        format += 'i'
                        data += struct.pack('<i', val)
                    elif val < 9223372036854775807 and val > -9223372036854775808:
                        format += 'q'
                        data += struct.pack('<q', val)
                    else:
                        raise ValueError('the passed integer value is too big to be transfered using 8 bytes')
                elif type(val) == long:
                    if val < 127 and val > -128:
                        format += 'b'
                        data += struct.pack('<b', val)
                    elif val < 32767 and val > -32768:
                        format += 'h'
                        data += struct.pack('<h', val)
                    elif val < 2147483647 and val > -2147483648:
                        format += 'i'
                        data += struct.pack('<i', val)
                    elif val < 9223372036854775807 and val > -9223372036854775808:
                        format += 'q'
                        data += struct.pack('<q', val)
                    else:
                        raise ValueError('the passed integer value is too big to be transfered using 8 bytes')
                elif type(val) == float:
                    format += 'd'
                    data += struct.pack('<d', val)
                elif type(val) == str:
                    format += 's'
                    data += struct.pack('<i', len(val))
                    data += struct.pack('<%ds' % len(val), val)
                elif type(val) == bytearray:
                    format += 'X'
                    data += struct.pack('<i', len(val)) + val
                else:
                    raise ValueError('value at index %d in passed list is of unsupported %s' % (index, str(type(val))))
                    
        return struct.pack('<i', len(format)) + struct.pack('<%ds' % len(format), format) + data
    