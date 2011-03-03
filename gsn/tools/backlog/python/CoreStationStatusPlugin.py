# -*- coding: UTF-8 -*-
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
from pyutmp import UtmpFile

from ScheduleHandler import SUBPROCESS_BUG_BYPASS
if SUBPROCESS_BUG_BYPASS:
    import SubprocessFake
    subprocess = SubprocessFake
else:
    import subprocess
    
import BackLogMessage
from AbstractPlugin import AbstractPluginClass

DEFAULT_BACKLOG = True

STATIC_TYPE = 1
HW_TYPE = 2
SW_TYPE = 3

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
            
        self._conf_calibrate = False
        if int(self.getOptionValue('calibrate')) == 1:
            self._conf_calibrate = True
            
        self._initFinish = False
    
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
        
        self.info('checking status providers for correctness')
        self._checkNumberOfUsers()
        self._checkLastLog()
        self._checkChronyStats()
        self._checkStatVFS()
        self._checkDiskStats()
        self._checkInterrupts()
        self._checkLoadAvg()
        self._checkMemInfo()
        self._checkSchedStat()
        self._checkNetDev()
        self._checkNetSNMP()
        self._checkNetStat()
        self._checkSockStat()
        self._checkSoftIRQ()
        self._checkStat()
        self._checkUptime()
        
        self._checkLM92Temp()
        self._initAD77x8()
        
        self.processMsg(self.getTimeStamp(), [STATIC_TYPE] + self._getInitStats())
        self._initFinish = True
        
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
        
#        print '##########################################################################'
#        print '_getNumberOfUsers :' + self._getNumberOfUsers()
#        print '_getLastLog :' + str(self._getLastLog())
#        print '_getStatVFS :' + str(self._getStatVFS())
#        print '_getDiskStats :' + str(self._getDiskStats())
#        print '_getInterrupts :' + str(self._getInterrupts())
#        print '_getLoadAvg :' + str(self._getLoadAvg())
#        print '_getMemInfo :' + str(self._getMemInfo())
#        print '_getSchedStat :' + str(self._getSchedStat())
#        print '_getNetDev :' + str(self._getNetDev())
#        print '_getNetSNMP :' + str(self._getNetSNMP())
#        print '_getNetStat :' + str(self._getNetStat())
#        print '_getSockStat :' + str(self._getSockStat())
#        print '_getSoftIRQ :' + str(self._getSoftIRQ())
#        print '_getStat :' + str(self._getStat())
#        print '_getUptime :' + str(self._getUptime())
#        print '_getChronyStats :' + str(self._getChronyStats())
#        print '_getLM92Temp :' + str(self._getLM92Temp())
        
        if self._initFinish:
            hw_data_list = [HW_TYPE]
            hw_timestamp = self.getTimeStamp()
            hw_data_list += self._getLM92Temp()
            hw_data_list += self._getAD77x8()
            
            sw_data_list = [SW_TYPE]
            sw_timestamp = self.getTimeStamp()
            sw_data_list += self._getNumberOfUsers()
            sw_data_list += self._getLastLog()
            sw_data_list += self._getStatVFS()
            sw_data_list += self._getDiskStats()
            sw_data_list += self._getInterrupts()
            sw_data_list += self._getLoadAvg()
            sw_data_list += self._getMemInfo()
            sw_data_list += self._getSchedStat()
            sw_data_list += self._getNetDev()
            sw_data_list += self._getNetSNMP()
            sw_data_list += self._getNetStat()
            sw_data_list += self._getSockStat()
            sw_data_list += self._getSoftIRQ()
            sw_data_list += self._getStat()
            sw_data_list += self._getUptime()
            sw_data_list += self._getChronyStats()
            
            self.processMsg(hw_timestamp, hw_data_list)
            self.processMsg(sw_timestamp, sw_data_list)
            
    
    
    def _getInitStats(self):
        '''
            [OS (string),
             hostname (string),
             kernel version (string),
             compiled by  (string),
             gcc version (string),
             compile time (string),
             processor (string),
             distribution name  (string),
             distribution timestamp  (string)]
        '''
        ret = [None]*9
        
        uname = os.uname()
#            print '#############################################################################'
#            print 'init stats'
#            print '[last user logged into the system (string)]'
#            print ''
#            print 'os.uname'
#            print str(uname)
#            print ''
        
        ret[0] = uname[0]
        ret[1] = uname[1]
        ret[2] = uname[2]
        
        try:
            file = open("/proc/version", "r")
            line = file.readline()
#            print '/proc/version'
#            print ''
#            print line
#            print ''
            file.close()
            lst = line.split()
            
            ret[3] = lst[3][1:-1]
            if lst[4].find('gcc') != -1:
                ret[4] = lst[6]
            else:
                self.error('os has not been compiled by gcc')
        except Exception, e:
            self.exception(str(e))
            
        ret[5] = uname[3]
        ret[6] = uname[4]
        
        try:
            file = open("/etc/version", "r")
            line = file.readline()
#            print '/etc/version'
#            print ''
#            print line
#            print ''
            file.close()
            lst = line.split()
            
            if len(lst) == 2:
                ret[7] = lst[0]
                ret[8] = lst[1]
            else:
                self.error('/etc/version does not split into 2 values')
        except Exception, e:
            self.exception(str(e))
        
        return ret
    
    
    def _checkNumberOfUsers(self):
        self._numberOfUsers = True
        try:
            users = 0
            for utmp in UtmpFile():
                if utmp.ut_user_process:
                    users += 1
        except Exception, e:
            self._numberOfUsers = False
            self.error(str(e))
    
    
    def _getNumberOfUsers(self):
        '''
            [number of users logged into the system (smallint)]
        '''
        ret = [None]
        if self._numberOfUsers:
            try:
                users = 0
                for utmp in UtmpFile():
                    if utmp.ut_user_process:
                        users += 1
                ret = [users]
            except Exception, e:
                self.exception(e)
        return ret
        
        
    def _checkLastLog(self):
        self._lastLog = True
        try:
            file = open("/var/log/lastlog", "r")
            line = file.read()
            file.close()
        except Exception, e:
            self._lastLog = False
            self.error(str(e))
        
        
    def _getLastLog(self):
        '''
            [last user logged into the system (string)]
        '''
        ret = [None]
        if self._lastLog:
            try:
                file = open("/var/log/lastlog", "r")
                line = file.read()
