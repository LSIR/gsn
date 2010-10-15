
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import time
import struct
import array
import re
import os
import signal
import pickle
import shlex
import subprocess
import Queue
import logging
from datetime import datetime, timedelta
from threading import Event, Lock, Thread

import BackLogMessage
import tos
from crontab import CronTab

############################################
# Some Constants
DEFAULT_BACKLOG = False

JOB_PROCESS_CHECK_INTERVAL_SECONDS = 10

SEND_QUEUE_SIZE = 25

MESSAGE_PRIORITY = 5

# The GSN packet types
GSN_TYPE_NO_SCHEDULE_AVAILABLE = 0
GSN_TYPE_SCHEDULE_SAME = 1
GSN_TYPE_NEW_SCHEDULE = 2
GSN_TYPE_GET_SCHEDULE = 3

# The AM Msg Type
AM_CONTROL_CMD_MSG = 0x21

# The Commands to send
CMD_WAKEUP_QUERY = 1
CMD_SERVICE_WINDOW = 2
CMD_NEXT_WAKEUP = 3
CMD_SHUTDOWN = 4
CMD_NET_STATUS = 5
CMD_RESET_WATCHDOG = 6

# The wakeup types
WAKEUP_TYPE_SCHEDULED = 1
WAKEUP_TYPE_SERVICE = 2
WAKEUP_TYPE_BEACON = 4
WAKEUP_TYPE_NODE_REBOOT = 8

# ping and watchdog timing
PING_INTERVAL_SEC = 30
WATCHDOG_TIMEOUT_SEC = 300

# Command Structure
TOS_CMD_STRUCTURE = [('command', 'int', 1), ('argument', 'int', 4)]

# Backward looking time naming
BACKWARD_TOLERANCE_NAME = 'backward_tolerance_minutes'
MAX_RUNTIME_NAME = 'max_runtime_minutes'
############################################


