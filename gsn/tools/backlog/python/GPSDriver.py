
__author__      = "Ben Buchli <bbuchli@ethz.ch"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Ben Buchli"
__license__     = "GPL"
'''
backlog imports


import BackLogMessage
from AbstractPlugin import AbstractPluginClass
'''
'''
stdlib imports
'''
import struct
#from time import gmtime, time, strftime
import time
import serial
from threading import Event
import commands
import os
import logging
import scanlinux
'''
defines
'''

#Reads raw GPS messages from a u-blox device and sends them to GSN
class GPSDriver():
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
    	# RAW 
    	#
        # init GPS message headers using RXM type
        # header format: sync char 1 | sync char 2
        #                   0xb5     |    0x62
        self._gpsHeader = struct.pack('2B', 0xB5, 0x62)
        
        # This is the raw message output we want to read
    	if (config[2] == "raw"):
            self._messageId = struct.pack('2B', 0x02, 0x10)
    	elif (config[2] == "nav"):
    		self._messageId = struct.pack('2B',0x01,0x06)
    	else:
    		self._logger.info("No gps_mode specified!")
    	self._mode = config[2]        
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
    	
        if (config[0] != None): #Device string
    		self._deviceStr = config[0]
    		self._device = self._deviceStr
    	if (config[1] != None): # The measurement interval in seconds
    		self._interval = config[1]

    	#in case we can't find a device on given port
    	self.retries = 10
    	
    	# serial port timeout
    	self._serialTimeout = 1
    	
    	#STATS
    	self._serialCount = 0
    	self._zombiesKilled = 0
        self._measurementNo=0
        self._goodSatelliteCounter=0
        self._SatelliteCounter=0
           
        self._runEv = Event()
    	self._initialized = False
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
	if (self._mode == "raw"):
		return self._readRaw(msgId)
	else:
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
                self._logger.info("pollGpsMessage: sending gps message didn't succeed")
                return False
        d = self._readRaw(msgId)
        if (d == False or len(d) == 0):
                self._logger.info("pollGpsMessage: reading gps message didn't succeed")
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
            #self._logger.debug("readGpsMessage: Returned payload")
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
    	success = False
    	timeout = 5
    	while timeout and not success:
    		timeout -= 1
    		a = ''
    		while (a != "$"):
    			a = self._serialAccess(self._device,1,'r')
    		if (a == "$"):
    			while (a != "GP"):
    				a = self._serialAccess(self._device,2,'r')
    				#print(str(a))
    			while (a != "GG"):
    				a = self._serialAccess(self._device,2,'r')
    			while (a != "A,"):
    				a = self._serialAccess(self._device,2,'r')
    			a = self._serialAccess(self._device,100,'r')
    			b = a.split('\r')[0]
    			b = b.split(',')
    			header = ["$GPGGA", "0xF0 0x00"] 
    			payload = b 
    			success = True
    			break
    		else:
    			success = False
    	if (success):
    		#self._logger.debug("readGpsMessage: Returned payload")
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
	#self._logger.info("Opening " + str(dev))
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
			self._logger.info("serialAccess: Wrong mode specified")
			self._device.close()
			return False
	except Exception as e:
		self._logger.debug( "serialAccess Exception (1)" + str(e))
		dev = scanlinux.scan()
		while (dev == ''):
			dev = scanlinux.scan()
		for i in range(0, len(dev)):
			if (len(dev[i])):
				dev = dev[i]
				break
                if (len(dev)):
		       self._logger.debug("Found new device: " + str(dev))
	 	else:
			if (self.retries > 0):
				self.retries -= 1
				self._logger.debug("No device found, trying again!")
				self._runEv.wait(1)
				return self._serialAccess(dev,data,mode)
			else:
				self._logger.info("No device found... Giving up!!")
				exit()
		self._deviceStr = dev
		try:
			self._device = serial.Serial(dev, 19200, timeout=self._serialTimeout)
			self._logger.debug("Successfully opened " + str(self._device))
			return self._serialAccess(dev,data,mode)
		except Exception as e:
			self.cleanUp(fd,dev)
			self._logger.debug("serialAccess Exception (2)" + str(e))
			return False

    '''
    ###########################################################################################
    returns true if it can remove the file
    ###########################################################################################
    '''
    def cleanUp(self,fd,dev):
		pid = os.getpid()
		print(dev)
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
    def _config_device(self):
    	change = False        
    
    	#########################################
        #set Message type depending on mode
    	#########################################
        self._logger.debug("Setting message type...")
    	if (self._mode == "nav"):
    		newport = struct.pack('19B',0x03,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x03,0x00,0x03,0x00,0x00,0x00,0x00,0x00)
    	else:
            newport=struct.pack('19B',0x03,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x03,0x00,0x01,0x00,0x00,0x00,0x00,0x00)
            ret = self._write(self._prtMessageId, struct.pack('b',3))
    	old = ''
    	while (not old):
            old = self._pollGpsMessage(self._prtMessageId)
            rec=struct.unpack('20B',old[2])
            want=struct.unpack('19B',newport)
            prtchange=False
            if want[0]!=rec[0]:
                prtchange=True
                self._logger.debug('Wrong Portnumber')
            for i in range(3,19):
                if want[i]!=rec[i+1]:
                    prtchange=True
                    self._logger.debug('Bit '+str(i)+' from prtMessage not matching ' +str(rec[i]) + ' instead of ' +str(want[i]))
                    
            if prtchange==True:
                self._logger.debug('Port protocols changed')
                self._logger.info("New Protocol: " + str(want))
                cnt=0
                ACK=0
                while not ACK and cnt<=3:
                    self._write(self._prtMessageId,newport)
                    ACK=self._readRaw(self._ACK)
                    cnt=cnt+1
                    if cnt==3:
                        self._logger.info('New configuration could not be sent!')
                        break
                    self._runEv.wait(1)
                change=True
            self._logger.debug("Done setting message type...")       
    
            ###################################
            #set the measurement rate
            ##################################
            self._logger.debug("Setting rate...")
            newrate=struct.pack('3H', self._interval*1000, 0x01,0x01)
            rate = False
            while (not rate):
                rate = self._pollGpsMessage(self._rateMessageId)
            if rate[2] != newrate:
                self._logger.debug('Rate changed from ' + str(struct.unpack('3H',rate[2])[0]) + " to " + str(self._interval*1000))
                cnt=0
                ACK=0
                while not ACK and cnt<=3:
                    self._write(self._rateMessageId, newrate)
                    ACK=self._readRaw(self._ACK)
                    #self._logger.info(ACK)
                    cnt=cnt+1
                    if cnt==3:
                        self._logger.info('New configuration could not be sent!')
                    self._runEv.wait(1)
                change=True
            self._logger.debug("Done setting rate.")
    
    	##################################################
    	#Set mode
    	#IMPORTANT: Next 3 statements only for LEA-6
    	##################################################
    	self._logger.debug("Setting " + self._mode + " on USB...")
    	if (self._mode == "raw"):
            newOutput=struct.pack('8B',0x02,0x10,0x00,0x00,0x00,0x01,0x00,0x00)
    	elif (self._mode == "nav"):
            newOutput = struct.pack('10B',0x08,0x00,0xF0,0x00,0x00,0x00,0x00,0x01,0x00,0x01) 
            self._write(self._msgMessageId,self._messageId)
    	old = self._pollGpsMessage(self._msgMessageId)
    	while (old == None):
            old = self._readRaw(self._msgMessageId)
            if old!=newOutput:
                self._logger.debug('Output message changed')
                self._logger.info("Msg Port Mode: " + str(newOutput))
                cnt=0
                ACK=0
                while not ACK and cnt<=3:
                    self._write(self._msgMessageId,newOutput)
                    ACK=self._readRaw(self._ACK)
                    cnt=cnt+1
                    if cnt==3:
                        self._logger.info('New configuration could not be sent!')
                    self._runEv.wait(1)
                change=True
            
    	self._logger.debug("Done setting " + self._mode + " on USB...")
    
    	self._logger.debug("WARNING! NOT setting Powermode!!")
    	'''
    	self._logger.debug("Setting Powermode...")
            newPowerMode=struct.pack('2B',0,1)
            if self._pollGpsMessage(self._rxmMessageId)[2]!= newPowerMode:
                self._logger.debug('PowerMode changed')
                cnt=0
                ACK=0
                while not ACK and cnt<3:
                    self._write(self._rxmMessageId,newPowerMode)
                    ACK=self._readRaw(self._ACK)
                    cnt=cnt+1
                    if cnt==3:
                        self._logger.info('New configuration could not be sent!')
                    self._runEv.wait(1)
                change=True
            
            newPSM=struct.pack('24B',0,0x3C,0,0,0x10,0,0,0,0x88,0x13,0,0,0xE8,0x03,0,0,0,0,0,0,0,0,0,0)
            if self._pollGpsMessage(self._pmMessageId)[2]!=newPSM:
                self._logger.debug('Power Save Mode settings changed')
                cnt=0
                ACK=0
                while not ACK and cnt<=3:
                    self._write(self._pmMessageId,newPSM)
                    ACK=self._readRaw(self._ACK)
                    cnt=cnt+1
                    if cnt==3:
                        self._logger.info('New configuration could not be sent!')
                    self._runEv.wait(1)
                change=True
    
    	self._logger.debug("Done setting Powermode")
    	'''
    	
        #Save configuration on Flash memory of LEA-6T
    	self._logger.debug("Saving changes (if any) to flash")
        if change==True:
            savecfg=struct.pack('13B',0x00,0x00,0x00,0x00,0xFF,0xFF,0x00,0x00,0x00,0x00,0x00,0x00,0x07)
            self._logger.info('Saving new GPS configuration')
            cnt=0
            ACK=0
            while not ACK and cnt<=3:
                self._write(self._saveMessageId,savecfg)
                ACK=self._readRaw(self._ACK)
                cnt=cnt+1
                if cnt==3:
                    self._logger.info('New configuration could not be saved!')
                self._runEv.wait(1)
        self._logger.debug("Done saving")
        return True
