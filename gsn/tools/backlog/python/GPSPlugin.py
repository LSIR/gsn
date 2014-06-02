# -*- coding: UTF-8 -*-
__author__      = "Ben Buchli <bbuchli@ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
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
from SpecialAPI import Statistics
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

#How often should driver be restarted in case of problems
INIT_DRIVER_RESTART = 3

#Reads raw GPS messages from a u-blox device and sends them to GSN
class GPSPluginClass(AbstractPluginClass):
    '''
    ##########################################################################################
    __init__
    ##########################################################################################
    '''
    def __init__(self, parent, config):
        AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG, needPowerControl=True)
        self._stopped = False
        self._busy = True
        self.gps = None
        self._workEvent = Event()

        #How often should driver be restarted in case of problems
        self._DriverRestarts = INIT_DRIVER_RESTART

        # The measurement interval in seconds
        self._interval = int(self.getOptionValue('poll_interval'))
        self.info('poll interval: %s seconds' % (self._interval,))

        # The device identifier
        self._deviceStr = self.getOptionValue('gps_device')
        self.info('using GPS device: %s' % (self._deviceStr,))
        
        # GPS configuration file as produced by u-center
        self._config_file = str(self.getOptionValue('gps_config_file'))
        self.info('using GPS configuration file: %s' % (self._config_file,))

        self._cnt_file = str(self.getOptionValue('cnt_file'))
        self.info('using GPS sample count file: %s' % (self._cnt_file,))
        #if count file does not exist, create it!
        if (not os.path.exists(str(self._cnt_file))):
            self.info("GPS count file does not exist! Creating it!")
            try:
                fp = open(str(self._cnt_file),"w")
                fp.write('0')
                fp.close()
            except Exception as e:
                self.stop()
                raise Exception("GPS count file could not be created!!\n" + str(e))

        try:
            fp = open(str(self._cnt_file),"r+")
	except Exception as e:
	    raise Exception("Could not open sample count file")
	try:
            cnt = int(fp.readline())
	except Exception as e:
	    cnt = 0
	try:
            if (cnt >= (pow(2,32)-1) or cnt == ""):
                self._logger.warning("Sample counter wrapped around! %s" % (cnt,))
                cnt = 0
            self._stats = Statistics()
            self.info("Starting with %d" % (cnt,))
            self._counterID = self._stats.createCounter(0,cnt)
            fp.close()
        except Exception as e:
            raise Exception( "Exception in creating counter instance")

        self.debug("Done init GPS Plugin")

    '''
    ##########################################################################################
    run():     This function gets called when the plugin is loaded. This occurs either when
               BackLog is started and the GPS plugin is activated or when action is called
               the first time.
    ##########################################################################################
    '''
    def run(self):
        self.name = 'GPSPlugin-Thread'
        self.info('started')

        while not self._stopped:
            if (self.gps is not None and self.gps.isInitialized()):
                self.debug('Reading raw at %s' %(strftime("%H:%M:%S +0000", gmtime())))
                rawMsg = self.gps._readRaw()

                if (rawMsg):
                    self._stats.counterAction(self._counterID)
                    cnt = self._stats.getCounterValue(self._counterID)
                    self.processMsg(self.getTimeStamp(), [RAW_TYPE, RAW_DATA_VERSION, cnt, ((rawMsg[4]-8)/24), bytearray(rawMsg[2])])
                    self.writeToFile(cnt)
                    self._DriverRestarts = INIT_DRIVER_RESTART
                    self.debug('Got a sample @ %s' %(strftime("%H:%M:%S +0000", gmtime())))
                    self.debug("GPS Sample Nr: %d" % (cnt,))
                    self.debug("%d Satellites (%d Bytes)" %((rawMsg[4]-8)/24, rawMsg[4]))
                else:
                    self.debug(str(rawMsg))
            else:
                if (self._DriverRestarts <= 0):
                    self.error("Restarting the driver did not help! Giving up!!")
                    if (self.isDutyCycleMode()):
                        #if we stop immediately, system will shutdown
                        self.info("Waiting for 15 minutes for help to come...")
                        self._workEvent.wait(15*60)
                    self.stop()
                else:
                    self.warning("Trying to (re)start GPSDriver!")
                    self._DriverRestarts -= 1
                    self.gps = GPSDriver(self,[self._deviceStr, self._interval, self._config_file])
                    if not self.gps.isInitialized():
                        self.warning('Initializing GPS Driver failed!!')

        self.info('died')


    '''
    ##########################################################################################
    stop()
    ##########################################################################################
    '''
    def stop(self):
        self.info('GPSPlugin stopping...')
        try:
            self.gps.stop()
        except Exception as e:
            self.debug("GPSDriver already stopped\n" + str(e))
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
            fp = open(str(self._cnt_file),"w")
            fp.write(str(val))
            fp.close()
        except Exception as e:
            self.exception("Unable to write to GPS count file!\n" + str(e))