class ScheduleHandlerClass(Thread):
    '''
    The ScheduleHandler offers the functionality to schedule different
    jobs (bash scripts, programs, etc.) on the deployment system in a
    well defined interval. The schedule is formated in a crontab-like
    manner and can be defined and altered on side of GSN as needed using
    the virtual sensors web input. A new schedule will be directly
    transmitted to the deployment if a connection exists or will be
    requested as soon as a connection opens.
    
    This handler can be run in duty-cycle mode or not. If the duty-cycle mode
    is enabled the plugin controls the duty-cycling of the deployment system.
    Scheduled jobs in a configurable time interval will be executed in a
    controlled environment. After their execution the system will be shutdown
    and taken from the power supply. A TinyNode running BBv2PowerControl
    offers the functionality (timers, startup/shutdown commands, etc.) to
    manage the power supply of the deployment system. Thus, job scheduling
    combined with duty-cycling can be used to minimize the energy consumption
    of the system. If the duty-cycle mode is disabled the ScheduleHandler only
    schedules jobs without offering any duty-cycling functionality.


    data/instance attributes:
    _connectionEvent
    _scheduleEvent
    _scheduleLock
    _stopEvent
    _allJobsFinishedEvent
    _jobsObserver
    _gsnconnected
    _schedulereceived
    _schedule
    _newSchedule
    _duty_cycle_mode
    _max_next_schedule_wait_delta
    _max_job_runtime_sec
    _pingThread
    _config
    _logger
    _tosMessageHandler
    _stopped
    '''
    
    def __init__(self, parent, dutycyclemode, options):
        Thread.__init__(self)
        self._logger = logging.getLogger(self.__class__.__name__)
        
        self._parent = parent
        self._duty_cycle_mode = dutycyclemode
        self._config = options
        
        if dutycyclemode and not self._parent.tospeer:
            raise TypeError('TOSPeerClass is needed if in duty-cycle mode but has not been started')
        
        service_wakeup_minutes = int(self.getOptionValue('service_wakeup_minutes'))
        if service_wakeup_minutes is None:
            raise TypeError('service_wakeup_minutes not specified in config file')
        
        max_gsn_connect_wait_minutes = int(self.getOptionValue('max_gsn_connect_wait_minutes'))
        if max_gsn_connect_wait_minutes is None:
            raise TypeError('max_gsn_connect_wait_minutes not specified in config file')
        
        max_gsn_get_schedule_wait_minutes = int(self.getOptionValue('max_gsn_get_schedule_wait_minutes'))
        if max_gsn_get_schedule_wait_minutes is None:
            raise TypeError('max_gsn_get_schedule_wait_minutes not specified in config file')
        
        max_next_schedule_wait_minutes = int(self.getOptionValue('max_next_schedule_wait_minutes'))
        if max_next_schedule_wait_minutes is None:
            raise TypeError('max_next_schedule_wait_minutes not specified in config file')
        
        max_plugins_finish_wait_minutes = int(self.getOptionValue('max_plugins_finish_wait_minutes'))
        if max_plugins_finish_wait_minutes is None:
            raise TypeError('max_plugins_finish_wait_minutes not specified in config file')
        
        max_default_job_runtime_minutes = int(self.getOptionValue('max_default_job_runtime_minutes'))
        if max_default_job_runtime_minutes is None:
            raise TypeError('max_default_job_runtime_minutes not specified in config file')
        
        hard_shutdown_offset_minutes = int(self.getOptionValue('hard_shutdown_offset_minutes'))
        if hard_shutdown_offset_minutes is None:
            raise TypeError('hard_shutdown_offset_minutes not specified in config file')
        
        approximate_startup_seconds = int(self.getOptionValue('approximate_startup_seconds'))
        if approximate_startup_seconds is None:
            raise TypeError('approximate_startup_seconds not specified in config file')
        
        self._connectionEvent = Event()
        self._scheduleEvent = Event()
        self._scheduleLock = Lock()
        self._stopEvent = Event()
        self._allJobsFinishedEvent = Event()
        self._allJobsFinishedEvent.set()
        
        self._jobsObserver = JobsObserver(self, max_default_job_runtime_minutes)
        self._gsnconnected = False
        self._schedulereceived = False
        self._schedule = None
        self._newSchedule = False
        self._stopped = False
            
        self._max_next_schedule_wait_delta = timedelta(minutes=max_next_schedule_wait_minutes)
            
        self._tosMessageHandler = None
        if self._duty_cycle_mode:
            self._tosMessageHandler = TOSMessageHandler(self)
            self._pingThread = PingThread(self, PING_INTERVAL_SEC, WATCHDOG_TIMEOUT_SEC)
        
        if os.path.isfile(self.getOptionValue('schedule_file')+'.parsed'):
            try:
                # Try to load the parsed schedule
                parsed_schedule_file = open(self.getOptionValue('schedule_file')+'.parsed', 'r')
                self._schedule = pickle.load(parsed_schedule_file)
                parsed_schedule_file.close()
            except Exception, e:
                self.error(e.__str__())
        else:
            self._logger.info('there is no local schedule file available')
        

    def getOptionValue(self, key):
        for entry in self._config:
            entry_key = entry[0]
            entry_value = entry[1]
            if key == entry_key:
                return entry_value
        return None
    
    
    def connectionToGSNestablished(self):
        self._logger.debug('connection established')
        self._gsnconnected = True
        self._connectionEvent.set()
        
        
    def run(self):
        self._logger.info('started')
        
        self._jobsObserver.start()
        
        if self._duty_cycle_mode:
            self._tosMessageHandler.start()
            self._pingThread.start()
            self._tosMessageHandler.addMsg(CMD_WAKEUP_QUERY, blocking=True)
            if self._stopped:
                self._logger.info('died')
                return
                
          
        if self._schedule and self._duty_cycle_mode:  
            # Schedule duty wakeup after this session, for safety reasons.
            # (The scheduled time here could be in this session if schedules are following
            # each other in short intervals. In this case it could be possible, that
            # we have to wait for the next service window in case of an unexpected shutdown.)
            min = int(self.getOptionValue('max_gsn_connect_wait_minutes'))
            min += int(self.getOptionValue('max_gsn_get_schedule_wait_minutes'))
            min += int(self.getOptionValue('max_next_schedule_wait_minutes'))
            min += int(self.getOptionValue('max_plugins_finish_wait_minutes'))
            min += int(self.getOptionValue('max_default_job_runtime_minutes'))
            min += int(self.getOptionValue('hard_shutdown_offset_minutes'))
            td = timedelta(minutes=min)
            safety_schedule = self._schedule.getNextSchedules(datetime.utcnow() + td)[0]
            self._scheduleNextDutyWakeup(safety_schedule[0] - datetime.utcnow(), safety_schedule[1])
            
        # wait some time for GSN to connect
        if self._parent.gsnpeer.isConnected():
            self._gsnconnected = True
        else:
            self._logger.info('waiting for gsn to connect for a maximum of ' + self.getOptionValue('max_gsn_connect_wait_minutes') + ' minutes')
            self._connectionEvent.wait((int(self.getOptionValue('max_gsn_connect_wait_minutes')) * 60))
            self._connectionEvent.clear()
            if self._stopped:
                self._logger.info('died')
                return
        
        # if GSN is connected try to get a new schedule for a while
        if self._gsnconnected:
            timeout = 0
            self._logger.info('waiting for gsn to answer a schedule request for a maximum of ' + self.getOptionValue('max_gsn_get_schedule_wait_minutes') + ' minutes')
            while timeout < (int(self.getOptionValue('max_gsn_get_schedule_wait_minutes')) * 60):
                self._logger.info('request schedule from gsn')
                if self._schedule:
                    self._parent.gsnpeer.processMsg(self.getMsgType(), self._schedule.getCreationTime(), struct.pack('<B', GSN_TYPE_GET_SCHEDULE), MESSAGE_PRIORITY, False)
                else:
                    self._parent.gsnpeer.processMsg(self.getMsgType(), int(time.time()*1000), struct.pack('<B', GSN_TYPE_GET_SCHEDULE), MESSAGE_PRIORITY, False)
                self._scheduleEvent.wait(3)
                if self._stopped:
                    self._logger.info('died')
                    return
                if self._scheduleEvent.isSet():
                    self._scheduleEvent.clear()
                    break
                timeout += 3
            
            if timeout >= int(self.getOptionValue('max_gsn_get_schedule_wait_minutes')) * 60:
                self._logger.warning('gsn has not answered on any schedule request')
        else:
            self._logger.warning('gsn has not connected')
        
        # if there is no schedule at all shutdown again and wait for next service window  
        if not self._schedule:
            if self._duty_cycle_mode:
                self._logger.warning('no schedule available at all -> shutdown')
                self._shutdown()
                self._logger.info('died')
                return
            else:
                self._logger.info('no schedule available yet -> waiting for a schedule')
                self._scheduleEvent.wait()
        
        if self._duty_cycle_mode:
            lookback = True
        else:
            lookback = False
        stop = False
        service_time = timedelta()
        while not stop and not self._stopped:
            self._max_job_runtime_sec = int(self.getOptionValue('max_default_job_runtime_minutes'))*60
            dtnow = datetime.utcnow() 
            # get the next schedule(s) in time
            if not self._duty_cycle_mode:
                self._scheduleLock.acquire()
            t = time.time()
            nextschedules = self._schedule.getNextSchedules(dtnow, lookback)
            self._logger.debug('next schedule: %f s' % (time.time() - t))
            lookback = False
            if not self._duty_cycle_mode:
                self._scheduleLock.release()
            
            for schedule in nextschedules:
                self._logger.debug('(' + str(schedule[0]) + ',' + str(schedule[1]) + ')')
                dtnow = datetime.utcnow()
                timediff = schedule[0] - dtnow
                if self._duty_cycle_mode:
                    service_time = self._serviceTime()
                    if schedule[0] <= dtnow:
                        self._logger.debug('start >' + schedule[1] + '< now')
                    elif timediff < self._max_next_schedule_wait_delta or timediff < service_time:
                        self._logger.debug('start >' + schedule[1] + '< in ' + str(timediff.seconds + timediff.days * 86400 + timediff.microseconds/1000000.0) + ' seconds')
                        self._stopEvent.wait(timediff.seconds + timediff.days * 86400 + timediff.microseconds/1000000.0)
                        if self._stopped:
                            break
                    else:
                        if service_time <= self._max_next_schedule_wait_delta:
                            self._logger.debug('nothing more to do in the next ' + self.getOptionValue('max_next_schedule_wait_minutes') + ' minutes (max_next_schedule_wait_minutes)')
                        else:
                            self._logger.debug('nothing more to do in the next ' + str(service_time.seconds/60.0 + service_time.days * 1440.0 + int(self.getOptionValue('max_next_schedule_wait_minutes'))) + ' minutes (rest of service time plus max_next_schedule_wait_minutes)')
                        
                        self._tosMessageHandler.addMsg(CMD_WAKEUP_QUERY, blocking=True)
                        if self._stopped:
                            self._logger.info('died')
                            return
                        if self._duty_cycle_mode:
                            stop = True
                            break
                else:
                    if schedule[0] > dtnow:
                        self._logger.debug('start >' + schedule[1] + '< in ' + str(timediff.seconds + timediff.days * 86400 + timediff.microseconds/1000000.0) + ' seconds')
                        self._stopEvent.wait(timediff.seconds + timediff.days * 86400 + timediff.microseconds/1000000.0)
                        if self._stopped or self._newSchedule:
                            self._newSchedule = False
                            self._stopEvent.clear()
                            break
                
                try:
                    if self._duty_cycle_mode:
                        self._logger.info('executing >' + schedule[1] + '< now')
                    proc = subprocess.Popen(shlex.split(schedule[1]), stdout=subprocess.PIPE, stderr=subprocess.PIPE)
                except Exception, e:
                    self.error('error in scheduled job >' + schedule[1] + '<:' + str(e))
                else:
                    self._allJobsFinishedEvent.clear()
                    if schedule[2] > self._max_job_runtime_sec:
                        self._max_job_runtime_sec = schedule[2]
                    self._jobsObserver.observeJob(proc, schedule[1], schedule[2])
                    
        if self._duty_cycle_mode and not self._stopped:
            self._shutdown(service_time)
            
        if self._duty_cycle_mode:
            self._pingThread.join()
            self._tosMessageHandler.join()
        self._jobsObserver.join()
                
        self._logger.info('died')
    
    
    def stop(self):
        self._stopped = True
        self._jobsObserver.stop()
        self._connectionEvent.set()
        self._scheduleEvent.set()
        self._allJobsFinishedEvent.set()
        self._stopEvent.set()
        if self._duty_cycle_mode:
            self._pingThread.stop()
            self._tosMessageHandler.stop()
            
        self._logger.info('stopped')
        
        
    def allJobsFinished(self):
        self._logger.debug('all jobs finished')
        self._allJobsFinishedEvent.set()
    
    
    def beaconReceived(self):
        self._duty_cycle_mode = False
    
    
    def getMsgType(self):
        return BackLogMessage.SCHEDULE_MESSAGE_TYPE


    def msgReceived(self, message):
        '''
        Try to interpret a new received Config-Message from GSN
        '''

        if not self._schedulereceived or not self._duty_cycle_mode:
            # Is the Message filled with content or is it just an emty response?
            pktType = struct.unpack('B', message[0])[0]
            if pktType == GSN_TYPE_NO_SCHEDULE_AVAILABLE:
                self._logger.info('GSN has no schedule available')
            elif pktType == GSN_TYPE_SCHEDULE_SAME:
                self._logger.info('no new schedule from GSN')
            elif pktType == GSN_TYPE_NEW_SCHEDULE:
                self._logger.info('new schedule from GSN received')
                # Get the schedule creation time
                creationtime = struct.unpack('<q', message[1:9])[0]
                self._logger.debug('creation time: ' + str(creationtime))
                # Get the schedule
                schedule = message[9:]
                try:
                    sc = ScheduleCron(creationtime, fake_tab=schedule)
                    if not self._duty_cycle_mode:
                        self._scheduleLock.acquire()   
                    self._schedule = sc
                    if not self._duty_cycle_mode:
                        self._scheduleLock.release()
                    
                    self._logger.info('updated internal schedule with the one received from GSN.')
                   
                    # Write schedule to disk (the plaintext one for debugging and the parsed one for better performance)
                    schedule_file = open(self.getOptionValue('schedule_file'), 'w')
                    schedule_file.write(schedule)
                    schedule_file.close()
                
                    compiled_schedule_file = open(self.getOptionValue('schedule_file')+'.parsed', 'w')
                    pickle.dump(self._schedule, compiled_schedule_file)
                    compiled_schedule_file.close()
    
                    self._logger.info('updated %s and %s with the current schedule' % (schedule_file.name, schedule_file.name+".parsed")) 
                except Exception, e:
                    self.error('received schedule can not be used: ' + str(e))
                    if self._schedule:
                        self._logger.info('using locally stored schedule file')
    
            if self._schedulereceived and not self._duty_cycle_mode:
                if not self._schedule:
                    return
                else:
                    self._newSchedule = True
                    self._stopEvent.set()
            
            self._schedulereceived = True
            self._scheduleEvent.set()
        else:
            self._logger.info('schedule already received')
            
            
    def tosMsgReceived(self, timestamp, payload):
        if self._tosMessageHandler:
            self._tosMessageHandler.received(payload)
        
        
    def error(self, msg):
        self._parent.incrementErrorCounter()
        self._logger.error(msg)
        
        
    def _serviceTime(self):
        now = datetime.utcnow()
        start, end = self._getNextServiceWindowRange()
        if start < (now + self._max_next_schedule_wait_delta):
            return end - now
        else:
            return timedelta()
    
    
    def _getNextServiceWindowRange(self):
        wakeup_minutes = timedelta(minutes=int(self.getOptionValue('service_wakeup_minutes')))
        hour, minute = self.getOptionValue('service_wakeup_schedule').split(':')
        now = datetime.utcnow()
        service_time = datetime(now.year, now.month, now.day, int(hour), int(minute))
        if (service_time + wakeup_minutes) < now:
            twentyfourhours = timedelta(hours=24)
            return (service_time + twentyfourhours, service_time + twentyfourhours + wakeup_minutes)
        else:
            return (service_time, service_time + wakeup_minutes)
        
        
    def _scheduleNextDutyWakeup(self, time_delta, schedule_name):
        if self._duty_cycle_mode:
            time_to_wakeup = time_delta.seconds + time_delta.days * 86400 - int(self.getOptionValue('approximate_startup_seconds'))
            self._tosMessageHandler.addMsg(CMD_NEXT_WAKEUP, argument=time_to_wakeup)
            self._logger.info('successfully scheduled the next duty wakeup for '+schedule_name+' (that\'s in '+str(time_to_wakeup)+' seconds)')
            

    def _shutdown(self, sleepdelta=timedelta()):
        if self._duty_cycle_mode:
            now = datetime.utcnow()
            if now + sleepdelta > now:
                waitfor = sleepdelta.seconds + sleepdelta.days * 86400 + sleepdelta.microseconds/1000000.0
                self._logger.info('waiting ' + str(waitfor/60.0) + ' minutes for service windows to finish')
                self._stopEvent.wait(waitfor)
                if self._stopped:
                    return
            
            # wait for jobs to finish
            if not self._allJobsFinishedEvent.isSet():
                self._logger.info('waiting for all active jobs to finish for a maximum of ' + str(self._max_job_runtime_sec/60.0) + ' minutes')
                self._allJobsFinishedEvent.wait(1+self._max_job_runtime_sec)
                if self._stopped:
                    return
                if not self._allJobsFinishedEvent.isSet():
                    self.error('not all jobs have been killed (should not happen)')
                    
            # wait for all plugins to finish
            max_plugins_finish_wait_seconds = int(self.getOptionValue('max_plugins_finish_wait_minutes'))*60
            self._logger.info('waiting for all active plugins to finish for a maximum of ' + self.getOptionValue('max_plugins_finish_wait_minutes') + ' minutes')
            while not max_plugins_finish_wait_seconds <= 0:
                if not self._parent.pluginsBusy():
                    break
                else:
                    self._logger.debug('plugins are still busy')
                    self._stopEvent.wait(3)
                    if self._stopped:
                        return
                    max_plugins_finish_wait_seconds -= 3
                    
            # last possible moment to check if a beacon has been sent to the node
            # (if so, we do not want to shutdown)
            self._logger.info('get node wakeup states')
            self._tosMessageHandler.addMsg(CMD_WAKEUP_QUERY, blocking=True)
            if self._stopped:
                return

            # Synchronize Service Wakeup Time
            time_delta = self._getNextServiceWindowRange()[0] - datetime.utcnow()
            time_to_service = time_delta.seconds + time_delta.days * 86400 - int(self.getOptionValue('approximate_startup_seconds'))
            self._logger.info('next service window is in ' + str(time_to_service/60.0) + ' minutes')
            self._tosMessageHandler.addMsg(CMD_SERVICE_WINDOW, argument=time_to_service)
            self._logger.info('successfully scheduled the next service window wakeup (that\'s in '+str(time_to_service)+' seconds)')
    
            # Schedule next duty wakeup
            if self._schedule:
                next_schedule = self._schedule.getNextSchedules(datetime.utcnow())[0]
                self._logger.info('schedule next duty wakeup')
                self._scheduleNextDutyWakeup(next_schedule[0] - datetime.utcnow(), next_schedule[1])
                    
            # Tell TinyNode to shut us down in X seconds
            self._pingThread.stop()
            shutdown_offset = int(self.getOptionValue('hard_shutdown_offset_minutes'))*60
            self._tosMessageHandler.addMsg(CMD_SHUTDOWN, argument=shutdown_offset, blocking=True)
            self._logger.info('we\'re going to do a hard shut down in '+str(shutdown_offset)+' seconds ...')
    
            self._parent.shutdown = True
            parentpid = os.getpid()
            self._logger.info('sending myself (pid=' + str(parentpid) + ' SIGINT')
            os.kill(parentpid, signal.SIGINT)
        else:
            self._logger.warning('shutdown called even if we are not in shutdown mode')
            
            
            
