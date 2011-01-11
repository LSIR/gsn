__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"


# as soon as the subprocess.Popen() bug has been fixed the functionality related
# to this variable should be removed
SUBPROCESS_BUG_BYPASS = True


import time
import string
import struct
import re
import os
import signal
import pickle
import shlex

if SUBPROCESS_BUG_BYPASS:
    from SubprocessFake import SubprocessFakeClass
else:
    import subprocess
    
import queue
import logging
from datetime import datetime, timedelta
from threading import Event, Lock, Thread

import BackLogMessage
import tos
from crontab import CronTab

############################################
# Some Constants
DEFAULT_BACKLOG = False

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

# Schedule file format
SCHEDULE_TYPE_PLUGIN = 'plugin'
SCHEDULE_TYPE_SCRIPT = 'script'
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
    _backlogMain
    _connectionEvent
    _scheduleEvent
    _scheduleLock
    _stopEvent
    _allJobsFinishedEvent
    _gsnconnected
    _schedule
    _newSchedule
    _duty_cycle_mode
    _max_next_schedule_wait_delta
    _max_job_runtime_min
    _pingThread
    _config
    _logger
    _scheduleHandlerStop
    _tosMessageLock
    _tosMessageAckEvent
    _tosSentCmd
    _tosNodeState
    '''
    
    def __init__(self, parent, dutycyclemode, options):
        Thread.__init__(self)
        self._logger = logging.getLogger(self.__class__.__name__)
        
        self._backlogMain = parent
        self._duty_cycle_mode = dutycyclemode
        self._config = options
        
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
        
        hard_shutdown_offset_minutes = int(self.getOptionValue('hard_shutdown_offset_minutes'))
        if hard_shutdown_offset_minutes is None:
            raise TypeError('hard_shutdown_offset_minutes not specified in config file')
        
        approximate_startup_seconds = int(self.getOptionValue('approximate_startup_seconds'))
        if approximate_startup_seconds is None:
            raise TypeError('approximate_startup_seconds not specified in config file')
        
        if dutycyclemode:
            self._backlogMain.registerTOSListener(self)
        
        self._connectionEvent = Event()
        self._scheduleEvent = Event()
        self._scheduleLock = Lock()
        self._stopEvent = Event()
        self._allJobsFinishedEvent = Event()
        self._allJobsFinishedEvent.set()
        self._tosMessageLock = Lock()
        self._tosMessageAckEvent = Event()
        self._tosSentCmd = None
        self._tosNodeState = None
        
        self._gsnconnected = False
        self._schedule = None
        self._newSchedule = False
        self._scheduleHandlerStop = False
        self._beacon = False
            
        self._max_next_schedule_wait_delta = timedelta(minutes=max_next_schedule_wait_minutes)
            
        if self._duty_cycle_mode:
            self._pingThread = PingThread(self, PING_INTERVAL_SEC, WATCHDOG_TIMEOUT_SEC)
        
        if os.path.isfile(self.getOptionValue('schedule_file')+'.parsed'):
            try:
                # Try to load the parsed schedule
                with open(self.getOptionValue('schedule_file')+'.parsed', 'rb') as f:
                    self._schedule = pickle.load(f)
            except Exception as e:
                self.exception(str(e))
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
        stop = False
        
        if self._duty_cycle_mode:
            self._pingThread.start()
            self.tosMsgSend(CMD_WAKEUP_QUERY)
            if self._scheduleHandlerStop:
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
            min += int(self.getOptionValue('hard_shutdown_offset_minutes'))
            sec = int(self.getOptionValue('approximate_startup_seconds'))
            maxruntime = self._backlogMain.jobsobserver.getOverallPluginMaxRuntime()
            if maxruntime and maxruntime != -1:
                sec += maxruntime
            td = timedelta(minutes=min, seconds=sec)
            nextschedule, error = self._schedule.getNextSchedules(datetime.utcnow() + td)
            if error:
                for e in error:
                    self.error('error while parsing the schedule file: ' + str(e))
            if nextschedule:
                nextdt, pluginclassname, commandstring, runtimemax = nextschedule[0]
                self._scheduleNextDutyWakeup(nextdt - datetime.utcnow(), pluginclassname + ' ' + commandstring)
            
        # wait some time for GSN to connect
        if self._backlogMain.gsnpeer.isConnected():
            self._gsnconnected = True
        else:
            self._logger.info('waiting for gsn to connect for a maximum of ' + self.getOptionValue('max_gsn_connect_wait_minutes') + ' minutes')
            self._connectionEvent.wait((int(self.getOptionValue('max_gsn_connect_wait_minutes')) * 60))
            self._connectionEvent.clear()
            if self._scheduleHandlerStop:
                self._logger.info('died')
                return
        
        # if GSN is connected try to get a new schedule for a while
        if self._gsnconnected:
            timeout = 0
            self._logger.info('waiting for gsn to answer a schedule request for a maximum of ' + self.getOptionValue('max_gsn_get_schedule_wait_minutes') + ' minutes')
            while timeout < (int(self.getOptionValue('max_gsn_get_schedule_wait_minutes')) * 60):
                self._logger.debug('request schedule from gsn')
                if self._schedule:
                    self._backlogMain.gsnpeer.processMsg(self.getMsgType(), self._schedule.getCreationTime(), struct.pack('<B', GSN_TYPE_GET_SCHEDULE), MESSAGE_PRIORITY, False)
                else:
                    self._backlogMain.gsnpeer.processMsg(self.getMsgType(), int(time.time()*1000), struct.pack('<B', GSN_TYPE_GET_SCHEDULE), MESSAGE_PRIORITY, False)
                self._scheduleEvent.wait(3)
                if self._scheduleHandlerStop:
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
        
        if self._duty_cycle_mode:
            lookback = True
        else:
            lookback = False
        service_time = timedelta()
        self._newSchedule = False
        self._stopEvent.clear()
        while not stop and not self._scheduleHandlerStop:
            dtnow = datetime.utcnow()
            
            nextschedules = None
            if self._schedule:
                # get the next schedule(s) in time
                self._scheduleLock.acquire()
                nextschedules, error = self._schedule.getNextSchedules(dtnow, lookback)
                for e in error:
                    self.error('error while parsing the schedule file: ' + str(e))
                self._scheduleEvent.clear()
                lookback = False
                self._scheduleLock.release()
                
            # if there is no schedule shutdown again and wait for next service window or wait for a schedule
            if not nextschedules:
                if self._duty_cycle_mode and not self._beacon:
                    self._logger.warning('no schedule or empty schedule available -> shutdown')
                    stop = self._shutdown()
                else:
                    self._logger.info('no schedule or empty schedule available -> waiting for a schedule')
                    self._scheduleEvent.clear()
                    self._scheduleEvent.wait()
                    continue
            
            for nextdt, pluginclassname, commandstring, runtimemax in nextschedules:
                self._logger.debug('(' + str(nextdt) + ',' + pluginclassname + ',' + commandstring  + ',' + str(runtimemax) + ')')
                dtnow = datetime.utcnow()
                timediff = nextdt - dtnow
                if self._duty_cycle_mode and not self._beacon:
                    service_time = self._serviceTime()
                    if nextdt <= dtnow:
                        self._logger.debug('executing >' + pluginclassname + ' ' + commandstring  + '< now')
                    elif timediff < self._max_next_schedule_wait_delta or timediff < service_time:
                        if pluginclassname:
                            if self._duty_cycle_mode:
                                self._logger.info('executing >' + pluginclassname + '.action("' + commandstring + '")< in ' + str(timediff.seconds + timediff.days * 86400 + timediff.microseconds/1000000.0) + ' seconds')
                            else:
                                self._logger.debug('executing >' + pluginclassname + '.action("' + commandstring + '")< in ' + str(timediff.seconds + timediff.days * 86400 + timediff.microseconds/1000000.0) + ' seconds')
                        else:
                            if self._duty_cycle_mode:
                                self._logger.info('executing >' + commandstring + '< in ' + str(timediff.seconds + timediff.days * 86400 + timediff.microseconds/1000000.0) + ' seconds')
                            else:
                                self._logger.debug('executing >' + commandstring + '< in ' + str(timediff.seconds + timediff.days * 86400 + timediff.microseconds/1000000.0) + ' seconds')
                        self._stopEvent.wait(timediff.seconds + timediff.days * 86400 + timediff.microseconds/1000000.0)
                        if self._scheduleHandlerStop:
                            break
                        if self._stopEvent.isSet():
                            self._stopEvent.clear()
                            break
                    else:
                        if service_time <= self._max_next_schedule_wait_delta:
                            self._logger.debug('nothing more to do in the next ' + self.getOptionValue('max_next_schedule_wait_minutes') + ' minutes (max_next_schedule_wait_minutes)')
                        else:
                            self._logger.debug('nothing more to do in the next ' + str(service_time.seconds/60.0 + service_time.days * 1440.0 + int(self.getOptionValue('max_next_schedule_wait_minutes'))) + ' minutes (rest of service time plus max_next_schedule_wait_minutes)')
                        
                        self.tosMsgSend(CMD_WAKEUP_QUERY)
                        if self._scheduleHandlerStop:
                            self._logger.info('died')
                            return
                        if not self._beacon:
                            stop = True
                        break
                else:
                    if nextdt > dtnow:
                        if pluginclassname:
                            self._logger.debug('executing >' + pluginclassname + '.action("' + commandstring + '")< in ' + str(timediff.seconds + timediff.days * 86400 + timediff.microseconds/1000000.0) + ' seconds')
                        else:
                            self._logger.debug('executing >' + commandstring + '< in ' + str(timediff.seconds + timediff.days * 86400 + timediff.microseconds/1000000.0) + ' seconds')
                        self._stopEvent.wait(timediff.seconds + timediff.days * 86400 + timediff.microseconds/1000000.0)
                        if (self._scheduleHandlerStop or self._newSchedule) or (self._duty_cycle_mode and not self._beacon):
                            self._newSchedule = False
                            self._stopEvent.clear()
                            break
                
                if pluginclassname:
                    if self._duty_cycle_mode:
                        self._logger.info('executing >' + pluginclassname + '.action("' + commandstring + '")< now')
                    else:
                        self._logger.debug('executing >' + pluginclassname + '.action("' + commandstring + '")< now')
                    try:
                        self._allJobsFinishedEvent.clear()
                        plugin = self._backlogMain.pluginAction(pluginclassname, commandstring, runtimemax)
                    except Exception as e:
                        self.error('error in scheduled plugin >' + pluginclassname + ' ' + commandstring + '<:' + str(e))
                else:
                    if self._duty_cycle_mode:
                        self._logger.info('executing >' + commandstring + '< now')
                    else:
                        self._logger.debug('executing >' + commandstring + '< now')
                    try:
                        if SUBPROCESS_BUG_BYPASS:
                            job = SubprocessFakeClass(commandstring)
                        else:
                            job = subprocess.Popen(shlex.split(commandstring), stdout=subprocess.PIPE, stderr=subprocess.PIPE, preexec_fn=os.setsid)
                    except Exception as e:
                        self.error('error in scheduled script >' + commandstring + '<:' + str(e))
                    else:
                        self._allJobsFinishedEvent.clear()
                        self._backlogMain.jobsobserver.observeJob(job, commandstring, False, runtimemax)
                    
            if stop and self._duty_cycle_mode and not self._scheduleHandlerStop and not self._beacon:
                stop = self._shutdown(service_time)
                    
            
        if self._duty_cycle_mode:
            self._pingThread.join()
                
        self._logger.info('died')
    
    
    def stop(self):
        self._scheduleHandlerStop = True
        self._connectionEvent.set()
        self._scheduleEvent.set()
        self._allJobsFinishedEvent.set()
        self._stopEvent.set()
        if self._duty_cycle_mode:
            self._backlogMain.deregisterTOSListener(self)
            self._pingThread.stop()
            
        self._logger.info('stopped')
        
        
    def allJobsFinished(self):
        self._logger.debug('all jobs finished')
        self._allJobsFinishedEvent.set()
    
    
    def getMsgType(self):
        return BackLogMessage.SCHEDULE_MESSAGE_TYPE


    def msgReceived(self, message):
        '''
        Try to interpret a new received Config-Message from GSN
        '''

        # Is the Message filled with content or is it just an emty response?
        pktType = struct.unpack_from('<B', message)[0]
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
            schedule = message[9:].decode('ascii')
            try:
                sc = ScheduleCron(creationtime, fake_tab=schedule)
                self._scheduleLock.acquire()   
                self._schedule = sc
                self._scheduleLock.release()
                    
                self._logger.info('updated internal schedule with the one received from GSN.')
               
                # Write schedule to disk (the plaintext one for debugging and the parsed one for better performance)
                schedule_file = open(self.getOptionValue('schedule_file'), 'w')
                schedule_file.write(schedule)
                schedule_file.close()
            
                with open(self.getOptionValue('schedule_file')+'.parsed', 'wb') as f:
                    pickle.dump(self._schedule, f)

                self._logger.info('updated %s and %s with the current schedule' % (schedule_file.name, schedule_file.name+".parsed"))
            except Exception as e:
                self.exception('received schedule can not be used: ' + str(e))
                if self._schedule:
                    self._logger.info('using locally stored schedule file')
                    
            self._newSchedule = True
            self._stopEvent.set()
            
        self._scheduleEvent.set()
            
            
    def tosMsgReceived(self, timestamp, payload):
        response = tos.Packet(TOS_CMD_STRUCTURE, payload['data'])
        self._logger.debug('rcv (cmd=' + str(response['command']) + ', argument=' + str(response['argument']) + ')')
        if response['command'] == CMD_WAKEUP_QUERY:
            node_state = response['argument']
            self._logger.debug('CMD_WAKEUP_QUERY response received with argument: ' + str(node_state))
            if node_state != self._tosNodeState:
                s = 'TinyNode wakeup states are: '
                if (node_state & WAKEUP_TYPE_SCHEDULED) == WAKEUP_TYPE_SCHEDULED:
                    s += 'SCHEDULE '
                if (node_state & WAKEUP_TYPE_SERVICE) == WAKEUP_TYPE_SERVICE:
                    s += 'SERVICE '
                if (node_state & WAKEUP_TYPE_BEACON) == WAKEUP_TYPE_BEACON:
                    self._beacon = True
                    s += 'BEACON '
                elif self._beacon:
                    self._beacon = False
                    self._scheduleEvent.set()
                    self._stopEvent.set()
                if (node_state & WAKEUP_TYPE_NODE_REBOOT) == WAKEUP_TYPE_NODE_REBOOT:
                    s += 'NODE_REBOOT'
                self._logger.info(s)
                self._tosNodeState = node_state
        elif response['command'] == CMD_SERVICE_WINDOW:
            self._logger.info('CMD_SERVICE_WINDOW response received with argument: ' + str(response['argument']))
        elif response['command'] == CMD_NEXT_WAKEUP:
            self._logger.info('CMD_NEXT_WAKEUP response received with argument: ' + str(response['argument']))
        elif response['command'] == CMD_SHUTDOWN:
            self._logger.info('CMD_SHUTDOWN response received with argument: ' + str(response['argument']))
        elif response['command'] == CMD_NET_STATUS:
            self._logger.info('CMD_NET_STATUS response received with argument: ' + str(response['argument']))
        elif response['command'] == CMD_RESET_WATCHDOG:
            self._logger.debug('CMD_RESET_WATCHDOG response received with argument: ' + str(response['argument']))
        else:
            self.error('unknown command type response received (' + str(response['command']) + ')')
        
              
        if response['command'] == self._tosSentCmd:
            self._logger.debug('TOS packet acknowledge received')
            self._tosMessageAckEvent.set()
        else:
            self.error('received TOS message type (' + str(response['command']) + ') does not match the sent command type (' + str(self._tosSentCmd) + ')')
        
        
    def tosMsgSend(self, cmd, argument=0):
        '''
        Send a command to the TinyNode
        
        @param cmd: the 1 Byte Command Code
        @param argument: the 4 byte argument for the command
        '''
        self._tosMessageLock.acquire()
        resendCounter = 1
        self._tosSentCmd = cmd
        while True:
            self._logger.debug('snd (cmd=' + str(cmd) + ', argument=' + str(argument) + ')')
            self._backlogMain._tospeer.sendTOSMsg(tos.Packet(TOS_CMD_STRUCTURE, [cmd, argument]), AM_CONTROL_CMD_MSG, 1)
            self._tosMessageAckEvent.wait(3)
            if self._scheduleHandlerStop:
                break
            elif self._tosMessageAckEvent.isSet():
                self._tosMessageAckEvent.clear()
                break
            else:
                if resendCounter == 5:
                    self.error('no answer for TOS command (' + str(self._tosSentCmd) + ') received from TOS node')
                    self._tosMessageLock.release()
                    return False
                self._logger.info('resend command (' + str(self._tosSentCmd) + ') to TOS node')
                resendCounter += 1
        self._tosMessageLock.release()
        return True
        
        
    def exception(self, exception):
        self._backlogMain.incrementExceptionCounter()
        self._logger.exception(exception)
        
        
    def error(self, msg):
        self._backlogMain.incrementErrorCounter()
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
            if self.tosMsgSend(CMD_NEXT_WAKEUP, time_to_wakeup):
                self._logger.info('successfully scheduled the next duty wakeup for >'+schedule_name+'< (that\'s in '+str(time_to_wakeup)+' seconds)')
            else:
                self.error('could not schedule the next duty wakeup for >'+schedule_name+'<')
            

    def _shutdown(self, sleepdelta=timedelta()):
        if self._duty_cycle_mode:
            now = datetime.utcnow()
            if now + sleepdelta > now:
                waitfor = sleepdelta.seconds + sleepdelta.days * 86400 + sleepdelta.microseconds/1000000.0
                self._logger.info('waiting ' + str(waitfor/60.0) + ' minutes for service windows to finish')
                self._stopEvent.wait(waitfor)
                if self._scheduleHandlerStop:
                    return True
                if self._scheduleEvent.isSet():
                    self._scheduleEvent.clear()
                    return False
            
            # wait for jobs to finish
            maxruntime = self._backlogMain.jobsobserver.getOverallPluginMaxRuntime()
            if not self._allJobsFinishedEvent.isSet() and maxruntime:
                if maxruntime != -1:
                    self._logger.info('waiting for all active jobs to finish for a maximum of ' + str(maxruntime) + ' seconds')
                    self._allJobsFinishedEvent.wait(5+maxruntime)
                else:
                    self._logger.info('waiting for all active jobs to finish indefinitely')
                    self._allJobsFinishedEvent.wait()
                if self._scheduleHandlerStop:
                    return True
                if self._scheduleEvent.isSet():
                    self._scheduleEvent.clear()
                    return False
                if not self._allJobsFinishedEvent.isSet():
                    self._backlogMain.incrementErrorCounter()
                    self.error('not all jobs have been killed (should not happen)')
                    
            # last possible moment to check if a beacon has been sent to the node
            # (if so, we do not want to shutdown)
            self._logger.info('get node wakeup states')
            self.tosMsgSend(CMD_WAKEUP_QUERY)
            if self._scheduleHandlerStop:
                return True
            if self._beacon:
                return False

            # Synchronize Service Wakeup Time
            time_delta = self._getNextServiceWindowRange()[0] - datetime.utcnow()
            time_to_service = time_delta.seconds + time_delta.days * 86400 - int(self.getOptionValue('approximate_startup_seconds'))
            if time_to_service < 0-int(self.getOptionValue('approximate_startup_seconds')):
                time_to_service += 86400
            self._logger.info('next service window is in ' + str(time_to_service/60.0) + ' minutes')
            if self.tosMsgSend(CMD_SERVICE_WINDOW, time_to_service):
                self._logger.info('successfully scheduled the next service window wakeup (that\'s in '+str(time_to_service)+' seconds)')
            else:
                self.error('could not schedule the next service window wakeup')
    
            # Schedule next duty wakeup
            if self._schedule:
                td = timedelta(seconds=int(self.getOptionValue('approximate_startup_seconds')))
                nextschedule, error = self._schedule.getNextSchedules(datetime.utcnow() + td)
                for e in error:
                    self.error('error while parsing the schedule file: ' + str(e))
                if nextschedule:
                    nextdt, pluginclassname, commandstring, runtimemax = nextschedule[0]
                    self._logger.info('schedule next duty wakeup')
                    self._scheduleNextDutyWakeup(nextdt - datetime.utcnow(), pluginclassname + ' ' + commandstring)
                    
            # last time to check if a new schedule has been sent from GSN
            if self._scheduleHandlerStop:
                return True
            if self._scheduleEvent.isSet():
                self._scheduleEvent.clear()
                return False
                    
            # Tell TinyNode to shut us down in X seconds
            self._pingThread.stop()
            shutdown_offset = int(self.getOptionValue('hard_shutdown_offset_minutes'))*60
            if self.tosMsgSend(CMD_SHUTDOWN, shutdown_offset):
                self._logger.info('we\'re going to do a hard shut down in '+str(shutdown_offset)+' seconds ...')
            else:
                self.error('could not communicate the hard shut down time with the TOS node')
    
            self._backlogMain.shutdown = True
            parentpid = os.getpid()
            self._logger.info('sending myself (pid=' + str(parentpid) + ') SIGINT')
            os.kill(parentpid, signal.SIGINT)
            return True
        else:
            self.error('shutdown called even if we are not in shutdown mode')
            return False
        
        
        
class PingThread(Thread):
    
    def __init__(self, parent, ping_interval_seconds=30, watchdog_timeout_seconds=300):
        Thread.__init__(self)
        self._logger = logging.getLogger(self.__class__.__name__)
        self._ping_interval_seconds = ping_interval_seconds
        self._watchdog_timeout_seconds = watchdog_timeout_seconds
        self._scheduleHandler = parent
        self._work = Event()
        self._pingThreadStop = False
        
        
    def run(self):
        self._logger.info('started')
        while not self._pingThreadStop:
            self._scheduleHandler.tosMsgSend(CMD_RESET_WATCHDOG, self._watchdog_timeout_seconds)
            self._logger.debug('reset watchdog')
            self._work.wait(self._ping_interval_seconds)
        self._logger.info('died')


    def stop(self):
        self._pingThreadStop = True
        self._work.set()
        self._logger.info('stopped')
        
        
        
            
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
        error = []
        for schedule in self.crons:
            runtimemax = None
            commandstring = str(schedule.command).strip()
            
            try:
                backwardmin, commandstring = self._getSpecialParameter(commandstring, BACKWARD_TOLERANCE_NAME)
                runtimemax, commandstring = self._getSpecialParameter(commandstring, MAX_RUNTIME_NAME)
            except TypeError as e:
                error.append(e)
            
            splited = commandstring.split(None, 1)
            type = splited[0]
            try:
                commandstring = splited[1]
            except IndexError:
                error.append('PLUGIN or SCRIPT definition is missing in the current schedule >' + str(schedule) + '<')
                continue
            pluginclassname = ''
            if type.lower() == SCHEDULE_TYPE_PLUGIN:
                splited = commandstring.split(None, 1)
                pluginclassname = splited[0]
                try:
                    commandstring = splited[1]
                except IndexError:
                    commandstring = ''
            elif type.lower() != SCHEDULE_TYPE_SCRIPT:
                error.append('PLUGIN or SCRIPT definition is missing in the current schedule >' + str(schedule) + '<')
                continue
            
            if look_backward and backwardmin:
                td = timedelta(minutes=backwardmin)
                nextdt = self._getNextSchedule(date_time - td, schedule)
                if nextdt < now:
                    backward_schedules.append((nextdt, pluginclassname, commandstring.strip(), runtimemax))
                
            nextdt = self._getNextSchedule(date_time, schedule)
            if not future_schedules or nextdt < future_schedules[0][0]:
                future_schedules = []
                future_schedules.append((nextdt, pluginclassname, commandstring.strip(), runtimemax))
            elif nextdt == future_schedules[0][0]:
                future_schedules.append((nextdt, pluginclassname, commandstring.strip(), runtimemax))
            
        return ((backward_schedules + future_schedules), error)


    def _getSpecialParameter(self, commandstring, param_name):
        param_start_index = commandstring.lower().find(param_name)
        if param_start_index == -1:
            return (None, commandstring)
        param_end_index = param_start_index+len(param_name)
        
        try:
            if commandstring[param_end_index] != '=':
                raise TypeError('wrongly formatted \'' + param_name + '\' parameter in the schedule file (format: ' + param_name + '=INTEGER)')
            else:
                param_end_index += 1
        except IndexError as e:
            raise TypeError('wrongly formatted \'' + param_name + '\' parameter in the schedule file (format: ' + param_name + '=INTEGER)')
            
        digit = ''
        while True:
            try:
                if commandstring[param_end_index] in string.digits:
                    digit += commandstring[param_end_index]
                    param_end_index += 1
                elif commandstring[param_end_index] in string.whitespace:
                    param_end_index += 1
                    break
                else:
                    raise TypeError('wrongly formatted \'' + param_name + '\' parameter in the schedule file (format: ' + param_name + '=INTEGER)')
            except IndexError:
                break
            
        if not digit:
            raise TypeError('wrongly formatted \'' + param_name + '\' parameter in the schedule file (format: ' + param_name + '=INTEGER)')
            
        commandstring = commandstring[:param_start_index].strip() + ' ' + commandstring[param_end_index:].strip()
        return (int(digit), commandstring)
        
    
    def _getNextSchedule(self, date_time, schedule):
        second = 0
        year = date_time.year
        date_time_month = datetime(date_time.year, date_time.month, 1)
        date_time_day = datetime(date_time.year, date_time.month, date_time.day)
        date_time_hour = datetime(date_time.year, date_time.month, date_time.day, date_time.hour)
        date_time_min = datetime(date_time.year, date_time.month, date_time.day, date_time.hour, date_time.minute)
        
        firsttimenottoday = True
        stop = False
        while not stop:
            for month in self._getRange(schedule.month()):
                if datetime(year, month, 1) >= date_time_month:
                    for day in self._getRange(schedule.dom()):
                        try:
                            nextdatetime = datetime(year, month, day)
                        except ValueError:
                            continue
                        if nextdatetime >= date_time_day:
                            
                            if nextdatetime.isoweekday() in self._getRange(schedule.dow()):
                                try:
                                    dt = datetime(date_time.year, date_time.month, date_time.day+1)
                                except ValueError:
                                    try:
                                        dt = datetime(date_time.year, date_time.month+1, 1)
                                    except ValueError:
                                        dt = datetime(date_time.year+1, 1, 1)
                                        
                                if nextdatetime < dt:
                                    for hour in self._getRange(schedule.hour()):
                                        if datetime(year, month, day, hour) >= date_time_hour:
                                            for minute in self._getRange(schedule.minute()):
                                                nextdatetime = datetime(year, month, day, hour, minute)
                                                if nextdatetime < date_time_min+timedelta(seconds=59):
                                                    continue
                                                else:
                                                    stop = True
                                                    break
                                        if stop:
                                            break
                                elif firsttimenottoday:
                                    minute = self._getFirst(schedule.minute())
                                    hour = self._getFirst(schedule.hour())
                                    firsttimenottoday = False
                                    stop = True
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
        except ValueError as e:
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
                result += list(range(part.value_from,part.value_to+1,int(part.seq)))
            else:
                result.append(part)
        return result
