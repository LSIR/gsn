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
    This class can be used to get statistics like packet count per time unit.
    
    '''

    '''
    data/instance attributes:
    _timers
    _counters
    _counterLocks
    _counterRingBuffers
    _counterRingBufSizeDelta
    '''

    def __init__(self, counterRingBufferSizeInSeconds=0):
        self._timers = dict()
        self._counters = dict()
        self._counterLocks = dict()
        
        self._calcStats = True
        
        self._calcMean = False
        if counterRingBufferSizeInSeconds > 0:
            self._calcMean = True
            self._counterRingBuffers = dict()
            self._counterRingBufSizeSec = counterRingBufferSizeInSeconds
            self._counterRingBufSizeDelta = timedelta(seconds=counterRingBufferSizeInSeconds)
        
        
    def counterAction(self, uniqueCounterId=None, increaseBy=1):
        if self._calcStats:
            if uniqueCounterId:
                self._counterLocks[uniqueCounterId].acquire()
                cnt = self._counters[uniqueCounterId]
                self._counters.update({uniqueCounterId: cnt+increaseBy})
                if self._calcMean:
                    now = datetime.utcnow()
                    buffer = self._counterRingBuffers[uniqueCounterId]
                    buffer.append([now, increaseBy])
                    self._counterRingBuffers.update({uniqueCounterId: buffer})
            else:
                uniqueCounterId = uuid.uuid4()
                self._counterLocks.update({uniqueCounterId: Lock()})
                self._counterLocks[uniqueCounterId].acquire()
                self._counters.update({uniqueCounterId: increaseBy})
                if self._calcMean:
                    now = datetime.utcnow()
                    buffer = [[now, increaseBy]]
                    self._counterRingBuffers.update({uniqueCounterId: buffer})
                    self._minTimerId = self.timeMeasurementStart()
               
            if self._calcMean and self.timeMeasurementDiff(self._minTimerId, False) > MIN_RING_BUFFER_CLEAR_INTERVAL_SEC:
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
        if self._calcStats:
            self._counterLocks[uniqueCounterId].acquire()
            cnt = self._counters[uniqueCounterId]
            self._counterLocks[uniqueCounterId].release()
            return cnt
        else:
            return None
        
        
    def getMeanPerSecond(self, uniqueCounterId, meanOverLastNSecondsList):
        n = len(meanOverLastNSecondsList)
        if self._calcMean and self._calcStats:
            counters = [0]*n
            self._counterLocks[uniqueCounterId].acquire()
            now = datetime.utcnow()
            for dt, entry in self._counterRingBuffers[uniqueCounterId]:
                for i in range(n):
                    if meanOverLastNSecondsList[i] > self._counterRingBufSizeSec:
                        raise Exception('meanOverLastNSecondsList can not be bigger than counterRingBufferSizeInSeconds')
                    if dt >= now - timedelta(seconds=meanOverLastNSecondsList[i]):
                        counters[i] += entry
            self._counterLocks[uniqueCounterId].release()
            
            ret = []
            for i in range(n):
                ret.append(counters[i]/float(meanOverLastNSecondsList[i]))
            if self._calcStats:
                return ret
       
        return [None]*n
        
        
    def removeCounter(self, uniqueCounterId):
        self._counterLocks[uniqueCounterId].acquire()
        del self._counters[uniqueCounterId]
        if self._calcMean:
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
            return (diff.microseconds + (diff.seconds + diff.days * 24 * 3600) * 10**6) / 10.0**6
        else:
            return None
    
    
    def stopAccumulation(self):
        self._calcStats = False
    
    
    def restartAccumulation(self):
        self._calcStats = True
        