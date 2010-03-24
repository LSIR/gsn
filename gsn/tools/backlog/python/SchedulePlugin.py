'''
Created on Mar 23, 2010

@author: Tonio Gsell
'''

import time
import struct
import re
import os
import pickle
from threading import Event

import BackLogMessage
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
        
        
        # Find out the current time
        now = time.strftime('%H:%M')
      
        for task_time in sorted(self._schedule.iterkeys(), None, None, True):
            if(task_time <= now):
                # Ok, we've found our Task!
                if self._wakeup_reason == self.WAKEUP_TYPE_SCHEDULED:
                    pluginParamDict = self._schedule[task_time]
                    self.info('ReverseModePlugin: Our current time is %s, so we execute the task for time %s.' % (now, task_time))

                    # Iterate through the plugins and initialize 'em
                    module = __import__('plugins')
                    for plugin, params in pluginParamDict.iteritems():
                        try:
                            self.info('ReverseModePlugin: loading plugin: %s' % plugin)
                            # fetch the plugin object from the 'plugins' module
                            classobj = getattr(module, plugin)
                        except AttributeError:
                            self.error('ReverseModePlugin: could not load plugin %s: %s class does not exist in plugins.py' % (plugin, plugin))
                            continue

                        if classobj is not None:
                            try:
                                # instantiate the plugin
                                plug_obj = classobj()
                                # initialize the plugin
                                plug_obj.initialize(parent, params)
                                # start the plugin
                                plug_obj.start()
                                # append the plugin to the list for further usage
                                parent._plugins[plugin] = plug_obj
                                self._plugins[plugin] = plug_obj
                            except Exception, e:
                                self.error('ReverseModePlugin: Could not start plugin %s: %s' % (plugin, e.__str__()))
                        else:
                            self.error('ReverseModePlugin: could not load plugin %s' % plugin)

                # All Plugins are initialized, so we don't have to iterate further through the schedule
                break
                
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
                self.debug('schedule length: ' + str(len(message[9:])))
                self.debug('schedule: ' + message[9:])
    
                schedule = message[9:]
                self._schedule = self._parseSchedule(schedule)
                self.info('updated internal schedule with the one received from GSN.')
               
                # Write schedule to disk (the plaintext one for debugging and the parsed one for better performance)
                try:
                    schedule_file = open(self.getOptionValue('schedule_file'), 'w')
                    schedule_file.write(schedule)
                    schedule_file.close()
                
                    compiled_schedule_file = open(self.getOptionValue('schedule_file')+'.parsed', 'w')
                    pickle.dump(self._schedule, compiled_schedule_file)
                    compiled_schedule_file.close()
    
                    self.info('updated %s and %s with the current schedule' % (schedule_file.name, schedule_file.name+".parsed")) 
                except Exception, e:
                    self.warning(e)
    
            self._schedulereceived = True
            self._work.set()
        else:
            self.info('schedule already received')
            
            
            
    def _shutdown(self):
        # TODO: next wakeup and shutdown
        pass



    def _parseSchedule(self, schedule):
        '''
        Parses the schedule file consisting of lines formatted as
            HH:MM Plugin1:param1=value1,param2=value2 Plugin2:param3=value3 Plugin4
        or e.g.
            13:45 GPSPlugin:samplerate=5,duration=3600
        and returns a dictionary like
            {'HH:MM' => {'Plugin1'=>{'param1'=>'value1', 'param2'=>'value2'}, 'Plugin2'=>{'param3'=>'value3'}, 'Plugin4'=>{}}} 
        '''
        time_expr = re.compile('(\d{2}:\d{2})')
        plugin_expr = re.compile('\s+(\w+)')
        param_expr = re.compile('[,:](\w+)=(\w+)')

        tasks = {}

        scheduleLines = schedule.splitlines()

        for line in scheduleLines:

            plugins = {}
            # Match the time hh:mm
            time_match = time_expr.match(line)
            if time_match:
                task_time = time_match.group(1)
                endIndex = time_match.end()

                # Match the Plugin-Name
                plugin_match = plugin_expr.match(line[endIndex:])
                while plugin_match:
                    pluginName = plugin_match.group(1)
                    plugins[pluginName] = {}
                    endIndex += plugin_match.end(0)
    
                    # Match the Parameter Names
                    param_match = param_expr.match(line[endIndex:])
                    while param_match:
                        plugins[pluginName][param_match.group(1)] = param_match.group(2)
                        endIndex += param_match.end()
                        param_match = param_expr.match(line[endIndex:])
                        
                    plugin_match = plugin_expr.match(line[endIndex:])
            
            tasks[task_time] = plugins

        return tasks