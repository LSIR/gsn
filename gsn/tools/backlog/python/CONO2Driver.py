#! /usr/bin/python
# -*- coding: UTF-8 -*-
__author__      = "David Hasenfratz <hasenfratz@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2011, ETH Zurich, Switzerland, David Hasenfratz"
__license__     = "GPL"
__version__     = "$Revision: 4007 $"
__date__        = "$Date: 2012-03-22 15:25:24 +0100 (Thu, 22 Mar 2012) $"
__id__          = "$Id: CONO2Driver.py 4007 2012-03-22 14:25:24Z dhasenfratz $"
__source__      = "$URL: http://gsn.svn.sourceforge.net/svnroot/gsn/branches/permasense/gsn/tools/backlog/python/CONO2Driver.py $"

import struct
import serial
import logging

class CONO2Driver():
    '''
    ##########################################################################################
    __init__: config[0] = device
    ##########################################################################################
    '''

    def __init__(self,config):
        self._logger = logging.getLogger(self.__class__.__name__)
        
        self._logger.info('Init CONO2 Driver...')
        
        # serial port timeout
        self._serialTimeout = 1
        
        # retries to read message from sensor
        self._readCount = 5
        
        if (config[0] != None):
            self._deviceStr = config[0]
            self._device = self._deviceStr
        else:
            self._deviceStr = '/dev/usb/CONO2'
            self._device = self._deviceStr
        
        try:
            self._device = serial.Serial(self._deviceStr, 19200, timeout=self._serialTimeout)
            self._device.close()
            self._logger.info("Successfully opened " + str(self._device))
        except Exception as e:
            self._logger.error("serial access exception " + str(e))
            self._logger.error("Could not access CONO2 device " + self._deviceStr)
            self._device = 0
            return

    '''
    ##########################################################################################
    # PUBLIC FUNCTIONS
    ##########################################################################################
    '''
    '''
    ##########################################################################################
    # _read(self): returns sensor reading
    ##########################################################################################
    '''
    def _read(self):
        
        try:
            self._device.open()
            while self._device.inWaiting() != 0:
                self._device.flushInput()
            self._logger.debug("input buffer flushed")
            self._device.close()
        except Exception as e:
            self._logger.error( "serialAccess Exception (1)" + str(e))
            self._logger.error("Could not flush input buffer")
            self._device.close()
            return False
        
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
                self._logger.warning("No answer from the CONO2 device to {M} request")
            else:
                self._logger.debug("Read CONO2 device: " + d)
            return d.strip()

        except Exception as e:
            self._logger.error("serial access exception: " + str(e))
            self._logger.error("Could not read CONO2 sensor reading")
            self._device.close()
            
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
                self._logger.warning("No answer from the CONO2 device to {S} request")
            else:
                self._logger.debug("Write CONO2 device: " + d)
            return d.strip()

        except Exception as e:
            self._logger.error("serial access exception: " + str(e))
            self._logger.error("Could not read CONO2 sensor reading")
            self._device.close()

    def _setgTimer(self):
        msg = '{W0?3<05821>}'        #0?0:00C30:}'   #59<21
        
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
                self._logger.warning("No answer from the CONO2 device to {W0?3<00330:} request")
            else:
                self._logger.debug("Write CONO2 device: " + d)
            return d.strip()

        except Exception as e:
            self._logger.error("serial access exception: " + str(e))
            self._logger.error("Could not read CONO2 sensor reading")
            self._device.close()
