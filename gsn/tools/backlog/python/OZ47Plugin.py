#! /usr/bin/python
# -*- coding: UTF-8 -*-
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import BackLogMessage
from AbstractPlugin import AbstractPluginClass

import OZ47Driver

DEFAULT_BACKLOG = True

# Reads messages from the OZ47Driver and sends them to GSN
class OZ47PluginClass(AbstractPluginClass):
    
    def __init__(self, parent, config):
	    
        AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG)
        self._timer = None
        self._stopped = False
        self._interval = None

        self.info('Init OZ47Plugin...')
        
    	self._deviceStr = self.getOptionValue('oz47_device')
    	self.oz47 = OZ47Driver.OZ47Driver([self._deviceStr])

        self.info("Set sensor into automatic mode")
        msg = ''
        while len(msg) != 3:
            msg = self.oz47._setAuto()
        self.info("Set gTimer_delay to 10s")
        msg = ''
        while len(msg) != 13:
            msg = self.oz47._setgTimer()
    
    	self.info("Done init")

    def getMsgType(self):
        return BackLogMessage.OZ47_MESSAGE_TYPE

    def isBusy(self):
        return False

    def needsWLAN(self):
        return False

    def run(self):
        self.info('OZ47Plugin running...')

    def action(self, parameters):

        self.info('OZ47Plugin started...')

        # Read message
        msg = ''
        while len(msg) != 18:
            msg = self.oz47._read()
            self.info(msg)
        
        self.processMsg(self.getTimeStamp(), [msg])

        self.info('OZ47 reading done')

    def stop(self):
        self._stopped = True
        self.info('stopped')
