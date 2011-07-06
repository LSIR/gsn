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
	
    	self._messageId = struct.pack('2B',0x01,0x06)
        
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
    def _read(self,msgId):
	if (msgId == ""):
	    msgId = self._messageId
	return self._readNav(msgId)

    '''
    #########################################################################
    #PRIVATE FUNCTIONS
    #########################################################################
    '''
                                                   
    '''	
    #########################################################################
    #_readNav(): reads GPGGA data
    #########################################################################
    '''	
    def _readNav(self,msgId):
        
        try:
            self._device.open()
            while self._device.inWaiting() != 0:
                self._device.flushInput()
            self._logger.info("readGpsMessage: input buffer flushed")
        
            success = False
            timeout = 5
            while timeout and not success:
                timeout -= 1
                a = ''
                while (timeout and a != "$"):
                    a = self._serialAccess(1,'r')
                    if a == False:
                        timeout -= 1
                if (a == "$"):
                    while (timeout and a != "GP"):
                        a = self._serialAccess(2,'r')
                        if a == False:
                            timeout -= 1
                    while (timeout and a != "GG"):
                        a = self._serialAccess(2,'r')
                        if a == False:
                            timeout -= 1
                    while (timeout and a != "A,"):
                        a = self._serialAccess(2,'r')
                        if a == False:
                            timeout -= 1
                    a = self._serialAccess(100,'r')
                    if a == False:
                        timeout -= 1
                        success = False
                        break
                    b = a.split('\r')[0]
                    b = b.split(',')
                    header = ["$GPGGA", "0xF0 0x00"] 
                    payload = b 
                    success = True
                    break
                else:
                    success = False
            self._device.close()
            if (success):
                return (header[0], header[1], payload) #ID, class, payload
            else:
                self._logger.debug("readGpsMessage: returned nothing!")
                return False
        
        except Exception as e:
            self._logger.error( "serialAccess Exception" + str(e))
            self._logger.error("Could not execute readNav")
            self._device.close()
            return False
        

    '''
    ##########################################################################################
    serialAccess()
    	mode = w: data is written
	mode = r: data specifies number of bytes to read
	this is the ONLY function that opens serial port and reads/writes from/to serial
	keep port open only as long as necessary

	time the function to determine sleep time
	imperative to clean up properly using cleanUp
	
	return TRUE or data if success, FALSE otherwise
    ##########################################################################################
    '''
    def _serialAccess(self,data,mode):
        d = False
        fd = None
        try:
            if (mode == 'w'):
                self._device.write(data)
                return True
            elif (mode == 'r'):
                d = self._device.read(data)
                return d
            else:
                self._logger.error("serialAccess: Wrong mode specified")
                return False
        except Exception as e:
            self._logger.error( "serialAccess Exception" + str(e))
            self._logger.error("Could not access gps device!")
            return False