#                print '#############################################################################'
#                print '/var/log/lastlog'
#                print '[last user logged into the system (string)]'
#                print ''
#                print line
#                print ''
                file.close()
                ret = [line[36:line.index('\0', 36)]]
            except Exception, e:
                self.debug(str(e))
        return ret
        
        
    def _checkChronyStats(self):
        self._chronyTrackingLineIndexes = [None]*4
        self._chronyRTCDataAvailable = [False]*2
        
        self._chronyTracking = True
        try:
            p = subprocess.Popen(['chronyc', 'tracking'])
            p.wait()
            output = p.communicate()
            if output[0]:
                lines = output[0].split('\n')
                for lineindex, line in enumerate(lines):
                    if line.strip().startswith('Stratum'):
                        lst = line.split()
                        if len(lst) != 3:
                            self.error('splitted line containing Stratum from chronyc tracking did not return 3 values')
                        else:
                            self._chronyTrackingLineIndexes[0] = lineindex
                    elif line.strip().startswith('System time'):
                        lst = line.split()
                        if len(lst) != 9:
                            self.error('splitted line containing System time from chronyc tracking did not return 9 values')
                        elif lst[4] != 'seconds':
                            self.error('System time value from chronyc tracking is not in seconds')
                        else:
                            self._chronyTrackingLineIndexes[1] = lineindex
                    elif line.strip().startswith('Frequency'):
                        lst = line.split()
                        if len(lst) != 5:
                            self.error('splitted line containing Frequency from chronyc tracking did not return 5 values')
                        elif lst[3] != 'ppm':
                            self.error('Frequency value from chronyc tracking is not in ppm')
                        else:
                            self._chronyTrackingLineIndexes[2] = lineindex
                    elif line.strip().startswith('Skew'):
                        lst = line.split()
                        if len(lst) != 4:
                            self.error('splitted line containing Skew from chronyc tracking did not return 4 values')
                        elif lst[3] != 'ppm':
                            self.error('Skew value from chronyc tracking is not in ppm')
                        else:
                            self._chronyTrackingLineIndexes[3] = lineindex
            elif not output[1]:
                self.error('chronyc tracking has not generated any output')
                self._chronyTracking = False
            if output[1]:
                self.error('chronyc tracking: (STDERR=' + output[1] + ')')
                self._chronyTracking = False
        except Exception, e:
            self._chronyTracking = False
            self.exception(str(e))
        
        self._chronyRTC = True
        try:
            p = subprocess.Popen(['chronyc', 'rtcdata'])
            p.wait()
            output = p.communicate()
            if output[0]:
                lines = output[0].split('\n')
                if len(lines) != 6:
                    self.error('chronyc rtcdata did not return the expected output')
                    self._chronyRTC = False
                else:
                    line = lines[4]
                    lst = line.split()
                    if lst[0] != 'RTC':
                        self.error('chronyc rtcdata: RTC expected')
                    elif lst[-1] != 'seconds':
                        self.error('chronyc rtcdata: seconds expected')
                    else:
                        self._chronyRTCDataAvailable[0] = True
                        
                    line = lines[5]
                    lst = line.split()
                    if lst[0] != 'RTC':
                        self.error('chronyc rtcdata: RTC expected')
                    elif lst[-1] != 'ppm':
                        self.error('chronyc rtcdata: ppm expected')
                    else:
                        self._chronyRTCDataAvailable[1] = True
            elif not output[1]:
                self.error('chronyc tracking has not generated any output')
                self._chronyRTC = False
            if output[1]:
                self.error('chronyc tracking: (STDERR=' + output[1] + ')')
                self._chronyRTC = False
        except Exception, e:
            self._chronyRTC = False
            self.exception(str(e))
        
        
    def _getChronyStats(self):
        '''
            [stratum (int),
             system time (float),
             frequency (float),
             skew (float),
             RTC is fast/slow in seconds (float),
             RTC gains/loses time in ppm (float)]
        '''
        ret = [None]*6
        
        if self._chronyTracking:
            try:
                p = subprocess.Popen(['chronyc', 'tracking'])
                p.wait()
                output = p.communicate()
#                print '#############################################################################'
#                print 'chronyc tracking'
#                print '[stratum (int), system time (float), frequency (float), skew (float), RTC is fast/slow in seconds (float), RTC gains/loses time in ppm (float)]'
#                print ''
#                print output[0]
#                print ''
                if output[0]:
                    lines = output[0].split('\n')
                    for lineindex, line in enumerate(lines):
                        if self._chronyTrackingLineIndexes[0] == lineindex:
                            ret[0] = int(line.split()[2])
                        if self._chronyTrackingLineIndexes[1] == lineindex:
                            lst = line.split()
                            if lst[5] == 'fast':
                                ret[1] = float(lst[3])
                            elif lst[5] == 'slow':
                                ret[1] = -float(lst[3])
                            else:
                                self.error('System time value from chronyc tracking is not slow or fast')
                        if self._chronyTrackingLineIndexes[2] == lineindex:
                            lst = line.split()
                            if lst[4] == 'fast':
                                ret[2] = float(lst[2])
                            elif lst[4] == 'slow':
                                ret[2] = -float(lst[2])
                            else:
                                self.error('Frequency value from chronyc tracking is not slow or fast')
                        if self._chronyTrackingLineIndexes[3] == lineindex:
                            ret[3] = float(line.split()[2])
            except Exception, e:
                self.exception(e)
        
        if self._chronyRTC:
            try:
                p = subprocess.Popen(['chronyc', 'rtcdata'])
                p.wait()
                output = p.communicate()
