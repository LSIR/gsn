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
from datetime import datetime, timedelta
from threading import Lock

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
            return (diff.microseconds + (diff.seconds + diff.days * 24 * 3600) * 10**6) / 10.0**6
        else:
            return None
    
    
    def stopAccumulation(self):
        self._calcStats = False
    
    
    def restartAccumulation(self):
        self._calcStats = True



import tos
import TOSTypes


DEFAULT_LINK_FOLDER = '/etc/gpio/'

WLAN_GPIO_LINK_PREFIX = 'wlan_power'
PHOTOCAM_GPIO_LINK_PREFIX = 'photocam_power'

GPIO_RESET_ON_CLEAR_SUFFIX = 'reset_on_clear'
GPIO_RESET_ON_SET_SUFFIX = 'reset_on_set'
GPIO_ON_ON_SET_SUFFIX = 'on_on_set'
GPIO_OFF_ON_SET_SUFFIX = 'off_on_set'


class PowerControl:
    '''
    This class can be used to control the power of different hardware
    on a Core Station.
    
    WARNING: Do not use if you do not know what you do! ;)
    '''

    '''
    data/instance attributes:
    _linkfolder
    _wlanGPIOLink
    _wlanGPIOLock
    _wlanGPIOOnOnSet
    '''
    
#    make this class a singleton
    __shared_state = {}

    def __init__(self, backlogMain, linkFolder=DEFAULT_LINK_FOLDER):
        self.__dict__ = self.__shared_state
        
        self._logger = logging.getLogger(self.__class__.__name__)
        
        self._backlogMain = backlogMain
        
        self._linkfolder = None
        if not os.path.isdir(linkFolder):
            self._logger.warning('linkFolder >%s< is not an existing folder => power control API can not be used' % (linkFolder,))
            raise
        
        self._linkfolder = linkFolder
        
        self._wlanGPIOLink = ''
        self._wlanGPIOLock = Lock()
        self._photocamGPIOLink = ''
        self._photocamGPIOLock = Lock()
        
        for file in os.listdir(linkFolder):
            if file.startswith(WLAN_GPIO_LINK_PREFIX):
                self._wlanGPIOLink = os.path.join(linkFolder, file)
                if file.endswith(GPIO_ON_ON_SET_SUFFIX):
                    self._wlanGPIOOnOnSet = True
                elif file.endswith(GPIO_OFF_ON_SET_SUFFIX):
                    self._wlanGPIOOnOnSet = False
                else:
                    raise Exception('file >%s< does not end with a proper suffix' % (os.path.join(linkFolder, file),))
            elif file.startswith(PHOTOCAM_GPIO_LINK_PREFIX):
                self._photocamGPIOLink = os.path.join(linkFolder, file)
                if file.endswith(GPIO_ON_ON_SET_SUFFIX):
                    self._photocamGPIOOnOnSet = True
                elif file.endswith(GPIO_OFF_ON_SET_SUFFIX):
                    self._photocamGPIOOnOnSet = False
                else:
                    raise Exception('file >%s< does not end with a proper suffix' % (os.path.join(linkFolder, file),))
        
        if self._backlogMain.duty_cycle_mode:
            self._backlogMain.registerTOSListener(self, [TOSTypes.AM_BEACONCOMMAND])
                
    
    
    def photoCamOn(self):
        '''
        Turns the photo camera power on
        
        @raise Exception: if no photo camera GPIO link file exists
        '''
        if not self._linkfolder:
            raise Exception('link folder does not exist')
        if self._photocamGPIOLink:
            self._logger.info('turning photo camera on')
            self._photocamGPIOLock.acquire()
            if self._photocamGPIOOnOnSet:
                self._gpioLinkAction(self._photocamGPIOLink, True)
            else:
                self._gpioLinkAction(self._photocamGPIOLink, False)
            self._photocamGPIOLock.release()
        else:
            raise Exception('photo camera GPIO link file is inexistent in >%s<' % (self._linkfolder,))
        
    
    
    def photoCamOff(self):
        '''
        Turns the photo camera power off.
        
        @raise Exception: if no photo camera GPIO link file exists
        '''
        if not self._linkfolder:
            raise Exception('link folder does not exist')
        if self._photocamGPIOLink:
            self._logger.warning('turning photo camera off')
            self._photocamGPIOLock.acquire()
            if self._photocamGPIOOnOnSet:
                self._gpioLinkAction(self._photocamGPIOLink, False)
            else:
                self._gpioLinkAction(self._photocamGPIOLink, True)
            self._photocamGPIOLock.release()
        else:
            raise Exception('photo camera GPIO link file is inexistent in >%s<' % (self._linkfolder,))
    
    
    def getPhotoCamStatus(self):
        '''
        Returns True if the photo camera is on otherwise False
        
        @return: True if the photo camera is on otherwise False
        
        @raise Exception: if no photo camera GPIO link file exists
        '''
        if not self._linkfolder:
            raise Exception('link folder does not exist')
        if self._photocamGPIOLink:
            self._photocamGPIOLock.acquire()
            stat = self._getGPIOStatus(self._photocamGPIOLink).rsplit(None, 1)[1]
            self._photocamGPIOLock.release()
            if (self._photocamGPIOOnOnSet and stat == 'set') or (not self._photocamGPIOOnOnSet and stat == 'clear'):
                return True
            else:
                return False
        else:
            raise Exception('photo camera GPIO link file is inexistent in >%s<' % (self._linkfolder,))
                
    
    
    def wlanOn(self):
        '''
        Turns the wlan on
        
        @raise Exception: if no wlan GPIO link file exists
        '''
        if not self._linkfolder:
            raise Exception('link folder does not exist')
        if self._wlanGPIOLink:
            self._logger.info('turning wlan on')
            self._wlanGPIOLock.acquire()
            if self._wlanGPIOOnOnSet:
                self._gpioLinkAction(self._wlanGPIOLink, True)
            else:
                self._gpioLinkAction(self._wlanGPIOLink, False)
            self._wlanGPIOLock.release()
        else:
            raise Exception('Wlan GPIO link file is inexistent in >%s<' % (self._linkfolder,))
        
    
    
    def wlanOff(self):
        '''
        Turns the wlan off if no one still needs it.
        
        @return: True if wlan has been switched off otherwise False
        
        @raise Exception: if no wlan GPIO link file exists
        '''
        if not self._linkfolder:
            raise Exception('link folder does not exist')
        if not self._backlogMain.duty_cycle_mode:
            raise Exception('wlan can only be turned off in duty-cycle mode')
        if self._wlanGPIOLink:
            if self._backlogMain.wlanNeeded():
                return False
            else:
                self._logger.warning('turning wlan off')
                self._wlanGPIOLock.acquire()
                if self._wlanGPIOOnOnSet:
                    self._gpioLinkAction(self._wlanGPIOLink, False)
                else:
                    self._gpioLinkAction(self._wlanGPIOLink, True)
                self._wlanGPIOLock.release()
                return True
        else:
            raise Exception('Wlan GPIO link file is inexistent in >%s<' % (self._linkfolder,))
    
    
    def getWlanStatus(self):
        '''
        Returns True if the wlan is on otherwise False
        
        @return: True if the wlan is on otherwise False
        
        @raise Exception: if no wlan GPIO link file exists
        '''
        if not self._linkfolder:
            raise Exception('link folder does not exist')
        if self._wlanGPIOLink:
            self._wlanGPIOLock.acquire()
            stat = self._getGPIOStatus(self._wlanGPIOLink).rsplit(None, 1)[1]
            self._wlanGPIOLock.release()
            if (self._wlanGPIOOnOnSet and stat == 'set') or (not self._wlanGPIOOnOnSet and stat == 'clear'):
                return True
            else:
                return False
        else:
            raise Exception('Wlan GPIO link file is inexistent in >%s<' % (self._linkfolder,))
            
            
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
    
    
    def _gpioLinkAction(self, link, set):
        '''
        This function should not be used directly!
        '''
        file = open(link, 'w')
        if set:
            file.write('set')
        else:
            file.write('clear')
        file.close()
        
        
    def _getGPIOStatus(self, link):
        '''
        This function should not be used directly!
        '''
        file = open(link, 'r')
        stat = file.read()
        file.close()
        return stat
        
    