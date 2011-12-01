# -*- coding: UTF-8 -*-
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import struct
import platform
import resource
import threading

import BackLogMessage
from AbstractPlugin import AbstractPluginClass

DEFAULT_BACKLOG = True

STATIC_TYPE = 1
REVISION_TYPE = 2
DYNAMIC_TYPE = 3

class BackLogStatusPluginClass(AbstractPluginClass):
    '''
    This plugin offers the functionality to receive commands from the GSN Backlog wrapper.
    It also sends BackLogStatus messages.
    
    Any new status information coming from this program should be implemented here.
    '''

    '''
    _timer
    _interval
    '''
    
    def __init__(self, parent, config):
        AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG)
        self._timer = None
        
        self._sleeper = threading.Event()
        self._stopped = False
        self._initFinish = False
        
        value = self.getOptionValue('poll_interval')
        if value is None:
            self._interval = None
        else:
            self._interval = float(value)
            self.info('interval: %s' % (self._interval,))
    
    
    def getMsgType(self):
        return BackLogMessage.BACKLOG_STATUS_MESSAGE_TYPE
        
        
    def isBusy(self):
        return False
        
        
    def needsWLAN(self):
        return False
    
    
    def msgReceived(self, data):
        if data[0] == 1:
            self.info('received command resend')
            self._backlogMain.backlog.resend()
       
        
    def run(self):
        self.name = 'BackLogStatusPlugin-Thread'
        self.info('started')
        
        self._checkStatus()
        
        self.processMsg(self.getTimeStamp(), [STATIC_TYPE] + self._getInitStats())
        
        for entry in self._backlogMain.getCodeRevisionList():
            self.processMsg(self.getTimeStamp(), [REVISION_TYPE] + entry)
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
                self._timer = threading.Timer(int(paramlist[0]), self.action, [''])
                self._timer.start()
            else:
                self.error('parameter has to be a digit (parameter=%s)' % (parameters,))
        
        if self._initFinish:
            payload = [DYNAMIC_TYPE]
            timestamp = self.getTimeStamp()
            payload += [self.getUptime(), self.getErrorCounter(), self.getExceptionCounter()]
            payload += self._backlogMain.gsnpeer.getStatus()
            payload += self._backlogMain.backlog.getStatus(30)
            
            payload += self._backlogMain.schedulehandler.getStatus()
            payload += self._backlogMain.jobsobserver.getStatus()
            payload += self._backlogMain.getTOSPeerStatus()
            
            payload += self._getStatus()
            payload += self._getRUsage()
            
            self.processMsg(timestamp, payload)
            
    
    
    def _getInitStats(self):
        '''
            [python_implementation (string),
             python_version (string),
             python_compiler (string),
             python_build (string),
             python_build date (string),
             last clean shutdown (bigint)]
        '''
        ret = [None]*6
        try:
            ret[0] = platform.python_implementation()
            ret[1] = platform.python_version()
            ret[2] = platform.python_compiler()
            ret[3] = platform.python_build()[0]
            ret[4] = platform.python_build()[1]
            ret[5] = self._backlogMain.lastCleanShutdown()
        except Exception, e:
            self.exception(e)
        return ret
        
        
    def _checkStatus(self):
        names = ['VmPeak:', 'VmSize:', 'VmLck:', 'VmHWM:', 'VmRSS:', 'VmData:', 'VmStk:', 'VmExe:', 'VmLib:', 'VmPTE:', 'Threads:', 'voluntary_ctxt_switches:', 'nonvoluntary_ctxt_switches:']
        self._statusLineIndexes = [None]*len(names)
        try:
            file = open("/proc/self/status", "r")
            lst = file.readlines()
            file.close()
            for nameindex, name in enumerate(names):
                for lineindex, line in enumerate(lst):
                    if line.strip().startswith(name):
                        linelist = line.split()
                        value = None
                        check = False
                        if nameindex <= 9:
                            if len(linelist) != 3:
                                self.exception('splitted line /proc/self/status containing %s did not return 3 values' % (name,))
                            elif linelist[2] != 'kB':
                                self.exception('/proc/self/status %s does not end with kB' % (name,))
                            else:
                                check = True
                        else:
                            if len(linelist) != 2:
                                self.exception('splitted line /proc/self/status containing %s did not return 2 values' % (name,))
                            else:
                                check = True
                        if check:
                            self._statusLineIndexes[nameindex] = lineindex
                        break
        except Exception, e:
            self.exception(e)
            
        for index, b in enumerate(self._statusLineIndexes):
            if b == None:
                self.exception('/proc/self/status %s could not be found' % (names[index],))
        
        
    def _getStatus(self):
        '''
            [VmPeak in kB (int),
             VmSize in kB (int),
             VmLck in kB (int),
             VmHWM in kB (int),
             VmRSS in kB (int),
             VmData in kB (int),
             VmStk in kB (int),
             VmExe in kB (int),
             VmLib in kB (int),
             VmPTE in kB (int),
             # of threads (int),
             voluntary_ctxt_switches (int),
             nonvoluntary_ctxt_switches (int)]
        '''
        ret = [None]*len(self._statusLineIndexes)
        try:
            file = open("/proc/self/status", "r")
            lines = file.readlines()
            file.close()
#            print '#############################################################################'
#            print '/proc/self/status'
#            print '[VmPeak in kB (int), VmSize in kB (int), VmLck in kB (int), VmHWM in kB (int), VmRSS in kB (int), VmData in kB (int), VmStk in kB (int), VmExe in kB (int), VmLib in kB (int), VmPTE in kB (int), # of threads (int), voluntary_ctxt_switches (int), nonvoluntary_ctxt_switches (int)]'
#            print ''
#            print str(lines)
#            print ''
            for index, lineindex in enumerate(self._statusLineIndexes):
                if lineindex != None:
                    ret[index] = int(lines[lineindex].split()[1])
        except Exception, e:
            self.exception(e)
        return ret
    
    
    def _getRUsage(self):
        '''
            [ru_utime (double),
             ru_stime (double),
             ru_minflt (int),
             ru_majflt (int),
             ru_nvcsw (int),
             ru_nivcsw (int)]
        '''
        ret = [None]*6
        try:
            r = resource.getrusage(resource.RUSAGE_SELF)
            ret[0] = r[0]
            ret[1] = r[1]
            ret[2] = r[6]
            ret[3] = r[7]
            ret[4] = r[14]
            ret[5] = r[15]
        except Exception, e:
            self.exception(e)
        return ret
    
    
    def stop(self):
        self._stopped = True
        self._sleeper.set()
        if self._timer:
            self._timer.cancel()
        self.info('stopped')