#                print ''
#                print 'chronyc rtcdata'
#                print ''
#                print output[0]
#                print ''
                if output[0]:
                    lines = output[0].split('\n')
                    if self._chronyRTCDataAvailable[0]:
                        ret[4] = float(lines[4].split()[-2])
                        
                    if self._chronyRTCDataAvailable[1]:
                        ret[5] = float(lines[5].split()[-2])
            except Exception, e:
                self.exception(e)

        return ret
        
        
    def _checkStatVFS(self):
        self._statVFSAvailabe = [False]*3
        try:
            file = open("/proc/self/mounts", "r")
            lines = file.readlines()
            file.close()

            for lineindex, line in enumerate(lines):
                if line.find(' / ') != -1:
                    self._statVFSAvailabe[0] = True
                elif line.find(' /var/volatile ') != -1:
                    self._statVFSAvailabe[1] = True
                elif line.find(' /media/card ') != -1:
                    self._statVFSAvailabe[2] = True
        except Exception, e:
            self.exception(str(e))
        
        
    def _getStatVFS(self):
        '''
            [statvfs f_blocks * f_frsize for / (long),
             statvfs f_bfree * f_frsize for / (long),
             statvfs f_files * f_frsize for / (long),
             statvfs f_ffree * f_frsize for / (long),
             statvfs f_blocks * f_frsize for /var/volatile/ (long),
             statvfs f_bfree * f_frsize for /var/volatile/ (long),
             statvfs f_files * f_frsize for /var/volatile/ (long),
             statvfs f_ffree * f_frsize for /var/volatile/ (long),
             statvfs f_blocks * f_frsize for /media/card/ (long),
             statvfs f_bfree * f_frsize for /media/card/ (long),
             statvfs f_files * f_frsize for /media/card/ (long),
             statvfs f_ffree * f_frsize for /media/card/ (long)]
        '''
        ret = [None]*12
        try:
            if self._statVFSAvailabe[0]:
                stats = os.statvfs('/')
                ret[0] = stats.f_blocks * stats.f_frsize
                ret[1] = stats.f_bfree * stats.f_frsize
                ret[2] = stats.f_files * stats.f_frsize
                ret[3] = stats.f_ffree * stats.f_frsize
            if self._statVFSAvailabe[1]:
                stats = os.statvfs('/var/volatile/')
                ret[4] = stats.f_blocks * stats.f_frsize
                ret[5] = stats.f_bfree * stats.f_frsize
                ret[6] = stats.f_files * stats.f_frsize
                ret[7] = stats.f_ffree * stats.f_frsize
            if self._statVFSAvailabe[2]:
                stats = os.statvfs('/media/card/')
                ret[8] = stats.f_blocks * stats.f_frsize
                ret[9] = stats.f_bfree * stats.f_frsize
                ret[10] = stats.f_files * stats.f_frsize
                ret[11] = stats.f_ffree * stats.f_frsize
        except Exception, e:
            self.exception(e)
        return ret
    
    
    def _checkDiskStats(self):
        self._diskStatsLineIndex = None
        try:
            file = open("/proc/diskstats", "r")
            lines = file.readlines()
            file.close()
            for index, line in enumerate(lines):
                if line.find('mmcblk0p1') != -1:
                    lst = line.split()
                    if len(lst) != 14:
                        self.error('splitted line containing mmcblk0p1 from /proc/diskstats did not return 14 values')
                    else:
                        self._diskStatsLineIndex = index
        except Exception, e:
            self.exception(str(e))
        
        
    def _getDiskStats(self):
        '''
            [# of reads issued (long),
             # of sectors read (long),
             # of milliseconds spent reading (long),
             # of writes completed (long),
             # of sectors written (long),
             # of milliseconds spent writing (long),
             # of I/Os currently in progress (int),
             # of milliseconds spent doing I/Os (long),
             weighted # of milliseconds spent doing I/Os (long)]
        '''
        ret = [None]*9
        if self._diskStatsLineIndex != None:
            try:
                file = open("/proc/diskstats", "r")
                line = file.readlines()[self._diskStatsLineIndex]
                file.close()
#                print '#############################################################################'
#                print '/proc/diskstats'
#                print '[# of reads issued (long), # of sectors read (long), # of milliseconds spent reading (long), # of writes completed (long), # of sectors written (long), # of milliseconds spent writing (long), # of I/Os currently in progress (long), # of milliseconds spent doing I/Os (long), weighted # of milliseconds spent doing I/Os (long)]'
#                print ''
#                print str(line)
#                print ''
                lst = line.split()
                ret = [long(lst[3]), long(lst[5]), long(lst[6]), long(lst[7]), long(lst[9]), long(lst[10]), long(lst[11]), long(lst[12]), long(lst[13])]
            except Exception, e:
                self.exception(e)
        return ret
    
    
    def _checkInterrupts(self):
        names = ['ohci_hcd:usb1', 'pxa2xx-spi.2', 'pxa_i2c-i2c.0', 'STUART', 'BTUART', 'FFUART', 'pxa2xx-mci', 'DMA', 'ost0']
        self._interruptsLineIndexes = [None]*len(names)
        self._interrupts = True
        try:
            file = open("/proc/interrupts", "r")
            lst = file.readlines()
            file.close()
            for nameindex, name in enumerate(names):
                for lineindex, line in enumerate(lst):
                    if line.find(name) != -1:
                        linelist = line.split()
                        if len(linelist) != 4:
                            self.error('splitted line /proc/interrupts containing ' + name + ' did not return 4 values')
                        elif linelist[2] != 'SC':
                            self.error('/proc/interrupts ' + name + ' does not contain SC on 3. position')
                        else:
                            self._interruptsLineIndexes[nameindex] = lineindex
                        break
        except Exception, e:
            self._interrupts = False
            self.exception(str(e))
            
        for index, b in enumerate(self._interruptsLineIndexes):
            if b == None:
                self.warning('/proc/interrupts ' + names[index] + ' could not be found')
        
        
    def _getInterrupts(self):
        '''
            [# of ohci_hcd:usb1 interrupts (long),
             # of pxa2xx-spi.2 interrupts (long),
             # of pxa_i2c-i2c.0 interrupts (long),
             # of STUART interrupts (long),
             # of BTUART interrupts (long),
             # of FFUART interrupts (long),
             # of pxa2xx-mci interrupts (long),
             # of DMA interrupts (long),
             # of ost0 interrupts (long)]
        '''
        ret = [None]*len(self._interruptsLineIndexes)
        if self._interrupts:
            try:
                file = open("/proc/interrupts", "r")
                lst = file.readlines()
                file.close()
