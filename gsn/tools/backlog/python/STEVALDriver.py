#! /usr/bin/python
# -*- coding: UTF-8 -*-
__author__      = "xxx"
__copyright__   = "Copyright 2011, ETH Zurich, Switzerland"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import struct
import serial
import logging
import time

# Communication with the STEVAL-MKI004V1, MEMS inertial sensing head.
class STEVALDriver():
    '''
    ##########################################################################################
    __init__: config[0] = device
    ##########################################################################################
    '''

    def __init__(self,config):
        self._logger = logging.getLogger(self.__class__.__name__)
        self._logger.info("Init %s" % self.__class__.__name__)
        #self._logger.info('Init STEVAL-MKI004V1 Driver...')
        
        # serial port timeout
        self._serialTimeout = 1
        
        # retries to read message from sensor
        self._readCount = 5
        
        if (config[0] != None):
            self._deviceStr = config[0]
            self._device = self._deviceStr
        else:
            self._deviceStr = '/dev/acm/steval'
            self._device = self._deviceStr
        
        try:
            self._device = serial.Serial(self._deviceStr, 115200, timeout=self._serialTimeout)
            self._logger.info("Device Init Successful" + str(self._device))
        except Exception as e:
            self._logger.error("serial access exception " + str(e))
            self._logger.error("Could not access STEVAL-MKI004V1 device " + self._deviceStr)
            self._device = 0
            return

    '''
    ##########################################################################################
    # PUBLIC FUNCTIONS
    ##########################################################################################
    '''

    def _openDevice(self):
        try:
            self._device.open()
            self._device.flush()

        except Exception as e:
            self._logger.error("serial access exception: " + str(e))
            self._logger.error("Could not open device STEVAL-MKI004V1")
            
    def _closeDevice(self):
        try:
            self._device.flush()
            self._device.close()

        except Exception as e:
            self._logger.error("serial access exception: " + str(e))
            self._logger.error("Could not close device STEVAL-MKI004V1")
            
                    
    def _startDataAcquisition(self):
        try:
            self._device.write('*')
            self._device.write('s')
            self._device.write('t')
            self._device.write('a')
            self._device.write('r')
            self._device.write('t')
            self._device.write('\r')
            
        except Exception as e:
            self._logger.error("serial access exception: " + str(e))
            self._logger.error("Could not read STEVAL-MKI004V1 sensor reading")
   
    def _startDataAcquisitionDebug(self, val):
        try:
            while self._device.inWaiting() != 0:
                self._device.flushInput()
            
            self._device.write('*')
            self._device.write('d')
            self._device.write('e')
            self._device.write('b')
            self._device.write('u')
            self._device.write('g')
            self._device.write('\r')
            d = ''
            tmpVal = ''
            c = 2 
            while c > 0:
                d = self._device.read(val)                
                pos = d.find('x=')
                if pos >= 0:
                    d = d[pos:]
                    idx = 0
                    l = d.find('\n')
                    newL = len(str(d))
                    i = idx
                    while (i < newL):
                        i += 1
                    while idx < (newL) and (idx + l) < (newL):
                        total = idx + l
                        if total < len(str(d)):
                                          
                            if len(tmpVal) == 0:
                                tmpVal = d[idx:total]
                                idx = total + 1
                            else:
                                tmp = d[idx]
                                tmpVal = tmpVal + d[idx:total]
                                i = idx
                                idx = total + 1
                        else:
                            break
                    c -= 1                
            return tmpVal

        except Exception as e:
            self._logger.error("serial access exception: " + str(e))
            self._logger.error("Could not read STEVAL-MKI004V1 sensor reading")

    def _stopDataAcquisition(self):
        try:
            self._device.write('*')
            self._device.write('s')
            self._device.write('t')
            self._device.write('o')
            self._device.write('p')
            self._device.write('\r')
            time.sleep(0.3)

        except Exception as e:
            self._logger.error("serial access exception: " + str(e))
            self._logger.error("Could not read STEVAL-MKI004V1 sensor reading")
                         
    def _setSensor(self):   
        try:
            self._device.write('*')
            self._device.write('Z')
            self._device.write('o')
            self._device.write('f')
            self._device.write('f')
            self._device.write('\r')
            return 1
        
        except Exception as e:
            self._logger.error("serial access exception: " + str(e))
            self._logger.error("Could not read STEVAL-MKI004V1 sensor reading")

    def _unsetSensor(self):   
        try:
            self._device.write('*')
            self._device.write('Z')
            self._device.write('o')
            self._device.write('n')
            self._device.write('\r')
  
        except Exception as e:
            self._logger.error("serial access exception: " + str(e))
            self._logger.error("Could not read STEVAL-MKI004V1 sensor reading")


    def _getDevName(self):   
        try:
            self._device.write('*')
            self._device.write('d')
            self._device.write('e')
            self._device.write('v')
            self._device.write('\r')
            d = ''
            c = self._readCount
            while d == '' and c > 0:
                d = self._device.read(12)
		if d.find('3LV') >= 0:
		    break
		else:
                    c -= 1
            if d == '':
                self._logger.warning("No answer from the STEVAL-MKI004V1 device to *dev request")
            else:
                self._logger.info("Read STEVAL-MKI004V1 device: " + d)
            return d.strip()

        except Exception as e:
            self._logger.error("serial access exception: " + str(e))
            self._logger.error("Could not read STEVAL-MKI004V1 sensor reading")
            
    def _getVerNumber(self):   
        try:
            self._device.write('*')
            self._device.write('v')
            self._device.write('e')
            self._device.write('r')
            self._device.write('\r')
            d = ''
            c = self._readCount
            while d == '' and c > 0:
                d = self._device.read(12)
                if d.find('LVD') >= 0:
                    break
                else:
                    c -= 1
            if d == '':
                self._logger.warning("No answer from the STEVAL-MKI004V1 device to *ver request")
            else:
                self._logger.info("Read STEVAL-MKI004V1 device: " + d)
            return d.strip()

        except Exception as e:
            self._logger.error("serial access exception: " + str(e))
            self._logger.error("Could not read STEVAL-MKI004V1 sensor reading")
    