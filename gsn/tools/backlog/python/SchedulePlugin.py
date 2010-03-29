'''
Created on Mar 23, 2010

@author: Tonio Gsell
'''

import time
import struct
import re
import os
import pickle
import shlex
import subprocess
from datetime import datetime, timedelta
from threading import Event

import BackLogMessage
#import tos
from crontab import CronTab
from AbstractPlugin import AbstractPluginClass

DEFAULT_BACKLOG = False


class SchedulePluginClass(AbstractPluginClass):
    '''
    This plugin handles the schedules.
    '''
    ############################################
    # Some Constants
    
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

    # The wakeup types
    WAKEUP_TYPE_SCHEDULED = 0
    WAKEUP_TYPE_SERVICE = 1
    WAKEUP_TYPE_BEACON = 2
    WAKEUP_TYPE_NODE_REBOOT = 3

    # Command Structure
    TOS_CMD_STRUCTURE = [('command', 'int', 1), ('argument', 'int', 4)]
    
    # Backward looking time naming
    BACKWARD_LOOK_NAME = 'backward_look'
    ############################################
    '''
    This plugin offers the functionality to 
    '''

    '''
    data/instance attributes:
    '''
    
    def __init__(self, parent, options):
        AbstractPluginClass.__init__(self, parent, options, DEFAULT_BACKLOG)
        
        self._connectionEvent = Event()
        self._scheduleEvent = Event()
        self._stopEvent = Event()
        
        self._gsnconnected = False
        self._schedulereceived = False
        self._schedule = None
        self._stopped = False
        
        address = self.getOptionValue('tos_source_addr')
        
        if address is None:
            raise TypeError('tos_source_addr not specified in config file')

        # Initialize the serial source to the TinyNode
        
        # split the address (it should have the form serial@port:baudrate)