class TOSMessageHandler(Thread):
    
    def __init__(self, scheduleHandler):
        Thread.__init__(self)
        self._scheduleHandler = scheduleHandler
        self._stopped = False
        self._node_state = None
        
        self._ackEvent = Event()
        self._work = Event()
        self._sendqueue = Queue.Queue(SEND_QUEUE_SIZE)
        
        
    def run(self):
        self._scheduleHandler._logger.info('TOSMessageHandler: started')
        
        while not self._stopped:
            self._work.wait()
            if self._stopped:
                break
            self._work.clear()
            # is there something to do?
            while not self._sendqueue.empty() and not self._stopped:
                try:
                    item = self._sendqueue.get_nowait()
                except Queue.Empty:
                    self._logger.warning('send queue is empty')
                    break
                
                if item[1]:
                    self._sentCmd = item[0]['command']

                self._scheduleHandler._logger.debug('snd...')
                self._scheduleHandler._parent.tospeer.sendTOSMsg(item[0], AM_CONTROL_CMD_MSG)
                
                if item[1]:
                    while True:
                        self._ackEvent.wait(3)
                        if self._stopped:
                            self._scheduleHandler._logger.info('TOSMessageHandler: died')
                            return
                        elif not self._ackEvent.isSet():
                            self._scheduleHandler._logger.info('resend command (' + str(self._sentCmd) + ') to TOS node')
                            self._scheduleHandler._parent.tospeer.sendTOSMsg(item[0], AM_CONTROL_CMD_MSG)
                        else:
                            self._sentCmd = None
                            self._ackEvent.clear()
                            break
            
                self._sendqueue.task_done()

        self._scheduleHandler._logger.info('TOSMessageHandler: died')
            
            
    def received(self, msg):
        response = tos.Packet(TOS_CMD_STRUCTURE, msg['data'])
        if response['command'] == CMD_WAKEUP_QUERY:
            node_state = response['argument']
            self._scheduleHandler._logger.debug('CMD_WAKEUP_QUERY response received with argument: ' + str(node_state))
            if node_state != self._node_state:
                s = 'TinyNode wakeup states are: '
                if (node_state & WAKEUP_TYPE_SCHEDULED) == WAKEUP_TYPE_SCHEDULED:
                    s += 'SCHEDULE '
                if (node_state & WAKEUP_TYPE_SERVICE) == WAKEUP_TYPE_SERVICE:
                    s += 'SERVICE '
                if (node_state & WAKEUP_TYPE_BEACON) == WAKEUP_TYPE_BEACON:
                    self._scheduleHandler.beaconReceived()
                    s += 'BEACON '
                if (node_state & WAKEUP_TYPE_NODE_REBOOT) == WAKEUP_TYPE_NODE_REBOOT:
                    s += 'NODE_REBOOT'
                self._scheduleHandler._logger.info(s)
                self._node_state = node_state
        elif response['command'] == CMD_SERVICE_WINDOW:
            self._scheduleHandler._logger.info('CMD_SERVICE_WINDOW response received with argument: ' + str(response['argument']))
        elif response['command'] == CMD_NEXT_WAKEUP:
            self._scheduleHandler._logger.info('CMD_NEXT_WAKEUP response received with argument: ' + str(response['argument']))
        elif response['command'] == CMD_SHUTDOWN:
            self._scheduleHandler._logger.info('CMD_SHUTDOWN response received with argument: ' + str(response['argument']))
        elif response['command'] == CMD_NET_STATUS:
            self._scheduleHandler._logger.info('CMD_NET_STATUS response received with argument: ' + str(response['argument']))
        elif response['command'] == CMD_RESET_WATCHDOG:
            self._scheduleHandler._logger.info('CMD_RESET_WATCHDOG response received with argument: ' + str(response['argument']))
        
              
        if response['command'] == self._sentCmd:
            self._scheduleHandler._logger.debug('packet acknowledge received')
            self._ackEvent.set()
        
        
    def addMsg(self, cmd, argument=0, blocking=False):
        '''
        Send a command to the TinyNode
        
        @param cmd: the 1 Byte Command Code
        @param argument: the 4 byte argument for the command
        '''
        if not self._stopped:
            try:
                self._sendqueue.put_nowait((tos.Packet(TOS_CMD_STRUCTURE, [cmd, argument]), blocking))
            except Queue.Full:
                self._scheduleHandler.error('send queue is full')
                return False
            self._work.set()
            if blocking:
                self._ackEvent.wait()
            return True
        return False
    
    
    def stop(self):
        self._stopped = True
        self._work.set()
        self._ackEvent.set()
        self._scheduleHandler._logger.info('TOSMessageHandler: stopped')
         
        
        
