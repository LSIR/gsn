
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision: 2371 $"
__date__        = "$Date: 2010-11-09 17:53:21 +0100 (Die, 09. Nov 2010) $"
__id__          = "$Id: TOSPlugin.py 2371 2010-11-09 16:53:21Z tgsell $"
__source__      = "$URL: https://gsn.svn.sourceforge.net/svnroot/gsn/branches/permasense/gsn/tools/backlog/python/TOSPlugin.py $"

import array
import time

import BackLogMessage
from AbstractPlugin import AbstractPluginClass

DEFAULT_BACKLOG = True

class TOSPluginClass(AbstractPluginClass):
    '''
    This plugin forwards all incoming TOS packets to GSN and vice versa.
    
    data/instance attributes:
    _ready
    '''
    
    def __init__(self, parent, options):
        AbstractPluginClass.__init__(self, parent, options, DEFAULT_BACKLOG)
        
        self._stopped = False
        self._ready = False
    
    
    def run(self):
        # open accessnode queue, just in case if we closed it before...
        while not self._sendOpenQueueCommand() and not self._stopped:
            self.error('could not send OpenQueue command')
            time.sleep(5)
            
        self._ready = True
        
    
    def stop(self):
        self._ready = False
        self._stopped = True
        # send close queue cmd to access node
        self._sendCloseQueueCommand()
        self.info('stopped')
    
    
    def tosMsgReceived(self, timestamp, packet):
        return self.processMsg(timestamp, self._tos2backlog(packet), self._priority, self._backlog)
        
        
    def getMsgType(self):
        return BackLogMessage.TOS_MESSAGE_TYPE
    
            
    def msgReceived(self, message):
        if self._ready:
            self.sendTOSmsg(self._backlog2tos(message), 0x00, 0.2, True, 10)


    def _sendCloseQueueCommand(self):
        if self.sendTOSmsg(array.array('B', [0x00, 0x00, 0x00, 0x00, 0x05, 0x22, 0x50, 0xff, 0xff, 0x80, 0x00, 0x00]).tolist(), 0x00, 0.2, True, 10):
            self.info('waiting 35 seconds for close queue command to be complete')
            time.sleep(35)


    def _sendOpenQueueCommand(self):
        return self.sendTOSmsg(array.array('B', [0x00, 0x00, 0x00, 0x00, 0x05, 0x22, 0x50, 0xff, 0xff, 0x80, 0x01, 0x00]).tolist(), 0x00, 0.2, True, 10)
        

    def _backlog2tos(self, message):
        return array.array('B', message[1:]).tolist()


    def _tos2backlog(self, packet):
        # TODO: append zero at start should not really happen here -> needs bugfix for tos.py
        return array.array('B', [0] + packet.payload()).tostring()
    