#        source = address.split('@')
#        if source[0] == 'serial':
#            try:
#                # try to open a connection to the specified serial port
#                serial = tos.getSource(address, debug)
#                self._serialsource = tos.AM(serial)
#            except Exception, e:
#                raise TypeError('could not initialize serial source: ' + e.__str__())
#        else:
#            raise TypeError('address type must be serial')
        
        if os.path.isfile(self.getOptionValue('schedule_file')+'.parsed'):
            try:
                # Try to load the parsed schedule
                parsed_schedule_file = open(self.getOptionValue('schedule_file')+'.parsed', 'r')
                self._schedule = pickle.load(parsed_schedule_file)
                parsed_schedule_file.close()
            except Exception, e:
                self.error(e.__str__())
        else:
            self.info('there is no local schedule file available')
            
        self._max_next_schedule_wait_delta = timedelta(minutes=int(self.getOptionValue('max_next_schedule_wait_minutes')))
    
    
    def getMsgType(self):
        return BackLogMessage.SCHEDULE_MESSAGE_TYPE
    
    
    def connectionToGSNestablished(self):
        self.debug('connection established')
        self._gsnconnected = True
        self._connectionEvent.set()
       
        
    def run(self):
        self.info('started')

        # wait some time for GSN to connect
        if self.isGSNConnected():
            self._gsnconnected = True
        else:
            self.info('waiting for gsn to connect')
            self._connectionEvent.wait((int(self.getOptionValue('max_gsn_connect_wait_minutes')) * 60))
            self._connectionEvent.clear()
            if self._stopped:
                self.info('died')
                return
        
        # if GSN is connected try to get a new schedule for a while
        if self._gsnconnected:
            timeout = 0
            while timeout < (int(self.getOptionValue('max_gsn_get_schedule_wait_minutes')) * 60):
                self.info('request schedule from gsn')
                self.processMsg(self.getTimeStamp(), struct.pack('<B', self.GSN_TYPE_GET_SCHEDULE))
                self._scheduleEvent.wait(3)
                if self._stopped:
                    self.info('died')
                    return
                if self._scheduleEvent.isSet():
                    break
                timeout += 3
            
            if timeout >= int(self.getOptionValue('max_gsn_get_schedule_wait_minutes')):
                self.warning('gsn has not answered on any schedule request')
        else:
            self.warning('gsn has not connected')
        
        # if there is no schedule at all shutdown again and wait for next service window  
        if not self._schedule:
            self.warning('no schedule available at all -> shutdown')
            self._shutdown()
            self.info('died')
            return
        
        dtnow = None
        firstloop = True
        stop = False
        while not stop:
            if dtnow and dtnow.second == datetime.utcnow().second:
                dtnow = datetime.utcnow()
                dtnow += timedelta(seconds=1)
            else:
                dtnow = datetime.utcnow()
            
            # get the next schedule(s) in time
            t = time.time()
            nextschedules = self._schedule.getNextSchedules(dtnow, firstloop)
            self.debug('next schedule: %f s' % (time.time() - t))
            firstloop = False
            
            for schedule in nextschedules:
                self.debug('(' + str(schedule[0]) + ',' + str(schedule[1]) + ')')
                dtnow = datetime.utcnow()
                timediff = schedule[0] - dtnow
                args = shlex.split(schedule[1])
                if schedule[0] <= dtnow:
                    self.debug('start >' + schedule[1] + '< now')
                elif timediff < self._max_next_schedule_wait_delta:
                    self.debug('start >' + schedule[1] + '< in ' + str(timediff.seconds + timediff.days * 86400) + ' seconds')
                    self._stopEvent.wait(timediff.seconds + timediff.days * 86400)
                    if self._stopped:
                        break
                else:
                    self.debug('nothing more to do in the next ' + self.getOptionValue('max_next_schedule_wait_minutes') + ' minutes (max_next_schedule_wait_minutes)')
                    stop = True
                    break
                
                try:
                    proc = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
                except Exception, e:
                    self.error('error in schedule script >' + schedule[1] + '<:' + str(e))
                else:
                    out = proc.communicate()
                    if out[0]:
                        self.info(out[0])
                    if out[1]:
                        self.error(out[1])
                    ret = proc.poll()
                    if ret != 0:
                        self.error('return code from schedule script >' + schedule[1] + '< is ' + str(ret))
                    
        self._shutdown()
                
        self.info('died')
    
    
    def stop(self):
        self._stopped = True
        self._connectionEvent.set()
        self._scheduleEvent.set()
        self._stopEvent.set()
        self._serialsource.stop()
        self.info('stopped')


    def msgReceived(self, message):
        '''
        Try to interpret a new received Config-Message from GSN
        '''

        if not self._schedulereceived:
            # Is the Message filled with content or is it just an emty response?
            pktType = struct.unpack('B', message[0])[0]
            if pktType == self.GSN_TYPE_NO_SCHEDULE_AVAILABLE:
                self.info('GSN has no schedule available')
            elif pktType == self.GSN_TYPE_SCHEDULE_SAME:
                self.info('no new schedule from GSN')
            elif pktType == self.GSN_TYPE_NEW_SCHEDULE:
                self.info('new schedule from GSN received')
                # Get the schedule creation time
                creationtime = struct.unpack('<q', message[1:9])[0]
                self.debug('creation time: ' + str(creationtime))
                # Get the schedule
                schedule = message[9:]
                try:
                    self._schedule = ScheduleCron(fake_tab=schedule)
                    
                    self.info('updated internal schedule with the one received from GSN.')
                   
                    # Write schedule to disk (the plaintext one for debugging and the parsed one for better performance)
                    schedule_file = open(self.getOptionValue('schedule_file'), 'w')
                    schedule_file.write(schedule)
                    schedule_file.close()
                
                    compiled_schedule_file = open(self.getOptionValue('schedule_file')+'.parsed', 'w')
                    pickle.dump(self._schedule, compiled_schedule_file)
                    compiled_schedule_file.close()
    
                    self.info('updated %s and %s with the current schedule' % (schedule_file.name, schedule_file.name+".parsed")) 
                except Exception, e:
                    self.error('received schedule can not be used: ' + str(e))
                    if self._schedule:
                        self.info('using locally stored schedule file')
    
            self._schedulereceived = True
            self._scheduleEvent.set()
        else:
            self.info('schedule already received')


    def _getCmdResponse(self, cmd, argument=0):
        '''
        Send a command to the TinyNode and return the response's argument
        
        @param cmd: the 1 Byte Command Code
        @param argument: the 4 byte argument for the command

        @return the response's argument on success and -1 on fail
        '''
        packet = tosPlugPacket.tos.Packet(self.TOS_CMD_STRUCTURE, [cmd, argument])

        resp_packet = None
        while not resp_packet:
            self._serialsource.write(packet, self.AM_CONTROL_CMD_MSG)
            resp_packet = self._serialsource.read(1)

        response = tos.Packet(self.TOS_CMD_STRUCTURE, resp_packet['data'])
        if response['command'] == cmd:
            return response['argument']
        else:
            return -1
        


    def _shutdown(self):
        now = datetime.utcnow()
        sc = ScheduleCron(fake_tab=(self.getOptionValue('service_wakeup_schedule') + ' ' + self.BACKWARD_LOOK_NAME + '=' + self.getOptionValue('service_wakeup_minutes')))
        next_service_wakeup = sc.getNextSchedules(now, True)[0][0]
        if next_service_wakeup < now + self._max_next_schedule_wait_delta:
            td = (next_service_wakeup + timedelta(minutes=int(self.getOptionValue('service_wakeup_minutes')))) - now
            waitfor = td.seconds + td.days * 86400 + 1
            self.info('waiting ' + str(waitfor/60.0) + ' minutes for service windows to finish')
            self._stopEvent.wait(waitfor)
            now = datetime.utcnow()
            next_service_wakeup = sc.getNextSchedules(now)[0]
            #TODO: next schedule could be soon...
        
        # Syncronize Service Wakeup Time
        time_delta = next_service_wakeup - now
        time_to_service = time_delta.seconds + time_delta.days * 86400
        self.debug('next service window is in ' + str(time_to_service/60.0) + ' minutes')
        if self._getCmdResponse(self.CMD_SERVICE_WINDOW, time_to_service) == time_to_service:
            self.info('ReverseModePlugin: Successfully scheduled the next service window wakeup (that\'s in '+str(time_to_service)+' minutes)')

        # Get Wakeup Type
        self._wakeup_reason = self._getCmdResponse(self.CMD_WAKEUP_QUERY)
        self.info("ReverseModePlugin: TinyNode woke me up for reason code "+str(self._wakeup_reason))
            
        
        
            
