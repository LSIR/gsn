
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

from ScheduleHandler import SUBPROCESS_BUG_BYPASS

JOB_PROCESS_CHECK_INTERVAL_SECONDS = 2
        
        
class JobsObserverClass(Thread):
    
    def __init__(self, parent):
        Thread.__init__(self)
        self._logger = logging.getLogger(self.__class__.__name__)
        
        self._backlogMain = parent
        self._lock = Lock()
        self._jobList = []
        self._work = Event()
        self._wait = Event()
        self._waitforjob = Event()
        self._jobsObserverStop = False
        
        
    def run(self):
        self._logger.info('started')
        while not self._jobsObserverStop:
            self._work.wait()
            if self._jobsObserverStop:
                break
            self._work.clear()
            
            while not self._jobsObserverStop:
                self._wait.wait(JOB_PROCESS_CHECK_INTERVAL_SECONDS)
                if self._jobsObserverStop:
                    break
                
                for index, joblistentry in enumerate(self._jobList):
                    isPlugin, job, job_name, runtime_end = joblistentry
                    if isPlugin:
                        if job.isBusy():
                            if runtime_end < datetime.utcnow():
                                st = self._backlogMain.pluginStop(job_name)
                                if st:
                                    self._logger.warning('plugin (' + job_name + ') has not finished in time -> stop it')
                                del self._jobList[index]
                            else:
                                self._logger.debug('plugin (' + job_name + ') has not yet finished -> ' + str(runtime_end-datetime.utcnow()) + ' time to run')
                        else:
                            st = self._backlogMain.pluginStop(job_name)
                            if st:
                                if self._backlogMain.duty_cycle_mode:
                                    self._logger.info('plugin (' + job_name + ') finished successfully')
                                else:
                                    self._logger.debug('plugin (' + job_name + ') finished successfully')
                            del self._jobList[index]
                    else:
                        ret = job.poll()
                        if ret == None:
                            if runtime_end != -1:
                                if runtime_end < datetime.utcnow():
                                    if SUBPROCESS_BUG_BYPASS:
                                        self.error('job (' + job_name + ') has not finished in time -> kill it')
                                        job.kill()
                                        self._logger.warning('wait for job (' + job_name + ') to be killed')
                                    else:
                                        self.error('job (' + job_name + ') with PID ' + str(job.pid) + ' has not finished in time -> kill it')
                                        os.killpg(job.pid, signal.SIGTERM)
                                        self._logger.warning('wait for job (' + job_name + ') to be killed')
                                        self._wait.wait(0.1)
                                        if not job.poll():
                                            self._wait.wait(3)
                                        if self._wait.isSet():
                                            break
                                        if not job.poll():
                                            os.killpg(job.pid, signal.SIGKILL)
                                        job.wait()
                                    stdoutdata, stderrdata = job.communicate()
                                    self._logger.warning('job (' + job_name + ') has been killed (STDOUT=' + stdoutdata.decode() + ' /STDERR=' + stderrdata.decode() + ')')
                                    del self._jobList[index]
                                else:
                                    if SUBPROCESS_BUG_BYPASS:
                                        self._logger.debug('job (' + job_name + ') not yet finished -> ' + str(runtime_end-datetime.utcnow()) + ' time to run')
                                    else:
                                        self._logger.debug('job (' + job_name + ') with PID ' + str(job.pid) + ' not yet finished -> ' + str(runtime_end-datetime.utcnow()) + ' time to run')
                        else:
                            stdoutdata, stderrdata = job.communicate()
                            if ret == 0:
                                if self._backlogMain.duty_cycle_mode:
                                    self._logger.info('job (' + job_name + ') finished successfully (STDOUT=' + stdoutdata.decode() + ' /STDERR=' + stderrdata.decode() + ')')
                                else:
                                    self._logger.debug('job (' + job_name + ') finished successfully (STDOUT=' + stdoutdata.decode() + ' /STDERR=' + stderrdata.decode() + ')')
                            else:
                                self.error('job (' + job_name + ') finished with return code ' + str(ret) + ' (STDOUT=' + stdoutdata.decode() + ' /STDERR=' + stderrdata.decode() + ')')
                            del self._jobList[index]
                
                self._lock.acquire()
                if not self._jobList:
                    self._work.clear()
                    self._backlogMain.schedulehandler.allJobsFinished()
                    self._lock.release()
                    break
                else:
                    self._lock.release()
 
        self._logger.info('died')
        
        
        
    def observeJob(self, job, job_name, isPlugin, max_runtime_minutes):
        if (max_runtime_minutes or not isPlugin) and not self._jobsObserverStop:
            self._backlogMain.schedulehandler.newJobStarted()
            self._lock.acquire()
            if isPlugin:
                jobexists = False
                for index, joblistentry in enumerate(self._jobList):
                    if job_name == joblistentry[2]:
                        self._jobList[index] = (isPlugin, job, job_name, datetime.utcnow()+timedelta(minutes=max_runtime_minutes))
                        jobexists = True
                        self._logger.debug('job (' + job_name + ') updated with the maximum runtime of ' + str(max_runtime_minutes) + ' minutes')
                        break
                if not jobexists:
                    self._jobList.append((isPlugin, job, job_name, datetime.utcnow()+timedelta(minutes=max_runtime_minutes)))
                    self._logger.debug('new job (' + job_name + ') added with a maximum runtime of ' + str(max_runtime_minutes) + ' minutes')
            else:
                if max_runtime_minutes:
                    self._jobList.append((isPlugin, job, job_name, datetime.utcnow()+timedelta(minutes=max_runtime_minutes)))
                else:
                    self._jobList.append((isPlugin, job, job_name, -1))
                self._logger.debug('new job (' + job_name + ') added with a maximum runtime of ' + str(max_runtime_minutes) + ' minutes')
            self._lock.release()
            self._work.set()
            
            
    def getOverallJobsMaxRuntimeSec(self):
        if not self._jobList:
            return None
        overallMaxRuntime = timedelta()
        for isPlugin, job, job_name, runtime_end in self._jobList:
            if runtime_end == -1:
                return runtime_end
            else:
                runtime_left = runtime_end-datetime.utcnow()
                if  overallMaxRuntime < runtime_left:
                    overallMaxRuntime = runtime_left
        return overallMaxRuntime.seconds + overallMaxRuntime.days * 86400 + overallMaxRuntime.microseconds/1000000.0


    def stop(self):
        self._jobsObserverStop = True
        self._work.set()
        self._wait.set()
        
        for isPlugin, job, job_name, runtime_end in self._jobList:
            if isPlugin:
                self._backlogMain.pluginStop(job_name, True)
            else:
                if SUBPROCESS_BUG_BYPASS:
                    self.error('job (' + job_name + ') has not finished yet -> kill it')
                    job.kill()
                    self._logger.warning('wait for job (' + job_name + ') to be killed')
                else:
                    self.error('job (' + job_name + ') with PID ' + str(job.pid) + ' has not finished yet -> kill it')
                    os.killpg(job.pid, signal.SIGTERM)
                    self._logger.warning('wait for job (' + job_name + ') to be killed')
                    self._wait.wait(0.1)
                    if not job.poll():
                        self._wait.wait(3)
                    if not job.poll():
                        os.killpg(job.pid, signal.SIGKILL)
                    job.wait()
                output = job.communicate()
                self.error('job (' + job_name + ') has been killed (STDOUT=' + str(output[0]) + ' /STDERR=' + str(output[1]) + ')')
            
        self._logger.info('stopped')
        
        
    def error(self, msg):
        self._backlogMain.incrementErrorCounter()
        self._logger.error(msg)