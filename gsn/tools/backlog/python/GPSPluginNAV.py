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

    def isBusy(self):
        return False

    def action(self, parameters):
        try:
          self.debug('GPSPluginNAV started...')
          
          if parameters == '' or parameters <= 0:
            parameters = -1
          
          # Read GPS message
          gpsMsg = self.gps._read()
          
          if gpsMsg != 0 and gpsMsg != '' and gpsMsg is not None:
          
            # Parse message
            dataPackage = self._parseNavMsg(gpsMsg)
            dataPackage += [parameters]
            self.processMsg(self.getTimeStamp(), dataPackage)
        
            self.debug('GPS reading done')
          else:
            self.warning('No GPS data')
        except Exception as e:
          self._logger.error( "Exception: " + str(e))
          self._logger.error("Could not execute action")
          return

    def recvInterPluginCommand(self, command):
        self.action(command)

    def stop(self):
        self._stopped = True
        self.info('stopped')

    def _parseNavMsg(self, msg):
        if msg != 0:
            dataPackage = [NAV_TYPE]
            for i in range(1, len(msg)):
                dataPackage += [msg[i]]

            self.debug(dataPackage)
            return dataPackage

        else:
            self.debug("WARNING: MSG packet was empty!")
        
