'''
Created on Jul 15, 2009

@author: Tonio Gsell
@author: Mustafa Yuecel
'''

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
                self._serialsource = tos1x.AM(serial)
            except Exception, e:
                raise TypeError('could not initialize serial source: ' + e.__str__())
        else:
            raise TypeError('address type must be serial')
        
        self._stopped = False

    def getMsgType(self):
        # TODO: use TOS1x_MESSAGE_TYPE after separation in GSN
        return BackLogMessage.TOS_MESSAGE_TYPE
        #return BackLogMessage.TOS1x_MESSAGE_TYPE
    