#                print '#############################################################################'
#                print '/proc/interrupts'
#                print '[# of ohci_hcd:usb1 interrupts (long), # of pxa2xx-spi.2 interrupts (long), # of pxa_i2c-i2c.0 interrupts (long), # of STUART interrupts (long), # of BTUART interrupts (long), # of FFUART interrupts (long), # of pxa2xx-mci interrupts (long), # of DMA interrupts (long), # of ost0 interrupts (long)]'
#                print ''
#                print str(lst)
#                print ''
                for index, lineindex in enumerate(self._interruptsLineIndexes):
                    if lineindex != None:
                        ret[index] = long(lst[lineindex].split()[1])
            except Exception, e:
                self.exception(e)
            
        return ret
        
        
    def _checkLoadAvg(self):
        self._loadAvg = True
        try:
            file = open("/proc/loadavg", "r")
            line = file.readline()
            file.close()
            lst = line.split()
            if len(lst) != 5:
                self.error('reading /proc/loadavg did not return 5 values')
                self._loadAvg = False
        except Exception, e:
            self._loadAvg = False
            self.exception(str(e))
        
        
    def _getLoadAvg(self):
        '''
            [# of processes in the system run queue averaged over the last 1 (float),
             # of processes in the system run queue averaged over the last 5 (float),
             # of processes in the system run queue averaged over the last 15 (float),
             # of runnable processes (int)]
        '''
        ret = [None]*4
        if self._loadAvg:
            try:
                file = open("/proc/loadavg", "r")
                line = file.readline()
                file.close()
                lst = line.split()
#                print '#############################################################################'
#                print '/proc/loadavg'
#                print '[# of processes in the system run queue averaged over the last 1 (float), # of processes in the system run queue averaged over the last 5 (float), # of processes in the system run queue averaged over the last 15 (float), # of runnable processes (int)]'
#                print ''
#                print line
#                print ''
                if len(lst) != 5:
                    self.exception('reading /proc/loadavg did not return 5 values')
                else:
                    ret = [float(lst[0]), float(lst[1]), float(lst[2]), int(lst[3].split('/')[1])]
            except Exception, e:
                self.exception(e)
        return ret
        
        
    def _checkMemInfo(self):
        names = ['MemTotal', 'MemFree', 'Buffers', 'Cached', 'Shmem', 'KernelStack', 'Slab', 'Mapped']
        self._memInfoLineIndexes = [None]*len(names)
        self._memInfo = True
        try:
            file = open("/proc/meminfo", "r")
            lst = file.readlines()
            file.close()
            for nameindex, name in enumerate(names):
                for lineindex, line in enumerate(lst):
                    if line.strip().startswith(name):
                        linelist = line.split()
                        if len(linelist) != 3:
                            self.error('splitted line /proc/meminfo containing ' + name + ' did not return 3 values')
                        elif linelist[2] != 'kB':
                            self.error('/proc/meminfo ' + name + ' does not end with kB')
                        else:
                            self._memInfoLineIndexes[nameindex] = lineindex
                        break
        except Exception, e:
            self._memInfo = False
            self.exception(str(e))
            
        for index, b in enumerate(self._memInfoLineIndexes):
            if b == None:
                self.error('/proc/meminfo ' + names[index] + ' could not be found')
        
        
    def _getMemInfo(self):
        '''
            [MemTotal in kB (int),
             MemFree in kB (int),
             Buffers in kB (int),
             Cached in kB (int),
             Shmem in kB (int),
             KernelStack in kB (int),
             Slab in kB (int),
             Mapped in kB (int)]
        '''
        ret = [None]*len(self._memInfoLineIndexes)
        
        if self._memInfo:
            try:
                file = open("/proc/meminfo", "r")
                lst = file.readlines()
                file.close()
#                print '#############################################################################'
#                print '/proc/meminfo'
#                print '[MemTotal in kB (int), MemFree in kB (int), Buffers in kB (int), Cached in kB (int), Shmem in kB (int), KernelStack in kB (int), Slab in kB (int), Mapped in kB (int)]'
#                print ''
#                print str(lst)
#                print ''
                for index, lineindex in enumerate(self._memInfoLineIndexes):
                    if lineindex != None:
                        ret[index] = int(lst[lineindex].split()[1])
            except Exception, e:
                self.exception(e)
        return ret
        
        
    def _checkSchedStat(self):
        self._schedstatLineIndex = None
        try:
            file = open("/proc/schedstat", "r")
            lines = file.readlines()
            file.close()
            for index, line in enumerate(lines):
                if index == 0:
                    lst = line.split()
                    if lst[1] != '15':
                        self.error('/proc/schedstat is not using version 15')
                if line.strip().startswith('cpu0'):
                    lst = line.split()
                    if len(lst) != 10:
                        self.error('splitted line containing cpu0 from /proc/schedstat did not return 10 values')
                    else:
                        self._schedstatLineIndex = index
                    break
            if self._schedstatLineIndex == None:
                self.error('cpu0 could not be found in /proc/schedstat')
        except Exception, e:
            self.exception(str(e))
        
        
    def _getSchedStat(self):
        '''
            [# of times sched_yield() was called (int),
             sum of all time spent running by tasks on this processor (in ms) (long),
             sum of all time spent waiting to run by tasks on this processor (in ms) (long),
             # of tasks (not necessarily unique) given to the processor (long)]
        '''
        ret = [None]*4
        if self._schedstatLineIndex != None:
            try:
                file = open("/proc/schedstat", "r")
                line = file.readlines()[self._schedstatLineIndex]
                file.close()
