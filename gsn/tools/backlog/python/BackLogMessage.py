
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import struct


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
        @param payload: payload of the message. Should not be
            bigger than MAX_PAYLOAD_SIZE.

        @throws IOException if firstArg can not be interpreted as a BackLog
            message or if the payload length exceeds MAX_PAYLOAD_SIZE
        '''
        
        self._type = type
        self._timestamp = timestamp
        try:
            self._header = struct.pack('<Bq', type, timestamp)
        except Exception, e:
            raise TypeError('cannot pack message: ' + e.__str__())
        self._payload = payload 
    
    
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
            self._type = int(struct.unpack('B', bytes[0])[0])
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

    
    def getPayload(self):
        '''
        Get the payload of the message as byte array.

        @return: the payload as byte array
        '''
        return self._payload
