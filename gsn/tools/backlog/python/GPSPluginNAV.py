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

'''
stdlib imports
'''
import struct
import copy
import time
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
        self._interval = 1
        
        self.info('Init GPSPluginNAV...')
        
        # serial port timeout
        self._serialTimeout = 1
        self._serialCount = 0
        self._zombiesKilled = 0
        
        self._deviceStr = self.getOptionValue('gps_device')
        self.gps = GPSDriverNAV.GPSDriverNAV([self._deviceStr, self._interval])
        
        self.info("Done init")

    def getMsgType(self):
        return BackLogMessage.GPS_MESSAGE_TYPE

#    def isBusy(self):
#        return False
#    
#    def needsWLAN(self):
#        return False
#
#    def run(self):
#	
#        self.info('GPSPluginNAV running...')
#	#self.action('')	

    def action(self, parameters):

        self.info('GPSPluginNAV started...')
        
        # Read GPS message
        gpsMsg = self.gps._read("")
        # Parse message
        dataPackage = self._parseNavMsg(gpsMsg)
        self.processMsg(self.getTimeStamp(), dataPackage)
        
        self.info('GPS reading done')

#    def stop(self):
#        self._stopped = True
#        self.info('stopped')

    def _parseNavMsg(self, msg):
        if (msg):
            dataPackage = [NAV_TYPE]
            for i in range(0, len(msg[2])):
                dataPackage += [msg[2][i]]

            self.info(dataPackage)
            return dataPackage

        else:
            self.info("WARNING: MSG packet was empty!")
        