#                print '#############################################################################'
#                print '/proc/schedstat'
#                print '[# of times sched_yield() was called (int), sum of all time spent running by tasks on this processor (in ms) (long), sum of all time spent waiting to run by tasks on this processor (in ms) (long), # of tasks (not necessarily unique) given to the processor (long)]'
#                print ''
#                print str(line)
#                print ''
                lst = line.split()
                ret = [int(lst[1]), long(lst[7]), long(lst[8]), long(lst[9])]
            except Exception, e:
                self.exception(e)
        return ret
        
        
        
    def _checkNetDev(self):
        self._netDev = True
        try:
            file = open("/proc/net/dev", "r")
            lines = file.readlines()
            file.close()
        except Exception, e:
            self._netDev = False
            self.exception(str(e))
        
        
        
    def _getNetDev(self):
        '''
            [# of interfaces (smallint),
             eth0 in octets (long),
             eth0 out octets (long),
             ppp0 in octets (long),
             ppp0 out octets (long),
             wlan0 in octets (long),
             wlan0 out octets (long)]
        '''
        ret = [None]*7
        
        if self._netDev:
            try:
                file = open("/proc/net/dev", "r")
                lines = file.readlines()
                file.close()
#                print '#############################################################################'
#                print '/proc/net/dev'
#                print '[# of interfaces (smallint), eth0 in octets (long), eth0 out octets (long), ppp0 in octets (long), ppp0 out octets (long), wlan0 in octets (long), wlan0 out octets (long)]'
#                print ''
#                print str(lines)
#                print ''
                ret[0] = len(lines)-3
                for line in lines:
                    line = line.strip()
                    if line.startswith('eth0:'):
                        lst = line.split()
                        ret[1] = long(lst[1])
                        ret[2] = long(lst[9])
                    elif line.startswith('ppp0:'):
                        lst = line.split()
                        ret[3] = long(lst[1])
                        ret[4] = long(lst[9])
                    elif line.startswith('wlan0:'):
                        lst = line.split()
                        ret[5] = long(lst[1])
                        ret[6] = long(lst[9])
            except Exception, e:
                self.exception(e)
        return ret
        
        
    def _checkNetSNMP(self):
        names = ['InReceives', 'InHdrErrors', 'InAddrErrors', 'InUnknownProtos', 'InDiscards', 'InDelivers', 'OutRequests', 'OutDiscards', 'OutNoRoutes', 'InMsgs', 'InErrors', 'InDestUnreachs', 'InEchos', 'InEchoReps', 'OutMsgs', 'OutErrors', 'OutDestUnreachs', 'OutEchos', 'OutEchoReps', 'ActiveOpens', 'PassiveOpens', 'AttemptFails', 'EstabResets', 'CurrEstab', 'InSegs', 'OutSegs', 'RetransSegs', 'InErrs', 'OutRsts', 'InDatagrams', 'NoPorts', 'InErrors', 'OutDatagrams', 'RcvbufErrors', 'SndbufErrors']
        self._netSNMPLineIndexPair = [None]*len(names)
        
        self._netSNMP = True
        try:
            file = open("/proc/net/snmp", "r")
            lines = file.readlines()
            file.close()
            
            for nameindex, name in enumerate(names):
                for lineindex, line in enumerate(lines):
                    if line.strip().startswith('Ip:') or line.strip().startswith('Icmp:') or line.strip().startswith('Tcp:') or line.strip().startswith('Udp:') and line.find(name) != -1:
                        lst = line.split()
                        for valueindex, entry in enumerate(lst):
                            if entry == name:
                                self._netSNMPLineIndexPair[nameindex] = [lineindex+1, valueindex]
        except Exception, e:
            self._netSNMP = False
            self.exception(str(e))
            
        for index, b in enumerate(self._netSNMPLineIndexPair):
            if b == None:
                self.error('/proc/net/snmp ' + names[index] + ' could not be found')
        
        
    def _getNetSNMP(self):
        '''
            [Ip InReceives (long),
             Ip InHdrErrors (int),
             Ip InAddrErrors (int),
             Ip InUnknownProtos (int),
             Ip InDiscards (int),
             Ip InDelivers (long),
             Ip OutRequests (long),
             Ip OutDiscards (int),
             Ip OutNoRoutes (int),
             Icmp InMsgs (int),
             Icmp InErrors (int),
             Icmp InDestUnreachs (int),
             Icmp InEchos (int),
             Icmp InEchoReps (int),
             Icmp OutMsgs (int),
             Icmp OutErrors (int),
             Icmp OutDestUnreachs (int),
             Icmp OutEchos (int),
             Icmp OutEchoReps (int),
             Tcp ActiveOpens (int),
             Tcp PassiveOpens (int),
             Tcp AttemptFails (int),
             Tcp EstabResets (int),
             Tcp CurrEstab (int),
             Tcp InSegs (long),
             Tcp OutSegs (long),
             Tcp RetransSegs (int),
             Tcp InErrs (int),
             Tcp OutRsts (int),
             Udp InDatagrams (long),
             Udp NoPorts (int),
             Udp InErrors (int),
             Udp OutDatagrams (long),
             Udp RcvbufErrors (int),
             Udp SndbufErrors (int)]
        '''
        ret = [None]*len(self._netSNMPLineIndexPair)
        
        if self._netSNMP:
            try:
                file = open("/proc/net/snmp", "r")
                lines = file.readlines()
                file.close()
