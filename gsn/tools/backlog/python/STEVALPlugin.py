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
import math
import numpy
import time
from numpy import *

DEFAULT_BACKLOG = True

STATIC_DATA = 1
DYNAMIC_RAW_DATA = 2
DYNAMIC_PROC_DATA = 3
DYNAMIC_RAW_PROC_DATA = 4

RAW_OPT = '1'
PROC_OPT = '2'
RAW_PROC_OPT = '3'

# Static data for helper functions
g_range = 2
adc_factor = pow(2,15)
NOTEXIST = 0
sampling_rate = 40
selection_points = 10

    
# Reads messages from the STEVALDriver and send it to GSN
class STEVALPluginClass(AbstractPluginClass):
    
    def __init__(self, parent, config):
        
        AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG)
        self._timer = None
        self._stopped = False
        self._interval = None
        self._data = [[],[],[]]

        self.info('Init STEVALPlugin...')
        
        self._deviceStr = self.getOptionValue('STEVAL_MKIxx_device')
        self.steval = STEVALDriver.STEVALDriver([self._deviceStr])
        self._outputOpt = self.getOptionValue('STEVAL_MKIxx_outputOpt')
        self._threshold_local_extrema = self.getOptionValue('STEVAL_MKIxx_threshold_local_extrema')
        self._duration = float(self.getOptionValue('STEVAL_MKIxx_duration'))
        
        self.steval._openDevice()
        self._deviceNum = self.steval._getDevName();
        self._firmwareNum = self.steval._getVerNumber();
        self.steval._closeDevice()
        self.info('STEVAL: init steval success')
        dataPackage = [STATIC_DATA]
        dataPackage += [self._deviceNum]
        dataPackage += [self._firmwareNum]
        self.processMsg(self.getTimeStamp(), dataPackage)

    def getMsgType(self):
        return BackLogMessage.STEVAL_MESSAGE_TYPE        

    def isBusy(self):
        return False

    def action(self, parameters):
        self.info('STEVALPlugin action...')
        
        self.steval._openDevice()
        self._data = self.steval._startDataAcquisition(self._duration)
        
        self.info("STEVAL: Data read done -- data points per axes: " + str(len(self._data[0])))
        
        if self._outputOpt == RAW_OPT:
            dataPackage = [DYNAMIC_RAW_DATA]
        elif self._outputOpt == PROC_OPT:
            dataPackage = [DYNAMIC_PROC_DATA]
        else:
            dataPackage = [DYNAMIC_RAW_PROC_DATA]
        
        self.dataProcessing(dataPackage)
        dataPackage += [parameters]
        self.processMsg(self.getTimeStamp(), dataPackage)
        if self._outputOpt == PROC_OPT:
            self.info(dataPackage)
            

    def remoteAction(self, parameters):
        self.action(parameters)

    def stop(self):
        self._stopped = True
        self.steval._closeDevice()
        self.info('STEVALPlugin stopped')
        
    '''
    Helper function
    '''
    def compute_local_extremas(self, buffer, threshold):
        lmin = lmax = update = 0
        i = 1
        while i<len(buffer):
            if (lmin<lmax):
                if (((buffer[lmax] - buffer[lmin]) >  threshold) and (update != 1)):
                    update = 1
                    
                if (buffer[i]>buffer[lmax]):
                    buffer[lmax] = NOTEXIST
                    lmax = i
                elif ((buffer[i]<buffer[lmin]) or ((buffer[lmax]-buffer[i]) > threshold)):
                    lmin = i
                    update = 0
                else: 
                    buffer[i] = NOTEXIST
            else:
                if (((buffer[lmax]-buffer[lmin]) > threshold) and (update != 1)) :
                    update = 1
                    
                if (buffer[i] < buffer[lmin]):
                    buffer[lmin] = NOTEXIST
                    lmin = i
                elif ((buffer[i] > buffer[lmax]) or ((buffer[i]-buffer[lmin]) > threshold)):
                    update = 0
                    lmax = i
                else:
                    buffer[i] = NOTEXIST
            i=i+1
        buffer[0]=abs(buffer[0])
        return buffer
    
    def compute_fft_and_l2(self, buffer):
        if len(buffer) == 0:
            return []
        
        for i in range(len(buffer)): 
            buffer[i] = buffer[i]*float(g_range)/float(adc_factor)
          
        fft = numpy.fft.rfft(buffer,len(buffer))
        
        re = numpy.real(fft)
        im = numpy.imag(fft)
        l2array = []
        for i in range(len(re)): 
            l2array.append(math.sqrt(re[i]**2 + im[i]**2))
        return l2array
    
    def generate_fftresult(self, axis):
        arrayFFT = self.compute_fft_and_l2(self._data[axis])
        arrayLE = self.compute_local_extremas(arrayFFT, self._threshold_local_extrema)
        return arrayLE
            
    def dataSelection(self, array):
        max = numpy.zeros((selection_points,2))
        freq_ar = [x * float(sampling_rate)/float(len(array)) for x in range(0, len(array))]
        count = 0
        a = numpy.array(array)
        while count < selection_points:
            maxindex = a.argmax()
            max[count][0] = a[maxindex]
            max[count][1] = freq_ar[maxindex]
            a[maxindex] = 0
            count = count+1
        return max

    def dataProcessing(self, dataPackage):
         
        # packet creation for GSN        
        if self._outputOpt == RAW_OPT:
            dataPackage += [self._duration]
            dataPackage += [time.time()]
            dataPackage += [str(self._data[0])]
            dataPackage += [str(self._data[1])]
            dataPackage += [str(self._data[2])]
            
        else:
            xData = self.generate_fftresult(0)
            yData = self.generate_fftresult(1)
            zData = self.generate_fftresult(2)
            
            xOut = self.dataSelection(xData)
            yOut = self.dataSelection(yData)
            zOut = self.dataSelection(zData)
            if self._outputOpt == PROC_OPT:
                dataPackage += [self._duration]
                dataPackage += [time.time()]
                
                for i in range(0,selection_points):
                    for j in range(0,2):
                        dataPackage += [float(xOut[i][j])]
                for i in range(0,selection_points):
                    for j in range(0,2):
                        dataPackage += [float(yOut[i][j])]
                for i in range(0,selection_points):
                    for j in range(0,2):
                        dataPackage += [float(zOut[i][j])]
            
            else: # self._outputOpt == RAW_PROC_OPT :
                dataPackage += [self._duration]
                dataPackage += [time.time()]
                dataPackage += [str(self._data[0])]
                dataPackage += [str(self._data[1])]
                dataPackage += [str(self._data[2])]
                
                for i in range(0,selection_points):
                    for j in range(0,2):
                        dataPackage += [float(xOut[i][j])]
                for i in range(0,selection_points):
                    for j in range(0,2):
                        dataPackage += [float(yOut[i][j])]
                for i in range(0,selection_points):
                    for j in range(0,2):
                        dataPackage += [float(zOut[i][j])]
            