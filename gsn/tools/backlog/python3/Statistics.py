__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import uuid
from datetime import datetime, timedelta
from threading import Lock, Thread

MIN_RING_BUFFER_CLEAR_INTERVAL_SEC = 5

class StatisticsClass:
    '''
    This class can be used to generate statistics information. It offers counter
    and timing functionality.
    
    '''

    '''
    data/instance attributes:
    _timers
    _calcStats
    _calcAvgMinMeanMax
    _counters
    _counterLocks
    _counterRingBuffers
    _counterRingBufSizeSec
    _counterRingBufSizeDelta
    '''

    def __init__(self, counterRingBufferSizeInSeconds=0):
        '''
        @param counterRingBufferSizeInSeconds: Specifies how old entries in the internal ringbuffer
                                                can maximally get in seconds. If this parameter is
                                                set to 0 no ringbuffer will be used. Thus, no
                                                min/mean/max values can be requested. If min/mean/max
                                                values are needed, it should be set at least to the
                                                biggest period over which min/mean/max values should
                                                be calculated.
        '''
        self._timers = dict()
        self._counters = dict()
        self._counterLocks = dict()
        
        self._calcStats = True
        
        self._calcAvgMinMeanMax = False
        if counterRingBufferSizeInSeconds > 0:
            self._calcAvgMinMeanMax = True
            self._counterRingBuffers = dict()
            self._counterRingBufSizeSec = counterRingBufferSizeInSeconds
            self._counterRingBufSizeDelta = timedelta(seconds=counterRingBufferSizeInSeconds)
        
        
    def counterAction(self, uniqueCounterId=None, increaseBy=1):
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
            if uniqueCounterId:
                self._counterLocks[uniqueCounterId].acquire()
                cnt = self._counters[uniqueCounterId]
                self._counters.update({uniqueCounterId: cnt+increaseBy})
                if self._calcAvgMinMeanMax:
                    now = datetime.utcnow()
                    buffer = self._counterRingBuffers[uniqueCounterId]
                    buffer.append([now, increaseBy])
                    self._counterRingBuffers.update({uniqueCounterId: buffer})
            else:
                uniqueCounterId = uuid.uuid4()
                self._counterLocks.update({uniqueCounterId: Lock()})
                self._counterLocks[uniqueCounterId].acquire()
                self._counters.update({uniqueCounterId: increaseBy})
                if self._calcAvgMinMeanMax:
                    now = datetime.utcnow()
                    buffer = [[now, increaseBy]]
                    self._counterRingBuffers.update({uniqueCounterId: buffer})
                    self._minTimerId = self.timeMeasurementStart()
               
            if self._calcAvgMinMeanMax and self.timeMeasurementDiff(self._minTimerId, False) > MIN_RING_BUFFER_CLEAR_INTERVAL_SEC:
                self.timeMeasurementDiff(self._minTimerId)
                self._minTimerId = self.timeMeasurementStart()
                newerEntries = False
                for index, entry in enumerate(buffer):
                    if entry[0] >= now - self._counterRingBufSizeDelta:
                        newerEntries = True
                        break
                if newerEntries:
                    self._counterRingBuffers.update({uniqueCounterId: buffer[index:]})
                else:
                    self._counterRingBuffers.update({uniqueCounterId: list()})
                    
            self._counterLocks[uniqueCounterId].release()
            
        return uniqueCounterId
        
        
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
        if self._calcAvgMinMeanMax and self._calcStats:
            counters = [0]*n
            self._counterLocks[uniqueCounterId].acquire()
            now = datetime.utcnow()
            for dt, entry in self._counterRingBuffers[uniqueCounterId]:
                for i in range(n):
                    if avgPerSecOverLastNSecondsList[i] > self._counterRingBufSizeSec:
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
        if self._calcAvgMinMeanMax and self._calcStats:
            min = [None]*n
            self._counterLocks[uniqueCounterId].acquire()
            now = datetime.utcnow()
            for dt, entry in self._counterRingBuffers[uniqueCounterId]:
                for i in range(n):
                    if minOverLastNSecondsList[i] > self._counterRingBufSizeSec:
                        raise Exception('minOverLastNSecondsList entry can not be bigger than counterRingBufferSizeInSeconds')
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
        if self._calcAvgMinMeanMax and self._calcStats:
            counters = [0]*n
            entries = [0]*n
            self._counterLocks[uniqueCounterId].acquire()
            now = datetime.utcnow()
            for dt, entry in self._counterRingBuffers[uniqueCounterId]:
                for i in range(n):
                    if avgOverLastNSecondsList[i] > self._counterRingBufSizeSec:
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
        if self._calcAvgMinMeanMax and self._calcStats:
            max = [None]*n
            self._counterLocks[uniqueCounterId].acquire()
            now = datetime.utcnow()
            for dt, entry in self._counterRingBuffers[uniqueCounterId]:
                for i in range(n):
                    if maxOverLastNSecondsList[i] > self._counterRingBufSizeSec:
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
        if self._calcAvgMinMeanMax:
            del self._counterRingBuffers[uniqueCounterId]
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
            return diff.total_seconds()
        else:
            return None
    
    
    def stopAccumulation(self):
        self._calcStats = False
    
    
    def restartAccumulation(self):
        self._calcStats = True
        