#                print '#############################################################################'
#                print '/proc/net/snmp'
#                print '[Ip InReceives (long), Ip InHdrErrors (int), Ip InAddrErrors (int), Ip InUnknownProtos (int), Ip InDiscards (int), Ip InDelivers (long), Ip OutRequests (long), Ip OutDiscards (int), Ip OutNoRoutes (int), Icmp InMsgs (int), Icmp InErrors (int), Icmp InDestUnreachs (int), Icmp InEchos (int), Icmp InEchoReps (int), Icmp OutMsgs (int), Icmp OutErrors (int), Icmp OutDestUnreachs (int), Icmp OutEchos (int), Icmp OutEchoReps (int), Tcp ActiveOpens (int), Tcp PassiveOpens (int), Tcp AttemptFails (int), Tcp EstabResets (int), Tcp CurrEstab (int), Tcp InSegs (long), Tcp OutSegs (long), Tcp RetransSegs (int), Tcp InErrs (int), Tcp OutRsts (int), Udp InDatagrams (long), Udp NoPorts (int), Udp InErrors (int), Udp OutDatagrams (long), Udp RcvbufErrors (int), Udp SndbufErrors (int)]'
#                print ''
#                print str(lines)
#                print ''
                splittedlines = [None]*len(lines)
                for i, line in enumerate(lines):
                    splittedlines[i] = line.split()
                for index, linepair in enumerate(self._netSNMPLineIndexPair):
                    if linepair != None:
                        ret[index] = long(splittedlines[linepair[0]][linepair[1]])
            except Exception, e:
                self.exception(e)
        return ret
        
        
        
    def _checkNetStat(self):
        names = ['TCPLoss', 'TCPLossUndo', 'InNoRoutes', 'InMcastOctets', 'OutMcastOctets', 'OutBcastPkts', 'OutOctets', 'InOctets', 'InBcastOctets', 'InMcastPkts', 'OutBcastOctets', 'InBcastPkts', 'OutMcastPkts', 'InTruncatedPkts']
        self._netStatLineIndexPair = [None]*len(names)
        
        self._netStat = True
        try:
            file = open("/proc/net/netstat", "r")
            lines = file.readlines()
            file.close()
            
            for nameindex, name in enumerate(names):
                for lineindex, line in enumerate(lines):
                    if line.strip().startswith('TcpExt:') or line.strip().startswith('IpExt:') and line.find(name) != -1:
                        lst = line.split()
                        for valueindex, value in enumerate(lst):
                            if name == value:
                                self._netStatLineIndexPair[nameindex] = [lineindex+1, valueindex]
        except Exception, e:
            self._netStat = False
            self.exception(str(e))
            
        for index, b in enumerate(self._netStatLineIndexPair):
            if b == None:
                self.error('/proc/net/netstat ' + names[index] + ' could not be found')
        
        
        
    def _getNetStat(self):
        '''
            [TCPLoss (int),
             TCPLossUndo (int),
             InNoRoutes (long),
             InMcastOctets (long),
             OutMcastOctets (long),
             OutBcastPkts (long),
             OutOctets (long),
             InOctets (long),
             InBcastOctets (long),
             InMcastPkts (long),
             OutBcastOctets (long),
             InBcastPkts (long),
             OutMcastPkts (long),
             InTruncatedPkts (long)]
        '''
        ret = [None]*len(self._netStatLineIndexPair)
        
        if self._netStat:
            try:
                file = open("/proc/net/netstat", "r")
                lines = file.readlines()
                file.close()
#                print '#############################################################################'
#                print '/proc/net/netstat'
#                print '[TCPLoss (int), TCPLossUndo (int), InNoRoutes (long), InMcastOctets (long), OutMcastOctets (long), OutBcastPkts (long), OutOctets (long), InOctets (long), InBcastOctets (long), InMcastPkts (long), OutBcastOctets (long), InBcastPkts (long), OutMcastPkts (long), InTruncatedPkts (long)]'
#                print ''
#                print str(lines)
#                print ''
                splittedlines = [None]*len(lines)
                for i, line in enumerate(lines):
                    splittedlines[i] = line.split()
                for index, linepair in enumerate(self._netStatLineIndexPair):
                    if linepair != None:
                        ret[index] = long(splittedlines[linepair[0]][linepair[1]])
            except Exception, e:
                self.exception(e)
        return ret
        
        
    def _checkSockStat(self):
        self._sockStatLineIndexes = [None]*3

        self._sockStat = True
        try:
            file = open("/proc/net/sockstat", "r")
            lines = file.readlines()
            file.close()

            socketsAvailable = False
            tcpAvailable = False
            udpAvailable = False
            for lineindex, line in enumerate(lines):
                if line.strip().startswith('sockets:'):
                    socketsAvailable = True
                    lst = line.split()
                    if len(lst) != 3:
                        self.error('splitted line containing sockets from /proc/net/sockstat did not return 3 values')
                    else:
                        self._sockStatLineIndexes[0] = lineindex
                elif line.strip().startswith('TCP:'):
                    tcpAvailable = True
                    lst = line.split()
                    if len(lst) != 11:
                        self.error('splitted line containing TCP from /proc/net/sockstat did not return 11 values')
                    else:
                        self._sockStatLineIndexes[1] = lineindex
                elif line.strip().startswith('UDP:'):
                    udpAvailable = True
                    lst = line.split()
                    if len(lst) != 5:
                        self.error('splitted line containing UDP from /proc/net/sockstat did not return 5 values')
                    else:
                        self._sockStatLineIndexes[2] = lineindex
            if not socketsAvailable:
                self.error('sockets could not be found in /proc/net/sockstat')
            if not tcpAvailable:
                self.error('TCP could not be found in /proc/net/sockstat')
            if not udpAvailable:
                self.error('UDP could not be found in /proc/net/sockstat')
        except Exception, e:
            self._sockStat = False
            self.exception(str(e))
        
        
    def _getSockStat(self):
        '''
            [# of used sockets (int),
             TCP inuse (int),
             TCP orphan (int),
             TCP tw (int),
             TCP alloc (int),
             TCP mem (int),
             UDP inuse (int),
             UDP mem (int)]
        '''
        ret = [None]*8
        if self._sockStat:
            try:
                file = open("/proc/net/sockstat", "r")
                lines = file.readlines()
                file.close()
