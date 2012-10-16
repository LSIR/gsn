# -*- coding: UTF-8 -*-
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
from time import gmtime, time, strftime
from threading import Event, Thread
from SpecialAPI import PowerControl, Statistics
import os.path
import serial
import logging


from ScheduleHandler import SUBPROCESS_BUG_BYPASS
if SUBPROCESS_BUG_BYPASS:
    import SubprocessFake
    subprocess = SubprocessFake
else:
    import subprocess

'''
defines
'''

DEFAULT_BACKLOG = True

NAV_TYPE = 1
RAW_TYPE = 2
#Which conversion must be applied to this data at the GSN end? 0 -> NONE, 1 -> RAW->ASCII
RAW_DATA_VERSION = 1
NAV_DATA_VERSION = 1
'''
TODO
-log all exceptions with self.exception(string)
-what to do if gps can't be inited
-Override the default config file values if provided in backlog config
'''
#Reads raw GPS messages from a u-blox device and sends them to GSN
class GPSPluginClass(AbstractPluginClass):
    '''
    ##########################################################################################
    __init__
    ##########################################################################################
    '''
    def __init__(self, parent, config):
    	AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG, needPowerControl=True)
    	self._runEv = Event()
    	self.name = 'GPSPlugin-Thread'
        self.info('Init GPSPlugin...')
        self._stopped = False
        self._busy = True
        
        #How often should driver be restarted in case of problems
        self._DriverRestarts = 4
        
        # The measurement interval in seconds
        self._interval = int(self.getOptionValue('poll_interval'))
            
        self.isDutyCycled = self.isDutyCycleMode()
	  
        # The device identifier
        self._deviceStr = self.getOptionValue('gps_device')
        # GPS configuration file as produced by u-center
        self._config_file = str(self.getOptionValue('gps_config_file'))
        self._cnt_file = str(self.getOptionValue('cnt_file'))
	self._stay_online = str(self.getOptionValue('stay_online_for_db_resend'))
	self._dc_wlan = int(self.getOptionValue('dc_wlan'))
	self._WlanThread = None
	if (self._dc_wlan): # and self.isDutyCycled):
	  self.info("WLAN duty-cycle enabled!")
	  wlan_ontime = int(self.getOptionValue('wlan_on_time'))
	  wlan_offtime = int(self.getOptionValue('wlan_off_time'))
	  #Wlan must only be cycled when in dc mode!!
          self._WlanThread = WlanThread(self,wlan_ontime, wlan_offtime,self._stay_online)
        else:
	  self.info("WLAN duty-cycle disabled!")
	  
        #if count file does not exist, create it!
        if (not os.path.exists(str(self._cnt_file))):
	  self.debug("GPS count file does not exist! Creating it!")
	  try:
	    self._fp = open(str(self._cnt_file),"r+")
	    self._fp.write('0')
	    self._fp.close()
	  except Exception as e:
	    self.debug("GPS count file could not be created!!\n" + str(e))
	    self.error("GPS count file could not be created!!\n" + str(e))
	    self.stop()
	    return None
        try:
	  self._fp = open(str(self._cnt_file),"r+")
	  cnt = int(self._fp.readline())
	  if (cnt >= (pow(2,32)-1) or cnt == ""):
            self._logger.warning("Sample counter wrapped around! %s" % (cnt,))
            cnt = 0
          self._stats = Statistics() 
          self.info("Starting with %d" % (cnt,))
	  self._counterID = self._stats.createCounter(0,cnt) 
	  self._fp.close()
        except Exception as e:
            self.exception( "Could not open sample count file: %s %s" % (self._cnt_file, e))
            self.info("Could not open sample count file! Exiting!")
            self.stop()
            return None
        
        self.gps = GPSDriver(self,[self._deviceStr, self._interval, self._config_file])

	if (self.gps == None or not self.gps._isInitialized() or self.gps._isStopped()):
            self.error('Initializing GPS Driver failed!!')
            self.stop()
            return None
	
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
        self.debug("Action called!")
	if (self._stopped):
	  self.warning("Action called but we're stopped!! Exiting...")
	  return
	  
        if (not self.isDutyCycled):
            self.warning("GPSPlugin's action called even though we are not in duty-cycle mode!")
            return
        if (not self.gps._isStopped()):
	  if (parameters != ""):
	    self.info("GPSPlugin's action called with paramaters:\n" + str(parameters))
	  else:
	    self.info("GPSPlugin's action called with no parameters!")
	  self.runPlugin(parameters)
	else:
	  self.info("Stopping GPSPlugin in action()")
	  self.stop()
        
    '''
    ##########################################################################################
    run():     This function gets called when the plugin is loaded. In duty-cycle mode the
               action() function takes care of running the plugin
    ##########################################################################################
    '''
    def run(self):
        if (self.isDutyCycled or self._stopped):
            return
        elif (not self._stopped):
            self.runPlugin('')
 
    '''
    ##########################################################################################
    runPlugin():  This function is essentially the run function
    ##########################################################################################
    '''
    def runPlugin(self,param):
    
        self.info('GPSPlugin running...')
        #if (self.isDutyCycled):  
	if (self._dc_wlan and not self._WlanThread.isAlive()):
            self._WlanThread.start()
        
        rawMsg = self.gps._readMSG()
        rawMsg = self.gps._readMSG()
        
        while (not self._stopped): 
	    if (self.gps is not None and self.gps._isInitialized()):
	      self.debug('Reading raw at %s' %(strftime("%H:%M:%S +0000", gmtime())))
	      rawMsg = self.gps._readMSG()

	      if (rawMsg):
		self._stats.counterAction(self._counterID)
		cnt = self._stats.getCounterValue(self._counterID)
		self.processMsg(self.getTimeStamp(), [RAW_TYPE, RAW_DATA_VERSION, cnt, ((rawMsg[4]-8)/24), bytearray(rawMsg[2])])
		self.writeToFile(cnt)
                self._DriverRestarts = 4
		self.debug('Got a sample @ %s' %(strftime("%H:%M:%S +0000", gmtime())))
		self.debug("GPS Sample Nr: %d" % (cnt,))
                self.debug("%d Satellites (%d Bytes)" %((rawMsg[4]-8)/24, rawMsg[4]))
              else:
		self.debug(str(rawMsg))
            else:
	      if (self._DriverRestarts <= 0):
		self.error("Restarting the driver did not help! Giving up!!")
		self.stop()
	      else:
		self._DriverRestarts -= 1
		self.warning("GPS receiver disappeared! Trying to restart Driver!")
		del self.gps
		self.gps = GPSDriver(self,[self._deviceStr, self._interval, self._config_file])
		if (self.gps is None or not self.gps._isInitialized() or self.gps._isStopped()):
		  self.warning('Initializing GPS Driver failed!!')
    
    '''
    ##########################################################################################
    stop()
    ##########################################################################################
    '''
    def stop(self):
        self.info('GPSPlugin stopping...')
        #self.writeToFile(self._cnt)
        #self._fp.close()
        try:
	  self.gps._stop()
	except Exception as e:
	  self.debug("GPSDriver already stopped\n" + str(e))
        self._runEv.set()
        #if (self.isDutyCycled and self._WlanThread != None):  
        if (self._WlanThread != None):
	    #self._WlanThread.join()
            self._WlanThread.stop()
        self._busy = False
        self._stopped = True
        self.info('GPSPlugin stopped')
    
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
    
    def writeToFile(self, val):
        try:
	  self._fp = open(str(self._cnt_file),"w")
	  self._fp.write(str(val))
	  self._fp.close()
	except Exception as e:
	  self.exception("Unable to write to GPS count file!\n" + str(e))
	  self.info("Unable to write to GPS count file!\n" + str(e))
      
