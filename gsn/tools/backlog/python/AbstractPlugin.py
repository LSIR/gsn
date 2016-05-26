# -*- coding: UTF-8 -*-
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"

import logging
import time
import inspect
from threading import Thread

from SpecialAPI import Statistics
from BackLogMessage import PLUGIN_MESSAGE_TYPES
from JobsObserver import DEFAULT_RUNTIME_MODE, RUNTIME_MODE_STOP_ALLWAYS, RUNTIME_MODE_STOP_DC_ALLWAYS, RUNTIME_MODE_STOP_LAST, RUNTIME_MODE_STOP_DC_LAST, RUNTIME_MODE_NO_OBSERVE


class AbstractPluginClass(Thread, Statistics):
    '''
    A plugin has to extend this class. It offers the API for
    plugins to communicate with the backlog core functionality and
    vice versa.
    
    Some abstract functions have to be implemented by all plugins:
        isBusy(self)
        getMsgType(self)
    
    There should be no direct interaction to any core module
    bypassing this API! If someone needs more functionality, please
    make a request to the author. Thus, integrity and consistency
    can be guaranteed.
    '''

    '''
    data/instance attributes:
    _logger
    _backlogMain
    _pluginName
    _config
    _backlog
    _priority
    _maxruntime
    _minruntime
    _powerControl
    '''

    def __init__(self, parent, config, backlog_default=True, priority_default=99, needPowerControl=False):
        Thread.__init__(self, name='AbstractPlugin-Thread')
        self._logger = logging.getLogger(self.__class__.__name__)
        Statistics.__init__(self)
        if needPowerControl:
            self._powerControl = parent.powerControl
        else:
            self._powerControl = None
        self._backlogMain = parent
        self._config = config
        backlog = self.getOptionValue('backlog')
        if backlog:
            if backlog == '0' or backlog.lower() == 'false':
                self._backlog = False
            elif backlog == '1' or backlog.lower() == 'true':
                self._backlog = True
            else:
                self._backlog = backlog_default
        else:
            self._backlog = backlog_default
        
        value = self.getOptionValue('priority')
        if value is None:
            self._priority = priority_default
        else:
            self._priority = int(value)
        
        value = self.getOptionValue('max_runtime')
        if value is None:
            self._maxruntime = None
        else:
            self._maxruntime = int(value)
        
        value = self.getOptionValue('min_runtime')
        if value is None:
            self._minruntime = None
        else:
            self._minruntime = int(value)
        
        value = self.getOptionValue('runtime_mode')
        if value is None:
            self._runtimemode = DEFAULT_RUNTIME_MODE
        else:
            value = int(value)
            if value == RUNTIME_MODE_STOP_ALLWAYS or value == RUNTIME_MODE_STOP_DC_ALLWAYS or \
               value == RUNTIME_MODE_STOP_LAST or value == RUNTIME_MODE_STOP_DC_LAST or \
               value == RUNTIME_MODE_NO_OBSERVE:
                self._runtimemode = value
            else:
                raise TypeError('runtime_mode is set to an unknown value in the configuration file')
            
        self._procMsg = self._backlogMain.gsnpeer.processMsg
        self._pluginName = inspect.stack()[1][0].f_locals['self'].__class__.__name__[:-5]
            
        self.info('backlog: %s' % (self._backlog,))
        self.info('priority: %d' % (self._priority,))
        self.info('runtime_mode: %s' % (self._runtimemode,))
        if self._minruntime is not None:
            self.info('min_runtime: %s' % (self._minruntime,))
        if self._maxruntime:
            self.info('max_runtime: %s' % (self._maxruntime,))
            if self._minruntime > self._maxruntime:
                raise TypeError('min_runtime has to be smaller than max_runtime')
                
        

    def getOptionValue(self, key):
        '''
        This function returns a specific value from the configuration
        file specified by its key. There will be only key/value pairs
        available which belong to this plugins configuration section.
        
        @param key: The key as written in the configuration file as string.
                            
        @return: The value specified by key as string or None if key does not
                 exist.
        '''
        return self._config.get(key)
    
    def getOptionValues(self, keystart):
        '''
        This function returns all values from the configuration
        file of which the keys start with keystart string. There will
        be only key/value pairs available which belong to this plugins
        configuration section.
        
        @param keystart: The keystart as string.
                            
        @return: The values of keys starting with keystart
        '''
        entries = []
        for entry_key, entry_value in self._config.items():
            if entry_key.startswith(keystart):
                entries.append(entry_value)
        return entries
    
    
    def action(self, parameters):
        '''
        This function will be fired by the schedule handler each time
        this plugin is scheduled. The function is started in a new
        thread.
        
        @param parameters: The parameters as one string given in the
                            schedule file.
        '''
        pass
    
    
    def msgReceived(self, data):
        '''
        This function is called if a message has been received this plugin is listening to from GSN.
        If this function is not implemented by the plugin, any incoming message from GSN will just
        be ignored.
        
        @param data: The message to be processed by the plugin as a list
        '''
        self.warning('msgReceived triggered but nothing implemented')
        
        
    def ackReceived(self, timestamp):
        '''
        This function is called if an acknowledge message has been received from GSN.
        If a plugin needs to receive acknowledge information it can use this function.
        
        @param timestamp: The timestamp of the acknowledge message as integer
        '''
        pass
    
    
    def processMsg(self, timestamp, payload, priority=None, backlogging=None):
        '''
        Store the message in the backlog and backup database if needed and try to send
        it to GSN.
        
        This function should be used by the plugins to send any data to GSN.
        
        @param timestamp: the timestamp this message has been generated
        @param payload: payload of the message as a byte array or a list.
                         Should not be bigger than MAX_PAYLOAD_SIZE.
        @param backLog: True if this message should be backlogged in the database, otherwise False.
                        BackLogMessageside has to send an acknowledge to remove this message from
                        the backlog database after successful processing if set to True.
                       
        @return: True if the message has been stored successfully into the backlog database if needed,
                 otherwise False.
                 
        @raise ValueError: if something is wrong with the format of the payload.
        '''
        if backlogging == None:
            backlogging = self._backlog
        if priority == None:
            priority = self._priority
        
        return self._procMsg(self.getMsgType(), timestamp, payload, priority, backlogging)
    
    
    def registerTOSListener(self, types, excempted=False):
        '''
        Register a plugin as a TOS listener.
        
        This function should be used by the plugins to register themselves as TOS listeners.
        After registering the specified TOS AM messages will be received with tosMsgReceived(...).
        
        @param types: A list containing all TOS AM types this listener is listening to.
        
        @param excempted: if set to True all TOS AM types will be listened to except the ones
                          specified in types.
        
        @raise Exception: if the TOSPeerClass can not be started.
        '''
        self._backlogMain.registerTOSListener(self, types, excempted)
    
    
    def deregisterTOSListener(self):
        '''
        Deregister a plugin from TOS peer.
        
        This function should be used by the plugins to deregister themselves from the TOS peer.
        After deregistering no more TOS messages will be received with tosMsgReceived(...).
        If a plugin registered itself with registerTOSListener(), this function has to be called
        at least once, if stop() is called!
        '''
        self._backlogMain.deregisterTOSListener(self)
    
    
    def tosMsgReceived(self, timestamp, packet):
        '''
        This function will be executed if a TOS message has been received from the serial
        port and this plugin listening to the TOS AM type of this message.
                   
        @return: This function should ONLY return True if the message has been processed
                 successfully. Thus, it will be acknowledged over the serial port.
        '''
        pass
    
    
    def sendTOSmsg(self, packet, amId, timeout, blocking, maxretries):
        '''
        Send a TOS message over the serial port.
        
        This function should be used by the plugins to send any data to a node running
        TinyOS connected over the serial port.
        
        @param packet: The packet to be sent.
        @param amId: the amId
        @param timeout: should there be a timeout
        @param blocking: 
        @param maxretries: 
                       
        @return: True if the message has been put into sendbuffer successfully.
        '''
        return self._backlogMain._tospeer.sendTOSMsg(packet, amId, timeout, blocking, maxretries)
    
    
    def registerBOLTListener(self, types, excempted=False):
        '''
        Register a plugin as a BOLT listener.
        
        This function should be used by the plugins to register themselves as BOLT listeners.
        After registering the specified BOLT message types will be received with boltMsgReceived(...).
        
        @param types: A list containing all BOLT message types this listener is listening to.
        
        @param excempted: if set to True all BOLT message types will be listened to except the ones
                          specified in types.
        
        @raise Exception: if the BOLTPeerClass can not be started.
        '''
        self._backlogMain.registerBOLTListener(self, types, excempted)
    
    
    def deregisterBOLTListener(self):
        '''
        Deregister a plugin from BOLT peer.
        
        This function should be used by the plugins to deregister themselves from the BOLT peer.
        After deregistering no more BOLT messages will be received with boltMsgReceived(...).
        If a plugin registered itself with registerBOLTListener(), this function has to be called
        at least once, if stop() is called!
        '''
        self._backlogMain.deregisterBOLTListener(self)
    
    
    def boltMsgReceived(self, timestamp, boltMsg):
        '''
        This function will be executed if a BOLT message has been received from the I2C
        port and this plugin listening to the specific BOLT message type.
        
        @param boltMsg: the BOLT message as specified in BOLTTypes
                   
        @return: This function should ONLY return True if the message has been processed
                 successfully.
        '''
        pass
    
    
    def sendBOLTmsg(self, boltMsg):
        '''
        Send a BOLT message over the I2C port.
        
        This function should be used by the plugins to send any data to a node running
        Glossy/LWB connected over the I2C port.
        
        @param boltMsg: the BOLT message as specified in BOLTTypes
                       
        @return: True if the message has been successfully put into sendbuffer.
        '''
        return self._backlogMain._boltpeer.sendBOLTMsg(boltMsg)
       
        
    def run(self):
        '''
        This function will be executed as thread.
        '''
        self.info('started')
        
    
    def stop(self):
        '''
        This function has to stop the thread.
        '''
        self.info('stopped')
        
    
    def connectionToGSNestablished(self):
        '''
        This function is called if a new connection to GSN has been established
        '''
        pass
        
    
    def connectionToGSNlost(self):
        '''
        This function is called if the connection to GSN has been lost
        '''
        pass
    
    
    def getTimeStamp(self):
        '''
        This function returns the system timestamp.
        
        @return: the system timestamp (UTC) in milliseconds as float
        '''
        return int(time.time()*1000)
        
        
    def getUptime(self):
        '''
        Returns the uptime of the backlog program
        
        @return: uptime of the backlog program in seconds
        '''
        return self._backlogMain.getUptime()
    
    
    def resendStopped(self):
        '''
        This function is called by BacklogDB if resend is stopped.
        '''
        pass
    
    
    def resendStarted(self):
        '''
        This function is called by BacklogDB if resend is started.
        '''
        pass
        
        
    def isResendingDB(self):
        '''
        Returns True if BackLog is resending the content of the sqlite3
        database otherwise False.
        
        @return: True if BackLog is resending db entries
        '''
        return self._backlogMain.backlog.isBusy()
    
        
    def isBusy(self):
        '''
        This function should return True if the plugin has still things to do.
        If nothing more has to be processed it should return False.
        
        This function will get polled by the ScheduleHandler on shutdown. It will
        wait a given time for all plugins to return False before shutdown. Thus, this
        function should only return True if it changes back to returning False again
        after a given time.
        
        @return: True if this plugin has still important work to do
        
        @raise NotImplementedError: if this function is not implemented by the plugin
        '''
        raise NotImplementedError('isBusy is not implemented by %s' % (self._pluginName,))
    
        
    def needsWLAN(self):
        '''
        This function should return True if the plugin needs the WLAN to be powered on.
        If it does not care if the WLAN is powered on it should return False.
        
        This function will get polled by the underlying software implementation if
        someone wants to power off the WLAN. The WLAN will only be powered off if
        all plugins return False.
        
        @return: True if this plugin needs the WLAN to be powered on otherwise False
        
        @raise NotImplementedError: if this function is not implemented by this plugin
        '''
        raise NotImplementedError('needsWLAN is not implemented by %s' % (self._pluginName,))
    
    
    def isDutyCycleMode(self):
        '''
        Returns True if this Core Station is in duty-cycle mode.
        
        @return: True if this Core Station is in duty-cycle mode otherwise False
        '''
        return self._backlogMain.duty_cycle_mode


    def isGSNConnected(self):
        '''
        Returns True if GSN is connected otherwise False
        
        @return: True if GSN is connected otherwise False
        '''
        return self._backlogMain.gsnpeer.isConnected()
    
    
    def getMaxRuntime(self):
        '''
        Returns the 'max_runtime' value set in the configuration file or
        None if inexistent.
        
        @return: 'max_runtime' value set in the configuration file or
                 None if inexistent.
        '''
        return self._maxruntime
    
    
    def getMinRuntime(self):
        '''
        Returns the 'min_runtime' value set in the configuration file or
        None if inexistent.
        
        @return: 'min_runtime' value set in the configuration file or
                 None if inexistent.
        '''
        return self._minruntime
    
    
    def getRuntimeMode(self):
        '''
        Returns the 'runtime_mode' value set in the configuration file or
        None if inexistent.
        
        @return: 'runtime_mode' value set in the configuration file or
                 None if inexistent.
        '''
        return self._runtimemode


    def getExceptionCounter(self):
        '''
        Returns the number of errors occurred since the last program start.
        
        @return: the number of errors
        '''
        return self._backlogMain.getExceptionCounter()


    def getErrorCounter(self):
        '''
        Returns the number of errors occurred since the last program start.
        
        @return: the number of errors
        '''
        return self._backlogMain.getErrorCounter()
    
    
    def getDeviceId(self):
        '''
        Returns the device id.
        
        @return: the device id
        '''
        return self._backlogMain.device_id
        
        
    def beaconSet(self):
        '''
        This function will be called if the tiny node received a
        beacon set message. (duty-cycle mode)
        '''
        pass
        
        
    def beaconCleared(self):
        '''
        This function will be called if the tiny node received a
        beacon clear message. (duty-cycle mode)
        '''
        pass
    
    
    def getBatteryState(self):
        '''
        Returns the current state of the battery.
        
        @return:    -2 = mains operation (9V Power supply connected)
                    -1 = unknown
                     0 = discharging
                     1 = bulk
                     2 = absorption
                     3 = float
                     4 = trickle
                     None, if not set
        
        @raise Exception:    if PowerMonitor is not available
        '''
        if self._backlogMain.powerMonitor is None:
            raise Exception('PowerMonitor is not available')
        return self._backlogMain.powerMonitor._bat.get_state()
    
    
    def getBatterySOC(self):
        '''
        Returns the current state of the battery.
        
        @return:    The current state of charge of the battery [0,100]
                    or None if not set.
        
        @raise Exception:    if PowerMonitor is not available
        '''
        if self._backlogMain.powerMonitor is None:
            raise Exception('PowerMonitor is not available')
        return self._backlogMain.powerMonitor._bat.get_soc()
    
    
    def getRemainingBatteryTime(self):
        '''
        Returns the remaining time of the battery in minutes assuming a constant
        load level (System Current)
        
        @return:    Remaining Time of the battery in minutes or None if not set.
        
        @raise Exception:    if PowerMonitor is not available
        '''
        if self._backlogMain.powerMonitor is None:
            raise Exception('PowerMonitor is not available')
        return self._backlogMain.powerMonitor._bat.get_remainingTime()
    
    
    def getRemainingBatteryTimeDiff(self):
        '''
        Returns the remaining time of the battery in minutes based on the
        differential of the State of Charge!
        
        @return:    Remaining Time of the battery in minutes or None if not set.
        
        @raise Exception:    if PowerMonitor is not available
        '''
        if self._backlogMain.powerMonitor is None:
            raise Exception('PowerMonitor is not available')
        return self._backlogMain.powerMonitor._bat.get_remainingTimeDiff()
    
    
    def getRemainingBatteryTimeDiffLT(self):
        '''
        Returns the remaining time of the battery in minutes based on the long
        term differential of the SoC!
        
        @return:    Remaining Time of the battery in minutes or None if not set.
        
        @raise Exception:    if PowerMonitor is not available
        '''
        if self._backlogMain.powerMonitor is None:
            raise Exception('PowerMonitor is not available')
        return self._backlogMain.powerMonitor._bat.get_remainingTimeDiff_LT()
    
    
    def getBatteryVoltage(self):
        '''
        Returns the approximated battery voltage. This means the measured voltage
        of the Corestation corrected by the voltage drop of the powercable.
        
        @return:    Current voltage of the Battery or None if not set.
        
        @raise Exception:    if PowerMonitor is not available
        '''
        if self._backlogMain.powerMonitor is None:
            raise Exception('PowerMonitor is not available')
        return self._backlogMain.powerMonitor._bat.get_voltage()
    
    
    def getCurrent(self):
        '''
        Returns the approximated battery voltage. This means the measured voltage
        of the Corestation corrected by the voltage drop of the powercable.

        @return:    Current voltage of the Battery or None if not set.
        
        @raise Exception:    if PowerMonitor is not available
        '''
        if self._backlogMain.powerMonitor is None:
            raise Exception('PowerMonitor is not available')
        return self._backlogMain.powerMonitor._bat.get_current()
    
    
    def getPowerControlObject(self):
        '''
        Returns the singleton PowerControl object or None if not
        explicitly enabled in init. The PowerControl object can be
        used to control power supply of different hardware as specified
        in its API.
        
        WARNING: use with special care!!!
        
        @return: the singleton PowerControl object or None if not explicitly
                  enabled in init.
        '''
        return self._powerControl
            
            
    def sendInterPluginCommand(self, pluginName, command):
        '''
        Inter Plugin communication functionality. This function can be used
        to send a command to an other plugin.
        
        @param pluginName: The name (case sensitive) of the plugin the command
                            should be sent to. 
        @param command:    The command for the plugin.
        
        @raise KeyError:            if the plugin name is not in the
                                     PLUGIN_MESSAGE_TYPES map.
               NotImplementedError: if the plugin has not implemented the
                                     recvInterPluginCommand function.
               Exception:            if the plugin is not started yet.
        '''

        try:
            PLUGIN_MESSAGE_TYPES[pluginName]
        except:
            raise Exception('plugin name %s unknown -> can not send inter plugin command' % (pluginName))
        return self._backlogMain.sendInterPluginCommand(pluginName, command)
        
        
    def recvInterPluginCommand(self, command):
        '''
        Inter Plugin communication functionality. This function will be called if
         an other plugin wants to talk to this plugin.
        '''
        pass
    
    
    def setSchedule(self, schedule, merge=True):
        '''
        This function can be used to set a new schedule for this plugin
        in the ScheduleHandler. The schedule has to be in the proper
        crontab-like format as explained in the BackLog documentation.
        The newly set schedule for this plugin will be merged into the
        global one and replace all existing lines concerning this
        plugin (default).
        
        @param schedule: The crontab-like schedule as explained in the
                          BackLog documentation.
        @param merge:    If set to True (default), the new schedule will
                          be merged into the global one and replace all 
                          existing lines concerning this plugin. Otherwise
                          the whole schedule will be replaced by the new
                          one.
        
        @return: True if the new schedule is up and running, otherwise
                  return False.
        '''
        return self._backlogMain.schedulehandler.setSchedule(self._pluginName, schedule, merge)
    
    
    def scheduleEvent(self, origin, schedule):
        '''
        This function will be called if a new/changed schedule has
        been set and is now active.
        
        @param origin:   The name of the origin this new/changed schedule
                          has been initiated from. This may be 'GSN' or the
                          producing Plugin class name.
        @param schedule: The whole schedule as a string.
        '''
        pass
    

    def getMsgType(self):
        '''
        Returns the BackLog message type this plugin is 'working' with.
        
        The plugin message type has to be implemented and documented in
        BackLogMessage.
        
        
        @return: the BackLog message type this plugin is 'working' with.
        
        @raise KeyError: if this plugins name is not in the PLUGIN_MESSAGE_TYPES
                         map.
        
        @see: BackLogMessage
        '''
        return PLUGIN_MESSAGE_TYPES[self._pluginName]

    
    def exception(self, exception):
        '''
        This function should be used to log an exception.
        
        @param exception: the exception to be logged
        '''
        self._backlogMain.incrementExceptionCounter()
        self._logger.exception(str(exception))
    
    
    def error(self, error):
        '''
        This function should be used to log an error.
        
        @param error: the error to be logged
        '''
        self._backlogMain.incrementErrorCounter()
        self._logger.error(str(error))
        

    def warning(self, msg):
        '''
        This function should be used to log a warning.
        
        @param warning: the warning to be logged
        '''
        self._logger.warning(msg)
        
    
    def info(self, msg):
        '''
        This function should be used to log an info.
        
        @param info: the info to be logged
        '''
        self._logger.info(msg)
        
    
    def debug(self, msg):
        '''
        This function should be used to log a debug.
        
        @param debug: the debug to be logged
        '''
        if self._logger.isEnabledFor(logging.DEBUG):
            self._logger.debug(msg)
        