class JobsObserver(Thread):
    
    def __init__(self, parent, default_max_runtime_minute):
        Thread.__init__(self)
        
        self._parent = parent
        self._default_max_runtime_seconds = default_max_runtime_minute * 60.0
        self._lock = Lock()
        self._process_list = []
        self._work = Event()
        self._wait = Event()
        self._stopped = False
        
        
    def run(self):
        self._parent._logger.info('JobsObserver: started')
        while not self._stopped:
            self._work.wait()
            if self._stopped:
                break
            self._work.clear()
            
            while not self._stopped:
                new_list = []
                for proc in self._process_list:
                    ret = proc[0].poll()
                    if ret == None:
                        pid = proc[0].pid
                        if proc[1] <= JOB_PROCESS_CHECK_INTERVAL_SECONDS:
                            if pid <= 1:
                                self._parent.error('wanted to kill PID ' + str(pid))
                            else:
                                proc[0].kill()
                                self._parent._logger.warning('wait for job (' + proc[2] + ') to be killed')
                                proc[0].wait()
                                output = proc[0].communicate()
                                self._parent.error('job (' + proc[2] + ') with PID ' + str(pid) + ' has not finished in time  (STDOUT=' + str(output[0]) + ' /STDERR=' + str(output[1]) + ')')
                        else:
                            self._parent._logger.debug('job (' + proc[2] + ') with PID ' + str(pid) + ' not yet finished -> ' + str(proc[1]-JOB_PROCESS_CHECK_INTERVAL_SECONDS) + ' more seconds to run')
                            new_list.append((proc[0], proc[1]-JOB_PROCESS_CHECK_INTERVAL_SECONDS, proc[2]))
                    else:
                        output = proc[0].communicate()
                        if ret == 0:
                            if self._parent._duty_cycle_mode:
                                self._parent._logger.info('job (' + proc[2] + ') finished successfully (STDOUT=' + str(output[0]) + ' /STDERR=' + str(output[1]) + ')')
                            else:
                                self._parent._logger.debug('job (' + proc[2] + ') finished successfully (STDOUT=' + str(output[0]) + ' /STDERR=' + str(output[1]) + ')')
                        else:
                            self._parent.error('job (' + proc[2] + ') finished with return code ' + str(ret) + ' (STDOUT=' + str(output[0]) + ' /STDERR=' + str(output[1]) + ')')
                
                self._lock.acquire()
                self._process_list = new_list
                self._lock.release()
                if not self._process_list:
                    self._parent.allJobsFinished()
                    break
                
                self._wait.wait(JOB_PROCESS_CHECK_INTERVAL_SECONDS)
 
        self._parent._logger.info('JobsObserver: died')
        
        
        
    def observeJob(self, process, job_name, max_runtime_minutes=None):
        if not self._stopped:
            if not max_runtime_minutes:
                max_runtime_seconds = self._default_max_runtime_seconds
            else:
                max_runtime_seconds = max_runtime_minutes * 60
                
            self._lock.acquire()
            self._process_list.append((process, max_runtime_seconds, job_name))
            self._lock.release()
            self._parent._logger.debug('new job (' + job_name + ') added with a maximum runtime of ' + str(max_runtime_seconds/60) + ' minutes')
            
            self._work.set()
            return True
        else:
            return False


    def stop(self):
        self._stopped = True
        self._work.set()
        self._wait.set()
        
        for proc in self._process_list:
            self._parent.error('job (' + proc[2] + ') with PID ' + str(proc[0].pid) + ' has not finished yet -> kill it')
            proc[0].kill()
            self._parent._logger.warning('wait for job (' + proc[2] + ') to be killed')
            proc[0].wait()
            output = proc[0].communicate()
            self._parent.error('job (' + proc[2] + ') has been killed (STDOUT=' + str(output[0]) + ' /STDERR=' + str(output[1]) + ')')
            
        self._parent._logger.info('JobsObserver: stopped')
        
        
        
