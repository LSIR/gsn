#! /usr/bin/python
# -*- coding: UTF-8 -*-
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"

import logging
import Queue
import os
import shutil
import time
import thread
from threading import Thread, Event, Lock, Timer

from ScheduleHandler import SUBPROCESS_BUG_BYPASS
if SUBPROCESS_BUG_BYPASS:
    import SubprocessFake
    subprocess = SubprocessFake
else:
    import subprocess

import BackLogMessage
from AbstractPlugin import AbstractPluginClass

try: 
    import serial
except ImportError, e: 
    print "Please install PySerial first."
    sys.exit(1)

DEFAULT_BACKLOG = True
GPHOTO2 = '/usr/bin/gphoto2'
WGET = '/usr/bin/wget'
TMPPICTUREFOLDER = '/media/card/backlog/binaryplugin/tmp/'
DEFAULT_GPHOTO2_SETTINGS = ['/main/capturesettings/evstep=0',
                            '/main/imgsettings/imagequality=0',
                            '/main/imgsettings/imagesize=2',
                            '/main/imgsettings/whitebalance=0',
                            '/main/capturesettings/exposurecompensation=15',
                            '/main/capturesettings/expprogram=1',
                            '/main/imgsettings/autoiso=0',
                            '/main/capturesettings/bracketing=1',
                            '/main/capturesettings/burstinterval=0']

TASK_MESSAGE = 0
POWER_MESSAGE = 1

PANORAMA_TASK = 0
PICTURE_TASK = 1
POSITIONING_TASK = 2
MODE_TASK = 3
CALIBRATION_TASK = 4

DSLR = 1
WEBCAM = 2
DEFAULT_CAMERA = DSLR

