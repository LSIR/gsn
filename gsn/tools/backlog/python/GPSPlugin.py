
__author__      = "Ben Buchli <bbuchli@ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"
'''
backlog imports
'''

import BackLogMessage
from AbstractPlugin import AbstractPluginClass

'''
stdlib imports
'''
import struct
#from time import gmtime, time, strftime
import time
from threading import Event
import GPSDriver
'''
defines
'''

DEFAULT_BACKLOG = True

#Reads raw GPS messages from a u-blox device and sends them to GSN

class GPSPluginClass(AbstractPluginClass):
    '''
    ##########################################################################################
    __init__
    ##########################################################################################
    '''
    
    def __init__(self, parent, config):
    
    	AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG)
    	
        self.info('Init GPSPlugin...')
        self._progStart = time.time()
        self._stopped = False
    
        # The measurement interval in seconds
        self._interval = float(self.getOptionValue('poll_interval'))
    	# The measurement time in seconds
        self._measTime = int(self.getOptionValue('measurement_time'))
    	# serial port timeout
    	self._serialTimeout = 1
    	self._serialCount = 0
    	self._zombiesKilled = 0
    	
    	self._deviceStr = self.getOptionValue('gps_device')
    	self._mode = self.getOptionValue('gps_mode') 
    	self.gps = GPSDriver.GPSDriver([self._deviceStr, self._interval,self._mode])
    
        #For quality measurement
        self._measurementNo=0
        self._goodSatelliteCounter=0
        self._SatelliteCounter=0
           
        self._runEv = Event()
    
    	self._endTime = time.time() + self._measTime	
        self.registerTOSListener()
    	self.debug("Done init")


    '''
    ##########################################################################################
    run()
    ##########################################################################################
    '''
    def run(self):
	
        self.info('GPSPlugin running...')
	
        self._measurementNo = 1

        # Prepare for precise timing
        now = time.time()
        while time.time() <= self._endTime and not self._stopped:
            # Read that GPS RAW message
            rawMsg = self.gps._read("")
    	    # Parse the raw message
    	    if (self._mode == "nav"):
    		    dataPackage = self._parseNavMsg(rawMsg)
    	    elif (self._mode == "raw"):
    		    dataPackage = self._parseRawMsg(rawMsg)
    	    self._runEv.wait(self._interval-1)

        # die...

        self.debug('GPSPlugin died...')
	
        end = time.time()
        self.info('Number of measurements: ' + str(self._measurementNo))
        self.info('Number of satellites: '+ str(self._SatelliteCounter))
        self.info('Number of satellites above thresholds: '+str(self._goodSatelliteCounter))
        self.info("Killed " + str(self._zombiesKilled))
        self.info("Serial disconnected " + str(self._serialCount))
        self.stop()
 
    '''
    ##########################################################################################
    parseRawMsg()
    ##########################################################################################
    '''
    def _parseRawMsg(self, rawMsg):
    	if rawMsg!=0:   
    		dataPackage = False
    		payload = rawMsg[2]
    		# the first 8 bytes are: GPS Time (4B), GPS week (2B), Number of satellites following (1B), Reserved (1B)
    		payloadHeader = struct.unpack('ih2B', payload[0:8])
    		self.info('GPS Time: '+str(payloadHeader[0])+":"+str(payloadHeader[1]))
    		self.info('Number of satellites following: ' +str(payloadHeader[2]))
    		# extract data for each SV 
    		for i in range(0, payloadHeader[2]):
    			#Byte offset for SV data
    			startIndex = 8+i*24
    			#Payload format: Carrier Phase (8B - double), Pseudorange (8B - double), 
    			#                Doppler (4B - int), SV nbr (1B - unsigned char), 
    			#                Quality (1B - signed char), C/No (1B - signed char), 
    			#                LLI (1B - unsigned char)
    		
    			# We have to parse the meas-quality and signal strength fields because java does not support the corresponding data types
    			tmpData = struct.unpack('2b', payload[(startIndex+21):(startIndex+23)])                
    			dataPackage = payload[0:6] + payload[startIndex:(startIndex+21)] + struct.pack('2H', tmpData[0], tmpData[1]) + payload[(startIndex+23)]
    			
    			#if tmpData[0]>=int(self.getOptionValue('quality_threshold')) and tmpData[1]>=int(self.getOptionValue('signal_threshold')):
    			#      self._goodSatelliteCounter=self._goodSatelliteCounter+1
    			self._SatelliteCounter=self._SatelliteCounter+1
    			
    		if (dataPackage == False):
    			self.debug("parseMsg: WARNING! Function returned nothing!")
    			return dataPackage
    		self._measurementNo += 1
    		return dataPackage
        self.debug("parseMsg: WARNING! MSG packet was empty!")
    	return False

    '''
    ##########################################################################################
    parseNavMsg()
    ##########################################################################################
    '''
    def _parseNavMsg(self, msg):
    	if (msg):
            self.info(str(msg[2]))
            ''' 
    		payload = msg[2]
    		fixtype= ["No Fix", "Dead Reckoning Only", "2D Fix", "3D Fix", "GPS + DR", "Time only"]
    		# the first 8 bytes are: GPS Time (4B), fractional time (4B), week (2B)
    		p = struct.unpack('LlhB3lL3lL', payload[0:44])
    		sats = struct.unpack('B', payload[47:48])
    		print("###################################")
    		self.info('GPS Time: '+str(p[0])+":"+str(p[2]))
    		self.info('Fix: ' +str(fixtype[p[3]]))
    		self.info('X,Y,Z: ' + str(p[4]) + "," + str(p[5]) + "," + str(p[6])+"cm")
    		self.info('Acc.: ' + str(p[7]) + "cm")
    		self.info('Vel. X,Y,Z: ' + str(p[8]) + "," + str(p[9]) + "," + str(p[10]) + "cm/s")
    		self.info('Acc.: ' + str(p[11]) + "cm/s")
    		self.info('Num SVs: ' + str(sats[0]))
    		'''
    	else:
    		self.info("WARNING: MSG packet was empty!")
        
    '''
    ##########################################################################################
    stop()
    ##########################################################################################
    '''
    def stop(self):
        self.debug('GPSPlugin stopping...')
        self._stopped = True
        self.deregisterTOSListener()
        self.info('stopped')


    '''
    ##########################################################################################
    getMsgType()
    ##########################################################################################
    '''
    
    def getMsgType(self):
        return BackLogMessage.GPS_MESSAGE_TYPE