'''
***********************************************************************************************
#Reads raw GPS messages from a u-blox device and sends them to GSN
***********************************************************************************************
'''
class GPSDriver():

    def __init__(self,parent,config):
        self._logger = logging.getLogger(self.__class__.__name__)

        self._logger.info('Init GPS Driver...')
        self.DEBUG = False
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

        self._stopped = False
        self._initialized = False

        #handler to the serial interface
        self.device = None
        self._runEv = Event()

        #reset serial timeout to 30!
        if (self._interval is not None):
            self._serialTimeout = self._interval
        else:
            self._serialTimeout = 30

        success = self._testDevice(self._deviceStr)
        while (not success and not self._stopped):
            success = self.fixDevice()
            self._runEv.wait(10)
        if (not success):
            self.stop()
        else:
            #device is tested by config in that it tries to write to it.
            self._initialized = self._configDevice()


    '''
    ##########################################################################################
    # PUBLIC FUNCTIONS
    ##########################################################################################
    '''

    def stop(self):
        self._stopped = True
        self._initialized = False
        self._runEv.set()
        self._logger.info('GPSDriver stopped')

    '''
    ##########################################################################################
    # write(self,payload): writes payload appropriately prepended with sync chars & header, and
    # appended with checksums to the GPS device.
    ##########################################################################################
    '''
    def write(self,com=None):
        try:
            success = False
            com = com.split("-")[2]
            com = com.split()
            checksums = self._getChecksum(com)
            command = self._gpsHeader
            for item in com:
                command += struct.pack('B', int(item, 16))
            command += struct.pack('2B', int(checksums[0], 16), int(checksums[1],16))
            tries = 10
            while (not self._stopped and not success):
                success = self._serialAccess(command,'w')
                tries -= 1
                #print "Trying to write " + str(10-tries)
                if (tries == 0 and not success):
                    self._logger.warning("Unable to write to GPS")
                    return False
        except Exception as e:
            self._logger.warning("GPSDriver.write: Unhandled Exception!\n" + str(e))
            self._logger.debug(str(com))
            return False

        return success

    '''
    ##########################################################################################
    isInitialized():
    ##########################################################################################
    '''
    def isInitialized(self):
        return self._initialized

    '''
    #########################################################################
    #_readRaw(): reads RAW UBX data
    #########################################################################
        '''
    def _readRaw(self):

        success = False
        if (self.DEBUG):
            self._logger.debug('Entered _readRaw @ %s ' %(strftime("%H:%M:%S +0000", gmtime())))

        # Wait for the Header
        head = self._serialAccess(1,'r')
        tries = 10
        while (not self._stopped and tries > 0 and (len(head) != 1 or ord(head) != 0xB5)):
            head = self._serialAccess(1, 'r')
            tries -= 1

        msg = self._serialAccess(1,'r')

        if len(msg)==1 and ord(msg) == 0x62:
            if(self.DEBUG):
                self._logger.debug('Got a GPS Header @ %s ' %(strftime("%H:%M:%S +0000", gmtime())))
            # Got a message! :)  Read 2 bytes to determine class & id
            recMsgId = self._serialAccess(2,'r')
            # Is it the right Msg Type?
            if (recMsgId == self._messageId):
                if (self.DEBUG):
                    self._logger.debug('Got a GPS Sample @ %s ' %(strftime("%H:%M:%S +0000", gmtime())))

                try:
                    header = struct.unpack('2B', recMsgId)
                    rawPayloadLength = self._serialAccess(2,'r')
                    payloadLength = struct.unpack('H', rawPayloadLength)[0]
                    payload = self._serialAccess(payloadLength,'r')

                    ck = self._serialAccess(2,'r')
                    submitChecksum = struct.unpack('2B', ck)
                    success = self._verifyChecksum(recMsgId + rawPayloadLength + payload, submitChecksum)
                    if (not success):
                        self._logger.warning('The received checksum did not match the computed')
                except Exception as e:
                    self._parent.exception("Exception in readRaw: " + str(e))
                    return False

        if (success):
            if (self.DEBUG):
                self._logger.debug('Returning from _readRaw @ %s ' %(strftime("%H:%M:%S +0000", gmtime())))
            rawPayload = self._gpsHeader + struct.pack('2B', 0x02, 0x10) + rawPayloadLength + payload + ck
            self._serialConnTries = self._initialSerialConnTries
            return (header[0], header[1], rawPayload, payload, payloadLength) #ID, class, payload
        else:
            self._logger.debug("readGpsMessage: returned nothing!")
            return False

    '''
    ##########################################################################################
    _serialAccess()
        mode = w: data is written
        mode = r: data specifies number of bytes to read

        return TRUE or data if success, FALSE otherwise
    ##########################################################################################
    '''
    def _serialAccess(self,data,mode):
        if (mode == 'w'):
            try:
                self.device.write(data)
                self.device.flushInput()
                self.device.flushOutput()
                return True
            except Exception as e:
                self._logger.debug("serialAccess Exception: " + str(e))
                self.fixDevice()
                return False
        elif (mode == 'r'):
            try:
                d = self.device.read(data)
                if (len(d) != data):
                    return ''
                return d
            except Exception as e:
                self._logger.debug("serialAccess Exception: " + str(e))
                self.fixDevice()
                return ''
        else:
            self._logger.debug("serialAccess: Wrong mode specified")
            return False


    '''
    ##########################################################################################
    _testDevice(): Tests if the serial port can be opened at given device string
    ##########################################################################################
    '''
    def _testDevice(self, devString):
        self._logger.debug("Testing device at " + str(self._deviceStr))
        try:
            self.device = serial.Serial(devString, 19200, timeout=self._serialTimeout)
        except Exception as e:
            self._logger.debug("testDevice: " + str(e))
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
            self._logger.debug('testDevice Exception: ' + str(e))
            return False

    '''
    ##########################################################################################
    fixDevice
    ##########################################################################################
    '''
    def fixDevice(self):
        self._serialConnTries -= 1
        if (self._serialConnTries == 0):
            self._logger.error( "No GPS receiver found!")
            self.stop()

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
                        self._parent.getPowerControlObject().usb3On()
                    self._logger.debug("Power-cycled usb port " + str(i))
                    self._runEv.wait(10)

            return self._testDevice(self._deviceStr)
        except Exception as e:
            self._logger.warning("Unhandled Exception in fixDevice:\n" + str(e))
            return False

    '''
    ##########################################################################################
    _getChecksum(): computes checksum of given message
    ##########################################################################################
    '''
    def _getChecksum(self, msg):
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
    _verifyChecksum(): Verifies checksum of given message and checksum
    ##########################################################################################
    '''
    def _verifyChecksum(self, msg, chksum):
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
    _configDevice(): configures GPS receiver with string given in config file
    ##########################################################################################
    '''
    def _configDevice(self):
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
            if (self.write(line)):
                success += 1
            self._runEv.wait(.1)

        self._logger.debug("Successfully wrote " + str(success) + " of a total of " + str(cnt) + " default config strings")
        if (self._interval != None):
            try:
                self._logger.debug("Setting custom rate to " + str(self._interval) + "...")
                rate=str(hex((self._interval*1000)&0xff)).split('0x')[1] + " " + str(hex((self._interval*1000)>>8)).split('0x')[1]
                newrate="CFG-RATE - 06 08 06 00 " + rate + " 01 00 01 00"
                self._logger.debug(newrate)
                self.write(newrate)
                self.write("CFG-CFG - 06 09 0D 00 00 00 00 00 FF FF 00 00 00 00 00 00 07")
            except Exception as e:
                self._logger.debug(str(e))
        fp.close()
        self._logger.info("GPS receiver successfully configured!")
        return True
