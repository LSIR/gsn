# -*- coding: UTF-8 -*-
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

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
        return bytearray(array.array('B', packet.payload()).tostring())
