#! /usr/bin/python
# -*- coding: UTF-8 -*-
__author__      = "David Hasenfratz <hasenfratz@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2011, ETH Zurich, Switzerland, David Hasenfratz"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"

import struct
import time
import serial
import commands
import os
import logging
'''
defines
'''

#Reads GPS messages from a u-blox device and sends them to GSN
class GPSDriverNAV():
    '''
    ##########################################################################################
    __init__: config[0] = device (i.e. /dev/ttyACM) without number!
              config[1] = rate   
    ##########################################################################################
    '''
    
    def __init__(self,config):
        self._logger = logging.getLogger(self.__class__.__name__)

        self._logger.info('Init GPS Driver...')
        
    	# serial port timeout
    	self._serialTimeout = 1
    	
        if (config[0] != None):
            self._deviceStr = config[0]
        else:
            self._logger.error("deviceStr is missing")
            return

        try:
            self._device = serial.Serial(self._deviceStr, 19200, timeout=self._serialTimeout)
            self._logger.info("Successfully opened " + str(self._device))
        except Exception as e:
            self._logger.error("serialAccess Exception" + str(e))
            self._logger.error("Could not access gps device " + self._deviceStr)
            return

    '''
    ##########################################################################################
    # PUBLIC FUNCTIONS
    ##########################################################################################
    '''
    '''
    ##########################################################################################
    # _read(self): returns a GPS message according to configuration
    ##########################################################################################
    '''
    def _read(self):
        
        try:
            self._device.open()
            t = time.time()
            #self._device.read(self._device.inWaiting())
            while time.time() - t < 0.2: #self._device.inWaiting() != 0:
                self._device.flushInput()
            self._logger.info("readGpsMessage: input buffer flushed")
            pos = -1
            
            while pos == -1 and time.time() - t < 5:
                msg = self._device.read(self._device.inWaiting())
                
                pos = msg.find('GPGGA')
                while msg.find('GPGGA',pos+1) != -1:
                    pos = msg.find('GPGGA',pos+1)
                
            #self._device.close()
            
            if pos != -1:
                data = msg[pos:msg.find('\n',pos+1)]
                data = data.strip()
                data = data.split(',')
            
                return data
            else:
                self._logger.debug("readGpsMessage: no GPGGA data!")
                return 0
        
        except Exception as e:
            self._logger.error( "serialAccess Exception" + str(e))
            self._logger.error("Could not execute _read")
            self._device.close()
            return 0
        