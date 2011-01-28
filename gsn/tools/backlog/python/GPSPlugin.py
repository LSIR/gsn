
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
from SpecialAPI import PowerControl
'''
defines
'''

DEFAULT_BACKLOG = True

NAV_TYPE = 1
RAW_TYPE = 2

#Which conversion must be applied to this data at the GSN end? 0 -> NONE, 1 -> RAW->ASCII
RAW_DATA_VERSION = 1
NAV_DATA_VERSION = 1

#Reads raw GPS messages from a u-blox device and sends them to GSN

class GPSPluginClass(AbstractPluginClass):
    '''
    ##########################################################################################
    __init__
    ##########################################################################################
    '''
    
    def __init__(self, parent, config):
    
    	AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG)
    	self.power = PowerControl()
        self.info('Init GPSPlugin...')
        self._progStart = time.time()
        self._stopped = False
        self._busy = True
        
        # The measurement interval in seconds
        self._interval = float(self.getOptionValue('poll_interval'))
    	# The measurement time in seconds
        self._measTime = int(self.getOptionValue('measurement_time'))
        # The device identifier
        self._deviceStr = self.getOptionValue('gps_device')
        # The GPS Mode (nav or raw)
        self._mode = self.getOptionValue('gps_mode')
    	
        #For quality measurement
        self._measurementNo=0
        self._goodSatelliteCounter=0
        self._SatelliteCounter=0
           
        self._runEv = Event()
    
    	self._endTime = time.time() + self._measTime	
        
        self.registerTOSListener()
    	
        self.debug("Done init GPS Plugin")


    '''
    ##########################################################################################
    action():   This function will be fired by the schedule handler each time
                this plugin is scheduled. The function is started in a new
                thread.
        
                @param parameters:  The parameters as one string given in the
                                    schedule file.
    ##########################################################################################
    '''
    def action(self,parameters):
        self.info("Action called!")
        self.runPlugin(parameters)
        
    '''
    ##########################################################################################
    run():     This function gets called when the plugin is loaded. In duty-cycle mode the
               action() function takes care of running the plugin
    ##########################################################################################
    '''
    def run(self):
        if (self.isDutyCycleMode()):
            return
        else:
            self.runPlugin('')
 
    '''
    ##########################################################################################
    runPlugin():  This function is essentially the run function
    ##########################################################################################
    '''
    def runPlugin(self,param):  
        self.gps = GPSDriver.GPSDriver([self._deviceStr, self._interval,self._mode])
        
        if (not self.gps._isInitialized()):
            self.error('Initializing GPS Plugin failed!!')
            return
        
        self.info('GPSPlugin running...')
    
        # Prepare for precise timing
        now = time.time()
        while time.time() <= self._endTime and not self._stopped:
            # Read that GPS RAW message
            rawMsg = self.gps._read("")
            self.info(str(rawMsg))
            if (rawMsg):
                # Parse the raw message
                '''
                if (self._mode == "nav"):
                    dataPackage = self._parseNavMsg(rawMsg)
                elif (self._mode == "raw"):
                    dataPackage = self._parseRawMsg(rawMsg)
                '''
                if (self._mode == "nav"):
                    #self.processMsg(self.getTimeStamp(), [NAV_TYPE, NAV_DATA_VERSION]+rawMsg[2])
                    pass
                elif (self._mode == "raw"):
                    self.processMsg(self.getTimeStamp(), [RAW_TYPE, RAW_DATA_VERSION, rawMsg[2]])
            self._runEv.wait(self._interval-1)

        # die...

        self.debug('GPSPlugin died...')
    
        end = time.time()
        self.info('Number of measurements: ' + str(gps._measurementNo))
        self.info('Number of satellites: '+ str(gps._SatelliteCounter))
        self.info('Number of satellites above thresholds: '+str(gps._goodSatelliteCounter))
        self.info("Killed " + str(gps._zombiesKilled))
        self.info("Serial disconnected " + str(gps._serialCount))
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
            gps_time, gps_week, svs = struct.unpack('<ihB', payload[0:7])
            self.info('GPS Time: '+str(gps_time)+" - "+str(gps_week))
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
                
                if quality>=int(self.getOptionValue('quality_threshold')) and cno>=int(self.getOptionValue('signal_threshold')):
                      self._goodSatelliteCounter=self._goodSatelliteCounter+1
                self._SatelliteCounter=self._SatelliteCounter+1
    			
    		if (dataPackage == False):
    			self.warning("parseMsg: WARNING! Function returned nothing!")
    			return dataPackage
    		self._measurementNo += 1
    		return dataPackage
        else:
            self.warning("parseMsg: WARNING! MSG packet was empty!")
            return False

    '''
    ##########################################################################################
    parseNavMsg()
    ##########################################################################################
    '''
    def _parseNavMsg(self, msg):
    	if (msg):
            self.info(str(msg[2]))
            #dataPackage = [NAV_TYPE, ]
    	else:
    		self.warning("WARNING: MSG packet was empty!")
        
    '''
    ##########################################################################################
    stop()
    ##########################################################################################
    '''
    def stop(self):
        self.info('GPSPlugin stopping...')
        self._stopped = True
        self._busy = False
        self.deregisterTOSListener()
        self.info('GPSPlugin stopped')

    '''
    ##########################################################################################
    getMsgType()
    ##########################################################################################
    '''
    
    def getMsgType(self):
        return BackLogMessage.GPS_MESSAGE_TYPE
    
    '''
    ##########################################################################################
    isBusy(): This function is required to let the parent know we're still alive and busy.
    ##########################################################################################
    '''
    def isBusy(self):
        return self._busy
        
        
    def needsWLAN(self):
        # TODO: implement return value
        return False