class CamZillaPluginClass(AbstractPluginClass):
    '''
    This plugin offers the functionality to control the CamZilla robot.
    '''

    '''
    data/instance attributes:
    _manualControl
    _autofocus
    _focusAllowed
    _x
    _y
    _parkX
    _parkY
    _calibrated
    _delay
    _powerSaveMode
    _keepMinMbFree
    _dslrAvailable
    _pictureFolder
    _webcamAvailable
    _webcamHttp
    _webcamPictureFolder
    _robotAvailable
    _parkActionLock
    _parkTimer
    _writeLock
    _pulsesPerDegree
    _parkIdleTime
    _taskqueue
    _plugStop
    _serial
    _isBusy
    '''
    
    def __init__(self, parent, config):
        AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG, needPowerControl=True)
        self._isBusy = True
        
        self._serial = serial.Serial()
        self._taskqueue = Queue.Queue()
        self._delay = Event()
        self._parkTimer = None
        self._parkActionLock = Lock()
        self._writeLock = Lock()
        self._manualControl = True
        self._plugStop = False
        self._calibrated = False
        self._dslrAvailable = False
        self._x = 0.0
        self._y = 0.0
        self._parkX = 0
        self._parkY = 0
        self._autofocus = None
        self._focusAllowed = False
        self._powerSaveMode = False
        self._pictureFolder = None
        self._webcamAvailable = False
        self._webcamHttp = None
        self._webcamPictureFolder = None
        self._robotAvailable = False
        self._pulsesPerDegree = None
        self._parkIdleTime = None
            
        if not os.path.exists(GPHOTO2):
            raise TypeError('%s does not exist' % (GPHOTO2,))
        if not os.access(GPHOTO2, os.X_OK):
            raise TypeError('%s can not be executed' % (GPHOTO2,))
        
        value = self.getOptionValue('keep_min_mb_free')
        if value != None:
            self._keepMinMbFree = int(value)
            self.info('at least %dMb will be kept free' % (self._keepMinMbFree,))
        else:
            raise TypeError('keep_min_mb_free has to be specified')
        
        value = self.getOptionValue('dslr_available')
        if value != None and int(value) == 1:
            self.info('a primary DSLR camera is available')
            self._dslrAvailable = True
        else:
            self.info('there is no primary DSLR camera available')
        
        if self._dslrAvailable:
            self._pictureFolder = self.getOptionValue('picture_folder')
            if self._pictureFolder is None:
                raise TypeError('no picture_folder specified')
            if not os.path.isdir(self._pictureFolder):
                self.warning('picture folder >%s< is not a directory -> creating it' % (self._pictureFolder,))
                os.makedirs(self._pictureFolder)
                
            value = self.getOptionValue('focus')
            if value != None and int(value) == 1:
                self.info('focus can be used')
                self._focusAllowed = True
            else:
                self.info('focus is locked')
            
            value = self.getOptionValue('ext_port_dslr')
            if value is None:
                raise TypeError('no ext_port_dslr specified')
            elif int(value) == 1:
                self.dslrPowerStatus = self.getPowerControlObject().getExt1Status
                self.dslrPowerOn = self.getPowerControlObject().ext1On
                self.dslrPowerOff = self.getPowerControlObject().ext1Off
            elif int(value) == 2:
                self.dslrPowerStatus = self.getPowerControlObject().getExt2Status
                self.dslrPowerOn = self.getPowerControlObject().ext2On
                self.dslrPowerOff = self.getPowerControlObject().ext2Off
            elif int(value) == 3:
                self.dslrPowerStatus = self.getPowerControlObject().getExt3Status
                self.dslrPowerOn = self.getPowerControlObject().ext3On
                self.dslrPowerOff = self.getPowerControlObject().ext3Off
            else:
                raise TypeError('ext_port_dslr has to be set to 1, 2 or 3')
            
            if value == self.getOptionValue('ext_port_heater'):
                raise TypeError('ext_port_dslr and ext_port_heater must be different')
        
            value = self.getOptionValue('usb_port_dslr')
            if value is None:
                raise TypeError('no usb_port_dslr specified')
            elif int(value) == 1:
                self.dslrUsbStatus = self.getPowerControlObject().getUsb1Status
                self.dslrUsbOn = self.getPowerControlObject().usb1On
                self.dslrUsbOff = self.getPowerControlObject().usb1Off
            elif int(value) == 2:
                self.dslrUsbStatus = self.getPowerControlObject().getUsb2Status
                self.dslrUsbOn = self.getPowerControlObject().usb2On
                self.dslrUsbOff = self.getPowerControlObject().usb2Off
            elif int(value) == 3:
                self.dslrUsbStatus = self.getPowerControlObject().getUsb3Status
                self.dslrUsbOn = self.getPowerControlObject().usb3On
                self.dslrUsbOff = self.getPowerControlObject().usb3Off
            else:
                raise TypeError('usb_port_dslr has to be set to 1, 2 or 3')
        
        value = self.getOptionValue('ext_port_heater')
        if value is None:
            raise TypeError('no ext_port_heater specified')
        elif int(value) == 1:
            self.heaterPowerStatus = self.getPowerControlObject().getExt1Status
            self.heaterPowerOn = self.getPowerControlObject().ext1On
            self.heaterPowerOff = self.getPowerControlObject().ext1Off
        elif int(value) == 2:
            self.heaterPowerStatus = self.getPowerControlObject().getExt2Status
            self.heaterPowerOn = self.getPowerControlObject().ext2On
            self.heaterPowerOff = self.getPowerControlObject().ext2Off
        elif int(value) == 3:
            self.heaterPowerStatus = self.getPowerControlObject().getExt3Status
            self.heaterPowerOn = self.getPowerControlObject().ext3On
            self.heaterPowerOff = self.getPowerControlObject().ext3Off
        else:
            raise TypeError('ext_port_heater has to be set to 1, 2 or 3')
        
        
        value = self.getOptionValue('webcam_available')
        if value != None and int(value) == 1:
            self.info('a secondary webcam is available')
            self._webcamAvailable = True
        else:
            self.info('there is no secondary webcam available')
        
        if self._webcamAvailable:
            self._webcamHttp = self.getOptionValue('webcam_http')
            if self._webcamHttp is None:
                raise TypeError('webcam_http not specified')
            
            if not os.path.exists(WGET):
                raise TypeError('%s does not exist' % (WGET,))
            if not os.access(WGET, os.X_OK):
                raise TypeError('%s can not be executed' % (WGET,))
        
            self._webcamPictureFolder = self.getOptionValue('webcam_picture_folder')
            if self._webcamPictureFolder is None:
                raise TypeError('no webcam_picture_folder specified')
            if not os.path.isdir(self._webcamPictureFolder):
                self.warning('webcam picture folder >%s< is not a directory -> creating it' % (self._webcamPictureFolder,))
                os.makedirs(self._webcamPictureFolder)
        
        value = self.getOptionValue('robot_available')
        if value != None and int(value) == 1:
            self.info('a robot is available')
            self._robotAvailable = True
        else:
            self.info('there is no robot available')
        
        if self._robotAvailable:
            if value == self.getOptionValue('usb_port_robot'):
                raise TypeError('usb_port_dslr and usb_port_robot must be different')
            
            value = self.getOptionValue('usb_port_robot')
            if value is None:
                raise TypeError('no usb_port_robot specified')
            elif int(value) == 1:
                self.robotUsbStatus = self.getPowerControlObject().getUsb1Status
                self.robotUsbOn = self.getPowerControlObject().usb1On
                self.robotUsbOff = self.getPowerControlObject().usb1Off
            elif int(value) == 2:
                self.robotUsbStatus = self.getPowerControlObject().getUsb2Status
                self.robotUsbOn = self.getPowerControlObject().usb2On
                self.robotUsbOff = self.getPowerControlObject().usb2Off
            elif int(value) == 3:
                self.robotUsbStatus = self.getPowerControlObject().getUsb3Status
                self.robotUsbOn = self.getPowerControlObject().usb3On
                self.robotUsbOff = self.getPowerControlObject().usb3Off
            else:
                raise TypeError('usb_port_robot has to be set to 1, 2 or 3')
            
            value = self.getOptionValue('ext_port_robot')
            if value is None:
                raise TypeError('no ext_port_robot specified')
            elif int(value) == 1:
                self.robotPowerStatus = self.getPowerControlObject().getExt1Status
                self.robotPowerOn = self.getPowerControlObject().ext1On
                self.robotPowerOff = self.getPowerControlObject().ext1Off
            elif int(value) == 2:
                self.robotPowerStatus = self.getPowerControlObject().getExt2Status
                self.robotPowerOn = self.getPowerControlObject().ext2On
                self.robotPowerOff = self.getPowerControlObject().ext2Off
            elif int(value) == 3:
                self.robotPowerStatus = self.getPowerControlObject().getExt3Status
                self.robotPowerOn = self.getPowerControlObject().ext3On
                self.robotPowerOff = self.getPowerControlObject().ext3Off
            else:
                raise TypeError('ext_port_robot has to be set to 1, 2 or 3')
            
            if value == self.getOptionValue('ext_port_heater'):
                raise TypeError('ext_port_robot and ext_port_heater must be different')
            
            device = self.getOptionValue('device_name')
            if device is None:
                raise TypeError('no device_name specified')
            
            value = self.getOptionValue('pulses_per_degree')
            if value is None:
                raise TypeError('no pulses_per_degree value specified')
            else:
                self._pulsesPerDegree = float(value)
            
            value = self.getOptionValue('park_robot_idle_time')
            if value is None:
                raise TypeError('no park_robot_idle_time value specified')
            else:
                self._parkIdleTime = float(value)*60
            
            value = self.getOptionValue('park_position')
            if value is None:
                self.warning('no park_position value specified')
            else:
                s = value.strip().split('/')
                self._x = self._parkX = float(s[0])
                self._y = self._parkY = float(s[1])
                
            self.info('encoder pulses per degree: %f' % (self._pulsesPerDegree,))
            self.info('using device %s' % (device,))
            self._serial.setPort(device)
        
            value = self.getOptionValue('power_save_mode')
            if value != None and int(value) == 1:
                self.info('power save mode is turned on')
                self._powerSaveMode = True
            else:
                self.info('power save mode is turned off')
                self._startupRobot()
                if self._dslrAvailable:
                    self._powerOnCam()
        else:
            self._powerSaveMode = True
        
        
    def isBusy(self):
        return self._isBusy
        
        
    def needsWLAN(self):
        return False
    
    
    def msgReceived(self, data):
        try:
            thread.start_new_thread(self._parseMsg, (data,))
        except Exception, e:
            self.exception(e)
       
        
    def run(self):
        self.name = 'CamZillaPlugin-Thread'
        self.info('started')
        
        if self._robotAvailable and not self._powerSaveMode and not self._calibrated:
            try:
                self._calibrateRobot()
            except Exception, e:
                self.exception(e)
            if self._taskqueue.empty():
                try:
                    self._parkRobot()
                except Exception, e:
                    self.exception(str(e))
            
        if self._dslrAvailable and not self._plugStop:
            turnoff = False
            if not self._isDSLRPowered():
                turnoff = True
                self._powerOnCam()
            try:
                self.info('synchronize time of camera')
                self._execCommand([GPHOTO2, '--port="usb:"', '--quiet', '--set-config /main/settings/datetime=%s' % (time.time(),)])
            except Exception, e:
                self.error(e.__str__())
            if turnoff:
                self._powerOffCam()
            
        if self._dslrAvailable and not self._plugStop and not self._powerSaveMode:
            self._powerOnCam()
            try:
                self._downloadUnknownPictures()
            except Exception, e:
                self.error(e.__str__())

        while not self._plugStop:
            if self._taskqueue.empty():
                self._isBusy = False
            task = self._taskqueue.get()
            
            if self._parkTimer is not None:
                try:
                    self._parkTimer.cancel()
                except Exception, e:
                    self.exception(str(e))
            self._parkActionLock.acquire()
            
            self._isBusy = True
            if self._plugStop:
                try:
                    self._taskqueue.task_done()
                except ValueError, e:
                    self.exception(e)
                break
            
            try:
                now = time.time()
                if task[0] == PANORAMA_TASK:
                    try:
                        parsedTask = self._parseTask(task[1])
                    except Exception, e:
                        if not self._plugStop:
                            self.error(e.__str__())
                            self.processMsg(self.getTimeStamp(), [int(now*1000)] + ['panorama', 'could not execute task: %s' % (e.__str__(),), self._x,self._y] + [None]*9)
                    else:
                        if parsedTask[0] == DSLR and not self._dslrAvailable:
                            self.error('panorama DSLR task can not be executed without configuring a DSLR')
                            self.processMsg(self.getTimeStamp(), [int(now*1000)] + ['panorama', 'panorama DSLR task can not be executed without configuring a DSLR', self._x,self._y] + [None]*9)
                        elif parsedTask[0] == WEBCAM and not self._webcamAvailable:
                            self.error('panorama webcam task can not be executed without configuring a webcam')
                            self.processMsg(self.getTimeStamp(), [int(now*1000)] + ['panorama', 'panorama webcam task can not be executed without configuring a webcam', self._x,self._y] + [None]*9)
                        else:
                            if self._powerSaveMode:
                                if self._robotAvailable:
                                    self._startupRobot()
                                    
                                if self._dslrAvailable:
                                    self._powerOnCam()
                                    
                                if self._robotAvailable and not self._calibrated:
                                    self._calibrateRobot()
                            
                            if not self._plugStop:
                                if parsedTask[0] == DSLR:
                                    try:
                                        self._downloadUnknownPictures()
                                    except Exception, e:
                                        self.error(e.__str__())
                            
                                if not self._plugStop:
                                    now = time.time()
                                    if parsedTask[0] == DSLR:
                                        self.info('executing panorama picture task with DSLR: start(%s,%s) pictures(%s,%s) rotation(%s,%s) delay(%s) batch(%s) gphoto2(%s)' % (str(parsedTask[1]), str(parsedTask[2]), str(parsedTask[3]), str(parsedTask[4]), str(parsedTask[5]), str(parsedTask[6]), str(parsedTask[7]), str(parsedTask[8]), str(parsedTask[9])))
                                    else:
                                        self.info('executing panorama picture task with webcam: start(%s,%s) pictures(%s,%s) rotation(%s,%s) delay(%s)' % (str(parsedTask[1]), str(parsedTask[2]), str(parsedTask[3]), str(parsedTask[4]), str(parsedTask[5]), str(parsedTask[6]), str(parsedTask[7])))
                                    pic_name_list = []
                                    
                                    if (not self._robotAvailable or self._isRobotPowered()) and self._isDSLRPowered():
                                        pic = 1
                                        try:
                                            bracketing = False
                                            config = None
                                            if parsedTask[0] == DSLR:
                                                config, bracketing = self._configureDSLR(parsedTask[9])
                                            y_cnt = 0
                                            while y_cnt < parsedTask[4] and not self._plugStop:
                                                if self._robotAvailable:
                                                    ret = self._position(y=(parsedTask[2]+y_cnt*parsedTask[6]))
                                                    yLimit = ret[3]
                                                    
                                                x_cnt = 0
                                                while x_cnt < parsedTask[3] and not self._plugStop:
                                                    if self._robotAvailable:
                                                        ret = self._position(x=(parsedTask[1]+x_cnt*parsedTask[5]))
                                                        xLimit = ret[2]
                                                    if parsedTask[7] > 0:
                                                        self._delay.wait(parsedTask[7])
                                                    if not self._plugStop:
                                                        if bracketing:
                                                            self.info('taking pictures number %d-%d/%d at position (%f,%f)' % (1+(pic-1)*3,3+(pic-1)*3,parsedTask[3]*parsedTask[4]*3,self._x,self._y))
                                                        else:
                                                            self.info('taking picture number %d/%d at position (%f,%f)' % (pic,parsedTask[3]*parsedTask[4],self._x,self._y))
                                                        pic_str = '%s_pic%.3d_%dx_%dy' % (time.strftime('%Y%m%d_%H%M%S', time.gmtime(now)), pic, int(round(self._x*10)), int(round(self._y*10)))
                                                        self._takePicture(parsedTask[0], '%s.jpg' % (pic_str,))
                                                        if not self._plugStop:
                                                            s = 'successfully'
                                                            if self._robotAvailable:
                                                                if (xLimit is True and yLimit is True):
                                                                    s += ' (x and y limit reached)'
                                                                elif (xLimit is True and yLimit is False):
                                                                    s += ' (x limit reached)'
                                                                elif (xLimit is False and yLimit is True):
                                                                    s += ' (y limit reached)'
                                                                
                                                            if bracketing:
                                                                if parsedTask[8] == 0:
                                                                    l = []
                                                                    for i in range(3):
                                                                        l.append('%s_pic%.3d_%dx_%dy_bracket%d.%s' % (time.strftime('%Y%m%d_%H%M%S', time.gmtime(now)), pic, int(round(self._x*10)), int(round(self._y*10)), i+1, '%C'))
                                                                    self._downloadPictures(l)
                                                                else:
                                                                    for i in range(3):
                                                                        pic_name_list.append('%s_pic%.3d_%dx_%dy_bracket%d.%s' % (time.strftime('%Y%m%d_%H%M%S', time.gmtime(now)), pic, int(round(self._x*10)), int(round(self._y*10)), i+1, '%C'))
                                                                self.processMsg(self.getTimeStamp(), [int(now*1000)] + ['panorama', 'pictures number %d-%d/%d taken %s'  % (1+(pic-1)*3, 3+(pic-1)*3, parsedTask[3]*parsedTask[4]*3, s), self._x,self._y] + parsedTask[1:-1] + [config])
                                                            else:
                                                                if parsedTask[0] == DSLR:
                                                                    if parsedTask[8] == 0:
                                                                        self._downloadPictures(['%s.%s' % (pic_str, '%C')])
                                                                    else:
                                                                        pic_name_list.append('%s.%s' % (pic_str, '%C'))
                                                                self.processMsg(self.getTimeStamp(), [int(now*1000)] + ['panorama', 'picture number %d/%d taken %s' % (pic, parsedTask[3]*parsedTask[4], s), self._x,self._y] + parsedTask[1:-1] + [config])
                                                            pic += 1
                        
                                                            x_cnt += 1
                                                    
                                                y_cnt += 1
                                                
                                            if parsedTask[8] != 0 and parsedTask[0] == DSLR:
                                                self._downloadPictures(pic_name_list)
                                        except Exception, e:
                                            if not self._plugStop:
                                                self.processMsg(self.getTimeStamp(), [int(now*1000)] + ['panorama', 'could not finish task successfully (%s)' % (e.__str__(),), self._x, self._y] + parsedTask[1:-1] + [config])
                                                self.error(e.__str__())
                                        else:
                                            if not self._plugStop:
                                                self.info('all pictures taken successfully')
                                                self.processMsg(self.getTimeStamp(), [int(now*1000)] + ['panorama', 'finished successfully', self._x, self._y] + parsedTask[1:-1] + [config])
                                                self.info('panorama picture task finished successfully')
                                    else:
                                        self.processMsg(self.getTimeStamp(), [int(now*1000)] + ['panorama', 'CamZilla is not powered -> turn power on first', self._x,self._y] + [None]*9)
                                        self.error('CamZilla is not powered -> turn power on first')
                elif task[0] == PICTURE_TASK:
                    try:
                        parsedTask = self._parseTask(task[1])
                    except Exception, e:
                        if not self._plugStop:
                            self.error(e.__str__())
                            self.processMsg(self.getTimeStamp(), [int(now*1000)] + ['picture', 'could not execute task: %s' % (e.__str__(),), self._x,self._y] + [None]*9)
                    else:
                        if parsedTask[0] == DSLR and not self._dslrAvailable:
                            self.error('picture DSLR task can not be executed without configuring a DSLR')
                            self.processMsg(self.getTimeStamp(), [int(now*1000)] + ['picture', 'picture DSLR task can not be executed without configuring a DSLR', self._x, self._y] + [None]*9)
                        elif parsedTask[0] == WEBCAM and not self._webcamAvailable:
                            self.error('picture webcam task can not be executed without configuring a webcam')
                            self.processMsg(self.getTimeStamp(), [int(now*1000)] + ['picture', 'picture webcam task can not be executed without configuring a webcam', self._x, self._y] + [None]*9)
                        else:
                            if self._robotAvailable:
                                self.info('picture now task received -> taking picture(s) in current robot position (x=%f,y=%f)' % (self._x, self._y))
                            else:
                                self.info('picture now task received -> taking picture(s)')
                            
                            if parsedTask[0] == DSLR:
                                if self._powerSaveMode and not self._plugStop:
                                    self._powerOnCam()
                                    
                                try:
                                    self._downloadUnknownPictures()
                                except Exception, e:
                                    self.error(e.__str__())
                                
                            if not self._plugStop:
                                now = time.time()
                                
                                if not self._plugStop:
                                    try:
                                        bracketing = False
                                        config = None
                                        if parsedTask[0] == DSLR:
                                            config, bracketing = self._configureDSLR(parsedTask[9])
                                        self.info('taking picture(s) now')
                                        if self._robotAvailable:
                                            pic_str = '%s_pic001_%dx_%dy' % (time.strftime('%Y%m%d_%H%M%S', time.gmtime(now)), int(round(self._x*10)), int(round(self._y*10)))
                                        else:
                                            pic_str = time.strftime('%Y%m%d_%H%M%S', time.gmtime(now))
                                        self._takePicture(parsedTask[0], '%s.jpg' % (pic_str,))
                                    except Exception, e:
                                        if not self._plugStop:
                                            self.processMsg(self.getTimeStamp(), [int(now*1000)] + ['picture_now', 'could not take picture now (%s)' % (e.__str__(),), self._x, self._y] + [None]*8 + [config])
                                            self.error(e.__str__())
                                    else:
                                        if not self._plugStop:
                                            try:
                                                if bracketing:
                                                    l = []
                                                    for i in range(3):
                                                        if self._robotAvailable:
                                                            l.append('%s_pic001_%dx_%dy_bracket%d.%s' % (time.strftime('%Y%m%d_%H%M%S', time.gmtime(now)), int(round(self._x*10)), int(round(self._y*10)), i+1, '%C'))
                                                        else:
                                                            l.append('%s_bracket%d.%s' % (time.strftime('%Y%m%d_%H%M%S', time.gmtime(now)), i+1, '%C'))
                                                    self._downloadPictures(l)
                                                elif parsedTask[0] == DSLR:
                                                    self._downloadPictures(['%s.%s' % (pic_str, '%C')])
                                            except Exception, e:
                                                if not self._plugStop:
                                                    self.processMsg(self.getTimeStamp(), [int(now*1000)] + ['picture_now', 'could not download all pictures (%s)' % (e.__str__(),), self._x, self._y] + [None]*8 + [config])
                                                    self.error(e.__str__())
                                            else:
                                                if not self._plugStop:
                                                    self.processMsg(self.getTimeStamp(), [int(now*1000)] + ['picture_now', 'finished successfully', self._x, self._y] + [None]*8 + [config])
                                                    self.info('picture now task finished successfully')
                elif task[0] == POSITIONING_TASK:
                    self.info('positioning task received (x=%f,y=%f)' % (task[1], task[2]))
                    if self._robotAvailable:
                        if self._powerSaveMode:
                            self._startupRobot()
                            if not self._calibrated:
                                self._calibrateRobot()
                                
                        try:
                            pos = self._position(x=task[1], y=task[2])
                        except Exception, e:
                            if not self._plugStop:
                                self.processMsg(self.getTimeStamp(), [int(now*1000)] + ['positioning', 'not finished successfully (%s)' % (e.__str__(),), self._x, self._y] + [None]*9)
                                self.error(e.__str__())
                        else:
                            if not self._plugStop:
                                s = 'finished successfully'
                                if (pos[2] is True and pos[3] is True):
                                    s += ' (x and y limit reached)'
                                elif (pos[2] is True and pos[3] is False):
                                    s += ' (x limit reached)'
                                elif (pos[2] is False and pos[3] is True):
                                    s += ' (y limit reached)'
                                self.processMsg(self.getTimeStamp(), [int(now*1000)] + ['positioning', s, self._x, self._y] + [None]*9)
                                self.info('positioning task %s' % (s,))
                    else:
                        self.processMsg(self.getTimeStamp(), [int(now*1000)] + ['positioning', 'CamZilla can not be positioned without robot', self._x, self._y] + [None]*9)
                        self.error('CamZilla can not be positioned without robot')
                elif task[0] == MODE_TASK:
                    if self._robotAvailable:
                        if self._isRobotPowered():
                            if task[1] == 0:
                                self.info('mode task received from GSN >joystick off<')
                                self._write("j=off")
                                self.processMsg(self.getTimeStamp(), [int(now*1000)] + ['mode', 'joystick turned off', self._x, self._y] + [None]*9)
                            elif task[1] == 1:
                                self.info('mode task received from GSN >joystick on<')
                                self._write("j=on")
                                self.processMsg(self.getTimeStamp(), [int(now*1000)] + ['mode', 'joystick turned on', self._x, self._y] + [None]*9)
                            else:
                                self.processMsg(self.getTimeStamp(), [int(now*1000)] + ['mode', 'unknown mode', self._x, self._y] + [None]*9)
                                self.error('unknown mode task received from GSN')
                        else:
                            self.processMsg(self.getTimeStamp(), [int(now*1000)] + ['mode', 'CamZilla is not powered -> turn power on first', self._x, self._y] + [None]*9)
                            self.error('CamZilla is not powered -> turn power on first')
                    else:
                        self.processMsg(self.getTimeStamp(), [int(now*1000)] + ['mode', 'CamZilla mode command can not be used without robot', self._x, self._y] + [None]*9)
                        self.error('CamZilla mode command can not be used without robot')
                elif task[0] == CALIBRATION_TASK:
                    if self._robotAvailable:
                        if self._isRobotPowered():
                            self.info('calibration task received from GSN -> calibrate robot')
                        self._calibrateRobot()
                    else:
                        self.processMsg(self.getTimeStamp(), [int(now*1000)] + ['calibration', 'CamZilla can not be calibrated without robot', self._x, self._y] + [None]*9)
                        self.error('CamZilla can not be calibrated without robot')
            except Exception, e:
                self.exception(str(e))
                
            try:
                self._taskqueue.task_done()
            except ValueError, e:
                self.exception(e)
            self._parkActionLock.release()
            
            if self._taskqueue.empty() and not self._plugStop:
                if self._robotAvailable:
                    self._parkTimer = Timer(self._parkIdleTime, self._parkAction)
                    self._parkTimer.start()
                elif self._dslrAvailable:
                    self._powerOffCam()
        
        self.info('died')


    def action(self, parameters):
        if isinstance(parameters, str):
            parameters = parameters.strip()
            
            if parameters.lower().startswith('panorama('):
                self._taskqueue.put([PANORAMA_TASK, parameters[9:-1]])
            elif parameters.lower().startswith('picture('):
                self._taskqueue.put([PICTURE_TASK, parameters[8:-1]])
            else:
                self.error('action unrecognized >%s<' % (parameters,))
        else:
            self._taskqueue.put(parameters)
    
    
    def stop(self):
        self._isBusy = False
        self._plugStop = True
        self._delay.set()
        self._taskqueue.put('end')
        self.join()
        if self._robotAvailable:
            self._parkRobot()
            self._shutdownRobot()
        if self._dslrAvailable:
            self._powerOffCam()
        
        
    def _parkAction(self):
        self._parkActionLock.acquire()
        now = self.getTimeStamp()
        try:
            self._parkRobot()
        except Exception, e:
            self.exception(str(e))
        if self._powerSaveMode:
            try:
                self._shutdownRobot()
                if self._dslrAvailable:
                    self._powerOffCam()
            except Exception, e:
                self.exception(str(e))
        
        self._parkActionLock.release()
        log_str = 'CamZilla has been parked successfully after being idle for %f minutes' % (self._parkIdleTime/60,)
        self.processMsg(now, [now] + ['parking', log_str, self._x, self._y] + [None]*9)
        self.info(log_str)
            
            
    def _parseMsg(self, data):
        if data[0] == TASK_MESSAGE:
            self.info('new task message received from GSN')
            self.action(data[1:])
        elif data[0] == POWER_MESSAGE:
            now = time.time()
            self.info('power message received from GSN')
            if data[1] == 0:
                if self._robotAvailable and self._dslrAvailable:
                    self.info('turn robot and DSLR off')
                    log_str = 'DSLR and robot are turned off'
                    self._powerOffCam()
                    self._shutdownRobot()
                elif self._robotAvailable:
                    self.info('turn robot off')
                    log_str = 'Robot is turned off'
                    self._shutdownRobot()
                elif self._dslrAvailable:
                    self.info('turn DSLR off')
                    log_str = 'DSLR is turned off'
                    self._powerOffCam()
            elif data[1] == 1:
                try:
                    if self._robotAvailable and self._dslrAvailable:
                        self.info('turn robot and DSLR on')
                        self._startupRobot()
                        self._powerOnCam()
                        log_str = 'DSLR and robot are turned on'
                    elif self._robotAvailable:
                        self.info('turn robot on')
                        self._startupRobot()
                        log_str = 'Robot is turned on'
                    elif self._dslrAvailable:
                        self.info('turn DSLR on')
                        self._powerOnCam()
                        log_str = 'DSLR is turned on'
                except TypeError, e:
                    self.error(str(e))
            else:
                self.error('unknown power message received from GSN')
                
            heater = None
            if data[2] == 0:
                self.info('turn heater off')
                log_str += ' and heater is turned off'
                self.heaterPowerOff()
                heater = False
            elif data[2] == 1:
                self.info('turn heater on')
                log_str += ' and heater is turned on'
                self.heaterPowerOn()
                heater = True
            else:
                self.error('unknown heater message received from GSN')
                
            self.processMsg(self.getTimeStamp(), [int(now*1000)] + ['power', log_str, self._x, self._y] + [None]*9)
        else:
            self.error('unknown message type received from GSN')
        
        
    def _parseTask(self, task):
        params = task.strip().split(' ')
        ret = [None]*10
        for param in params:
            p = param.lower()
            if p.startswith('camera'):
                ret[0] = int(param[7:-1])
            elif p.startswith('start'):
                startX, startY = param[6:-1].split(',')
                ret[1] = float(startX)
                ret[2] = float(startY)
            elif p.startswith('pictures'):
                picsX, picsY = param[9:-1].split(',')
                ret[3] = int(picsX)
                ret[4] = int(picsY)
            elif p.startswith('rotation'):
                rotationX, rotationY = param[9:-1].split(',')
                ret[5] = float(rotationX)
                ret[6] = float(rotationY)
            elif p.startswith('delay'):
                ret[7] = int(param[6:-1])
            elif p.startswith('batch'):
                ret[8] = int(param[6:-1])
            elif p.startswith('gphoto2'):
                ret[9] = param[8:-1].split(',')
            else:
                self.error('unrecognized parameter >%s< in task >%s<' % (param,task))
        if ret[0] is None:
            ret[0] = DEFAULT_CAMERA
        if ret[1] is None:
            ret[1] = 0.0
        if ret[2] is None:
            ret[2] = 0.0
        if ret[3] is None:
            ret[3] = 1
        if ret[4] is None:
            ret[4] = 1
        if ret[5] is None and ret[3] > 1 and self._robotAvailable:
            raise TypeError('x-rotation has to be specified if more than one picture has to be taken in x-direction')
        if ret[6] is None and ret[4] > 1 and self._robotAvailable:
            raise TypeError('y-rotation has to be specified if more than one picture has to be taken in y-direction')
        if ret[5] is None:
            ret[5] = 1.0
        if ret[6] is None:
            ret[6] = 1.0
        if ret[7] is None:
            ret[7] = 0
        if ret[8] is None:
            ret[8] = 1
        if ret[9] is None:
            ret[9] = []
        return ret
    
    
    
    def _configureDSLR(self, settings):
        self.info('configure DSLR')
        configlist = []
        
        for default in DEFAULT_GPHOTO2_SETTINGS:
            notavailable = True
            def_begin = default.split('=')[0].strip()
            for setting in settings:
                if def_begin == setting.split('=')[0].strip():
                    notavailable = False
                    break
            if notavailable:
                settings.insert(0, default)
        
        sets = []
