
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import logging
import time
from threading import Thread

from SpecialAPI import Statistics

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
    _config
    _backlog
    _priority
    _maxruntime
    _powerControl
    '''

    def __init__(self, parent, config, backlog_default=True, priority_default=99, needPowerControl=False):
        Thread.__init__(self)
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
            
        self.info('backlog: ' + str(self._backlog))
        self.info('priority: ' + str(self._priority))
        if self._maxruntime:
            self.info('max_runtime: ' + str(self._maxruntime))
        

    def getOptionValue(self, key):
        '''
        This function returns a specific value from the configuration
        file specified by its key. There will be only key/value pairs
        available which belong to this plugins configuration section.
        
        @param key: The key as written in the configuration file as string.
                            
        @return: The value specified by key as string
        '''
        for entry in self._config:
            entry_key = entry[0]
            entry_value = entry[1]
            if key == entry_key:
                return entry_value
        return None
    
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
        for entry in self._config:
            entry_key = entry[0]
            entry_value = entry[1]
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
    

    def getMsgType(self):
        '''
        Return the BackLog message type this plugin is 'working' with.
        
        This function should be implemented as following:
            def getMsgType(self):
                return BackLogMessage.'MESSAGENAME'_MESSAGE_TYPE
                
        where 'MESSAGENAME' should be a unique name of the plugin.
        
        'MESSAGENAME'_MESSAGE_TYPE has to be implemented and documented in
        BackLogMessage.
        
        
        @return: the BackLog message type this plugin is 'working' with.
        
        @raise NotImplementedError: if this function is not implemented by the plugin
        
        @see: BackLogMessage
        '''
        raise NotImplementedError('getMsgType is not implemented')
    
    
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
        
        return self._backlogMain.gsnpeer.processMsg(self.getMsgType(), timestamp, payload, priority, backlogging)
    
    
    def registerTOSListener(self):
        '''
        Register a plugin as a TOS listener.
        
        This function should be used by the plugins to register themselves as TOS listeners.
        After registering all incoming TOS messages will be received with tosMsgReceived(...).
        
        @raise Exception: if the TOSPeerClass can not be started.
        '''
        self._backlogMain.registerTOSListener(self)
    
    
    def deregisterTOSListener(self):
        '''
        Deregister a plugin from TOS peer.
        
        This function should be used by the plugins to deregister themselves from the TOS peer.
        After deregistering no more TOS messages will be received with tosMsgReceived(...).
        If a plugin registered itself with registerTOSListener(), this function has to be called
        at least once, if stop() is called!
        '''
        self._backlogMain.deregisterTOSListener(self)
    
    
    def tosMsgReceived(self, timestamp, payload):
        '''
        This function will be executed if a TOS message has been received from the serial
        port and this plugin has been registered as a TOS listener (using registerTOSListener()).
        All incoming TOS messages will be received.
                   
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
    
    
    def resend(self):
        '''
        Tells the BackLogDB class to resend all unacknowledged packets.
        '''
        self._backlogMain.backlog.resend()
        
        
    def getUptime(self):
        '''
        Returns the uptime of the backlog program
        
        @return: uptime of the backlog program in seconds
        '''
        return self._backlogMain.getUptime()
        
        
    def isResendingDB(self):
        '''
        Returns True if BackLog is resending the content of the sqlite3
        database otherwise False.
        
        @return: True if BackLog is resending db entries
        '''
        return self._backlogMain.backlog.isBusy()


    def getBackLogDBStatus(self, intervalSec):
        '''
        Returns the status of the backlog sqlite3 database as list:
        
        @param intervalSec: the passed n seconds over which min/mean/max is calculated.
        
        @return: status of the backlog database [number of database entries,
                                                 database file size, 
                                                 stores per second, 
                                                 removes per second, 
                                                 store counter, 
                                                 remove counter, 
                                                 minimum store time, 
                                                 average store time, 
                                                 maximum store time, 
                                                 minimum remove time, 
                                                 average remove time, 
                                                 maximum remove time]
        '''
        return self._backlogMain.backlog.getStatus(intervalSec)


    def getGSNPeerStatus(self, intervalSec):
        '''
        Returns the status of the GSN peer as list.
        
        @param intervalSec: the passed n seconds over which messages per second is calculated.
        
        @return: status of the GSN peer as list
        '''
        return self._backlogMain.gsnpeer.getStatus(intervalSec)
    
        
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
        raise NotImplementedError('isBusy is not implemented')
    
        
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
        raise NotImplementedError('needsWLAN is not implemented')
    
    
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
    
    def stopIfNotInDutyCycle(self):
        '''
        This function can be overwritten by a plugin if it wants a
        special treatment for its stopping behavior if we are not
        in duty-cycle mode.
        
        @return: True if this plugin can be stopped by the scheduler if
                  we are NOT in duty-cycle mode (or beacon mode), otherwise
                  return False.
        '''
        return True
    
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
        self._logger.debug(msg)
        