#! /usr/bin/python
# -*- coding: UTF-8 -*-
__author__      = "David Hasenfratz <hasenfratz@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2011, ETH Zurich, Switzerland, David Hasenfratz"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import BackLogMessage
from AbstractPlugin import AbstractPluginClass

import AlphasenseDriver
'''
added time to give a break in the initialization process.
'''
import time

DEFAULT_BACKLOG = True

STATIC_CALIB_SENS_DATA = 1
DYNAMIC_SENSOR_READING = 2

# Reads messages from the AlphasenseDriver and sends them to GSN
class AlphasensePluginClass(AbstractPluginClass):
    
    '''
    Class variable to count the iterations of the read command.
    '''
    
    def __init__(self, parent, config):
        
        AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG)
        self._timer = None
        self._stopped = False
        self._interval = None
        self.statisticsCounterValue = 0

        self.info('Init AlphasensePlugin...')
        
        self._deviceStr = self.getOptionValue('alphasense_device')
        self.alphasense = AlphasenseDriver.AlphasenseDriver([self._deviceStr])
                
        #msgCalib = self.alphasense._getCalibData()
        #msgSens = self.alphasense._getSensitivityData()
        
        #dataPackage = [STATIC_CALIB_SENS_DATA]
        #dataPackage += [msgCalib]
        #dataPackage += [msgSens]

        #self.processMsg(self.getTimeStamp(), dataPackage)
        
        self.info("Done init")

    def isBusy(self):
        return False

    def action(self, parameters):

        self.debug('AlphasensePlugin started...')
        
        if parameters == '' or parameters <= 0:
            parameters = -1
        
        msg = self.alphasense._read()
        
        dataPackage = [DYNAMIC_SENSOR_READING]
        dataPackage += [msg]
        dataPackage += [parameters]

        self.processMsg(self.getTimeStamp(), dataPackage)

        self.debug('Alphasense reading done')

    def recvInterPluginCommand(self, command):
        self.action(command)

    def stop(self):
        self._stopped = True
        self.info('stopped')
        