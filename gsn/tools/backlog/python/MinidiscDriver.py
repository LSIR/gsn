#! /usr/bin/python
# -*- coding: UTF-8 -*-
__author__      = "David Hasenfratz"
__copyright__   = "Copyright 2011, ETH Zurich, Switzerland"
__license__     = "GPL"
__version__     = "$Revision: 3960 $"
__date__        = "$Date: 2012-02-28 18:27:24 +0100 (Tue, 28 Feb 2012) $"
__id__          = "$Id: MinidiscDriver.py 3960 2012-02-28 17:27:24Z dhasenfratz $"
__source__      = "$URL: http://gsn.svn.sourceforge.net/svnroot/gsn/branches/permasense/gsn/tools/backlog/python/MinidiscDriver.py $"

import serial
import logging

# Communication with the Minidisc
class MinidiscDriver():
    '''
    ##########################################################################################
    __init__: config[0] = device
    ##########################################################################################
    '''

    def __init__(self,config):
        self._logger = logging.getLogger(self.__class__.__name__)
        self._logger.info('Init Minidisc Driver...')
        
        if (config[0] != None):
            self._deviceStr = config[0]
            self._device = self._deviceStr
        else:
            self._deviceStr = '/dev/usb/MiniDisc'
            self._device = self._deviceStr
        
        try:
            self._device = serial.Serial(self._deviceStr, 256000, timeout=1)
            while self._device.inWaiting() != 0:
              self._device.flushInput()
            self._logger.debug("Device Init Successful for " + str(self._device))
        except Exception as e:
            self._logger.error("serial access exception " + str(e))
            self._logger.error("Could not initialize Minidisc device " + self._deviceStr)
            return

    '''
    ##########################################################################################
    # PUBLIC FUNCTIONS
    ##########################################################################################
    '''     
   
    def _read(self):
        try:
            d = self._device.read(self._device.inWaiting())
            return d.strip()

        except Exception as e:
            self._logger.error("serial access exception: " + str(e))
            self._logger.error("Could not read data from Minidisc")
