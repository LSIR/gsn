# -*- coding: UTF-8 -*-
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"


import os
import signal
import logging
from datetime import datetime, timedelta
from threading import Event, Lock, Thread

from SpecialAPI import Statistics

JOB_PROCESS_CHECK_INTERVAL_SECONDS = 10

DEFAULT_RUNTIME_MODE = 5
DEFAULT_MIN_RUNTIME = 0


RUNTIME_MODE_STOP_ALLWAYS = 1
RUNTIME_MODE_STOP_DC_ALLWAYS = 2
RUNTIME_MODE_STOP_LAST = 3
RUNTIME_MODE_STOP_DC_LAST = 4
RUNTIME_MODE_NO_OBSERVE = 5

        
class JobsObserverClass(Thread, Statistics):
    
    def __init__(self, parent):
        Thread.__init__(self, name='JobsObserver-Thread')
        self._logger = logging.getLogger(self.__class__.__name__)
        Statistics.__init__(self)
        
        self._backlogMain = parent
        self._lock = Lock()
        self._jobList = []
        self._work = Event()
        self._wait = Event()
        self._waitforjob = Event()
        self._jobsObserverStop = False
        
        self._plugFinInTimeCounterId = self.createCounter()
        self._plugNotFinInTimeCounterId = self.createCounter()
        self._scriptFinSucInTimeCounterId = self.createCounter()
        self._scriptFinUnsucInTimeCounterId = self.createCounter()
        self._scriptNotFinInTimeCounterId = self.createCounter()
        
        
    def run(self):
        self._logger.info('started')
        log_beacon_mode = dict()
        while not self._jobsObserverStop:
            self._work.wait()
            if self._jobsObserverStop:
                break
            self._work.clear()
            
            while not self._jobsObserverStop:
                self._wait.wait(JOB_PROCESS_CHECK_INTERVAL_SECONDS)
                if self._jobsObserverStop:
                    break
            
                if len(log_beacon_mode) > 0 and not self._backlogMain.schedulehandler._beacon:
                    log_beacon_mode.clear()
                
                allFinished = True
                index = len(self._jobList)
                for joblistentry in reversed(self._jobList):
                    index -= 1
                    isPlugin, job, job_name, runtime_end, min_runtime = joblistentry
                    if isPlugin:
                        if job.isBusy():
                            if runtime_end is not None:
                                if runtime_end <= datetime.utcnow():
                                    self._logger.warning('plugin (%s) has not finished in time -> stop it' % (job_name,))
                                    self._backlogMain.pluginStop(job_name)
                                    del self._jobList[index]
                                    self.counterAction(self._plugNotFinInTimeCounterId)
                                else:
                                    allFinished = False
                                    if self._logger.isEnabledFor(logging.DEBUG):
                                        self._logger.debug('plugin (%s) has not yet finished -> %s time to run' % (job_name, runtime_end-datetime.utcnow()))
                            else:
                                allFinished = False
                        else:
                            if min_runtime <= datetime.utcnow():
                                mode = job.getRuntimeMode()
                                if self._backlogMain.schedulehandler._beacon and self._backlogMain.duty_cycle_mode and (mode == RUNTIME_MODE_STOP_DC_ALLWAYS or mode == RUNTIME_MODE_STOP_DC_LAST):
                                    allFinished = False
                                    if log_beacon_mode.get(job_name) is None:
                                        self._logger.info('%s should not be stopped if in beacon mode => keep running' % (job_name,))
                                        log_beacon_mode.update({job_name:False})
                                elif mode == RUNTIME_MODE_STOP_ALLWAYS or (mode == RUNTIME_MODE_STOP_DC_ALLWAYS and self._backlogMain.duty_cycle_mode):
                                    self._backlogMain.pluginStop(job_name)
                                    self._dutyModeDependentLogging('plugin (%s) finished successfully in time' % (job_name,))
                                    del self._jobList[index]
                                    self.counterAction(self._plugNotFinInTimeCounterId)
                            else:
                                allFinished = False
                                if self._logger.isEnabledFor(logging.DEBUG):
                                    self._logger.debug('plugin (%s) has not yet reached min_runtime %s more seconds to go -> keep running' % (job_name, min_runtime-datetime.utcnow()))
                    else:
                        ret = job.poll()
                        if ret == None:
                            if runtime_end is not None:
                                if runtime_end <= datetime.utcnow():
                                    self.error('job (%s) with PID %s has not finished in time -> kill it' % (job_name, job.pid()))
                                    try:
                                        os.killpg(job.pid(), signal.SIGTERM)
                                    except:
                                        pass
                                    self._logger.warning('wait for job (%s) to be killed' % (job_name,))
                                    self._wait.wait(0.1)
                                    if job.poll() == None:
                                        self._wait.wait(3)
                                    if self._wait.isSet():
                                        break
                                    if job.poll() == None:
                                        try:
                                            os.killpg(job.pid(), signal.SIGKILL)
                                        except:
                                            pass
                                    job.wait()
                                    stdoutdata, stderrdata = job.communicate()
                                    self._logger.warning('job (%s) has been killed (STDOUT=%s /STDERR=%s)' % (job_name, stdoutdata.decode(), stderrdata.decode()))
                                    del self._jobList[index]
                                    self.counterAction(self._scriptNotFinInTimeCounterId)
                                else:
                                    allFinished = False
                                    if self._logger.isEnabledFor(logging.DEBUG):
                                        self._logger.debug('job (%s) with PID %s not yet finished -> %s time to run' % (job_name, job.pid(), runtime_end-datetime.utcnow()))
                            else:
                                allFinished = False
                        else:
                            stdoutdata, stderrdata = job.communicate()
                            if ret == 0:
                                self._dutyModeDependentLogging('job (%s) finished successfully (STDOUT=%s /STDERR=%s)' % (job_name, stdoutdata.decode(), stderrdata.decode()))
                                self.counterAction(self._scriptFinSucInTimeCounterId)
                            else:
                                self.error('job (%s) finished with return code %s (STDOUT=%s /STDERR=%s)' % (job_name, ret, stdoutdata.decode(), stderrdata.decode()))
                                self.counterAction(self._scriptFinUnsucInTimeCounterId)
                            del self._jobList[index]
                            
                if allFinished:
                    self._dutyModeDependentLogging('all observed jobs finished -> stopping the rest')
                    index = len(self._jobList)
                    for joblistentry in reversed(self._jobList):
                        index -= 1
                        isPlugin, job, job_name, runtime_end, min_runtime = joblistentry
                        if isPlugin:
                            self._dutyModeDependentLogging('stopping plugin (%s)' % (job_name,))
                            self._backlogMain.pluginStop(job_name)
                            del self._jobList[index]
                        else:
                            self.exception('there should be no more scripts around anymore (%s)' % (job_name,))
                            
                    
                
                self._lock.acquire()
                if not self._jobList:
                    self._work.clear()
                    self._backlogMain.schedulehandler.allJobsFinished()
                    self._lock.release()
                    break
                else:
                    self._lock.release()
 
        self._logger.info('died')
        
        
        
    def observeJob(self, job, job_name, isPlugin, max_runtime_minutes, min_runtime_minutes=None):
        if not self._jobsObserverStop:
            self._lock.acquire()
            if min_runtime_minutes is None:
                min_runtime_minutes = DEFAULT_MIN_RUNTIME
            now = datetime.utcnow()
            runtime_end = None
            max_string = 'and an unlimited maximum runtime'
            if max_runtime_minutes is not None:
                max_string = 'and a maximum runtime of %s minutes' % (max_runtime_minutes,)
                runtime_end = now+timedelta(minutes=max_runtime_minutes)
            if isPlugin:
                mode = job.getRuntimeMode()
                if max_runtime_minutes is not None or mode == RUNTIME_MODE_STOP_ALLWAYS or mode == RUNTIME_MODE_STOP_LAST or \
                   ((mode == RUNTIME_MODE_STOP_DC_ALLWAYS or mode == RUNTIME_MODE_STOP_DC_LAST) and self._backlogMain.duty_cycle_mode):
                    jobexists = False
                    for index, joblistentry in enumerate(self._jobList):
                        if job_name == joblistentry[2]:
                            self._jobList[index] = (isPlugin, job, job_name, runtime_end, now+timedelta(minutes=min_runtime_minutes))
                            jobexists = True
                            self._dutyModeDependentLogging('plugin job (%s) updated with a minimum runtime of %s minutes %s' % (job_name, min_runtime_minutes, max_string))
                            break
                    if not jobexists:
                        self._jobList.append((isPlugin, job, job_name, runtime_end, now+timedelta(minutes=min_runtime_minutes)))
                        self._dutyModeDependentLogging('new plugin job (%s) added with a minimum runtime of %s minutes %s' % (job_name, min_runtime_minutes, max_string))
                else:
                    self._lock.release()
                    return
            else:
                self._jobList.append((isPlugin, job, job_name, runtime_end, now+timedelta(minutes=min_runtime_minutes)))
                self._dutyModeDependentLogging('new script job (%s) added with a minimum runtime of %s minutes %s' % (job_name, min_runtime_minutes, max_string))
            self._lock.release()
            self._backlogMain.schedulehandler.newJobStarted()
            self._work.set()
            
            
    def getOverallJobsMaxRuntimeSec(self):
        if not self._jobList:
            return None
        overallMaxRuntime = timedelta()
        for isPlugin, job, job_name, runtime_end, min_runtime in self._jobList:
            if runtime_end == None:
                return -1
            else:
                runtime_left = runtime_end-datetime.utcnow()
                if  overallMaxRuntime < runtime_left:
                    overallMaxRuntime = runtime_left
        return overallMaxRuntime.seconds + overallMaxRuntime.days * 86400 + overallMaxRuntime.microseconds/1000000.0
            
            
    def getStatus(self):
        '''
        Returns the status of the jobs observer as list:
        
        @return: status of the jobs observer [plugin finished in time,
                                              plugin still busy after max runtime,
                                              script finished successfully in time,
                                              script finished unsuccessfully in time,
                                              script not finished in time]
        '''
        return [self.getCounterValue(self._plugFinInTimeCounterId), \
                self.getCounterValue(self._plugNotFinInTimeCounterId), \
                self.getCounterValue(self._scriptFinSucInTimeCounterId), \
                self.getCounterValue(self._scriptFinUnsucInTimeCounterId), \
                self.getCounterValue(self._scriptNotFinInTimeCounterId)]


    def stop(self):
        self._jobsObserverStop = True
        self._work.set()
        self._wait.set()
        
        for isPlugin, job, job_name, runtime_end, min_runtime in self._jobList:
            if isPlugin:
                self._backlogMain.pluginStop(job_name)
            else:
                self.error('job (%s) with PID %s has not finished yet -> kill it' % (job_name, job.pid()))
                try:
                    os.killpg(job.pid(), signal.SIGTERM)
                except:
                    pass
                self._logger.warning('wait for job (%s) to be killed' % (job_name,))
                self._wait.wait(0.1)
                if job.poll() == None:
                    self._wait.wait(3)
                if job.poll() == None:
                    try:
                        os.killpg(job.pid(), signal.SIGKILL)
                    except:
                        pass
                job.wait()
                output = job.communicate()
                self.error('job (%s) has been killed (STDOUT=%s /STDERR=%s)' % (job_name, output[0], output[1]))
            
        self._logger.info('stopped')
        
        
    def error(self, msg):
        self._backlogMain.incrementErrorCounter()
        self._logger.error(msg)
        
        
    def exception(self, msg):
        self._backlogMain.incrementExceptionCounter()
        self._logger.exception(msg)
        
        
    def _dutyModeDependentLogging(self, msg):
        if self._backlogMain.duty_cycle_mode:
            self._logger.info(msg)
        else:
            if self._logger.isEnabledFor(logging.DEBUG):
                self._logger.debug(msg)