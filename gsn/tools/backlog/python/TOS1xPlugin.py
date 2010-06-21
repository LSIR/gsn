
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

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
