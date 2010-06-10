
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import array
import time
import tos1x
import BackLogMessage
from AbstractPlugin import AbstractPluginClass
from TOSPlugin import TOSPluginClass

DEFAULT_BACKLOG = True

class TOS1xPluginClass(TOSPluginClass, AbstractPluginClass):
    '''
    This plugin offers the functionality to connect to a serial port or a serial
    forwarder and listens for incoming TOSv1.x packets.
    
    Over the serial port it can only talk to nodes using the TinyNode platform!
    '''

    '''
    data/instance attributes:
    _serialsource    
    _stopped
    '''

    def __init__(self, parent, options):
        AbstractPluginClass.__init__(self, parent, options, DEFAULT_BACKLOG)
        
        address = self.getOptionValue('tos_source_addr')
        
        if address is None:
            raise TypeError('tos_source_addr not specified in config file')
        
        debug = self.getOptionValue('debug')
        if debug is not None and debug == '1':
            debug = True
        else:
            debug = False
        
        # split the address (it should have the form serial@port:baudrate)
        source = address.split('@')
        if source[0] == 'serial':
            try:
                # try to open a connection to the specified serial port
                serial = tos1x.getSource(address, debug)
                serial.setTimeout(5)
                self._serialsource = tos1x.AM(serial)
            except Exception, e:
                raise TypeError('could not initialize serial source: ' + e.__str__())
        else:
            raise TypeError('address type must be serial')
        
        self._stopped = False

    def getMsgType(self):
        return BackLogMessage.TOS1x_MESSAGE_TYPE

    def tos2backlog(self, packet):
        return array.array('B', packet.payload()).tostring()

    def backlog2tos(self, message):
        return array.array('B', message).tolist()

    def sendCloseQueueCommand(self):
        if self._serialsource.write(array.array('B', [0x02, 0x00, 0x01, 0x00, 0x50, 0x7D, 0x00, 0x80]).tolist(), 0x00, 0.2, True, 10):
            time.sleep(35)

    def sendOpenQueueCommand(self):
        return self._serialsource.write(array.array('B', [0x02, 0x00, 0x01, 0x00, 0x50, 0x7D, 0x01, 0x80]).tolist(), 0x00, 0.2, True, 10)