class PingThread(Thread):
    
    def __init__(self, parent, ping_interval_seconds=30, watchdog_timeout_seconds=300):
        Thread.__init__(self)
        self._ping_interval_seconds = ping_interval_seconds
        self._watchdog_timeout_seconds = watchdog_timeout_seconds
        self._parent = parent
        self._work = Event()
        self._stopped = False
        
        
    def run(self):
        self._parent._logger.info('PingThread: started')
        while not self._stopped:
            self._parent._tosMessageHandler.addMsg(CMD_RESET_WATCHDOG, self._watchdog_timeout_seconds)
            self._parent._logger.debug('reset watchdog')
            self._work.wait(self._ping_interval_seconds)
        self._parent._logger.info('PingThread: died')


    def stop(self):
        self._stopped = True
        self._work.set()
        self._parent._logger.info('PingThread: stopped')
        
        
        
            
class ScheduleCron(CronTab):
    
    def __init__(self, creation_time, user=None, fake_tab=None):
        CronTab.__init__(self, user, fake_tab)
        self._creation_time = creation_time
        for schedule in self.crons:
            self._scheduleSanityCheck(schedule)
            
            
    def getCreationTime(self):
        return self._creation_time
        
    
    def getNextSchedules(self, date_time, look_backward=False):
        future_schedules = []
        backward_schedules = []
        now = datetime.utcnow()
        for schedule in self.crons:
            runtimemin = None
            commandstring = str(schedule.command).strip()
            back_index = commandstring.lower().find(BACKWARD_TOLERANCE_NAME)
            run_index = commandstring.lower().find(MAX_RUNTIME_NAME)
            td = timedelta()
            
            if back_index != -1 and run_index != -1:
                if back_index < run_index:
                    runtimemin = int(commandstring[run_index:].strip().replace(MAX_RUNTIME_NAME+'=',''))
                    if look_backward:
                        backwardmin = int(commandstring[back_index:run_index].strip().replace(BACKWARD_TOLERANCE_NAME+'=',''))
                        td = timedelta(minutes=backwardmin)
                        nextdt = self._getNextSchedule(date_time - td, schedule)
                        if nextdt < now:
                            backward_schedules.append((nextdt, commandstring.strip(), runtimemin))
                        
                    commandstring = commandstring[:back_index].strip()
                else:      
                    runtimemin = int(commandstring[run_index:back_index].strip().replace(MAX_RUNTIME_NAME+'=',''))
                    if look_backward:
                        backwardmin = int(commandstring[back_index:].strip().replace(BACKWARD_TOLERANCE_NAME+'=',''))
                        td = timedelta(minutes=backwardmin)
                        nextdt = self._getNextSchedule(date_time - td, schedule)
                        if nextdt < now:
                            backward_schedules.append((nextdt, commandstring.strip(), runtimemin))
                        
                    commandstring = commandstring[:run_index].strip()
            elif back_index != -1:
                if look_backward:
                    backwardmin = int(commandstring[back_index:].strip().replace(BACKWARD_TOLERANCE_NAME+'=',''))
                    td = timedelta(minutes=backwardmin)
                    nextdt = self._getNextSchedule(date_time - td, schedule)
                    if nextdt < now:
                        backward_schedules.append((nextdt, commandstring.strip(), runtimemin))
                    
                commandstring = commandstring[:back_index].strip()
            elif run_index != -1:
                runtimemin = int(commandstring[run_index:].strip().replace(MAX_RUNTIME_NAME+'=',''))
                commandstring = commandstring[:run_index].strip()
                
            nextdt = self._getNextSchedule(date_time, schedule)
            if not future_schedules or nextdt < future_schedules[0][0]:
                future_schedules = []
                future_schedules.append((nextdt, commandstring, runtimemin))
            elif nextdt == future_schedules[0][0]:
                future_schedules.append((nextdt, commandstring, runtimemin))
            
        return backward_schedules + future_schedules
    
    
    def _getNextSchedule(self, date_time, schedule):
        second = 0
        year = date_time.year
            
        firsttimenottoday = True
        stop = False
        while not stop:
            for month in self._getRange(schedule.month()):
                for day in self._getRange(schedule.dom()):
                    for weekday in self._getRange(schedule.dow()):
                        try:
                            nextdatetime = datetime(year, month, day)
                        except ValueError:
                            continue
                        
                        wd = weekday
                        if wd == 0:
                            wd = 7
                        
                        if nextdatetime < datetime(date_time.year, date_time.month, date_time.day) or wd != nextdatetime.isoweekday():
                            continue
                        
                        try:
                            dt = datetime(date_time.year, date_time.month, date_time.day+1)
                        except ValueError:
                            try:
                                dt = datetime(date_time.year, date_time.month+1, 1)
                            except ValueError:
                                dt = datetime(date_time.year+1, 1, 1)
                        if nextdatetime < dt:
                            for hour in self._getRange(schedule.hour()):
                                for minute in self._getRange(schedule.minute()):
                                    nextdatetime = datetime(year, month, day, hour, minute)
                                    if nextdatetime < date_time:
                                        continue
                                    stop = True
                                    break
                                if stop:
                                    break
                        elif firsttimenottoday:
                            minute = self._getFirst(schedule.minute())
                            hour = self._getFirst(schedule.hour())
                            firsttimenottoday = False
                            stop = True
                        break
                    if stop:
                        break
                if stop:
                    break
            if stop:
                break
            else:          
                year += 1
        
        return datetime(year, month, day, hour, minute)
    
    
    def _scheduleSanityCheck(self, schedule):
        try:
            self._scheduleSanityCheckHelper(schedule.minute(), 0, 59)
            self._scheduleSanityCheckHelper(schedule.hour(), 0, 23)
            self._scheduleSanityCheckHelper(schedule.dow(), 0, 7)
            self._scheduleSanityCheckHelper(schedule.month(), 1, 12)
            self._scheduleSanityCheckHelper(schedule.dom(), 1, 31)
        except ValueError, e:
            raise ValueError(str(e) + ' in >' + str(schedule) + '<')
        
        
    def _scheduleSanityCheckHelper(self, cronslice, min, max):
        for part in cronslice.parts:
            if str(part).find("/") > 0 or str(part).find("-") > 0 or str(part).find('*') > -1:
                if part.value_to > max or part.value_from < min:
                    raise ValueError('Invalid value ' + str(part))
            else:
                if part > max or part < min:
                    raise ValueError('Invalid value ' + str(part))
        
    
    
    def _getFirst(self, cronslice):
        smallestPart = None
        for part in cronslice.parts:
            if str(part).find("/") > 0 or str(part).find("-") > 0 or str(part).find('*') > -1:
                smallestPart = part.value_from
            else:
                if not smallestPart or part < smallestPart:
                    smallestPart = part
        return smallestPart
    
    
    def _getRange(self, cronslice):
        result = []
        for part in cronslice.parts:
            if str(part).find("/") > 0 or str(part).find("-") > 0 or str(part).find('*') > -1:
                result += range(part.value_from,part.value_to+1,int(part.seq))
            else:
                result.append(part)
        return result
