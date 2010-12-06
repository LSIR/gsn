
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision: 2084 $"
__date__        = "$Date: 2010-07-16 14:45:21 +0200 (Fre, 16. Jul 2010) $"
__id__          = "$Id: TOS1xPlugin.py 2084 2010-07-16 12:45:21Z tgsell $"
__source__      = "$URL: https://gsn.svn.sourceforge.net/svnroot/gsn/branches/permasense/gsn/tools/backlog/python/TOS1xPlugin.py $"

import array
import time

import BackLogMessage
from AbstractPlugin import AbstractPluginClass
from TOSPlugin import TOSPluginClass

DEFAULT_BACKLOG = True

class TOS1xPluginClass(TOSPluginClass, AbstractPluginClass):
    '''
    This plugin forwards all incoming TOSv1.x packets to GSN and vice versa.
    '''

    def getMsgType(self):
        return BackLogMessage.TOS1x_MESSAGE_TYPE


    def _sendCloseQueueCommand(self):
        if self.sendTOSmsg(array.array('B', [0x02, 0x00, 0x01, 0x00, 0x50, 0x7D, 0x00, 0x80]).tolist(), 0x00, 1, True, 10):
            self.info('waiting 35 seconds for close queue command')
            time.sleep(35)


    def _sendOpenQueueCommand(self):
        return self.sendTOSmsg(array.array('B', [0x02, 0x00, 0x01, 0x00, 0x50, 0x7D, 0x01, 0x80]).tolist(), 0x00, 1, True, 10)
        

    def _backlog2tos(self, message):
        return array.array('B', message).tolist()


    def _tos2backlog(self, packet):
        return array.array('B', packet.payload()).tostring()
