
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import BackLogMessage
from AbstractPlugin import AbstractPluginClass

DEFAULT_BACKLOG = True

class TOSPluginClass(AbstractPluginClass):
    '''
    This plugin forwards all incoming TOS packets to GSN and vice versa.
    '''
    
    def tosMsgReceived(self, timestamp, payload):
        return self.processMsg(timestamp, payload, self._priority, self._backlog)
    
        
    def getMsgType(self):
        return BackLogMessage.TOS_MESSAGE_TYPE
    
            
    def msgReceived(self, message):
        self.sendTOSmsg(message)
    