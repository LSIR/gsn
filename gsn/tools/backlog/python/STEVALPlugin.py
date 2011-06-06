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
import sys
import time
import os
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

        self.info('Init STEVALPlugin...')
        
        self._deviceStr = self.getOptionValue('STEVAL_MKIxx_device')
        self.steval = STEVALDriver.STEVALDriver([self._deviceStr])
        self._outputOpt = self.getOptionValue('STEVAL_MKIxx_outputOpt')
        self._threshold_local_extrema = self.getOptionValue('STEVAL_MKIxx_threshold_local_extrema')
        self._log_path = self.getOptionValue('STEVAL_MKIxx_log_path')
        self._log_save_flag = self.getOptionValue('STEVAL_MKIxx_log_save_flag')
        
        self.steval._openDevice()
        self._deviceNum = self.steval._getDevName();
        self._firmwareNum = self.steval._getVerNumber();
        self.steval._unsetSensor()
        self.steval._closeDevice()
        self.info('STEVAL: unsetSensor() success')
        dataPackage = [STATIC_DATA]
        dataPackage += [self._deviceNum]
        dataPackage += [self._firmwareNum]
        self.processMsg(self.getTimeStamp(), dataPackage)

    def getMsgType(self):
        return BackLogMessage.STEVAL_MESSAGE_TYPE        

    def isBusy(self):
        return False

    def action(self, parameters):
        self.steval._openDevice()
        self.info('STEVALPlugin started...')
        ret = self.steval._setSensor()
        if ret == 1:
            self.info('STEVAL: setSensor() success')
            setValue = 1
        readFlag = 0
        diff = 0
        dataFile = self._log_path
        dataFile += '/data.txt'
        if setValue == 1:
            msg = ''
            fHdl = open (dataFile, 'w' )
            start = time.time()
            end = start
            while (end - start) < 20:
                t0 = time.time()
                msg = str(self.steval._startDataAcquisitionDebug())
                t1 = time.time()
                diff += (t1-t0)
                end = t1
                fHdl.write (msg)
            fHdl.close ()
            self.steval._stopDataAcquisition()
            self.steval._unsetSensor()
            self.steval._closeDevice()
            self.info("STEVAL: Data read -- message length = " + str(len(msg)))
            readFlag = 1;
            timeFile = self._log_path
            timeFile +='/time.txt'            
            tFile = open(timeFile,'w')
            tFile.write(str(diff))
            tFile.write('\n')
            tFile.write(str(start))
            tFile.write('\n')
            tFile.close()
            
            self.info('STEVAL reading done')
        else:
            self.warning ('STEVAL read failed')
        
        if readFlag == 1:
            if self._outputOpt == RAW_OPT:
                dataPackage = [DYNAMIC_RAW_DATA]
            elif self._outputOpt == PROC_OPT:
                dataPackage = [DYNAMIC_PROC_DATA]
            else:
                dataPackage = [DYNAMIC_RAW_PROC_DATA]
            
            self.dataProcessing(self._outputOpt, dataPackage)
            self.processMsg(self.getTimeStamp(), dataPackage)
            

    def stop(self):
        self._stopped = True
        self.steval._unsetSensor()
        self.info('stopped')
        
        
        
        
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
        fft = numpy.fft.rfft(buffer,len(buffer))
        re = numpy.real(fft)
        im = numpy.imag(fft)
        l2array = []
        for i in range(len(re)): 
            l2array.append(math.sqrt(re[i]**2 + im[i]**2))
        return l2array
    
    def io_read_data(self, filename):
        buffer = []
        file = open(filename, 'r')
        while 1:
            line = file.readline()
            if not line:
                break
            val = float(line)
            buffer.append((val*g_range)/adc_factor)
        file.close()
        return buffer
    
    def generate_fftresult(self,filename):
        array = self.io_read_data(filename)
        freqs = [x * float(sampling_rate)/float(len(array)/2) for x in range(0, len(array)/2)]
        arrayFFT = self.compute_fft_and_l2(array)
        param = self._threshold_local_extrema 
        arrayLE = self.compute_local_extremas(arrayFFT, param)
        return arrayLE
    
    def generate_axisFile(self, source, destination, character, offset):
        sourceFile = open(source,'r')
        destinationFile = open(destination,'w')
        
        sourceFile.seek(0, 0);
        sourceFile.seek(offset);
        ch = sourceFile.read(1)
        arrayId = 0
        flag = 0
        if character == ch:
            throw_ch = sourceFile.read(1)
            while(arrayId == 0):
                num = 0
                sign_ch = sourceFile.read(1)
                if sign_ch == "":
                    arrayId = 1
                    break
                else:
                    if sign_ch == '+':
                        sign_val = 1
                    else:
                        sign_val = -1
                    i = 0
                    while i < 5:
                        multip =  math.pow(10,(4-i))
                        tmp = sourceFile.read(1)
                        i = i+1
                        if len(tmp) > 0:
                            tmp_val = ord(tmp)
                            ch = (tmp_val - 48) * multip
                            num = num + ch
        
                    if(sign_val*num < 65538):
                        destinationFile.write(str(int(sign_val*num)))
                        destinationFile.write('\n')
                    lastPos = sourceFile.tell()
                    sourceFile.seek(lastPos+32)
        
        destinationFile.close()
        sourceFile.close()
        
    def dataSelection(self, array):
        max = numpy.zeros((selection_points,2))
        freq_ar = [x * float(sampling_rate)/float(len(array)) for x in range(0, len(array))]
        count = 0
        a=numpy.array(array)
        while count < selection_points:
            maxindex = a.argmax()
            max[count][0] = a[maxindex]
            max[count][1] = freq_ar[maxindex]
            a[maxindex] = 0
            count = count+1
        return max

    
    def dataProcessing(self, option, dataPackage):
        dataFile = self._log_path
        dataFile += '/data.txt'
        xFile = self._log_path
        xFile += '/x.txt'
        self.generate_axisFile(dataFile, xFile, 'x', 0)
        yFile = self._log_path
        yFile += '/y.txt'
        self.generate_axisFile(dataFile, yFile, 'y', 13)
        zFile = self._log_path
        zFile += '/z.txt'        
        self.generate_axisFile(dataFile, zFile, 'z', 26)
         
        ## packet creation for GSN
        tFile = self._log_path
        tFile +='/time.txt'        
        timeFile = open(tFile,'r')
        duration = timeFile.readline()
        duration = duration.rstrip('\n')
        startTime = timeFile.readline()
        startTime = startTime.rstrip('\n')
        #msg = []
        
        if option == RAW_OPT:
            #msg += [str(option)]
            dataPackage += [duration]
            dataPackage += [startTime]
            fp = open(xFile,'r')
            tmp_msg = fp.read()
            fp.close()
            len1 = len(tmp_msg)
            dataPackage += [str(len1)]
            dataPackage += [tmp_msg.replace("\n",",")]
            fp = open(yFile,'r')
            tmp_msg = fp.read()
            fp.close()
            len1 = len(tmp_msg)
            dataPackage += [str(len1)]
            dataPackage += [tmp_msg.replace("\n",",")]
            fp = open(zFile,'r')
            tmp_msg = fp.read()
            fp.close()
            len1 = len(tmp_msg)
            dataPackage += [str(len1)]
            dataPackage += [tmp_msg.replace("\n",",")]            
            
        else:
            xData = self.generate_fftresult(xFile)
            yData = self.generate_fftresult(yFile)
            zData = self.generate_fftresult(zFile)
            
            xOut = self.dataSelection(xData)
            yOut = self.dataSelection(yData)
            zOut = self.dataSelection(zData)
            if option == PROC_OPT:
                #msg += [str(option))
                dataPackage += [duration]
                dataPackage += [startTime]
                dataPackage += [str(xOut.size)]
                dataPackage += [str(xOut.itemsize)]
                for i in range(0,selection_points):
                    for j in range(0,2):
                        dataPackage += [str(xOut[i][j])]
                dataPackage += [str(yOut.size)]
                dataPackage += [str(yOut.itemsize)]
                for i in range(0,selection_points):
                    for j in range(0,2):
                        dataPackage += [str(yOut[i][j])]
                dataPackage += [str(zOut.size)]
                dataPackage += [str(zOut.itemsize)]
                for i in range(0,selection_points):
                    for j in range(0,2):
                        dataPackage += [str(zOut[i][j])]
            
            else: # option == RAW_PROC_OPT :
                #msg += [str(option))
                dataPackage += [duration]
                dataPackage += [startTime]
                fp = open(xFile,'r')
                tmp_msg = fp.read()
                fp.close()
                len1 = len(tmp_msg)
                dataPackage += [str(len1)]
                dataPackage += [tmp_msg.replace("\n",",")]
                fp = open(yFile,'r')
                tmp_msg = fp.read()
                fp.close()
                len1 = len(tmp_msg)
                dataPackage += [str(len1)]
                dataPackage += [tmp_msg.replace("\n",",")]
                fp = open(zFile,'r')
                tmp_msg = fp.read()
                fp.close()
                len1 = len(tmp_msg)
                dataPackage += [str(len1)]
                dataPackage += [tmp_msg.replace("\n",",")]
                dataPackage += [str(xOut.size)]
                dataPackage += [str(xOut.itemsize)]
                dataPackage += [str(len(xOut))]
                for i in range(0,selection_points):
                    for j in range(0,2):
                        dataPackage += [str(xOut[i][j])]
                dataPackage += [str(yOut.size)]
                dataPackage += [str(yOut.itemsize)]
                for i in range(0,selection_points):
                    for j in range(0,2):
                        dataPackage += [str(yOut[i][j])]
                dataPackage += [str(zOut.size)]
                dataPackage += [str(zOut.itemsize)]
                for i in range(0,selection_points):
                    for j in range(0,2):
                        dataPackage += [str(zOut[i][j])]
            
            if self._log_save_flag == "TRUE":
                destTime = tFile
                destTime +='.'
                destTime += startTime
                os.rename(tFile,destTime)
                
                dest = xFile
                dest += '.'
                dest += startTime
                os.rename(xFile,dest)
                dest = yFile
                dest += '.'
                dest += startTime
                os.rename(yFile,dest)
                dest = zFile
                dest += '.'
                dest += startTime
                os.rename(zFile,dest)
            else:
                os.remove(tFile)
                os.remove(xFile)
                os.remove(yFile)
                os.remove(zFile)
            os.remove(dataFile)
        #return msg        

    

        