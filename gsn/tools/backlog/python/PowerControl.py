# -*- coding: UTF-8 -*-
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"


import sys
import time
import logging
import ConfigParser
import tos
import TOSTypes
from datetime import datetime
from threading import Event, Lock, Timer, Thread

# as soon as the subprocess.Popen() bug has been fixed the functionality related
# to this variable should be removed
SUBPROCESS_BUG_BYPASS = True

if SUBPROCESS_BUG_BYPASS:
    import SubprocessFake
    subprocess = SubprocessFake
else:
    import subprocess

# the interval in seconds to recheck, whether Wlan can be turned off or not
WLAN_TURN_OFF_CHECK_INTERVAL_SEC = 120

EXT_GPIO_MAP = {1:"/proc/gpio/GPIO65", 2:"/proc/gpio/GPIO58", 3:"/proc/gpio/GPIO29"}
USB_GPIO_MAP = {1:"/proc/gpio/GPIO72", 2:"/proc/gpio/GPIO66", 3:"/proc/gpio/GPIO73"}
EXT_CONFIG_FILE = "/etc/platform/bb_extpwr.conf"


class PowerControlClass(Thread):
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

    def __init__(self, backlogMain, wlanPort, dutycyclewhileresending, platform):
        self.__dict__ = self.__shared_state
        Thread.__init__(self, name='PowerControl-Thread')
        
        self._logger = logging.getLogger(self.__class__.__name__)
        
        self._stopped = False
        self._backlogMain = backlogMain
        self._platform = platform
        
        self._wlanPort = wlanPort
        self._dutycyclewhileresending = dutycyclewhileresending
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
        
        self._backlogMain.registerTOSListener(self, [TOSTypes.AM_BEACONCOMMAND])
        
        self._work = Event()
        self._wlanToBeState = self.getWlanStatus()
        self._serviceWindowEvent = Event()
        now = datetime.utcnow()
        start, end = self._backlogMain.getNextServiceWindowRange()
        self._timer = None
        if start < now:
            self._logger.info('we are in service window')
            self._servicewindow = True
            self._timer = Timer(0.5,self._serviceWindowTimer,[self._totalSeconds(end-now)+1])
        else:
            self._servicewindow = False
            self._timer = Timer(self._totalSeconds(start-now),self._serviceWindowTimer,[self._totalSeconds(end-start)+1])
        self._timer.start()
            
            
            
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
        
        if self._servicewindow:
            self._wlanStateAfterServiceWindow = True
            
        self._wlanToBeState = True
        if not self.getWlanStatus():
            self._logger.info('turn on wlan')
            self._setExtStatus(self._wlanPort, True)
        self._work.set()
        
        
    def wlanOff(self):
        '''
        Turns the wlan off as soon as no one needs it anymore.
        
        @raise Exception: if wlan port mapping is not specified
        '''
        if self._wlanPort is None:
            raise Exception('wlan port mapping has not been specified in the configuration file')
        
        if self._servicewindow:
            self._logger.info('we are in service window => wlan will be turned off after it')
            self._wlanStateAfterServiceWindow = False
        else:
            self._wlanToBeState = False
            self._work.set()
    
    
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
            self._logger.info('Wlan on beacon received')
            self.wlanOn()
        elif response['command'] == TOSTypes.CONTROL_CMD_WLAN_OFF:
            self._logger.info('Wlan off beacon received')
            self.wlanOff()
        else:
            return False
        
        return True


    def run(self):
        self._logger.info('started')

        try:
            while not self._stopped:
                logTurnOffWlan = True
                self._work.wait()
                self._work.clear()
                while not self._stopped and not self._wlanToBeState:
                    if not self.getWlanStatus():
                        self._logger.info('wlan is turned off')
                        break
                    if self._wlanOff():
                        break
                    else:
                        if logTurnOffWlan:
                            self._logger.info('could not turn off wlan yet, will try again continuously')
                            logTurnOffWlan = False
                        self._logger.debug('trying to turn off wlan again in %s seconds' % (WLAN_TURN_OFF_CHECK_INTERVAL_SEC,))
                        self._work.wait(WLAN_TURN_OFF_CHECK_INTERVAL_SEC)
                        if self._wlanToBeState:
                            self._logger.info('turning off wlan has been canceled => will not retry')
        except Exception, e:
            self._backlogMain.incrementExceptionCounter()
            self._logger.exception(e)
        self._logger.info('died')
        
        
    def stop(self):
        self._logger.info('stopping...')
        self._backlogMain.deregisterTOSListener(self)
        self._serviceWindowEvent.set()
        if self._timer:
            self._timer.cancel()
        self._stopped = True
        self._work.set()
        self._logger.info('stopped')
        
    
    
    def _wlanOff(self):
        if self._wlanPort is None:
            raise Exception('wlan port mapping has not been specified in the configuration file')
        
        if not self._backlogMain.wlanNeededByPlugins() and (not self._backlogMain.backlog.isBusy() or self._dutycyclewhileresending):
            self._logger.info('turn off wlan')
            self._setExtStatus(self._wlanPort, False)
            self._backlogMain.gsnpeer.wlanTurnedOff()
            return True
        else:
            if self._backlogMain.backlog.isBusy() and not self._dutycyclewhileresending:
                self._logger.info('can not turn off wlan, still %s messages to resend' % (self._backlogMain.backlog.getDBNumberOfEntries()))
            return False
        
        
    def _serviceWindowTimer(self, waitSeconds):
        self._servicewindow = True
        self._wlanStateAfterServiceWindow = self._wlanToBeState
        try:
            self._wlanToBeState = True
            if not self.getWlanStatus():
                self._logger.info('service window starts now and lasts for %s seconds' % (waitSeconds,))
                self._setExtStatus(self._wlanPort, True)
            self._work.set()
        except Exception, e:
            self._backlogMain.incrementExceptionCounter()
            self._logger.exception('could not turn on wlan even though we are in service window: %s' % (e,))
        self._serviceWindowEvent.wait(waitSeconds)
        if not self._stopped:
            start, end = self._backlogMain.getNextServiceWindowRange()
            now = datetime.utcnow()
            self._timer = Timer(self._totalSeconds(start-now),self._serviceWindowTimer,[self._totalSeconds(end-start)+1])
            self._timer.start()
            self._servicewindow = False
            if not self._wlanStateAfterServiceWindow:
                self._logger.info('service window is finish')
                self.wlanOff()
    
    
    def _totalSeconds(self, td):
        return (td.microseconds + (td.seconds + td.days * 24 * 3600) * 10**6) / 10**6;
        
        
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