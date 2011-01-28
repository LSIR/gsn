
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import struct
import os
from threading import Timer, Event

from ScheduleHandler import SUBPROCESS_BUG_BYPASS
if SUBPROCESS_BUG_BYPASS:
    from SubprocessFake import SubprocessFakeClass
else:
    import subprocess
    
import BackLogMessage
from AbstractPlugin import AbstractPluginClass

DEFAULT_BACKLOG = True

RTC_USR = '/sys/class/i2c-adapter/i2c-0/0-006f/usr'
CALIB_FILE = '/etc/i_sense.cal'

class CoreStationStatusPluginClass(AbstractPluginClass):
    '''
    This plugin sends status information from the CoreStation to GSN.
    
    Any new status information coming directly from the CoreStation should be implemented here.
    '''

    '''
    _calibrated
    _conf_calibrate
    _ain4_cal
    _ain9_cal
    _timer
    '''

    def __init__(self, parent, config):
        AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG)
        
        if SUBPROCESS_BUG_BYPASS:
            p = SubprocessFakeClass('modprobe ad77x8')
        else:
            p = subprocess.Popen(['modprobe', 'ad77x8'])
        self.info('wait for modprobe ad77x8 to finish')
        ret = p.wait()
        output = p.communicate()
        if output[0]:
            if output[1]:
                self.info('modprobe ad77x8: (STDOUT=' + output[0] + 'STDERR=' + output[1] + ')')
            else:
                self.info('modprobe ad77x8: (STDOUT=' + output[0] + ')')
        elif output[1]:
                self.info('modprobe ad77x8: (STDERR=' + output[1] + ')')
                
        if ret != 0:
            raise Exception('module ad77x8 is not available (modprobe ad77x8 returned with code ' + str(ret) + ')')
            
        self._conf_calibrate = False
        if int(self.getOptionValue('calibrate')) == 1:
            self._conf_calibrate = True
    
        self._calibrated = False
        self._ain4_cal = None
        self._ain9_cal = None
        
        self._timer = None
        
        self._sleeper = Event()
        self._stopped = False
        
        value = self.getOptionValue('poll_interval')
        if value is None:
            self._interval = None
        else:
            self._interval = float(value)
        
        self.info('interval: ' + str(self._interval))
        
    
    def getMsgType(self):
        return BackLogMessage.CORESTATION_STATUS_MESSAGE_TYPE
        
        
    def isBusy(self):
        return False
        
        
    def needsWLAN(self):
        return False
       
        
    def run(self):
        self.info('started')
        if self._interval != None:
            while not self._stopped:
                self._sleeper.wait(self._interval)
                if self._sleeper.isSet():
                    continue
                self.action('')
            self.info('died')
            

    def action(self, parameters):
        if self._timer:
            self._timer.cancel()
        
        paramlist = parameters.split()
        if paramlist:
            if paramlist[0].isdigit():
                self._timer = Timer(int(paramlist[0]), self.action, [''])
                self._timer.start()
            else:
                self.error('parameter has to be a digit (parameter=' + parameters + ')')
            
        if not self._calibrated and os.path.isfile(CALIB_FILE) and self._conf_calibrate:
            # get the calibration data with channel offsets
            try:
                fcalib = None
                frtc = None
                fcalib = open(CALIB_FILE, 'r')
                seed = fcalib.readline().split('=')[1].split('\n')[0]
                frtc = open(RTC_USR, 'r')
                seed_rtc = frtc.read(6)
                frtc.close()
                frtc = None
                
                if seed_rtc != '0x0000' and seed == seed_rtc:
                    for line in fcalib:
                        if line.split('=')[0] == 'avg_ain4':
                            self._ain4_cal = float(line.split('=')[1].split(' ')[0])
                        if line.split('=')[0] == 'avg_ain9':
                            self._ain9_cal = float(line.split('=')[1].split(' ')[0])
                    fcalib.close()
                    if self._ain4_cal and self._ain9_cal:
                        self._calibrated = True
            except Exception, e:
                self.warning(e.__str__())
                if fcalib:
                    fcalib.close()
                if frtc:
                    frtc.close()
        
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
            fc.write('calibrate')
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
        
            v1 = f1.read()
            v2 = f2.read()
            v3 = f3.read()
            v4 = f4.read()
            v5 = f5.read()
            v6 = f6.read()
            v7 = f7.read()
            v8 = f8.read()
            v9 = f9.read()
            v10 = f10.read()
            
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

            v1 = float(v1.split(' ')[0])
            v2 = float(v2.split(' ')[0])
            v3 = float(v3.split(' ')[0])
            v4 = float(v4.split(' ')[0])
            v5 = float(v5.split(' ')[0])
            v6 = float(v6.split(' ')[0])
            v7 = float(v7.split(' ')[0])
            v8 = float(v8.split(' ')[0])
            v9 = float(v9.split(' ')[0])
            v10 = float(v10.split(' ')[0])

            if self._calibrated and self._conf_calibrate:
                v6 = v6 - 0.3
                v4 = v4 - self._ain4_cal
                v9 = v9 - self._ain9_cal
            if v4 < 0:
                v4 = 0
            if v6 < 0:
                v6 = 0
            if v9 < 0:
                v9 = 0

            v1 = int(round(v1 * 11))
            v2 = None
            v3 = int(round(v3 * 23 / 3.0))
            v4 = int(round(v4 * 20000))
            v5 = int(round(v5 * 23 / 3.0))
            v6 = int(round(v6 * 2000))
            v7 = int(round(v7 * 151 / 51.0))
            v8 = int(round(v8 * 2))
            v9 = int(round(v9 * 200 / 3.0))
            v10 = int(round(v10 * 2))
            if not self._calibrated and self._conf_calibrate:
                v9 = None
        except Exception, e:
            self.warning(e.__str__())
            v1 = None
            v2 = None
            v3 = None
            v4 = None
            v5 = None
            v6 = None
            v7 = None
            v8 = None
            v9 = None
            v10 = None
        
        self.processMsg(self.getTimeStamp(), [v1, v2, v3, v4, v5, v6, v7, v8, v9, v10], self._priority, self._backlog)
    
    
    def stop(self):
        self._stopped = True
        self._sleeper.set()
        if self._timer:
            self._timer.cancel()
        self.info('stopped')
