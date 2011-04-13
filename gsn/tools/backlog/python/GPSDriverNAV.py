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
from threading import Event
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

    	#####################################################
    	#
        # init GPS message headers using RXM type
        # header format: sync char 1 | sync char 2
        #                   0xb5     |    0x62
        self._gpsHeader = struct.pack('2B', 0xB5, 0x62)
	
    	self._messageId = struct.pack('2B',0x01,0x06)
        
        # GPS Configuration
        # 0x08 = To set the measurements per ms
        self._rateMessageId = struct.pack('2B', 0x06, 0x08)
        # 0x00 = Set the port to give out RAW messages
        self._prtMessageId = struct.pack('2B',0x06,0x00)
        #0x01 = Give out raw message after every measurement
        self._msgMessageId = struct.pack('2B', 0x06, 0x01)
        #0x09 Clear,Save and Load configurations
        self._saveMessageId = struct.pack('2B',0x06,0x09)
        #0x11 Change power mode
        self._rxmMessageId = struct.pack('2B',0x06,0x11)
        #0x32 Set Power Management options
        self._pmMessageId = struct.pack('2B',0x06,0x32)
    	self._ACK = struct.pack('2B', 0x05, 0x01)
    	self._NACK = struct.pack('2B', 0x05, 0x00)
        
    	# serial port timeout
    	self._serialTimeout = 1
    	
        if (config[0] != None): #Device string
            #self._deviceStr = config[0]
            #self._device = self._deviceStr
            self._device = config[0]

        try:
            self._device = serial.Serial(self._device, 19200, timeout=self._serialTimeout)
            self._logger.info("Successfully opened " + str(self._device))
        except Exception as e:
            self._logger.error("serialAccess Exception (2) " + str(e))
            self._logger.error("Could not access gps device " + self._device)
            return

    	if (config[1] != None): # The measurement interval in seconds
            self._interval = config[1]
        else:
            self._interval = 1

        self._runEv = Event()
        #self._initialized = False
        #device is tested by config in that it tries to write to it.
        self._logger.info("Config GPS device")
        ret = self._config_device()
    	if (ret == True):
    	    self._logger.info("Done GPS Driver init")
    	else:
    	    self._logger.info("There was a problem initializing the GPS device")
	
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
    ##########################################################################################
    # _write(self,payload): writes payload appropriately prepended with sync chars & header, and
    # appended with checksums to the GPS device.
    ##########################################################################################
    '''
    def _write(self,msgId=None,payload=None):
        #self._logger.info("_write with msg: " + str(payload) + " msgId: " + str(msgId))

        if (msgId==None):
            msgId = self._messageId
        if(payload != None):
            msg = msgId + struct.pack('H', len(payload)) + payload
        else:
            msg = msgId + struct.pack('H', 0)

        checksums = self._calculateChecksum(msg,None)

        outBuffer = self._gpsHeader + msg + struct.pack('B', checksums[0]) + struct.pack('B', checksums[1])


        return self._serialAccess(self._device,outBuffer,'w')

    '''
    ##########################################################################################
    _pollGpsMessage(): Polls GPS device with msgId
    ##########################################################################################
    '''
    def _pollGpsMessage(self, msgId):
        '''
        Poll a UBX message which means sending a message of a particular type with
        empty payload and then receive a message of the same type
        '''
        #self._logger.info("pollGpsMessage: " + str(msgId))
        if (not self._write(msgId,'')):
                self._logger.error("pollGpsMessage: sending gps message didn't succeed")
                return False
        d = self._readRaw(msgId)
        if (d == False or len(d) == 0):
                self._logger.error("pollGpsMessage: reading gps message didn't succeed")
                return False
        return d


    '''
    #########################################################################
    #PRIVATE FUNCTIONS
    #########################################################################
    '''
    '''                                                                      
    #########################################################################                 
    #_readRaw(): reads RAW data                                                                 
    #########################################################################
    '''                                                                                   
    def _readRaw(self,msgId):                                            
        timeout = 5                                                      
        success = False                                                                 
        while timeout and not success:                                   
            timeout = timeout -1                                         
            # Wait for the Header                                        
            a = self._serialAccess(self._device,1,'r')                   
            while a == False or (len(a) == 1 and ord(a) != 0xB5):        
                a = self._serialAccess(self._device,1,'r')                                               
            a = self._serialAccess(self._device,1,'r')                                
            if a != False and len(a)==1 and ord(a) == 0x62:                         
                # Got a message! :)  Read 2 bytes to determine class & id           
                #self._logger.info("got msg")                                              
                recMsgId = ""                                                                              
                while (not recMsgId):                                               
                        recMsgId = self._serialAccess(self._device,2,'r')           
                # Is it the right Msg Type?                                                                
                if (recMsgId == msgId):                                                                    
                        #self._logger.info("got one")                                                      
                        header = struct.unpack('2B', recMsgId)                                             
                        rawPayloadLength = self._serialAccess(self._device,2,'r')                          
                        payloadLength = struct.unpack('H', rawPayloadLength)[0]                            
                        payload = self._serialAccess(self._device,payloadLength,'r')                                    
                        ck = self._serialAccess(self._device,2,'r')                                                     
                        while (ck == False):                                                                            
                                ck = self._serialAccess(self._device,2,'r')                                             
                        submitChecksum = struct.unpack('2B', ck)                                                        
                                                                                                                        
                        calculatedChecksum = self._calculateChecksum(recMsgId + rawPayloadLength + payload)             
                                                                                                                        
                        if( submitChecksum == calculatedChecksum):                                                      
                                success = True                                                                          
                        else:                                                                                           
                                self._logger.info('The submitted checksum did not match the expected one')              
                                self._logger.info('Expected: ' +str(calculatedChecksum) + ' got: ' +str(submitChecksum))
                                success = False                              
                                                                                    
        if (success):                                                        
            self._logger.debug("readGpsMessage: Returned payload")           
            return (header[0], header[1], payload) #ID, class, payload                        
        else:                                                                
            self._logger.debug("readGpsMessage: returned nothing!")                           
            return False                                                     
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
            self._device.close()
    	except Exception as e:
            self._logger.error( "serialAccess Exception (1)" + str(e))
            self._logger.error("Could not flush input buffer")
            self._device.close()
            return False
        
        success = False
    	timeout = 5
    	while timeout and not success:
            timeout -= 1
    	    a = ''
    	    while (timeout and a != "$"):
                a = self._serialAccess(self._device,1,'r')
                if a == False:
                    timeout -= 1
    	    if (a == "$"):
                while (timeout and a != "GP"):
                    a = self._serialAccess(self._device,2,'r')
                    if a == False:
                        timeout -= 1
                while (timeout and a != "GG"):
                    a = self._serialAccess(self._device,2,'r')
                    if a == False:
                        timeout -= 1
                while (timeout and a != "A,"):
                    a = self._serialAccess(self._device,2,'r')
                    if a == False:
                        timeout -= 1
                a = self._serialAccess(self._device,100,'r')
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
        if (success):
            return (header[0], header[1], payload) #ID, class, payload
        else:
            self._logger.debug("readGpsMessage: returned nothing!")
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
    def _serialAccess(self,dev,data,mode):
        d = False
        fd = None
        try:
            self._device.open()
            if (mode == 'w'):
                self._device.write(data)
                self._device.close()
                return True
            elif (mode == 'r'):
                d = self._device.read(data)
                self._device.close()
                return d
            else:
                self._logger.error("serialAccess: Wrong mode specified")
                self._device.close()
                return False
        except Exception as e:
            self._device.close()
            self._logger.error( "serialAccess Exception (1)" + str(e))
            self._logger.error("Could not access gps device!")
            return False

    '''
    ###########################################################################################
    returns true if it can remove the file
    ###########################################################################################
    '''
    def cleanUp(self,fd,dev):
		pid = os.getpid()
		ret = commands.getstatus(dev)
		self._logger.debug("cleanUp: " + str(ret))

		if (len(ret)):
			ret = commands.getoutput('rm -r ' + str(dev))
			self._logger.debug("Deleting stray file " + str(fd))
			ret = commands.getoutput('rm -r /proc/' + str(pid) + '/fd/' + str(fd))
			if (len(ret)):
				return True
			return False

    '''
    ##########################################################################################
    _calculateChecksum()
    ##########################################################################################
    '''
    def _calculateChecksum(self, msg, chksum=None):
        ck_a = 0
        ck_b = 0

        for i in msg:
            ck_a += ord(i)
            ck_b += ck_a
        
        ck_a &= 0xff
        ck_b &= 0xff
        
        if (chksum == None):
            return (ck_a, ck_b)
        
        return ((ck_a == chksum[0]) and (ck_b == chksum[1]))

    '''
    ##########################################################################################
    _config_device()
    ##########################################################################################
    '''
    '''
    def _config_device(self):
    	#change = False        
    
    	#########################################
        #set Message type depending on mode
    	#########################################
        self._logger.debug("Setting message type...")
    	newport = struct.pack('20B',0x03,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x03,0x00,0x03,0x00,0x00,0x00,0x00,0x00)

        want=struct.unpack('20B',newport)
        self._logger.info("New Protocol: " + str(want))
        cnt=0
        ACK=0
        while not ACK and cnt<=3:
            self._write(self._prtMessageId,newport)
            ACK=self._readRaw(self._ACK)
            cnt=cnt+1
            if cnt==3:
                self._logger.error("New port could not be sent!")
                break
            self._runEv.wait(1)
        self._logger.info("Done setting new protocol type...")
            
        ###################################
        #set the measurement rate
        ##################################
        self._logger.info("Setting rate...")
        newrate=struct.pack('3H', self._interval*1000, 0x01,0x01)
        cnt=0
        ACK=0
        while not ACK and cnt<=3:
            self._write(self._rateMessageId, newrate)
            ACK=self._readRaw(self._ACK)
            cnt=cnt+1
            if cnt==3:
                self._logger.error("New measurement rate could not be sent!")
                break
            self._runEv.wait(1)
        self._logger.info("Done setting rate.")
    
    	##################################################
    	#Set mode
    	#IMPORTANT: Next 3 statements only for LEA-6
    	##################################################
    	self._logger.info("Setting nav mode on USB...")
        newOutput = struct.pack('8B',0xF0,0x00,0x00,0x00,0x00,0x01,0x00,0x01) 
        cnt=0
        ACK=0
        while not ACK and cnt<=3:
            self._write(self._msgMessageId,newOutput)
            ACK=self._readRaw(self._ACK)
            cnt=cnt+1
            if cnt==3:
                self._logger.error("NAV mode could not be sent!")
                break
            self._runEv.wait(1)
            
    	self._logger.info("Done setting nav mode on USB...")
    
        return True
    '''
    
    def _config_device(self):
        #change = False        
    
        #########################################
        #set Message type depending on mode
        #########################################
        self._logger.info("Setting message type...")
        newport = struct.pack('19B',0x03,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x03,0x00,0x03,0x00,0x00,0x00,0x00,0x00)

        #cnt=0
        #old = ''
        #while (not old and cnt<=3):
        #    old = self._pollGpsMessage(self._prtMessageId)
        #    cnt=cnt+1
        #    if cnt==3:
        #        self._logger.error('New configuration could not be sent!')
        #        return False
        #rec=struct.unpack('20B',old[2])
        #want=struct.unpack('19B',newport)
        #prtchange=False
        #if want[0]!=rec[0]:
        #    prtchange=True
        #    self._logger.error('Wrong Portnumber')
        #for i in range(3,19):
        #    if want[i]!=rec[i+1]:
        #        prtchange=True
        #        if self._logger.isEnabledFor(logging.DEBUG):
        #            self._logger.debug('Bit %d from prtMessage not matching %s instead of %s' % (i, rec[i], want[i]))
                
        #if prtchange==True:
        #    self._logger.info('Port protocols changed')
        #    self._logger.info("New Protocol: %s" % (want,))
        cnt=0
        ACK=0
        while not ACK and cnt<=3:
            self._write(self._prtMessageId,newport)
            ACK=self._readRaw(self._ACK)
            cnt=cnt+1
            if cnt==3:
                self._logger.error('New configuration could not be sent!')
                break
            self._runEv.wait(1)
        #change=True
        self._logger.info("Done setting message type...")
    
        ###################################
        #set the measurement rate
        ##################################
        self._logger.info("Setting rate...")
        newrate=struct.pack('3H', self._interval*1000, 0x01,0x01)
        #rate = False
        #cnt=0
        #while (not rate and cnt<=3):
        #    rate = self._pollGpsMessage(self._rateMessageId)
        #    cnt=cnt+1
        #    if cnt==3:
        #        self._logger.error('New configuration could not be sent!')
        #        return False
        #if rate[2] != newrate:
        #    if self._logger.isEnabledFor(logging.DEBUG):
        #        self._logger.info('Rate changed from %d to %d' % (struct.unpack('3H',rate[2])[0], self._interval*1000))
        cnt=0
        ACK=0
        while not ACK and cnt<=3:
            self._write(self._rateMessageId, newrate)
            ACK=self._readRaw(self._ACK)
            #self._logger.info(ACK)
            cnt=cnt+1
            if cnt==3:
                self._logger.error('New configuration could not be sent!')
                break
            self._runEv.wait(1)
        change=True
        self._logger.info("Done setting rate.")
    
        ##################################################
        #Set mode
        #IMPORTANT: Next 3 statements only for LEA-6
        ##################################################
        self._logger.info('Setting NAV USB...')
        newOutput = struct.pack('10B',0x08,0x00,0xF0,0x00,0x00,0x00,0x00,0x01,0x00,0x01) 
        #self._write(self._msgMessageId,self._messageId)
        #old = self._pollGpsMessage(self._msgMessageId)
        #while (old == None):
        #    old = self._readRaw(self._msgMessageId)
        #    if old!=newOutput:
        #        self._logger.info('Output message changed')
        #        self._logger.info("Msg Port Mode: %s" % (newOutput,))
        cnt=0
        ACK=0
        while not ACK and cnt<=3:
            self._write(self._msgMessageId,newOutput)
            ACK=self._readRaw(self._ACK)
            cnt=cnt+1
            if cnt==3:
                self._logger.error('New configuration could not be sent!')
                break
            self._runEv.wait(1)
        #        change=True
            
        self._logger.info('Done setting NAV USB...')
    
        
        #Save configuration on Flash memory of LEA-6T
        #if change==True:
        #    savecfg=struct.pack('13B',0x00,0x00,0x00,0x00,0xFF,0xFF,0x00,0x00,0x00,0x00,0x00,0x00,0x07)
        #    self._logger.info('Saving new GPS configuration')
        #    cnt=0
        #    ACK=0
        #    while not ACK and cnt<=3:
        #        self._write(self._saveMessageId,savecfg)
        #        ACK=self._readRaw(self._ACK)
        #        cnt=cnt+1
        #        if cnt==3:
        #            self._logger.error('New configuration could not be saved!')
        #        self._runEv.wait(1)
        #    self._logger.info("Done saving")
        return True