#! /usr/bin/python
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

# Communication with the alphasense gas sensor
class AlphasenseDriver():
    
    '''
    ##########################################################################################
    __init__: config[0] = device
    ##########################################################################################
    '''

    def __init__(self,config):
        self._logger = logging.getLogger(self.__class__.__name__)
        
        self._logger.info('Init Alphasense Driver...')
        
        # serial port timeout
        self._serialTimeout = 2
        
        # retries to read message from sensor
        self._readCount = 5
        
        if (config[0] != None):
            self._deviceStr = config[0]
        else:
            self._deviceStr = '/dev/usb/alphasense'
        
        try:
            self._device = serial.Serial(self._deviceStr, 1200, timeout=self._serialTimeout)
            self._logger.debug("Successfully opened " + str(self._device))
        except Exception as e:
            self._logger.error("serial access exception " + str(e))
            self._logger.error("Could not access Alphasense device " + self._deviceStr)
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
        
        try:
            self._device.open()
            while self._device.inWaiting() != 0:
                self._device.flushInput()
            self._logger.debug("readGpsMessage: input buffer flushed")
            self._device.close()
        except Exception as e:
            self._logger.error( "serialAccess Exception (1)" + str(e))
            self._logger.error("Could not flush input buffer")
            self._device.close()
            return ''
        
        msg = '\x55\x02\x00\x00\xd0\x00\xaa'

        try:
            self._device.open()
            self._device.write(msg)
            d = ''
            c = self._readCount
            while d == '' and c > 0:
                # read 49 bytes
                d = self._device.read(49)
                c -= 1
            self._device.close()
            if d == '':
                self._logger.warning("No answer from the Alphasense device to read request")
            else:
                d = ''.join( [ "%02X " % ord( x ) for x in d ] ).strip()
                self._logger.debug("Sensor reading Alphasense: " + d)
            return d.strip()

        except Exception as e:
            self._logger.error("serial access exception: " + str(e))
            self._logger.error("Could not read Alphasense sensor reading")
            self._device.close()
            return ''
            
    def _getCalibData(self):
        
        try:
            self._device.open()
            while self._device.inWaiting() != 0:
                self._device.flushInput()
            self._logger.debug("readGpsMessage: input buffer flushed")
            self._device.close()
        except Exception as e:
            self._logger.error( "serialAccess Exception (1)" + str(e))
            self._logger.error("Could not flush input buffer")
            self._device.close()
            return False
        
        msg = '\x55\x41\x00\x00\x21\xd4\xaa'

        try:
            self._device.open()
            self._device.write(msg)
            d = ''
            c = self._readCount
            while d == '' and c > 0:
                # read 38 bytes
                d = self._device.read(38)
                c -= 1
            self._device.close()
            if d == '':
                self._logger.warning("No answer from the Alphasense device to get calibration data")
            else:
                d = ''.join( [ "%02X " % ord( x ) for x in d ] ).strip()
                self._logger.debug("Calibration data Alphasense: " + d)
            return d.strip()

        except Exception as e:
            self._logger.error("serial access exception: " + str(e))
            self._logger.error("Could not read Alphasense sensor reading")
            self._device.close()
            
    def _getSensitivityData(self):
        
        try:
            self._device.open()
            while self._device.inWaiting() != 0:
                self._device.flushInput()
            self._logger.debug("readGpsMessage: input buffer flushed")
            self._device.close()
        except Exception as e:
            self._logger.error( "serialAccess Exception (1)" + str(e))
            self._logger.error("Could not flush input buffer")
            self._device.close()
            return False
        
        msg = '\x55\x40\x00\x00\x70\x14\xaa'

        try:
            self._device.open()
            self._device.write(msg)
            d = ''
            c = self._readCount
            while d == '' and c > 0:
                # read 118 bytes
                d = self._device.read(118)
                c -= 1
            self._device.close()
            if d == '':
                self._logger.warning("No answer from the Alphasense device to get sensitivity data")
            else:
                d = ''.join( [ "%02X " % ord( x ) for x in d ] ).strip()
                self._logger.debug("Sensitivity data Alphasense: " + d)
            return d.strip()

        except Exception as e:
            self._logger.error("serial access exception: " + str(e))
            self._logger.error("Could not read Alphasense sensor reading")
            self._device.close()
            