#        ret = '/main/settings/capturetarget=1'
        ret = ''
        self._autofocus = '--set-config /main/actions/autofocusdrive=0'
        for setting in settings:
#            ret += ', ' + setting
            if ret != '':
                ret += ', '
            ret += setting
            if setting.find('/main/actions/autofocusdrive') != -1:
                if self._focusAllowed:
                    self._autofocus = '--set-config %s' % (setting.strip(),)
                else:
                    self.warning('using autofocus is not allowed -> configure DSLR without')
            elif setting.find('/main/actions/manualfocusdrive') != -1:
                if self._focusAllowed:
                    self._setFocus(setting)
                else:
                    self.warning('using manual focus is not allowed -> configure DSLR without')
            else:
                sets.append('--set-config-index %s' % (setting.strip(),))
            
        bracketing = False
        if ret.find('bracketing=0') != -1:
            bracketing = True
        command = [GPHOTO2, '--port="usb:"', '--quiet'] + sets
        self._execCommand(command)
        return ret.strip(), bracketing
        
        
    def _takePicture(self, camera, filename):
        if camera == DSLR:
            if self._autofocus:
                command = [GPHOTO2, '--port="usb:"', '--force-overwrite', '--quiet', '--set-config-index /main/settings/capturetarget=1', self._autofocus, '--capture-image']
                #command = [GPHOTO2, '--port="usb:"', '--force-overwrite', '--quiet', self._autofocus, '--capture-image']
            else:
                command = [GPHOTO2, '--port="usb:"', '--force-overwrite', '--quiet', '--set-config-index /main/settings/capturetarget=1', '--capture-image']
                #command = [GPHOTO2, '--port="usb:"', '--force-overwrite', '--quiet', '--capture-image']
        elif camera == WEBCAM:
            command = [WGET, '--output-document=%s' % (os.path.join(self._webcamPictureFolder, filename),), self._webcamHttp]
        else:
            self.error('camera type %s unknown' % (camera,))
            return
        
        self._execCommand(command)
        if camera == WEBCAM:
            self._checkFreeSpaceAndDeleteFilesIfNecessary(self._keepMinMbFree, self._webcamPictureFolder)
        
        
    def _setFocus(self, focus):
        command = [GPHOTO2, '--port="usb:"', '--force-overwrite', '--quiet', '--capture-preview', '--set-config /main/actions/manualfocusdrive=32767', '--set-config %s' % (focus,)]
        self._execCommand(command)
        
        
    def _downloadPictures(self, filenames):
        self.info('downloading all pictures from DSLR')
        
        try:
            self.sendInterPluginCommand('BinaryPlugin', 'stop')
        except Exception, e:
            self.error(str(e))
            
        if not os.path.isdir(TMPPICTUREFOLDER):
            os.makedirs(TMPPICTUREFOLDER)
        if os.listdir(TMPPICTUREFOLDER):
            self.warning('there are still files in the temporary directory -> move them to %s' % (self._pictureFolder,))
            for file in sorted(os.listdir(TMPPICTUREFOLDER)):
                shutil.move(os.path.join(TMPPICTUREFOLDER, file), self._pictureFolder)
        
        pic_count = 0
        for filename in filenames:
            if self._plugStop:
                break
            self._execCommand([GPHOTO2, '--port="usb:"', '--quiet', '--get-file=1', '--filename=' + filename, '--recurse', '--delete-file=1'], TMPPICTUREFOLDER)
            bugwait=5
            for file in os.listdir(TMPPICTUREFOLDER):
                file = os.path.join(TMPPICTUREFOLDER, file)
                bugwait = os.path.getsize(file) / 500000
                shutil.move(file, self._pictureFolder)
            self._delay.wait(bugwait)
            self._checkFreeSpaceAndDeleteFilesIfNecessary(self._keepMinMbFree, self._pictureFolder)
            pic_count += 1
        if pic_count > 0:
            self.info('downloaded %d pictures from DSLR' % (pic_count,))
            
        try:
            self.sendInterPluginCommand('BinaryPlugin', 'start')
        except Exception, e:
            self.error(str(e))

        os.rmdir(TMPPICTUREFOLDER)
        
        
    def _downloadUnknownPictures(self):
        self.info('downloading all unknown pictures from DSLR')
        if not os.path.isdir(TMPPICTUREFOLDER):
            os.makedirs(TMPPICTUREFOLDER)
        if os.listdir(TMPPICTUREFOLDER):
            self.warning('there are still files in the temporary directory -> move them to %s' % (self._pictureFolder,))
            for file in sorted(os.listdir(TMPPICTUREFOLDER)):
                shutil.move(os.path.join(TMPPICTUREFOLDER, file), self._pictureFolder)
               
        pic_count = 0
        filename_time = time.strftime('%Y%m%d_%H%M%S', time.gmtime(time.time()))
        ret = self._execCommand([GPHOTO2, '--port="usb:"', '--quiet', '--list-files'], stdOutput = False)
        for line in ret[0].splitlines():
            if line.find('DSC_') != -1 and not self._plugStop:
                pic_count += 1
                self._execCommand([GPHOTO2, '--port="usb:"', '--quiet', '--get-file=1', '--filename=%s_pic%.3d_unknown.%s' % (filename_time,pic_count,'%C'), '--recurse', '--delete-file=1'], TMPPICTUREFOLDER)
                bugwait=5
                for file in os.listdir(TMPPICTUREFOLDER):
                    file = os.path.join(TMPPICTUREFOLDER, file)
                    bugwait = os.path.getsize(file) / 500000
                    shutil.move(file, self._pictureFolder)
                self._delay.wait(bugwait)
                self._checkFreeSpaceAndDeleteFilesIfNecessary(self._keepMinMbFree, self._pictureFolder)
        if pic_count > 0:
            self.info('downloaded %d unknown pictures from DSLR' % (pic_count,))

        os.rmdir(TMPPICTUREFOLDER)
        
        
        
    def _execCommand(self, params, cwd=None, stdOutput=True):
        p = subprocess.Popen(params, stdout=subprocess.PIPE, stderr=subprocess.PIPE, cwd=cwd)
        ret = p.wait()
        output = p.communicate()
        if output[0] and stdOutput:
            self.info(output[0])
        if ret == 0:
            if output[1]:
                self.warning(output[1])
        else:
            if self._isDSLRPowered():
                if output[1]:
                    self.error(output[1])
            else:
                raise Exception('DSLR has no more power -> gphoto2 could not execute command')
        return output
    
    
    def _isRobotPowered(self):
        return self.robotPowerStatus() and self.robotUsbStatus()
    
    
    def _isDSLRPowered(self):
        return self.dslrPowerStatus() and self.dslrUsbStatus()
            
            
    def _parkRobot(self):
        if self._isRobotPowered():
            self._position(self._parkX, self._parkY)
    
    
    def _powerOffCam(self):
        if self._isDSLRPowered():
            # turn the DSLR USB port off
            self.dslrUsbOff()
            # turn the robot and DSLR off
            self.dslrPowerOff()
    
    
    def _shutdownRobot(self):
        if self._isRobotPowered():
            if self._robotAvailable:
                if self._serial.isOpen():
                    self._serial.close()
                # turn the robot USB port off
                self.robotUsbOff()
                # turn the robot EXT port off
                self.robotPowerOff()
                self._calibrated = False
            return True
        
        self.heaterPowerOff()
        
        return False
        
    
    def _powerOnCam(self):
        if not self._isDSLRPowered():
            # turn the DSLR USB port on
            self.dslrUsbOn()
            # turn the DSLR on
            self.dslrPowerOn()
        
    
    def _startupRobot(self):
        if not self._isRobotPowered():
            if self._robotAvailable:
                self.info('wait for robot to startup')
                # turn the robot USB port on
                self.robotUsbOn()
                # turn the robot on
                self.robotPowerOn()
                
                if not self._serial or not self._serial.isOpen():
                    connect_cnt = 0
                    while not self._plugStop:
                        try:
                            self._serial.open()
                        except serial.SerialException, e:
                            if connect_cnt == 5:
                                raise TypeError('could not initialize serial source: %s' % (e,))
                        else:
                            ans_cnt = 0
                            while not self._plugStop:
                                ans = self._serial.readline()
                                i = ans.rfind('Joystick')
                                if i != -1:
                                    self.info(ans[i:])
                                    self.info('robot ready')
                                    return True
                                else:
                                    self.warning('unexpected output received from arduino -> try again')
                                if ans_cnt >= 3:
                                    raise TypeError('Arduino did not startup properly')
                                ans_cnt += 1
                        self._delay.wait(0.5)
                        connect_cnt += 1
                    
        return False
        
        
    def _calibrateRobot(self):
        now = self.getTimeStamp()
        if self._isRobotPowered():
            self._write("j=off")
            cal = self._write("cal")
            self._calibrated = True
            xmax = cal[0] * 2 / self._pulsesPerDegree
            ymax = cal[1] * 2 / self._pulsesPerDegree
            self._x = cal[2] / self._pulsesPerDegree
            self._y = cal[3] / self._pulsesPerDegree
            
            log_str = ''
            if xmax != 0 and ymax != 0:
                 log_str = ' (maximal rotation in degrees: x=%f, y=%f)' % (xmax,ymax)
            self.processMsg(now, [now] + ['calibration', 'finished successfully' + log_str, self._x, self._y] + [None]*9)
            self.info('calibration finished successfully' + log_str)
        else:
            self.error('robot not powered -> can not calibrate')
            self.processMsg(now, [now] + ['calibration', 'CamZilla is not powered -> turn power on first', self._x, self._y] + [None]*9)
            
            
    def _position(self, x=None, y=None):
        ret = [None]*4
        if x is not None and x != self._x:
            tmp = self._write('x=%d' % (int(round(x*self._pulsesPerDegree)),))
            self._x = tmp[0] / self._pulsesPerDegree
            ret[0] = self._x
            ret[2] = tmp[2]
            ret[3] = tmp[3]
        if y is not None and y != self._y:
            tmp = self._write('y=%d' % (int(round(y*self._pulsesPerDegree)),))
            self._y = tmp[1] / self._pulsesPerDegree
            ret[1] = self._y
            ret[2] = tmp[2]
            ret[3] = tmp[3]
        return ret
        
        
        
    def _write(self, com):
        try:
            self._writeLock.acquire()
            self.debug('servo control write: %s' % (com,))
            if com == 'j=on' or com == 'j=off':
                self._serial.write(com + "\n")
                ans = self._serial.readline().strip()
                self.debug('servo control answer: j=..: %s' % (ans,))
                if com != ans:
                    raise Exception('return value (%s) does not match command (%s)' % (ans, com))
                self._manualControl = (com == 'j=on')
            elif self._manualControl:
                raise Exception('manual joystick control is turned on -> command (%s) will not be executed' % (com,))
            elif com == 'cal':
                self._serial.write(com + "\n")
                cal1 = self._serial.readline().strip()
                self.debug('servo control answer: cal(1): %s' % (cal1))
                if cal1 == 'j=on':
                    self._manualControl = True
                    raise Exception('manual joystick control has been turned on -> no more commands will be sent to CamZilla until joystick control has been turned off')
                elif cal1 == '!cal':
                    raise Exception('could not calibrate')
                elif cal1.lower().find('emergency') != -1:
                    raise Exception('could not calibrate (answer: %s)' % (cal1))
                cal2 = self._serial.readline().strip()
                self.debug('servo control answer: cal(2): %s' % (cal2))
                cal1 = cal1[5:-1].split(',')
                cal2 = cal2.split('=')[1].split('/')
                return (int(cal1[0]), int(cal1[1]), int(cal2[0]), int(cal2[1]))
            elif com.startswith('x=') or com.startswith('y='):
                self._serial.write(com + "\n")
                ans = self._serial.readline().strip()
                self.debug('servo control answer: x=..: %s' % (ans,))
                if ans == '!cal':
                    raise Exception('not yet calibrated')
                elif ans.startswith('x/y='):
                    spl = ans.split('=')[1].split('/')
                    xLimit = yLimit = False
                    if (spl[0].endswith('L')):
                        spl[0] = spl[0][:-1]
                        xLimit = True
                    if (spl[1].endswith('L')):
                        spl[1] = spl[1][:-1]
                        yLimit = True
                    return (int(spl[0]), int(spl[1]), xLimit, yLimit)
                elif ans == 'j=on':
                    self._manualControl = True
                    raise Exception('manual joystick control has been turned on -> no more commands will be sent to CamZilla until joystick control has been turned off')
                elif ans.lower().find('emergency') != -1:
                    raise Exception(ans)
                else:
                    raise Exception('unknown return value for command (%s): %s' % (com, ans))
            else:
                raise TypeError('command (%s) unknown' % (com,))
        except Exception, e:
            raise e
        finally:
            self._writeLock.release()
        
        
        
    def _checkFreeSpaceAndDeleteFilesIfNecessary(self, minFreeMb, path):
        while True:
            statvfs = os.statvfs(path)
            freeSpace = (statvfs.f_frsize * statvfs.f_bfree)/1048576
            if (minFreeMb >= freeSpace):
                matches = []
                for root, dirs, files in os.walk(path):
                    if root == path:
                        for file in files:
                            matches.append(os.path.join(root, file))
                toRemove = max(matches, key=os.path.getmtime)
                self.warning('only %dMb free on %s --> deleting %s' % (freeSpace, path, toRemove))
                os.remove(toRemove)
            else:
                return
            
    


