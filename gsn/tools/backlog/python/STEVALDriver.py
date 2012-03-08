#! /usr/bin/python
# -*- coding: UTF-8 -*-
__author__      = "David Hasenfratz"
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
        self._logger.info('Init STEVAL-MKI004V1 Driver...')
        
        if (config[0] != None):
            self._deviceStr = config[0]
            self._device = self._deviceStr
        else:
            self._deviceStr = '/dev/acm/steval'
            self._device = self._deviceStr
        
        try:
            self._device = serial.Serial(self._deviceStr, 115200, timeout=1)
            self._device.close()
            self._logger.info("Device Init Successful for " + str(self._device))
        except Exception as e:
            self._logger.error("serial access exception " + str(e))
            self._logger.error("Could not initialize STEVAL-MKI004V1 device " + self._deviceStr)
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
            self._setSensor()

        except Exception as e:
            self._logger.error("serial access exception: " + str(e))
            self._logger.error("Could not open device STEVAL-MKI004V1")
            
    def _closeDevice(self):
        try:
            self._device.close()

        except Exception as e:
            self._logger.error("serial access exception: " + str(e))
            self._logger.error("Could not close device STEVAL-MKI004V1")
            
   
    def _startDataAcquisition(self, deltaTime):
        try:
            self._stopDataAcquisition()
            while self._device.inWaiting() != 0:
                self._device.flushInput()
            
            self._device.write('*')
            self._device.write('d')
            self._device.write('e')
            self._device.write('b')
            self._device.write('u')
            self._device.write('g')
            self._device.write('\r')
            
            self._logger.info('start get data for ' + str(deltaTime) + ' seconds')
            
            startTime = time.time()
            d = ''
            while (time.time() - startTime) < float(deltaTime):
                d += self._device.read(128)
                
            self._stopDataAcquisition()
            
            d = d[d.find('x'):]
            lines = d.split('\r\n')
            self._logger.info('got ' + str(len(lines)) + ' lines of data')

            i = 0
            data = [[],[],[]]
            while i < len(lines) and (len(lines[i]) == 37 or len(lines[i]) == 31):
                if len(lines[i]) == 37:
                    data[0].append(int(lines[i][2:8]))
                    data[1].append(int(lines[i][15:21]))
                    data[2].append(int(lines[i][28:34]))
                else:
                    data[0].append(int(lines[i][2:8]))
                    data[1].append(int(lines[i][13:19]))
                    data[2].append(int(lines[i][25:31]))
                i = i + 1
                         
            self._logger.info('parsed ' + str(len(data[0])) + ' lines of data')
            return data

        except Exception as e:
            self._logger.error("serial access exception: " + str(e))
            self._logger.error("Could not finish data acquisition on STEVAL-MKI004V1")

    def _stopDataAcquisition(self):
        try:
            self._device.write('*')
            self._device.write('s')
            self._device.write('t')
            self._device.write('o')
            self._device.write('p')
            self._device.write('\r')
            #time.sleep(0.3)

        except Exception as e:
            self._logger.error("serial access exception: " + str(e))
            self._logger.error("Could not stop data acquisition of STEVAL-MKI004V1")
                         
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
            self._logger.error("Could not write *Zoff to STEVAL-MKI004V1")


    def _getDevName(self):   
        try:
            self._stopDataAcquisition()
            while self._device.inWaiting() != 0:
                self._device.flushInput()
            
            self._device.write('*')
            self._device.write('d')
            self._device.write('e')
            self._device.write('v')
            self._device.write('\r')
            d = self._device.read(7)

            if d == '':
                self._logger.warning("No answer from the STEVAL-MKI004V1 device to *dev request")
            else:
                self._logger.info("STEVAL-MKI004V1 device name: " + d.strip())
            return d.strip()

        except Exception as e:
            self._logger.error("serial access exception: " + str(e))
            self._logger.error("Could not get device name from STEVAL-MKI004V1")
            
    def _getVerNumber(self):   
        try:
            self._stopDataAcquisition()
            while self._device.inWaiting() != 0:
                self._device.flushInput()
            
            self._device.write('*')
            self._device.write('v')
            self._device.write('e')
            self._device.write('r')
            self._device.write('\r')
            d = self._device.read(7)

            if d == '':
                self._logger.warning("No answer from the STEVAL-MKI004V1 device to *ver request")
            else:
                self._logger.info("STEVAL-MKI004V1 version number: " + d.strip())
            return d.strip()

        except Exception as e:
            self._logger.error("serial access exception: " + str(e))
            self._logger.error("Could not get version number from STEVAL-MKI004V1")
    