#                print '#############################################################################'
#                print '/proc/net/sockstat'
#                print '[# of used sockets (int), TCP inuse (int), TCP orphan (int), TCP tw (int), TCP alloc (int), TCP mem (int), UDP inuse (int), UDP mem (int)]'
#                print ''
#                print str(lines)
#                print ''
                for lineindex, line in enumerate(lines):
                    if self._sockStatLineIndexes[0] == lineindex:
                        ret[0] = int(line.split()[2])
                    elif self._sockStatLineIndexes[1] == lineindex:
                        lst = line.split()
                        ret[1] = int(lst[2])
                        ret[2] = int(lst[4])
                        ret[3] = int(lst[6])
                        ret[4] = int(lst[8])
                        ret[5] = int(lst[10])
                    elif self._sockStatLineIndexes[2] == lineindex:
                        udpAvailable = True
                        lst = line.split()
                        ret[6] = int(lst[2])
                        ret[7] = int(lst[4])
            except Exception, e:
                self.exception(e)
        return ret
        
        
    def _checkSoftIRQ(self):
        names = ['TIMER', 'TASKLET', 'HRTIMER']
        self._softIRQLineIndexes = [None]*len(names)
        self._softIRQ = True
        try:
            file = open("/proc/softirqs", "r")
            lst = file.readlines()
            file.close()
            
            for nameindex, name in enumerate(names):
                for lineindex, line in enumerate(lst):
                    if line.strip().startswith(name):
                        linelist = line.split()
                        if len(linelist) != 2:
                            self.error('splitted line /proc/softirqs containing ' + name + ' did not return 2 values')
                        else:
                            self._softIRQLineIndexes[nameindex] = lineindex
                        break
        except Exception, e:
            self._softIRQ = False
            self.exception(str(e))
            
        for index, b in enumerate(self._softIRQLineIndexes):
            if b == None:
                self.error('/proc/softirqs ' + names[index] + ' could not be found')
        
        
    def _getSoftIRQ(self):
        '''
            [TIMER (long),
             TASKLET (long),
             HRTIMER (long)]
        '''
        ret = [None]*len(self._softIRQLineIndexes)
        if self._softIRQ:
            try:
                file = open("/proc/softirqs", "r")
                lines = file.readlines()
                file.close()
#                print '#############################################################################'
#                print '/proc/softirqs'
#                print '[TIMER (long), TASKLET (long), HRTIMER (long)]'
#                print ''
#                print str(lst)
#                print ''
                for index, lineindex in enumerate(self._softIRQLineIndexes):
                    if lineindex != None:
                        ret[index] = long(lines[lineindex].split()[1])
            except Exception, e:
                self.exception(e)
        return ret
        
        
    def _checkStat(self):
        self._statLineIndexes = [None]*4
        
        self._stat = True
        try:
            file = open("/proc/stat", "r")
            lines = file.readlines()
            file.close()
            
            cpu0Available = False
            ctxtAvailable = False
            processesAvailable = False
            procsBlockedAvailable = False
            for lineindex, line in enumerate(lines):
                if line.strip().startswith('cpu0'):
                    cpu0Available = True
                    lst = line.split()
                    if len(lst) != 11:
                        self.error('splitted line containing cpu0 from /proc/stat did not return 11 values')
                    else:
                        self._statLineIndexes[0] = lineindex
                elif line.strip().startswith('ctxt'):
                    ctxtAvailable = True
                    lst = line.split()
                    if len(lst) != 2:
                        self.error('splitted line containing ctxt from /proc/stat did not return 2 values')
                    else:
                        self._statLineIndexes[1] = lineindex
                elif line.strip().startswith('processes'):
                    processesAvailable = True
                    lst = line.split()
                    if len(lst) != 2:
                        self.error('splitted line containing processes from /proc/stat did not return 2 values')
                    else:
                        self._statLineIndexes[2] = lineindex
                elif line.strip().startswith('procs_blocked'):
                    procsBlockedAvailable = True
                    lst = line.split()
                    if len(lst) != 2:
                        self.error('splitted line containing procs_blocked from /proc/stat did not return 2 values')
                    else:
                        self._statLineIndexes[3] = lineindex
            if not cpu0Available:
                self.error('cpu0 could not be found in /proc/stat')
            if not ctxtAvailable:
                self.error('ctxt could not be found in /proc/stat')
            if not processesAvailable:
                self.error('processes could not be found in /proc/stat')
            if not procsBlockedAvailable:
                self.error('procs_blocked could not be found in /proc/stat')
        except Exception, e:
            self._stat = False
            self.exception(str(e))
        
        
    def _getStat(self):
        '''
            [cpu0 user: normal processes executing in user mode (long),
             cpu0 nice: niced processes executing in user mode (long),
             cpu0 system: processes executing in kernel mode (long),
             cpu0 idle: twiddling thumbs (long),
             cpu0 iowait: waiting for I/O to complete (long),
             cpu0 irq: servicing interrupts (long),
             cpu0 softirq: servicing softirqs (long),
             # of context switches across all CPUs (long),
             # of processes and threads created (int),
             # of processes currently blocked, waiting for I/O to complete (int)]
        '''
        ret = [None]*10
        if self._stat:
            try:
                file = open("/proc/stat", "r")
                lines = file.readlines()
                file.close()
