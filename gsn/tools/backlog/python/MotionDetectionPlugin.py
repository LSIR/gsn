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

DEFAULT_BACKLOG = True
    
# Query accelerometer to detect whether vehicle is moving or not
class MotionDetectionPluginClass(AbstractPluginClass):
    
    def __init__(self, parent, config):
        
        AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG)
        
        self._sleeper = Event()
        self._stopped = False
        self._std_x = float(0)
        self._std_y = float(0)
        self._std_z = float(0)
        self._moving = 0
        self._data = ''
        self._numPolls = 0

        self.info('Init MotionDetectionPlugin...')
        
        self._stdThreshold = float(self.getOptionValue('std_threshold'))
        self._pollInterval = float(self.getOptionValue('poll_interval'))
        self._pollDuration = float(self.getOptionValue('poll_duration'))
        self._sendStatistic = int(self.getOptionValue('send_statistic'))
        self._deviceStr = self.getOptionValue('acc_device')
        
        if self._pollInterval <= self._pollDuration:
            self.warning('Bad configuration, poll interval is smaller equal than poll duration!')
            self._pollInterval = self._pollDuration + 5
        
        self.steval = STEVALDriver.STEVALDriver([self._deviceStr])
        #self.steval._setSensor()
        
    def getMsgType(self):
        return BackLogMessage.MOTION_DETECTION_MESSAGE_TYPE 

    def isBusy(self):
        return False
    
    def run(self):
        self.name = 'MotionDetectionPlugin-Thread'
        self.info('started')
        t = time.time()
        
        while not self._stopped:
            self._sleeper.wait(self._pollInterval - (time.time() - t))
            if self._sleeper.isSet():
                continue
            t = time.time()
            self.action('')
        self.info('died')

    def action(self, parameters):
        
        self.steval._openDevice()
        self._data = ''
        setRes = self.steval._setSensor()
        if setRes == 1:
        
            startTime = time.time()
            endTime =  startTime
            while (endTime - startTime) < self._pollDuration:
                msg = str(self.steval._startDataAcquisitionDebug(150))
                self._data += msg
                self.steval._stopDataAcquisition()
                endTime = time.time()
            
            self.detectMotion()
        else:
            self.error('setSensor() failed, can not get sensor readings')
        
        self.steval._closeDevice()
        self._numPolls = self._numPolls + 1
        # Send statistics
        if self._numPolls == self._sendStatistic:
            self._numPolls = 0
            dataPackage = [self._stdThreshold, self._pollInterval, self._pollDuration, self._std_x, self._std_y, self._std_z, self._moving]
            self._moving = 0
            self.info(dataPackage)
            self.processMsg(self.getTimeStamp(), dataPackage)
            
    def isBusy(self):
        return False
        
    def needsWLAN(self):
        return False

    def stop(self):
        self._stopped = True
        self._sleeper.set()
        self.info('stopped')

    def detectMotion(self):

        xData = self.parseData('x')
        yData = self.parseData('y')
        zData = self.parseData('z')

        std_x = float(numpy.std(xData))
        std_y = float(numpy.std(yData))
        std_z = float(numpy.std(zData))
        
        self.info('xStd is ' + str(std_x) + ' yStd is ' + str(std_y) + ' and zStd is ' + str(std_z));

        if std_x <= self._stdThreshold and std_y <= self._stdThreshold and std_z <= self._stdThreshold:
            self.info('Vehicle is not moving')
            # Execute action function of the specified plugins
            pluginList = [BackLogMessage.OZ47_MESSAGE_TYPE, BackLogMessage.GPS_NAV_MESSAGE_TYPE, BackLogMessage.ALPHASENSE_MESSAGE_TYPE]
            num = self.runPluginRemoteAction(pluginList, None)
            self.info('remoteAction called from ' + str(num) + ' of ' + str(len(pluginList)) + ' plugins')
        else:
            self.info('Vehicle is moving')
            self._moving = self._moving + 1
            # Store std values from the last motion detection
            self._std_x = std_x
            self._std_y = std_y
            self._std_z = std_z
            
    def parseData(self, axis):
        axisData = []
  
        r = self._data.find(axis)
        while r != -1:
            axisData.append(int(self._data[r+2:r+8]))
            r = self._data.find(axis, r+1)

        return axisData