class ScheduleCron(CronTab):
    
    def __init__(self, user=None, fake_tab=None):
        CronTab.__init__(self, user, fake_tab)
        for schedule in self.crons:
            self._scheduleSanityCheck(schedule)
        
    
    def getNextSchedules(self, date_time, look_backward=False):
        future_schedules = []
        backward_schedules = []
        now = datetime.utcnow()
        for schedule in self.crons:
            commandstring = str(schedule.command)
            index = commandstring.lower().find(SchedulePluginClass.BACKWARD_LOOK_NAME)
            td = timedelta()
            if index != -1:
                if look_backward:
                    backwardmin = int(commandstring[index:].replace(SchedulePluginClass.BACKWARD_LOOK_NAME+'=',''))
                    td = timedelta(minutes=backwardmin)
                    nextdt = self._getNextSchedule(date_time - td, schedule)
                    if nextdt < now:
                        backward_schedules.append((nextdt, commandstring))
                    
                commandstring = commandstring[:index]
                
            nextdt = self._getNextSchedule(date_time, schedule)
            if not future_schedules or nextdt < future_schedules[0][0]:
                future_schedules = []
                future_schedules.append((nextdt, commandstring))
            elif nextdt == future_schedules[0][0]:
                future_schedules.append((nextdt, commandstring))
            
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
                        
                        if nextdatetime < datetime(date_time.year, date_time.month, date_time.day+1):
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
        
    
    
#    def _getNext(self, time, cronslice):
#        closestPart = None
#        for part in cronslice.parts:
#            if str(part).find("/") > 0 or str(part).find("-") > 0 or str(part).find('*') > -1:
#                for t in range(part.value_from,part.value_to+1,int(part.seq)):
#                    if t >= time and (not closestPart or t < closestPart):
#                        closestPart = t
#            else:
#                if part >= time and (not closestPart or part < closestPart):
#                    closestPart = part
#        
#        if not closestPart:
#            return (self._getFirst(cronslice), True)
#        else:
#            return (closestPart, False)
    
    
    def _getRange(self, cronslice):
        result = []
        for part in cronslice.parts:
            if str(part).find("/") > 0 or str(part).find("-") > 0 or str(part).find('*') > -1:
                result += range(part.value_from,part.value_to+1,int(part.seq))
            else:
                result.append(part)
        return result