#                print '#############################################################################'
#                print '/proc/stat'
#                print '[cpu0 user: normal processes executing in user mode (long), cpu0 nice: niced processes executing in user mode (long), cpu0 system: processes executing in kernel mode (long), cpu0 idle: twiddling thumbs (long), cpu0 iowait: waiting for I/O to complete (long), cpu0 irq: servicing interrupts (long), cpu0 softirq: servicing softirqs (long), # of context switches across all CPUs (long), # of processes and threads created (int), # of processes currently blocked, waiting for I/O to complete (int)]'
#                print ''
#                print str(lines)
#                print ''
                for lineindex, line in enumerate(lines):
                    if self._statLineIndexes[0] == lineindex:
                        lst = line.split()
                        ret[0] = long(lst[1])
                        ret[1] = long(lst[2])
                        ret[2] = long(lst[3])
                        ret[3] = long(lst[4])
                        ret[4] = long(lst[5])
                        ret[5] = long(lst[6])
                        ret[6] = long(lst[7])
                    if self._statLineIndexes[1] == lineindex:
                        ret[7] = long(line.split()[1])
                    if self._statLineIndexes[2] == lineindex:
                        ret[8] = long(line.split()[1])
                    if self._statLineIndexes[3] == lineindex:
                        ret[9] = long(line.split()[1])
            except Exception, e:
                self.exception(e)
        return ret
        
        
    def _checkUptime(self):
        self._uptime = True
        try:
            file = open("/proc/uptime", "r")
            line = file.read()
            file.close()
            lst = line.split()
            if len(lst) != 2:
                self.exception('reading /proc/uptime did not return 2 values')
                self._uptime = False
        except Exception, e:
            self._uptime = False
            self.error(str(e))
        
        
    def _getUptime(self):
        '''
            [# of seconds the system has been up (double),
             # of seconds the system has been idle (double)]
        '''
        ret = [None]*2
        if self._uptime:
            try:
                file = open("/proc/uptime", "r")
                line = file.read()
                file.close()
#                print '#############################################################################'
#                print '/proc/uptime'
#                print '[# of seconds the system has been up (double), # of seconds the system has been idle (double)]'
#                print ''
#                print line
#                print ''
                lst = line.split()
                ret = [float(lst[0]), float(lst[1])]
            except Exception, e:
                self.exception(e)
        return ret
    
    
    def _checkLM92Temp(self):
        self._lm92Temp = True
        try:
            file = open("/sys/bus/i2c/devices/0-004b/temp1_input", "r")
            line = file.readline()
            file.close()
            val = int(line)
        except Exception, e:
            self._lm92Temp = False
            self.error(str(e))
    
    
    def _getLM92Temp(self):
        '''
            [LM92 temperature (int)]
        '''
        ret = [None]
        if self._lm92Temp:
            try:
                file = open("/sys/bus/i2c/devices/0-004b/temp1_input", "r")
                line = file.readline()
                file.close()
#                print '#############################################################################'
#                print '/sys/bus/i2c/devices/0-004b/temp1_input'
#                print '[LM92 (int)]'
#                print ''
#                print line
#                print ''
                val = int(line)
                if val != -240000:
                    ret = [val]
            except Exception, e:
                self.exception(e)
        return ret
        
        
    def _initAD77x8(self):
        self._ad77x8 = True
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
            self.warning('module ad77x8 is not available (modprobe ad77x8 returned with code ' + str(ret) + ')')
            self._ad77x8 = False
        
        if self._ad77x8 and not self._calibrated and os.path.isfile(CALIB_FILE) and self._conf_calibrate:
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
                            self._ain4_cal = float(line.split('=')[1].split()[0])
                        if line.split('=')[0] == 'avg_ain9':
                            self._ain9_cal = float(line.split('=')[1].split()[0])
                    fcalib.close()
                    if self._ain4_cal and self._ain9_cal:
                        self._calibrated = True
            except Exception, e:
                self.warning(e.__str__())
                if fcalib:
                    fcalib.close()
                if frtc:
                    frtc.close()
                    
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
        
        
    def _getAD77x8(self):
        '''
            [ad77x8_1 (int),
             ad77x8_2 (int),
             ad77x8_3 (int),
             ad77x8_4 (int),
             ad77x8_5 (int),
             ad77x8_6 (int),
             ad77x8_7 (int),
             ad77x8_8 (int),
             ad77x8_9 (int),
             ad77x8_10 (int)]
        '''
        ret = [None]*10
        
        if self._ad77x8:
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
    
                if self._calibrated and self._conf_calibrate:
                    ad77x8_6 = ad77x8_6 - 0.3
                    ad77x8_4 = ad77x8_4 - self._ain4_cal
                    ad77x8_9 = ad77x8_9 - self._ain9_cal
                if ad77x8_4 < 0:
                    ad77x8_4 = 0
                if ad77x8_6 < 0:
                    ad77x8_6 = 0
                if ad77x8_9 < 0:
                    ad77x8_9 = 0
    
                ad77x8_1 = int(round(ad77x8_1 * 11))
                ad77x8_2 = None
                ad77x8_3 = int(round(ad77x8_3 * 23 / 3.0))
                ad77x8_4 = int(round(ad77x8_4 * 10000))
                ad77x8_5 = int(round(ad77x8_5 * 23 / 3.0))
                ad77x8_6 = int(round(ad77x8_6 * 2000))
                ad77x8_7 = int(round(ad77x8_7 * 151 / 51.0))
                ad77x8_8 = int(round(ad77x8_8 * 2))
                ad77x8_9 = int(round(ad77x8_9 * 200 / 3.0))
                ad77x8_10 = int(round(ad77x8_10 * 2))
                if not self._calibrated and self._conf_calibrate:
                    ad77x8_9 = None
                    
                ret = [ad77x8_1, ad77x8_2, ad77x8_3, ad77x8_4, ad77x8_5, ad77x8_6, ad77x8_7, ad77x8_8, ad77x8_9, ad77x8_10]
            except Exception, e:
                self.warning(e.__str__())
            
        return ret

    
    
    def stop(self):
        self._stopped = True
        self._sleeper.set()
        if self._timer:
            self._timer.cancel()
        self.info('stopped')
