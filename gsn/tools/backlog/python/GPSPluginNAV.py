
__author__      = "David Hasenfratz <hasenfratz@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2011, ETH Zurich, Switzerland"
__license__     = "GPL"
__version__     = "$Revision:  $"
__date__        = "$Date: 2011-02-14 10:58:41 +0100 (Mon, 14 Feb 2011) $"
__id__          = "$Id: GPSPluginNAV.py 2011-02-14 10:58:41Z hdavid $"
__source__      = "$URL: http://gsn.svn.sourceforge.net/svnroot/gsn/branches/permasense/gsn/tools/backlog/python/GPSPluginNAV.py $"
'''
backlog imports
'''

import BackLogMessage
from AbstractPlugin import AbstractPluginClass

'''
stdlib imports
'''
import struct
import copy
import time
import GPSDriverNAV
'''
defines
'''

DEFAULT_BACKLOG = True

NAV_TYPE = 1
RAW_TYPE = 2

#Reads raw or nav GPS messages from a u-blox device and sends them to GSN

class GPSPluginNAVClass(AbstractPluginClass):
    
    def __init__(self, parent, config):
	    
    	AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG)
	self._timer = None
	self._stopped = False
	self._interval = 5

        self.info('Init GPSPluginNAV...')
    
    	# serial port timeout
    	self._serialTimeout = 1
    	self._serialCount = 0
    	self._zombiesKilled = 0
    	
    	self._deviceStr = self.getOptionValue('gps_device')
    	self._mode = self.getOptionValue('gps_mode')
    	self.gps = GPSDriverNAV.GPSDriverNAV([self._deviceStr, self._interval,self._mode])
    
    	self.info("Done init")

    def getMsgType(self):
	return BackLogMessage.GPS_MESSAGE_TYPE

    def isBusy(self):
	return False

    def needsWLAN(self):
	return False

    def run(self):
	
        self.info('GPSPluginNAV running...')
	#self.action('')	

    def action(self, parameters):

	self.info('GPSPluginNAV started...')
	
        # Read GPS message
        gpsMsg = self.gps._read("")
    	# Parse message
    	if (self._mode == "nav"):
    	    dataPackage = self._parseNavMsg(gpsMsg)
    	elif (self._mode == "raw"):
    	    dataPackage = self._parseRawMsg(gpsMsg)
        self.processMsg(self.getTimeStamp(), dataPackage)

	self.info('GPS reading done')

    def stop(self):
	self._stopped = True
	self.info('stopped')

    def _parseRawMsg(self, rawMsg):
    	if rawMsg!=0:   
            dataPackage = False
            payload = rawMsg[2]
            # the first 8 bytes are: GPS Time (4B), GPS week (2B), Number of satellites following (1B), Reserved (1B)
            gps_time, gps_week, svs = struct.unpack('<ihB', payload[0:7])
            self.info('GPS Time: '+str(gps_time)+":"+str(gps_week))
            self.info('Number of satellites following: ' +str(svs))
            # extract data for each SV 
            for i in range(0, svs):
                #Byte offset for SV data
                startIndex = 8+i*24
                #Payload format: Carrier Phase (8B - double), Pseudorange (8B - double), 
                #                Doppler (4B - int), SV nbr (1B - unsigned char), 
                #                Quality (1B - signed char), C/No (1B - signed char), 
                #                LLI (1B - unsigned char)
                
                carrier_phase, pseudorange, doppler, sv, quality, cno, lli = struct.unpack_from('<2diB2bB', payload, startIndex)
                
                dataPackage = [RAW_TYPE, gps_time, gps_week, svs, carrier_phase, pseudorange, doppler, sv, quality, cno, lli]
                
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

    def _parseNavMsg(self, msg):
    	if (msg):

	    dataPackage = [NAV_TYPE]
	    for i in range(0, len(msg[2])):
		dataPackage += [msg[2][i]]

	    self.info(dataPackage)
    	    return dataPackage

    	else:
    	    self.info("WARNING: MSG packet was empty!")
        
