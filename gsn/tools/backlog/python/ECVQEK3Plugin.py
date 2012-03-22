#! /usr/bin/python
# -*- coding: UTF-8 -*-
__author__      = "David Hasenfratz <hasenfratz@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2011, ETH Zurich, Switzerland, David Hasenfratz"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"

import BackLogMessage
from AbstractPlugin import AbstractPluginClass

import ECVQEK3Driver

DEFAULT_BACKLOG = True

# Reads messages from the ECVQEK3Driver and sends them to GSN
class ECVQEK3PluginClass(AbstractPluginClass):
    
    def __init__(self, parent, config):
	    
        AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG)
        self._timer = None
        self._stopped = False
        self._interval = None

        self.info('Init ECVQEK3Plugin...')

        self._deviceStr = self.getOptionValue('ecvqek3_device')
        self.ecvqek3 = ECVQEK3Driver.ECVQEK3Driver([self._deviceStr])

        self.info("Done init")

    def isBusy(self):
        return False
    
    def remoteAction(self, parameters):
        self.action(parameters)

    def action(self, parameters):

        self.info('ECVQEK3Plugin started...')
	
        # Read message
        msg = ''
        countRead = 0
        readSuccessFlag = 0
        while len(msg) != 69 and countRead < 10:
            msg = self.ecvqek3._read()
            if str(msg) != 'None' and msg != '[NAK]' and msg.find('EK3 ECM') >= 0:
                self.info(msg)
                readSuccessFlag = 1
                break
            else:
                countRead += 1
                
        if readSuccessFlag == 1:
            dataPackage = [msg]
            dataPackage += [parameters]
            self.processMsg(self.getTimeStamp(), dataPackage)
            self.info('ECVQEK3 reading done')
        else:
            self.error('ECVQEK3 reading failed')

    def stop(self):
        self._stopped = True
#        self.info('stopped')