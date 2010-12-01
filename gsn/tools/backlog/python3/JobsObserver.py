
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision: 2381 $"
__date__        = "$Date: 2010-11-15 14:51:38 +0100 (Mon, 15. Nov 2010) $"
__id__          = "$Id: AbstractPlugin.py 2381 2010-11-15 13:51:38Z tgsell $"
__source__      = "$URL: https://gsn.svn.sourceforge.net/svnroot/gsn/branches/permasense/gsn/tools/backlog/python/AbstractPlugin.py $"


import time
import logging
from threading import Event, Lock, Thread


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
                
                for index, job in enumerate(self._jobList):
                    if job[0]:
                        if job[1].isBusy():
                            if job[3] <= JOB_PROCESS_CHECK_INTERVAL_SECONDS:
                                self._logger.warning('plugin (' + job[2] + ') has not finished in time -> stop it')
                                self._backlogMain.pluginStop(job[2])
                                del self._jobList[index]
                            else:
                                self._logger.debug('plugin (' + job[2] + ') has not yet finished -> ' + str(job[3]-JOB_PROCESS_CHECK_INTERVAL_SECONDS) + ' more seconds to run')
                                self._jobList[index][3] = job[3]-JOB_PROCESS_CHECK_INTERVAL_SECONDS
                        else:
                            if self._backlogMain.duty_cycle_mode:
                                self._logger.info('plugin (' + job[2] + ') finished successfully')
                            else:
                                self._logger.debug('plugin (' + job[2] + ') finished successfully')
                            self._backlogMain.pluginStop(job[2])
                            del self._jobList[index]
                    else:
                        ret = job[1].poll()
                        if ret == None:
                            pid = job[1].pid
                            if job[3] <= JOB_PROCESS_CHECK_INTERVAL_SECONDS:
                                if pid <= 1:
                                    self.error('wanted to kill PID ' + str(pid))
                                else:
                                    job[1].kill()
                                    self._logger.warning('wait for job (' + job[2] + ') to be killed')
                                    job[1].wait()
                                    output = job[1].communicate()
                                    self.error('job (' + job[2] + ') with PID ' + str(pid) + ' has not finished in time  (STDOUT=' + str(output[0]) + ' /STDERR=' + str(output[1]) + ')')
                                    del self._jobList[index]
                            else:
                                self._logger.debug('job (' + job[2] + ') with PID ' + str(pid) + ' not yet finished -> ' + str(job[3]-JOB_PROCESS_CHECK_INTERVAL_SECONDS) + ' more seconds to run')
                                self._jobList[index][3] = job[3]-JOB_PROCESS_CHECK_INTERVAL_SECONDS
                        else:
                            output = job[1].communicate()
                            if ret == 0:
                                if self._backlogMain.duty_cycle_mode:
                                    self._logger.info('job (' + job[2] + ') finished successfully (STDOUT=' + str(output[0]) + ' /STDERR=' + str(output[1]) + ')')
                                else:
                                    self._logger.debug('job (' + job[2] + ') finished successfully (STDOUT=' + str(output[0]) + ' /STDERR=' + str(output[1]) + ')')
                            else:
                                self.error('job (' + job[2] + ') finished with return code ' + str(ret) + ' (STDOUT=' + str(output[0]) + ' /STDERR=' + str(output[1]) + ')')
                            del self._jobList[index]
                
                self._lock.acquire()
                if not self._jobList:
                    self._work.clear()
                    self._lock.release()
                    self._backlogMain.schedulehandler.allJobsFinished()
                    break
                self._lock.release()
 
        self._logger.info('died')
        
        
        
    def observeJob(self, job, job_name, isPlugin, max_runtime_minutes):
        if max_runtime_minutes and not self._jobsObserverStop:
            self._lock.acquire()
            if isPlugin:
                jobexists = False
                for index, jobfromlist in enumerate(self._jobList):
                    if job_name == jobfromlist[2]:
                        self._jobList[index] = [isPlugin, job, job_name, max_runtime_minutes * 60]
                        jobexists = True
                        self._logger.debug('job (' + job_name + ') updated with the maximum runtime of ' + str(max_runtime_minutes) + ' minutes')
                        break
                if not jobexists:
                    self._jobList.append([isPlugin, job, job_name, max_runtime_minutes * 60])
                    self._logger.debug('new job (' + job_name + ') added with a maximum runtime of ' + str(max_runtime_minutes) + ' minutes')
            else:
                self._jobList.append([isPlugin, job, job_name, max_runtime_minutes * 60])
                self._logger.debug('new job (' + job_name + ') added with a maximum runtime of ' + str(max_runtime_minutes) + ' minutes')
            self._lock.release()
            self._work.set()


    def stop(self):
        self._jobsObserverStop = True
        self._work.set()
        self._wait.set()
        
        for job in self._jobList:
            if job[0]:
                self._backlogMain.pluginStop(job[2])
            else:
                self.error('job (' + job[2] + ') with PID ' + str(job[1].pid) + ' has not finished yet -> kill it')
                job[1].kill()
                self._logger.warning('wait for job (' + job[2] + ') to be killed')
                job[1].wait()
                output = job[1].communicate()
                self.error('job (' + job[2] + ') has been killed (STDOUT=' + str(output[0]) + ' /STDERR=' + str(output[1]) + ')')
            
        self._logger.info('stopped')
        
        
    def error(self, msg):
        self._backlogMain.incrementErrorCounter()
        self._logger.error(msg)