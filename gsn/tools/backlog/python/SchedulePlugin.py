'''
Created on Mar 23, 2010

@author: Tonio Gsell
'''

import time
import struct
import re
import os
import pickle
from datetime import datetime
from threading import Event

import BackLogMessage
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
    ############################################
    '''
    This plugin offers the functionality to 
    '''

    '''
    data/instance attributes:
    '''
    
    def __init__(self, parent, options):
        AbstractPluginClass.__init__(self, parent, options, DEFAULT_BACKLOG)
        
        self._work = Event()
        
        self._gsnconnected = False
        self._schedulereceived = False
        self._schedule = None
        self._stopped = False
        
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
    
    
    def getMsgType(self):
        return BackLogMessage.SCHEDULE_MESSAGE_TYPE
    
    
    def connectionToGSNestablished(self):
        self.debug('connection established')
        self._gsnconnected = True
        self._work.set()
       
        
    def run(self):
        self.info('started')

        # wait some time for GSN to connect
        if self.isGSNConnected():
            self._gsnconnected = True
        else:
            self.info('waiting for gsn to connect')
            self._work.wait(int(self.getOptionValue('gsn_connect_timeout')))
            self._work.clear()
            if self._stopped:
                self.info('died')
                return
        
        # if GSN is connected try to get a new schedule for a while
        if self._gsnconnected:
            timeout = 0
            while timeout < int(self.getOptionValue('gsn_schedule_timeout')):
                self.info('request schedule from gsn')
                self.processMsg(self.getTimeStamp(), struct.pack('<B', self.GSN_TYPE_GET_SCHEDULE))
                self._work.wait(3)
                if self._stopped:
                    self.info('died')
                    return
                if self._work.isSet():
                    break
                timeout += 3
            
            if timeout >= int(self.getOptionValue('gsn_schedule_timeout')):
                self.warning('gsn has not answered on any schedule request')
        else:
            self.warning('gsn has not connected')
        
        # if there is no schedule at all shutdown again and wait for next service window  
        if not self._schedule:
            self.warning('no schedule available at all -> shutdown')
            self._shutdown()
            self.info('died')
            return
        
        
        t = time.time()
        nextschedules = self._schedule.getNextSchedules(datetime.utcnow())
        self.debug('next schedules: %f s' % (time.time() - t))
        for schedule in nextschedules:
            self.debug('\t(' + str(schedule[0]) + ',' + str(schedule[1]) + ',' + str(schedule[2]) + ')')
                
        self.info('died')
    
    
    def stop(self):
        self._stopped = True
        self._work.set()
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
            self._work.set()
        else:
            self.info('schedule already received')
        


    def _shutdown(self):
        # TODO: next wakeup and shutdown
        pass
            
        
        
            
class ScheduleCron(CronTab):
    
    def __init__(self, user=None, fake_tab=None):
        CronTab.__init__(self, user, fake_tab)
        for schedule in self.crons:
            self._scheduleSanityCheck(schedule)
        
    
    def getNextSchedules(self, dt):
        schedules = []
        count = 1
        for schedule in self.crons:
            wd = dt.isoweekday()
            if wd == 7:
                wd = 0
            
            nxt = self._getNextSchedule(dt, schedule)
            if not schedules or nxt < schedules[0][0]:
                schedules = []
                schedules.append((nxt, count, schedule.command))
            elif nxt == schedules[0][0]:
                schedules.append((nxt, count, schedule.command))
            count += 1
        return schedules
    
    
    def _getNextSchedule(self, dt, schedule):
        second = 0
        year = dt.year
            
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
                        
                        if nextdatetime < datetime(dt.year, dt.month, dt.day) or wd != nextdatetime.isoweekday():
                            continue
                        
                        if nextdatetime < datetime(dt.year, dt.month, dt.day+1):
                            for hour in self._getRange(schedule.hour()):
                                for minute in self._getRange(schedule.minute()):
                                    nextdatetime = datetime(year, month, day, hour, minute)
                                    if nextdatetime < dt:
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