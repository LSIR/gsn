#! /usr/bin/python
# -*- coding: UTF-8 -*-
__author__      = "David Hasenfratz <hasenfratz@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2011, ETH Zurich, Switzerland, David Hasenfratz"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"

'''
backlog imports
'''
import BackLogMessage
from AbstractPlugin import AbstractPluginClass

import GPSDriverNAV
'''
defines
'''

DEFAULT_BACKLOG = True

NAV_TYPE = 1
RAW_TYPE = 2

#Reads raw or nav GPS messages from a u-blox device and sends them to GSN

class GPSPluginNAVClass(AbstractPluginClass):
    
    def __init__(self, parent, config):
	    
        AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG)
        self._timer = None
        self._stopped = False
        
        self.info('Init GPSPluginNAV...')
        
        self._deviceStr = self.getOptionValue('gps_device')
        self.gps = GPSDriverNAV.GPSDriverNAV([self._deviceStr])
        
        self.info("Done init")

    def getMsgType(self):
        return BackLogMessage.GPS_NAV_MESSAGE_TYPE

    def isBusy(self):
        return False

    def action(self, parameters):

        self.info('GPSPluginNAV started...')
        
        # Read GPS message
        gpsMsg = self.gps._read()
        # Parse message
        dataPackage = self._parseNavMsg(gpsMsg)
        dataPackage += [parameters]
        self.processMsg(self.getTimeStamp(), dataPackage)
        
        self.info('GPS reading done')
        
    def remoteAction(self, parameters):
        self.action(parameters)

    def stop(self):
        self._stopped = True
        self.info('stopped')

    def _parseNavMsg(self, msg):
        if msg != 0:
            dataPackage = [NAV_TYPE]
            for i in range(1, len(msg)):
                dataPackage += [msg[i]]

            self.info(dataPackage)
            return dataPackage

        else:
            self.info("WARNING: MSG packet was empty!")
        
