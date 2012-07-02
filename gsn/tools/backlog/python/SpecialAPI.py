# -*- coding: UTF-8 -*-
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import os
import uuid
import logging
import time
import ConfigParser
from datetime import datetime, timedelta
from threading import Lock


# as soon as the subprocess.Popen() bug has been fixed the functionality related
# to this variable should be removed
SUBPROCESS_BUG_BYPASS = True

if SUBPROCESS_BUG_BYPASS:
    import SubprocessFake
    subprocess = SubprocessFake
else:
    import subprocess

MIN_RING_BUFFER_CLEAR_INTERVAL_SEC = 5


class Statistics:
    '''
    This class can be used to generate statistics information. It offers counter
    and timing functionality.
    
    '''

    '''
    data/instance attributes:
    _timers
    _calcStats
    _counters
    _counterLocks
    _counterRingBuffers
    _counterRingBufSizeSec
    _minTimerIds
    '''

    def __init__(self):
        self._timers = dict()
        self._counters = dict()
        self._counterLocks = dict()
        self._counterRingBuffers = dict()
        self._counterRingBufSizeSec = dict()
        self._minTimerIds = dict()
        
        self._calcStats = True
            
            
    def createCounter(self, ringBuffSize=0, initCounterValue=0):
        '''
        @param ringBuffSize: Specifies how old entries in the internal ringbuffer
                              can maximally get in seconds. If this parameter is
                              set to 0 no ringbuffer will be used. Thus, no
                              min/mean/max values can be requested. If min/mean/max
                              values are needed, it should be set at least to the
                              biggest period over which min/mean/max values should
                              be calculated (default=0)
                              
        @param initCounterValue: initialize the counter with this value (default=0)
        '''
        uniqueCounterId = uuid.uuid4()
        self._counterLocks.update({uniqueCounterId: Lock()})
        self._counterRingBufSizeSec.update({uniqueCounterId: ringBuffSize})
        self._counters.update({uniqueCounterId: initCounterValue})
        
        if ringBuffSize > 0:
            self._minTimerIds.update({uniqueCounterId: self.timeMeasurementStart()})
            self._counterRingBuffers.update({uniqueCounterId: list()})
            
        return uniqueCounterId
        
        
    def counterAction(self, uniqueCounterId, increaseBy=1):
        '''
        Should be called on a counter action.
        
        It starts a new counter if uniqueCounterId is not set and initializes it with 1
        or with the value passed to increaseBy. In this case it returns a unique identifier
        which can be used for further counter actions for this specific counter.
        
        If a uniqueCounterId is passed it increases the specified counter by 1 or by the value
        passed to increaseBy.
        
        
        @param uniqueCounterId: the unique counter id which specifies the needed counter
        
        @param increaseBy: increase the specified counter by the passed value (default = 1)
        
        @return: the unique counter id for this specific counter or None if statistics
                  gathering has been stopped
        
        @raise KeyError: if the uniqueCounterId does not exist yet
        '''
        if self._calcStats:
            self._counterLocks[uniqueCounterId].acquire()
            cnt = self._counters[uniqueCounterId]
            self._counters.update({uniqueCounterId: cnt+increaseBy})
            if self._counterRingBufSizeSec[uniqueCounterId] > 0:
                now = datetime.utcnow()
                buffer = self._counterRingBuffers[uniqueCounterId]
                buffer.append([now, increaseBy])
                self._counterRingBuffers.update({uniqueCounterId: buffer})
               
            if self._counterRingBufSizeSec[uniqueCounterId] > 0 and self.timeMeasurementDiff(self._minTimerIds[uniqueCounterId], False) > MIN_RING_BUFFER_CLEAR_INTERVAL_SEC:
                counterRingBufSizeDelta = timedelta(seconds=self._counterRingBufSizeSec[uniqueCounterId])
                self.timeMeasurementDiff(self._minTimerIds[uniqueCounterId])
                self._minTimerIds.update({uniqueCounterId: self.timeMeasurementStart()})
                newerEntries = False
                for index, entry in enumerate(buffer):
                    if entry[0] >= now - counterRingBufSizeDelta:
                        newerEntries = True
                        break
                if newerEntries:
                    self._counterRingBuffers.update({uniqueCounterId: buffer[index:]})
                else:
                    self._counterRingBuffers.update({uniqueCounterId: list()})
                    
            self._counterLocks[uniqueCounterId].release()
        
        
    def getCounterValue(self, uniqueCounterId):
        '''
        Returns the value of the specified counter.
        
        
        @param uniqueCounterId: the unique counter id which specifies the needed counter
        
        @return: the value of the specified counter or None if statistics gathering has
                  been stopped
        
        @raise KeyError: if the uniqueCounterId does not exist yet
        '''
        if self._calcStats:
            self._counterLocks[uniqueCounterId].acquire()
            cnt = self._counters[uniqueCounterId]
            self._counterLocks[uniqueCounterId].release()
            return cnt
        else:
            return None
        
        
    def getAvgCounterIncPerSecond(self, uniqueCounterId, avgPerSecOverLastNSecondsList):
        '''
        Returns average counter increase per second for specified intervals
        and a specific counter.
        
        
        @param uniqueCounterId: the unique counter id which specifies the needed counter
        
        @param avgPerSecOverLastNSecondsList: a list which specifies the needed intervals over
                                          which the average should be calculated.
                                          (e.g. [30 60 300] would calculate the average
                                            of incoming values per second over the last
                                            30, 60 and 300 seconds)
        
        @return: a list of the same size as avgPerSecOverLastNSecondsList containing the
                  different averages.
        
        @raise KeyError: if the uniqueCounterId does not exist yet
        '''
        n = len(avgPerSecOverLastNSecondsList)
        if self._counterRingBufSizeSec[uniqueCounterId] > 0 and self._calcStats:
            counters = [0]*n
            self._counterLocks[uniqueCounterId].acquire()
            now = datetime.utcnow()
            for dt, entry in self._counterRingBuffers[uniqueCounterId]:
                for i in range(n):
                    if avgPerSecOverLastNSecondsList[i] > self._counterRingBufSizeSec[uniqueCounterId]:
                        raise Exception('avgPerSecOverLastNSecondsList entry can not be bigger than counterRingBufferSizeInSeconds')
                    if dt >= now - timedelta(seconds=avgPerSecOverLastNSecondsList[i]):
                        counters[i] += entry
            self._counterLocks[uniqueCounterId].release()
            
            ret = []
            for i in range(n):
                ret.append(counters[i]/float(avgPerSecOverLastNSecondsList[i]))
            if self._calcStats:
                return ret
       
        return [None]*n
    
        
    def getMinCounterInc(self, uniqueCounterId, minOverLastNSecondsList):
        '''
        Returns the minima for specified intervals and a specific counter.
        
        
        @param uniqueCounterId: the unique counter id which specifies the needed counter
        
        @param minOverLastNSecondsList: a list which specifies the needed intervals over
                                          which the minima should be calculated.
                                          (e.g. [30 60 300] would calculate the minima
                                            over the last 30, 60 and 300 seconds)
        
        @return: a list of the same size as minOverLastNSecondsList containing the
                  different minima.
        
        @raise KeyError: if the uniqueCounterId does not exist yet
        '''
        n = len(minOverLastNSecondsList)
        if self._counterRingBufSizeSec[uniqueCounterId] > 0 and self._calcStats:
            min = [None]*n
            self._counterLocks[uniqueCounterId].acquire()
            now = datetime.utcnow()
            for dt, entry in self._counterRingBuffers[uniqueCounterId]:
                for i in range(n):
                    if minOverLastNSecondsList[i] > self._counterRingBufSizeSec[uniqueCounterId]:
                        raise Exception('minOverLastNSecondsList entry can not be bigger than counterRingBufferSizeInSeconds (%s > %s)' % (minOverLastNSecondsList[i], self._counterRingBufSizeSec[uniqueCounterId]))
                    if dt >= now - timedelta(seconds=minOverLastNSecondsList[i]) and (min[i] == None or entry < min[i]):
                        min[i] = entry
            self._counterLocks[uniqueCounterId].release()
            
            if self._calcStats:
                return min
       
        return [None]*n
    
        
    def getAvgCounterInc(self, uniqueCounterId, avgOverLastNSecondsList):
        '''
        Returns the average for specified intervals and a specific counter.
        
        
        @param uniqueCounterId: the unique counter id which specifies the needed counter
        
        @param avgOverLastNSecondsList: a list which specifies the needed intervals over
                                          which the average should be calculated.
                                          (e.g. [30 60 300] would calculate the average
                                            over the last 30, 60 and 300 seconds)
        
        @return: a list of the same size as avgOverLastNSecondsList containing the
                  different averages.
        
        @raise KeyError: if the uniqueCounterId does not exist yet
        '''
        n = len(avgOverLastNSecondsList)
        if self._counterRingBufSizeSec[uniqueCounterId] > 0 and self._calcStats:
            counters = [0]*n
            entries = [0]*n
            self._counterLocks[uniqueCounterId].acquire()
            now = datetime.utcnow()
            for dt, entry in self._counterRingBuffers[uniqueCounterId]:
                for i in range(n):
                    if avgOverLastNSecondsList[i] > self._counterRingBufSizeSec[uniqueCounterId]:
                        raise Exception('avgOverLastNSecondsList entry can not be bigger than counterRingBufferSizeInSeconds')
                    if dt >= now - timedelta(seconds=avgOverLastNSecondsList[i]):
                        counters[i] += entry
                        entries[i] += 1
            self._counterLocks[uniqueCounterId].release()
            
            ret = [None]*n
            for i in range(n):
                if entries[i] > 0:
                    ret[i] = counters[i]/float(entries[i])
            if self._calcStats:
                return ret
       
        return [None]*n
    
        
    def getMaxCounterInc(self, uniqueCounterId, maxOverLastNSecondsList):
        '''
        Returns the maxima for specified intervals and a specific counter.
        
        
        @param uniqueCounterId: the unique counter id which specifies the needed counter
        
        @param maxOverLastNSecondsList: a list which specifies the needed intervals over
                                          which the maxima should be calculated.
                                          (e.g. [30 60 300] would calculate the maxima
                                            over the last 30, 60 and 300 seconds)
        
        @return: a list of the same size as maxOverLastNSecondsList containing the
                  different maxima.
        
        @raise KeyError: if the uniqueCounterId does not exist yet
        '''
        n = len(maxOverLastNSecondsList)
        if self._counterRingBufSizeSec[uniqueCounterId] > 0 and self._calcStats:
            max = [None]*n
            self._counterLocks[uniqueCounterId].acquire()
            now = datetime.utcnow()
            for dt, entry in self._counterRingBuffers[uniqueCounterId]:
                for i in range(n):
                    if maxOverLastNSecondsList[i] > self._counterRingBufSizeSec[uniqueCounterId]:
                        raise Exception('maxOverLastNSecondsList entry can not be bigger than counterRingBufferSizeInSeconds')
                    if dt >= now - timedelta(seconds=maxOverLastNSecondsList[i]) and (max[i] == None or entry > max[i]):
                        max[i] = entry
            self._counterLocks[uniqueCounterId].release()
            
            if self._calcStats:
                return max
       
        return [None]*n
        
        
    def removeCounter(self, uniqueCounterId):
        self._counterLocks[uniqueCounterId].acquire()
        del self._counters[uniqueCounterId]
        if self._counterRingBufSizeSec[uniqueCounterId] > 0:
            del self._minTimerIds[uniqueCounterId]
            del self._counterRingBuffers[uniqueCounterId]
        del self._counterRingBufSizeSec[uniqueCounterId]
        self._counterLocks[uniqueCounterId].release()
        del self._counterLocks[uniqueCounterId]
        
        
    def timeMeasurementStart(self):
        if self._calcStats:
            uniqueTimerId = uuid.uuid4()
            self._timers.update({uniqueTimerId: datetime.utcnow()})
            return uniqueTimerId
        else:
            return None
        
        
    def timeMeasurementDiff(self, uniqueTimerId, stopMeasurement=True):
        if self._calcStats:
            diff = datetime.utcnow() - self._timers[uniqueTimerId]
            if stopMeasurement:
                del self._timers[uniqueTimerId]
            return (diff.microseconds + (diff.seconds + diff.days * 86400) * 1000000) / 1000000.0
        else:
            return None
    
    
    def stopAccumulation(self):
        self._calcStats = False
    
    
    def restartAccumulation(self):
        self._calcStats = True