if __name__ == '__main__':
    import signal
    import ConfigParser
    import optparse
    import logging.config

    class GSNPeerDummy():
        def processMsg(self, msgType, timestamp, payload, priority, backlog=False):
            pass
    class MainDummy():
        def __init__(self):
            self.gsnpeer = GSNPeerDummy()
            self.duty_cycle_mode = False
        def incrementExceptionCounter(self):
            pass
        def incrementErrorCounter(self):
            pass
        def runPluginRemoteAction(self):
            pass
    
    parser = optparse.OptionParser('usage: %prog [options]')
    
    parser.add_option('-c', '--config', type='string', dest='config_file', default='/etc/backlog.cfg',
                      help='Configuration file. Default: /etc/backlog.cfg', metavar='FILE')
    parser.add_option('-x', '--startx', type='int', dest='startX', default=0,
                      help='Lower-left horizontal starting point', metavar='INT')
    parser.add_option('-y', '--starty', type='int', dest='startY', default=0,
                      help='Lower-left vertical starting point', metavar='INT')
    parser.add_option('--picsx', type='int', dest='picturesX', default=1,
                      help='Number of pictures taken horizontally', metavar='INT')
    parser.add_option('--picsy', type='int', dest='picturesY', default=1,
                      help='Number of pictures taken vertically', metavar='INT')
    parser.add_option('--rotx', type='int', dest='rotationX', default=1,
                      help='Horizontal rotation in degrees between pictures', metavar='INT')
    parser.add_option('--roty', type='int', dest='rotationY', default=1,
                      help='Vertical rotation in degrees between pictures', metavar='INT')
    parser.add_option('-d', '--delay', type='int', dest='delay', default=0,
                      help='Delay between rotation and picture taking', metavar='INT')
    parser.add_option('-b', '--batch', type='int', dest='batch', default=1,
                      help='Batch download after taking all pictures', metavar='INT')
    parser.add_option('-g', '--gphoto2', type='string', dest='gphoto2', default='/main/settings/capturetarget=1,/main/imgsettings/imagequality=0,/main/imgsettings/imagesize=2',
                      help='Comma separated configurations for gphoto2', metavar='CONFIGS')
    
    (opt, args) = parser.parse_args()

    # read config file for logging options
    try:
        logging.config.fileConfig(opt.config_file)
        logging.logProcesses = 0

        # read config file for other options
        config = ConfigParser.SafeConfigParser()
        config.optionxform = str # case sensitive
        config.read(opt.config_file)
    except ConfigParser.NoSectionError, e:
        print e.__str__()
    
    try:
        camZilla = CamZillaPluginClass(MainDummy(), dict(config.items('CamZillaPlugin_options')))
        camZilla.start()
        camZilla.action('start(%d,%d) pictures(%d,%d) rotation(%d,%d) delay(%d) batch(%d) gphoto2(%s)' % (opt.startX, opt.startY, opt.picturesX, opt.picturesY, opt.rotationX, opt.rotationY, opt.delay, opt.batch, opt.gphoto2))
        signal.pause()
    except KeyboardInterrupt:
        print 'KeyboardInterrupt'
        camZilla.stop()
        