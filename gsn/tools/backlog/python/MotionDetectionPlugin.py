#! /usr/bin/python
# -*- coding: UTF-8 -*-
__author__      = "David Hasenfratz <hasenfratz@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2011, ETH Zurich, Switzerland, David Hasenfratz"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import BackLogMessage
from AbstractPlugin import AbstractPluginClass

import STEVALDriver
import numpy
import time
from threading import Event
    
# Query accelerometer to detect whether vehicle is moving or not
class MotionDetectionPluginClass(AbstractPluginClass):
    
    def __init__(self, parent, config):
        
        AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG)
        
        self._sleeper = Event()
        self._stopped = False
        self._std_x = 0
        self._std_y = 0
        self._std_z = 0
        self._moving = -1
        self._data = ''

        self.info('Init MotionDetectionPlugin...')
        
        self._stdThreshold = self.getOptionValue('std_threshold')
        self._pollInterval = self.getOptionValue('poll_interval')
        self._pollDuration = self.getOptionValue('poll_duration')
        self._deviceStr = self.getOptionValue('acc_device')
        
        if self._pollInterval <= self._pollDuration:
            self.warning('Bad configuration, poll interval is smaller equal than poll duration!')
            self._pollInterval = self._pollDuration + 2
        
        self.steval = STEVALDriver.STEVALDriver([self._deviceStr])
        #steval._setSensor()
        
    def getMsgType(self):
        return BackLogMessage.MOTION_DETECTION_MESSAGE_TYPE 

    def isBusy(self):
        return False
    
    def run(self):
        self.name = 'MotionDetectionPlugin-Thread'
        self.info('started')
        
        while not self._stopped:
            # TODO: is self.action('') blocking? if yes then call wait with minus execution time!
            self._sleeper.wait(self._pollInterval)
            if self._sleeper.isSet():
                continue
            self.action('')
        self.info('died')

    def action(self, parameters):
                
        startTime = time.time()
        
        self.info('MotionDetectionPlugin started...')
        self._data = ''
        setRes = self.steval._setSensor()
        if setRes == 1:
            self.info('STEVAL: setSensor() success')
        
            endTime =  startTime
            while (endTime - startTime) < self._pollDuration:
                msg = str(steval._startDataAcquisitionDebug(150))
                self._data += msg
                steval._stopDataAcquisition()
                endTime = time.time()
            
            self.detectMotion()
        else:
            self.error('setSensor() failed, can not get sensor readings')
            self._moving = -1
        
        # Send statistics
        # TODO: no more than once per minute
        dataPackage = [self._stdThreshold, self._pollInterval, self._pollDuration, self._std_x, self._std_y, self._std_z, self._moving]
        self.processMsg(self.getTimeStamp(), dataPackage)
            
    def isBusy(self):
        return False
        
    def needsWLAN(self):
        return False

    def stop(self):
        self._stopped = True
        self._sleeper.set()
        self.steval._unsetSensor()
        self.steval._closeDevice()
        self.info('stopped')

    def detectMotion(self):

        xData = parseData('x')
        yData = parseData('y')
        zData = parseData('z')

        xStd = numpy.std(xData)
        yStd = numpy.std(yData)
        zStd = numpy.std(zData)
        
        self.info('xStd is ' + str(xStd) + ' yStd is ' + str(yStd) + ' and zStd is ' + str(zStd));

        if xStd <= self._stdThreshold and yStd <= self._stdThreshold and zStd <= self._stdThreshold:
            self.info('Vehicle is not moving')
            self._moving = 0
        else:
            self.info('Vehicle is moving')
            self._moving = 1
            
    def parseData(axis):
        axisData = []
  
        r = self._data.find(axis)
        while r != -1:
            axisData.append(int(self._data[r+2:r+8]))
            r = self._data.find(axis, r+1)

        return axisData