import sys
import tos
import TOSTypes

EXT_GPIO_MAP = {1:"/proc/gpio/GPIO65", 2:"/proc/gpio/GPIO58", 3:"/proc/gpio/GPIO29"}
USB_GPIO_MAP = {1:"/proc/gpio/GPIO72", 2:"/proc/gpio/GPIO66", 3:"/proc/gpio/GPIO73"}
EXT_CONFIG_FILE = "/etc/platform/bb_extpwr.conf"


class PowerControl:
    '''
    This class can be used to control the power of different hardware
    on a Core Station.
    
    WARNING: Do not use if you do not know what you do! ;)
    '''

    '''
    data/instance attributes:
    _logger
    _backlogMain
    _platform
    _extDefaultMap
    _ad77x8Lock
    _ad77x8Values
    _ad77x8Timer
    _ad77x8
    
    '''
    
#    make this class a singleton
    __shared_state = {}

    def __init__(self, backlogMain, wlanPort, platform):
        self.__dict__ = self.__shared_state
        
        self._logger = logging.getLogger(self.__class__.__name__)
        
        self._backlogMain = backlogMain
        self._platform = platform
        
        self._wlanPort = wlanPort
        self._extDefaultMap = {1:None, 2:None, 3:None}
        
        if platform == 1:
            self._extDefaultMap = {1:False, 2:False, 3:False}
        elif platform == 2:
            try:
                # Parse ext port config file
                config = ConfigParser.ConfigParser()
                config.read(EXT_CONFIG_FILE)
                for i in range(1,4):
                    try:
                        val = config.get("EXT%i" % i, "default")
                        if val == 'on':
                            self._extDefaultMap[i] = True
                        elif val == 'off':
                            self._extDefaultMap[i] = False
                        else:
                            raise Exception("default has to be 'on' or 'off'")
                    except (ConfigParser.NoSectionError, ConfigParser.NoOptionError):
                        pass
            except Exception, e:
                raise Exception("Could not parse config file %s: %s" % (EXT_CONFIG_FILE, str(e)))
            
        self._ad77x8Lock = Lock()
        self._ad77x8Values = [None]*10
        self._ad77x8Timer = None
        self._ad77x8 = False
        
        if platform is not None:
            self._initAD77x8()
        else:
            self._logger.info('unknown platform -> not initializing AD77x8')
        
        if self._backlogMain.duty_cycle_mode:
            self._backlogMain.registerTOSListener(self, [TOSTypes.AM_BEACONCOMMAND])
            
            
            
    def getVExt1(self):
        '''
        Returns V_EXT1 in millivolt.
        
        @raise Exception: if module ad77x8 is not available
        '''
        self._readAD77x8()
        return self._ad77x8Values[1]
            
            
            
    def getVExt2(self):
        '''
        Returns V_EXT2 in millivolt.
        
        @raise Exception: if module ad77x8 is not available
        '''
        self._readAD77x8()
        return self._ad77x8Values[0]
            
            
            
    def getVExt3(self):
        '''
        Returns V_EXT3 in millivolt.
        
        @raise Exception: if module ad77x8 is not available
        '''
        self._readAD77x8()
        return self._ad77x8Values[2]
            
            
            
    def getIV12DcExt(self):
        '''
        Returns I_V12DC_EXT in microampere.
        
        @raise Exception: if module ad77x8 is not available
        '''
        self._readAD77x8()
        return self._ad77x8Values[3]
            
            
            
    def getV12DcIn(self):
        '''
        Returns V12DC_IN in millivolt.
        
        @raise Exception: if module ad77x8 is not available
        '''
        self._readAD77x8()
        return self._ad77x8Values[4]
            
            
            
    def getIV12DcIn(self):
        '''
        Returns I_V12DC_IN in microampere.
        
        @raise Exception: if module ad77x8 is not available
        '''
        self._readAD77x8()
        return self._ad77x8Values[5]
            
            
            
    def getVcc50(self):
        '''
        Returns VCC_5_0 in millivolt.
        
        @raise Exception: if module ad77x8 is not available
        '''
        self._readAD77x8()
        return self._ad77x8Values[6]
            
            
            
    def getVccNode(self):
        '''
        Returns VCC_NODE in millivolt.
        
        @raise Exception: if module ad77x8 is not available
        '''
        self._readAD77x8()
        return self._ad77x8Values[7]
            
            
            
    def getIVccNode(self):
        '''
        Returns I_VCC_NODE in microampere.
        
        @raise Exception: if module ad77x8 is not available
        '''
        self._readAD77x8()
        return self._ad77x8Values[8]
            
            
            
    def getVcc42(self):
        '''
        Returns VCC_4_2 in millivolt.
        
        @raise Exception: if module ad77x8 is not available
        '''
        self._readAD77x8()
        return self._ad77x8Values[9]
                
    
    
    def usb1On(self):
        '''
        Turns the USB1 port power on
        
        @raise Exception: if USB1 port is not properly configured
        '''
        self._setUsbStatus(1, True)
        
    
    
    def usb1Off(self):
        '''
        Turns the USB1 port power off.
        
        @raise Exception: if USB1 port is not properly configured
        '''
        self._setUsbStatus(1, False)
    
    
    def getUsb1Status(self):
        '''
        Returns True if the USB1 port is on otherwise False
        
        @return: True if the USB1 port is on otherwise False
        
        @raise Exception: if USB1 port is not properly configured
        '''
        return self._getUsbSatus(1)
                
    
    
    def usb2On(self):
        '''
        Turns the USB2 port power on
        
        @raise Exception: if USB2 port is not properly configured
        '''
        self._setUsbStatus(2, True)
        
    
    
    def usb2Off(self):
        '''
        Turns the USB2 port power off.
        
        @raise Exception: if USB2 port is not properly configured
        '''
        self._setUsbStatus(2, False)
    
    
    def getUsb2Status(self):
        '''
        Returns True if the USB2 port is on otherwise False
        
        @return: True if the USB2 port is on otherwise False
        
        @raise Exception: if USB2 port is not properly configured
        '''
        return self._getUsbSatus(2)
                
    
    
    def usb3On(self):
        '''
        Turns the USB3 port power on
        
        @raise Exception: if USB3 port is not properly configured
        '''
        self._setUsbStatus(3, True)
        
    
    
    def usb3Off(self):
        '''
        Turns the USB3 port power off.
        
        @raise Exception: if USB3 port is not properly configured
        '''
        self._setUsbStatus(3, False)
    
    
    def getUsb3Status(self):
        '''
        Returns True if the USB3 port is on otherwise False
        
        @return: True if the USB3 port is on otherwise False
        
        @raise Exception: if USB3 port is not properly configured
        '''
        return self._getUsbSatus(3)
                
    
    
    def ext1On(self):
        '''
        Turns the ext1 port power on
        
        @raise Exception: if ext1 port is not properly configured
        '''
        self._setExtStatus(1, True)
        
    
    
    def ext1Off(self):
        '''
        Turns the ext1 port power off.
        
        @raise Exception: if ext1 port is not properly configured
        '''
        self._setExtStatus(1, False)
    
    
    def getExt1Status(self):
        '''
        Returns True if the ext1 port is on otherwise False
        
        @return: True if the ext1 port is on otherwise False
        
        @raise Exception: if ext1 port is not properly configured
        '''
        return self._getExtStatus(1)
                
    
    
    def ext2On(self):
        '''
        Turns the ext2 port power on
        
        @raise Exception: if ext2 port is not properly configured
        '''
        self._setExtStatus(2, True)
        
    
    
    def ext2Off(self):
        '''
        Turns the ext2 port power off.
        
        @raise Exception: if ext2 port is not properly configured
        '''
        self._setExtStatus(2, False)
    
    
    def getExt2Status(self):
        '''
        Returns True if the ext2 port is on otherwise False
        
        @return: True if the ext2 port is on otherwise False
        
        @raise Exception: if ext2 port is not properly configured
        '''
        return self._getExtStatus(2)
                
    
    
    def ext3On(self):
        '''
        Turns the ext3 port power on
        
        @raise Exception: if ext3 port is not properly configured
        '''
        self._setExtStatus(3, True)
        
    
    
    def ext3Off(self):
        '''
        Turns the ext3 port power off.
        
        @raise Exception: if ext3 port is not properly configured
        '''
        self._setExtStatus(3, False)
    
    
    def getExt3Status(self):
        '''
        Returns True if the ext3 port is on otherwise False
        
        @return: True if the ext3 port is on otherwise False
        
        @raise Exception: if ext3 port is not properly configured
        '''
        return self._getExtStatus(3)
                
    
    
    def wlanOn(self):
        '''
        Turns the wlan on
        
        @raise Exception: if ext# port linked to WLAN is not properly configured or wlan port mapping is not specified
        '''
        if self._wlanPort is None:
            raise Exception('wlan port mapping has not been specified in the configuration file')
        self._setExtStatus(self._wlanPort, True)
        
    
    
    def wlanOff(self):
        '''
        Turns the wlan off if no one still needs it.
        
        @return: True if wlan has been switched off otherwise False
        
        @raise Exception: if ext# port linked to WLAN is not properly configured or wlan port mapping is not specified
        '''
        if self._wlanPort is None:
            raise Exception('wlan port mapping has not been specified in the configuration file')
        self._setExtStatus(self._wlanPort, False)
    
    
    def getWlanStatus(self):
        '''
        Returns True if the wlan is on otherwise False
        
        @return: True if the wlan is on otherwise False
        
        @raise Exception: if ext# port linked to WLAN is not properly configured or wlan port mapping is not specified
        '''
        if self._wlanPort is None:
            raise Exception('wlan port mapping has not been specified in the configuration file')
        return self._getExtStatus(self._wlanPort)
            
            
    def tosMsgReceived(self, timestamp, payload):
        response = tos.Packet(TOSTypes.CONTROL_CMD_STRUCTURE, payload['data'])
        if self._logger.isEnabledFor(logging.DEBUG):
            self._logger.debug('rcv (cmd=' + str(response['command']) + ', argument=' + str(response['argument']) + ')')
        if response['command'] == TOSTypes.CONTROL_CMD_WLAN_ON:
            self.wlanOn()
        elif response['command'] == TOSTypes.CONTROL_CMD_WLAN_OFF:
            if not self.wlanOff():
                self._logger.info('Wlan has not been turned off because it is still used by some plugins')
        else:
            return False
        
        return True
        
        
    def stop(self):
        if self._backlogMain.duty_cycle_mode:
            self._backlogMain.deregisterTOSListener(self)
        
        
    def _initAD77x8(self):
        p = subprocess.Popen(['modprobe', 'ad77x8'], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        self._logger.info('wait for modprobe ad77x8 to finish')
        ret = p.wait()
        output = p.communicate()
        if output[0]:
            if output[1]:
                self._logger.info('modprobe ad77x8: (STDOUT=%s STDERR=%s)' % (output[0], output[1]))
            else:
                self._logger.info('modprobe ad77x8: (STDOUT=%s)' % (output[0],))
        elif output[1]:
                self._logger.info('modprobe ad77x8: (STDERR=%s)' % (output[1],))
                
        if ret != 0:
            self._logger.warning('module ad77x8 is not available (modprobe ad77x8 returned with code %d)' % (ret,))
        else:
            self._logger.info('modprobe ad77x8 finished successfully')
            self._ad77x8 = True
        
        
    def _readAD77x8(self):
        if self._ad77x8:
            self._ad77x8Lock.acquire()
            if self._ad77x8Timer is None or self._ad77x8Timer < time.time()-1:
                try:
                    fc = open('/proc/ad77x8/config', 'w')
                    fc.write('format mV')
                    fc.flush()                
                    fc.write('chopping on')
                    fc.flush()
                    fc.write('negbuf on')
                    fc.flush()
                    fc.write('sf 13')
                    fc.flush()
                    fc.write('range 7')
                    fc.flush()
                    fc.close()
                    
                    f1 = open('/proc/ad77x8/ain1', 'r')
                    f2 = open('/proc/ad77x8/ain2', 'r')
                    f3 = open('/proc/ad77x8/ain3', 'r')
                    f4 = open('/proc/ad77x8/ain4', 'r')
                    f5 = open('/proc/ad77x8/ain5', 'r')
                    f6 = open('/proc/ad77x8/ain6', 'r')
                    f7 = open('/proc/ad77x8/ain7', 'r')
                    f8 = open('/proc/ad77x8/ain8', 'r')
                    f9 = open('/proc/ad77x8/ain9', 'r')
                    f10 = open('/proc/ad77x8/ain10', 'r')
                
                    ad77x8_1 = f1.read()
                    ad77x8_2 = f2.read()
                    ad77x8_3 = f3.read()
                    ad77x8_4 = f4.read()
                    ad77x8_5 = f5.read()
                    ad77x8_6 = f6.read()
                    ad77x8_7 = f7.read()
                    ad77x8_8 = f8.read()
                    ad77x8_9 = f9.read()
                    ad77x8_10 = f10.read()
                    
                    f1.close()
                    f2.close()
                    f3.close()
                    f4.close()
                    f5.close()
                    f6.close()
                    f7.close()
                    f8.close()
                    f9.close()
                    f10.close()
        
                    ad77x8_1 = float(ad77x8_1.split()[0])
                    ad77x8_2 = float(ad77x8_2.split()[0])
                    ad77x8_3 = float(ad77x8_3.split()[0])
                    ad77x8_4 = float(ad77x8_4.split()[0])
                    ad77x8_5 = float(ad77x8_5.split()[0])
                    ad77x8_6 = float(ad77x8_6.split()[0])
                    ad77x8_7 = float(ad77x8_7.split()[0])
                    ad77x8_8 = float(ad77x8_8.split()[0])
                    ad77x8_9 = float(ad77x8_9.split()[0])
                    ad77x8_10 = float(ad77x8_10.split()[0])
        
                    if ad77x8_4 < 0:
                        ad77x8_4 = 0
                    if ad77x8_6 < 0:
                        ad77x8_6 = 0
                    if ad77x8_9 < 0:
                        ad77x8_9 = 0
        
                    ad77x8_1 = int(round(ad77x8_1 * 23 / 3.0))
                    ad77x8_2 = int(round(ad77x8_2 * 23 / 3.0))
                    ad77x8_3 = int(round(ad77x8_3 * 23 / 3.0))
                    if self._platform == 1:
                        ad77x8_4 = int(round(ad77x8_4 * 20000))
                    else:
                        ad77x8_4 = int(round(ad77x8_4 * 10000))
                    ad77x8_5 = int(round(ad77x8_5 * 23 / 3.0))
                    ad77x8_6 = int(round(ad77x8_6 * 2000))
                    ad77x8_7 = int(round(ad77x8_7 * 151 / 51.0))
                    ad77x8_8 = int(round(ad77x8_8 * 2))
                    ad77x8_9 = int(round(ad77x8_9 * 200 / 3.0))
                    ad77x8_10 = int(round(ad77x8_10 * 2))
                        
                    self._ad77x8Values = [ad77x8_1, ad77x8_2, ad77x8_3, ad77x8_4, ad77x8_5, ad77x8_6, ad77x8_7, ad77x8_8, ad77x8_9, ad77x8_10]
                    for i, val in enumerate(self._ad77x8Values):
                        if val > sys.maxint:
                            self._logger.warning("value %d at index %d in _ad77x8Values array out of range -> set it to None type" % (val, i))
                            self._ad77x8Values[i] = None
                    self._ad77x8Timer = time.time()
                except Exception, e:
                    self._logger.warning(e.__str__())
            self._ad77x8Lock.release()
        else:
            raise Exception('module ad77x8 is not available')
        
        
    def _getExtStatus(self, extNumber):
        if self._platform is not None:
            if self._extDefaultMap[extNumber] is not None:
                file = open(EXT_GPIO_MAP[extNumber], "r")
                gpio = file.read()
                file.close()
                if self._extDefaultMap[extNumber]:
                    if gpio.find('set') == -1:
                        return True
                    else:
                        return False
                else:
                    if gpio.find('set') == -1:
                        return False
                    else:
                        return True
            else:
                raise Exception('ext%d is not configured in %s -> can not get EXT status' % (extNumber, EXT_CONFIG_FILE))
        else:
            raise Exception('unknown platform -> can not get EXT status')
        
        
    def _setExtStatus(self, extNumber, status):
        if self._platform is not None:
            if self._extDefaultMap[extNumber] is not None:
                file = open(EXT_GPIO_MAP[extNumber], 'w')
                if self._extDefaultMap[extNumber]:
                    if status:
                        file.write('out clear')
                    else:
                        file.write('out set')
                else:
                    if status:
                        file.write('out set')
                    else:
                        file.write('out clear')
                file.close()
            else:
                raise Exception('ext%d is not configured in %s -> can not set EXT status' % (extNumber, EXT_CONFIG_FILE))
        else:
            raise Exception('unknown platform -> can not get EXT status')
        
        
    def _getUsbSatus(self, usbNumber):
        if self._platform is not None:
            file = open(USB_GPIO_MAP[usbNumber], 'r')
            gpio = file.read()
            file.close()
            if usbNumber == 1 and self._platform == 2:
                if gpio.find('set') == -1:
                    return True
                else:
                    return False
            else:
                if gpio.find('set') == -1:
                    return False
                else:
                    return True
        else:
            raise Exception('unknown platform -> can not get USB status')
        
        
    def _setUsbStatus(self, usbNumber, status):
        if self._platform is not None:
            if usbNumber == 1 and self._platform == 2:
                if status:
                    value = 'clear'
                else:
                    value = 'set'
            else:
                if status:
                    value = 'set'
                else:
                    value = 'clear'
                
            file = open(USB_GPIO_MAP[usbNumber], 'w')
            file.write('out ' + value)
            file.close()
        else:
            raise Exception('unknown platform -> can not get USB status')
    