'''
***********************************************************************************************
#Reads raw GPS messages from a u-blox device and sends them to GSN
***********************************************************************************************
'''
class GPSDriver():
    
    def __init__(self,parent,config):
        self._logger = logging.getLogger(self.__class__.__name__)

        self._logger.info('Init GPS Driver...')
    	
    	self._parent = parent
    	
    	self.pmEnable = None
    	self._interval = None
    	# config: [deviceStr, interval, config_file]
        if (config[0] != None): #Device string
    		self._deviceStr = config[0]
    	if (config[1] != None): # The measurement interval in seconds
    		self._interval = config[1]
        if (config[2] != None):
	    self._config_file = config[2]
    	
    	# serial port timeout
        self._serialTimeout = None
        #in case we can't find a device on given port
        self._initialSerialConnTries = 10
        self._serialConnTries = self._initialSerialConnTries
    	
    	#ubx gps header
        self._gpsHeader = struct.pack('2B', 0xB5, 0x62)  
        #raw messages
	self._messageId = struct.pack('2B', 0x02, 0x10)
           
        self.stopped = False
        self._initialized = False
        
        #handler to the serial interface
    	self.device = None
        self._runEv = Event()
        success = self.testDevice(self._deviceStr)
        while (not success and not self.stopped):
	  self._logger.debug("Testing device at " + str(self._deviceStr))
	  success = self.fixDevice()
	  self._runEv.wait(10)
	if (not success):
	  self._stop()
	  return None

    	#device is tested by config in that it tries to write to it.
    	self._initialized = self.config_device()
    	#reset serial timeout to 30!
    	self._serialTimeout = 30
    	return None

	
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
    def _readMSG(self):
	ret = self.readRaw()
	return ret
	
    def _stop(self):
      self.stopped = True
      self._initialized = False
      self._runEv.set()
      self._logger.info('GPSDriver stopped')
      return

    '''
    ##########################################################################################
    # _write(self,payload): writes payload appropriately prepended with sync chars & header, and
    # appended with checksums to the GPS device.
    ##########################################################################################
    '''
    def _write(self,com=None):
      try:
	success = False
        com = com.split("-")[2]
	com = com.split()
	checksums = self.getChecksum(com)
	command = self._gpsHeader
	for item in com:
	  command += struct.pack('B', int(item, 16))
	command += struct.pack('2B', int(checksums[0], 16), int(checksums[1],16))
	tries = 10
	while (not self.stopped and not success):
	  success = self.serialAccess(command,'w')
	  tries -= 1
	  #print "Trying to write " + str(10-tries)
	  if (tries == 0 and not success):
	    self._logger.warning("Unable to write to GPS")
	    return False
      except Exception as e:
	  self._logger.warning("GPSDriver._write: Unhandled Exception!\n" + str(e))
	  self._logger.debug(str(com))
	  return False
	  
      return success
    
    '''
    ##########################################################################################
    _isInitialized(): 
    ##########################################################################################
    '''
    def _isInitialized(self):
        return self._initialized
    
    '''
    ##########################################################################################
    _isStopped(): 
    ##########################################################################################
    '''
    def _isStopped(self):
      return self.stopped
    
    '''
    #########################################################################
    #PRIVATE FUNCTIONS
    #########################################################################
    '''
    
    '''
    #########################################################################
    #readRaw(): reads RAW UBX data
    #########################################################################
	'''
    def readRaw(self):
    	
	success = False
	self._logger.debug('Entered _readRaw @ %s ' %(strftime("%H:%M:%S +0000", gmtime())))

	while not self.stopped and not success:
	    
	  # Wait for the Header
	  head = self.serialAccess(1,'r')
	  while not self.stopped and (not head or (len(head) == 1 and ord(head) != 0xB5)):
	      head = self.serialAccess(1,'r')
	  msg = self.serialAccess(1,'r')
	  
	  if msg:
	    if len(msg)==1 and ord(msg) == 0x62:
	      self._logger.debug('Got a GPS Header @ %s ' %(strftime("%H:%M:%S +0000", gmtime())))
	      # Got a message! :)  Read 2 bytes to determine class & id
	      recMsgId = ""
	      while (not self.stopped and not recMsgId):
		      recMsgId = self.serialAccess(2,'r')
	      # Is it the right Msg Type? 
	      if (recMsgId == self._messageId):	
		  self._logger.debug('Got a GPS Sample @ %s ' %(strftime("%H:%M:%S +0000", gmtime())))
		  header = struct.unpack('2B', recMsgId)
		  rawPayloadLength = self.serialAccess(2,'r')
		  while (not self.stopped and ( not rawPayloadLength or (len(rawPayloadLength) != 2))):
		    rawPayloadLength = self.serialAccess(2,'r')
		  payloadLength = struct.unpack('H', rawPayloadLength)[0]
		  payload = self.serialAccess(payloadLength,'r')
		  while (not self.stopped and not payload):
		    payload = self.serialAccess(payloadLength, 'r')
		  if (len(payload) != payloadLength):
		    self._logger.debug("Expected " + payloadLength + "bytes, but got " + len(payload))
		  ck = self.serialAccess(2,'r')
		  while not self.stopped and ((ck == False) or (len(ck) != 2)):
		      ck = self.serialAccess(2,'r')
		  
		  try:
		    submitChecksum = struct.unpack('2B', ck)
		    calculatedChecksum = self.verifyChecksum(recMsgId + rawPayloadLength + payload, submitChecksum)
		  except Exception as e:
		    self._logger.debug("Exception in computing checksum: " + str(e))
		    calculatedChecksum = False
		  
		  if(calculatedChecksum): 
		      success = True
		  else:
		      self._logger.warning('The submitted checksum did not match the expected one')
		      self._logger.debug('Expected: %s got: %s' % (calculatedChecksum, submitChecksum))
		      success = False

	if (success):
	      rawPayload = self._gpsHeader + struct.pack('2B', 0x02, 0x10) + rawPayloadLength + payload + ck
	      self._logger.debug('Returning from _readRaw @ %s ' %(strftime("%H:%M:%S +0000", gmtime())))	      
	      self._serialConnTries = self._initialSerialConnTries
	      return (header[0], header[1], rawPayload, payload, payloadLength) #ID, class, payload
	else:
	      self._logger.debug("readGpsMessage: returned nothing!")
	      return False

    '''
    ##########################################################################################
    serialAccess()
    	mode = w: data is written
	mode = r: data specifies number of bytes to read
   
	return TRUE or data if success, FALSE otherwise
    ##########################################################################################
    '''
    def serialAccess(self,data,mode):
	  if (mode == 'w'):
	    try:
	      self.device.write(data)
	      return True
	    except Exception as e:
	      if self._logger.isEnabledFor(logging.DEBUG):
                self._logger.debug("serialAccess Exception: %s" % (e,))
	      self._logger.info("serialAccess Exception: " + str(e))
	      self.fixDevice()
	      return False
	  elif (mode == 'r'):
	    try:
	      d = self.device.read(data)
	      return d
	    except Exception as e:
	      if self._logger.isEnabledFor(logging.DEBUG):
                self._logger.debug("serialAccess Exception: %s" % (e,))
	      self._logger.info("serialAccess Exception: " + str(e))
	      self.fixDevice()
	      return False
	  else:
	    self._logger.info("serialAccess: Wrong mode specified")
	    return False
   
      
    '''
    ##########################################################################################
    testDevice(): Tests if the serial port can be opened at given device string
    ##########################################################################################
    '''
    def testDevice(self, devString):
      try:
        self.device = serial.Serial(devString, 19200, timeout=self._serialTimeout)
      except Exception as e:
	self._logger.info("testDevice: " + str(e))
	return False
      try:
        self.device.open()
        self.device.flushInput()
        self.device.flushOutput()
        self._logger.info("Successfully (re)opened connection with GPS receiver")
        return True
      except Exception as e:
	if (self.device.isOpen()):
	  self.device.close()
	self._logger.info('testDevice Exception: ' + str(e))
	return False
	
    '''
    ##########################################################################################
    fixDevice
    ##########################################################################################
    '''
    def fixDevice(self):
      self._serialConnTries -= 1
      if (self._serialConnTries == 0):
	  self._logger.error( "No GPS receiver found! Giving up!")
	  self._stop()
	  
      if (self.device is not None):
	try:
	  self.device.close()
	except Exception as e:
	  self._logger.debug("Unable to close the serial port\n" + str(e))
      try:
	p = subprocess.Popen(['bb_usbpwr.py', '--list'], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
	p.wait()
	dev = p.communicate()
	dev = dev[0].split('USB Port')
	
	if (not self._parent.getPowerControlObject().getUsb2Status()):
	  self._parent.getPowerControlObject().usb2On()
	if (not self._parent.getPowerControlObject().getUsb3Status()):
	  self._parent.getPowerControlObject().usb3On()
	self._runEv.wait(5)

	for i in range(2,len(dev)):
	    if dev[i].find('u-blox')!= -1:
	      if i == 2:
		self._parent.getPowerControlObject().usb2Off()
		self._runEv.wait(10)
		self._parent.getPowerControlObject().usb2On()
	      elif i == 3:
		self._parent.getPowerControlObject().usb3Off()
		self._runEv.wait(10)
		self.parent.getPowerControlObject().usb3On()
	      self._logger.debug("Power-cycled usb port " + str(i))
	      self._runEv.wait(10)
	ret = self.testDevice(self._deviceStr)
	if (ret):
	  return True
	return False
      except Exception as e:
	self._logger.warning("Unhandled Exception in fixDevice:\n" + str(e))
	return False
	
    '''
    ##########################################################################################
    getChecksum(): computes checksum of given message
    ##########################################################################################
    '''
    def getChecksum(self, msg):
        ck_a = 0
        ck_b = 0

        for i in msg:
          ck_a += int(i,16)
          ck_b += ck_a
        
        ck_a &= 0xff
        ck_b &= 0xff
        
        #dirty hack to make sure hex val is 2 digits otherwise a2b_base64 complains!
        if (len(hex(ck_b)) < 4):
	  ck_b = "0x0%x"%(ck_b & 0xff)
	else:
	  ck_b = hex(ck_b)
	  
	if (len(hex(ck_a)) < 4):
	  ck_a = "0x0%x"%(ck_a & 0xff)
	else:
	  ck_a = hex(ck_a)
	  
        return (ck_a, ck_b)
            
    '''
    ##########################################################################################
    verifyChecksum(): Verifies checksum of given message and checksum
    ##########################################################################################
    '''
    def verifyChecksum(self, msg, chksum):
	ck_a = 0
	ck_b = 0
	
	for i in msg:
	  ck_a += ord(i)
	  ck_b += ck_a
	ck_a &= 0xff
	ck_b &= 0xff
        return ((ck_a == chksum[0]) and (ck_b == chksum[1]))
        
    '''
    ##########################################################################################
    config_device(): configures GPS receiver with string given in config file
    ##########################################################################################
    '''
    def config_device(self):
      self._logger.info("Configuring GPS receiver...")
      try:
	fp = open(self._config_file,'r')
      except Exception as e:
	self._logger.warning("GPSDriver.config_device: " + str(e))
	return False
      success = 0
      cnt = 0
      for line in fp:
	  cnt += 1
	  if (self._write(line)):
	    success += 1
	  self._runEv.wait(.1)

      self._logger.debug("Successfully wrote " + str(success) + " of a total of " + str(cnt) + " default config strings")
      if (self._interval != None):
	try:
	  self._logger.debug("Setting custom rate to " + str(self._interval) + "...")
	  rate=str(hex((self._interval*1000)&0xff)).split('0x')[1] + " " + str(hex((self._interval*1000)>>8)).split('0x')[1]
	  newrate="CFG-RATE - 06 08 06 00 " + rate + " 01 00 01 00"
	  self._logger.debug(newrate)
	  self._write(newrate)
	  self._write("CFG-CFG - 06 09 0D 00 00 00 00 00 FF FF 00 00 00 00 00 00 07")
	except Exception as e:
	  self._logger.debug(str(e))
      fp.close()
      return True
      
#############################################################
# Class WlanThread
#############################################################
class WlanThread(Thread):

    #*********************************************************
    # init()
    #*********************************************************
    def __init__(self,parent,uptime=10,downtime=40,stay_online=1):
        Thread.__init__(self, name='Wlan-Thrad')
        self._uptime=uptime*60
        self._downtime=downtime*60
        self._parent = parent
        self._work = Event()
        self._stopped = False
	self._loggedin = False
	self._test = False
        self._online = parent.getPowerControlObject().getWlanStatus()
        self._stay_online = stay_online
        try:
	  self._utmp = open('/media/card/backlog/dummy') #open("/var/run/utmp")
	  self._utmp.close()
	except Exception as e:
	  self.info("Unable to open UTMP file!\n" + str(e))
	  
    
    #*********************************************************
    # run()
    #*********************************************************
    def run(self):
        self._parent._logger.info('WlanThread: started')
        while not self._stopped: 
            self._parent._logger.info('WlanThread: Waiting for %d secs before cycling WLAN' % (self._uptime,))
            self._work.wait(self._uptime-30)
            if (self._parent.getPowerControlObject().getWlanStatus()): #is WLAN on?		
		if (self._test):
		  self._loggedin = self.isUserLoggedIn()
		  
                start = time()
		while (not self._stopped and self._stay_online and self._parent.isResendingDB()):
                    self._parent._logger.debug('We are flushing DB... NOT power cycling WLAN!')
                    self._parent._logger.debug('Waiting for 10 sec')
                    self._work.wait(10)
                    
		while (not self._stopped and self._loggedin):
			self._parent._logger.debug('Someone is logged in... NOT power cycling WLAN!')          
                        self._parent._logger.debug('Waiting for 10 sec')                                       
                        self._work.wait(10)
                        if (self._test):
			  self._loggedin = self.isUserLoggedIn()
		
                if (not self._stopped):
                    duration = time() - start 
                    if (self._downtime - duration > 0):
			self._parent._logger.info('Will cut Wlan connection in 30 seconds for ' + str(self._downtime) + ' seconds!')
			self._work.wait(30)
			self._parent._logger.info("Cutting power to WLAN Now!!")
                        self._parent.getPowerControlObject().wlanOff()
                        self._parent._logger.debug('Waiting for %d secs' % (self._downtime-duration,))
                        self._work.wait(self._downtime - duration)
            #If WLAN is off, turn it on
            if (not self._stopped and not self._parent.getPowerControlObject().getWlanStatus()):
                self._parent._logger.info("We are not online, so turn on wlan")
                self._parent.getPowerControlObject().wlanOn()  
        self._parent._logger.info('WlanThread: died')

    def isUserLoggedIn(self):
      try:
	self._utmp = open('/media/card/backlog/dummy') #open("/var/run/utmp")
	for line in self._utmp:
	  if line.find("root") >= 0:
	    self._loggedin = True
	    break
	self._utmp.close()
	return True
      except Exception as e:
	self._parent._logger.debug("Exception in isUserLoggedIn!\n" + str(e))
	return False
	
    #*********************************************************
    # stop()
    #*********************************************************
    def stop(self):
	self._parent._logger.info('WlanThread: stopping...')
        self._work.set()
        self._stopped = True
        self._parent._logger.info('WlanThread: stopped')
        

