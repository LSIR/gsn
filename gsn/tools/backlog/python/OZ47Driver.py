# -*- coding: UTF-8 -*-
__author__      = "David Hasenfratz <hasenfratz@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2011, ETH Zurich, Switzerland, David Hasenfratz"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import struct
import serial
import logging

# Communication with the MiCS-OZ-47, ozone sensing head with smart transmitter PCB
class OZ47Driver():
    '''
    ##########################################################################################
    __init__: config[0] = device
    ##########################################################################################
    '''

    def __init__(self,config):
        self._logger = logging.getLogger(self.__class__.__name__)
        
        self._logger.info('Init OZ47 Driver...')
        
        # serial port timeout
        self._serialTimeout = 1
        
        # retries to read message from sensor
        self._readCount = 5
        
        if (config[0] != None):
            self._deviceStr = config[0]
            self._device = self._deviceStr
        else:
            self._deviceStr = '/dev/usb/MiCS-OZ-47'
            self._device = self._deviceStr
        
        try:
            self._device = serial.Serial(self._deviceStr, 19200, timeout=self._serialTimeout)
            self._logger.debug("Successfully opened " + str(self._device))
        except Exception as e:
            self._logger.info("serial access exception " + str(e))
            self._logger.info("Could not access OZ-47 device " + self._deviceStr)
            self._device = 0
            return

    '''
    ##########################################################################################
    # PUBLIC FUNCTIONS
    ##########################################################################################
    '''
    '''
    ##########################################################################################
    # _read(self): returns OZ sensor reading, very simple and only for testing at the moment
    ##########################################################################################
    '''
    def _read(self):
        msg = '{M}'

        try:
            self._device.open()
            self._device.write(msg)
            d = ''
            c = self._readCount
            while d == '' and c > 0:
                # read 19 bytes
                d = self._device.read(20)
                c -= 1
            self._device.close()
            if d == '':
                self._logger.info("No answer from the OZ-47 device to {M} request")
            else:
                self._logger.info("Read OZ-47 device: " + d)
            return d.strip()

        except Exception as e:
            self._logger.info("serial access exception: " + str(e))
            self._logger.info("Could not read OZ-47 sensor reading")
    
    def _setAuto(self):   
        msg = '{S}'                                                                       
                                                                
        try:
            self._device.open()
            self._device.write(msg)
            d = ''
            c = self._readCount
            while d == '' and c > 0:
                # read 4 bytes
                d = self._device.read(4)
                c -= 1
            self._device.close()
            if d == '':
                self._logger.info("No answer from the OZ-47 device to {S} request")
            else:
                self._logger.info("Read OZ-47 device: " + d)
            return d.strip()

        except Exception as e:
            self._logger.info("serial access exception: " + str(e))
            self._logger.info("Could not read OZ-47 sensor reading")

    def _setgTimer(self):
        msg = '{W0?0:059>02}'

        try:
            self._device.open()
            self._device.write(msg)
            d = ''
            c = self._readCount
            while d == '' and c > 0:
                # read 15 bytes
                d = self._device.read(15)
                c -= 1
            self._device.close()
            if d == '':
                self._logger.info("No answer from the OZ-47 device to {W0?0:059>02} request")
            else:
                self._logger.info("Read OZ-47 device: " + d)
            return d.strip()

        except Exception as e:
            self._logger.info("serial access exception: " + str(e))
            self._logger.info("Could not read OZ-47 sensor reading")