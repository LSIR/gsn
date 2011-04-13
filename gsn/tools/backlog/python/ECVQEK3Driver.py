# -*- coding: UTF-8 -*-
__author__      = "David Hasenfratz <hasenfratz@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2011, ETH Zurich, Switzerland, David Hasenfratz"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"

import struct
import serial
import logging

# Communication with the ECVQEK3 evauluation kit
class ECVQEK3Driver():
    '''
    ##########################################################################################
    __init__: config[0] = device
    ##########################################################################################
    '''
    
    def __init__(self,config):
        self._logger = logging.getLogger(self.__class__.__name__)

        self._logger.info('Init ECVQEK3 Driver...')

    	# serial port timeout
    	self._serialTimeout = 1

        # retries to read message from sensor
        self._readCount = 5
    	
        if (config[0] != None):
    	    self._deviceStr = config[0]
    	    self._device = self._deviceStr
        else:
            self._deviceStr = '/dev/usb/ecvq-ek3'
            self._device = self._deviceStr

        try:
            self._device = serial.Serial(self._deviceStr, 9600, timeout=self._serialTimeout)
            self._logger.debug("Successfully opened " + str(self._device))
        except Exception as e:
            self._logger.info("serial access exception " + str(e))
            self._logger.info("Could not access ECVQEK3 device " + self._deviceStr)
            self._device = 0
            return

    '''
    ##########################################################################################
    # PUBLIC FUNCTIONS
    ##########################################################################################
    '''
    '''
    ##########################################################################################
    # _read(self): returns reading from the evaluation kit, very simple and only for testing at the moment
    ##########################################################################################
    '''
    def _read(self):
        msg = '[EK3 ECM ENQ]'

        try:	
            self._device.open()
            self._device.write(msg)
            d = ''
            c = self._readCount
            while d == '' and c > 0:
                # read 69 bytes
                d = self._device.read(71)
                c -= 1
            self._device.close()
            if d == '':
                self._logger.info("No answer from the ECVQEK3 device to the request")
            else:
                self._logger.info("Read ECVQEK3 device: " + d)
            return d.strip()

        except Exception as e:
            self._logger.info("serial access exception: " + str(e))
            self._logger.info("Could not read from ECVQEK3")