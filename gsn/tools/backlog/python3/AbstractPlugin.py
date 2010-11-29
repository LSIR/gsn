
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision: 2381 $"
__date__        = "$Date: 2010-11-15 14:51:38 +0100 (Mon, 15. Nov 2010) $"
__id__          = "$Id: AbstractPlugin.py 2381 2010-11-15 13:51:38Z tgsell $"
__source__      = "$URL: https://gsn.svn.sourceforge.net/svnroot/gsn/branches/permasense/gsn/tools/backlog/python/AbstractPlugin.py $"

import logging
import time
from threading import Thread

class AbstractPluginClass(Thread):
    '''
    A plugin should extend this class.
    
    The abstract functions have to be implemented by all plugins.
    '''

    '''
    data/instance attributes:
    _logger
    _backlogMain
    _config
    _backlog
    '''

    def __init__(self, parent, config, backlog_default=False, priority_default=99):
        Thread.__init__(self)
        self._logger = logging.getLogger(self.__class__.__name__)
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
        self.info('backlog: ' + str(self._backlog))
        self.info('priority: ' + str(self._priority))
        

    def getOptionValue(self, key):
        for entry in self._config:
            entry_key = entry[0]
            entry_value = entry[1]
            if key == entry_key:
                return entry_value
        return None
    
    def getOptionValues(self, key):
        entries = []
        for entry in self._config:
            entry_key = entry[0]
            entry_value = entry[1]
            if entry_key.startswith(key):
                entries.append(entry_value)
        return entries

    def getMsgType(self):
        '''
        Return the BackLog message type this plugin is 'working' with.
        
        This function should be implemented as following:
            def getMsgType(self):
                return gsn.BackLogMessage.'MESSAGENAME'_MESSAGE_TYPE
                
        where 'MESSAGENAME' should be a unique name of the plugin.
        
        'MESSAGENAME'_MESSAGE_TYPE has to be implemented and documented in
        BackLogMessage.
        
        
        @return: the BackLog message type this plugin is 'working' with.
        
        @raise NotImplementedError: if this function is not implemented by the plugin
        
        @see: gsn.BackLogMessage
        '''
        raise NotImplementedError('getMsgType is not implemented')
    
    
    def msgReceived(self, message):
        '''
        This function is called if a message has been received this plugin is listening to from GSN.
        If this function is not implemented by the plugin, any incoming message from GSN will just
        be ignored.
        
        @param message: The message to be processed by the plugin in string format
        '''
        self.warning('msgReceived triggered but nothing implemented')
        
        
    def ackReceived(self, timestamp):
        '''
        This function is called if an acknowledge message has been received from GSN.
        If a plugin needs to receive acknowledge information it can use this function.
        
        @param timestamp: The timestamp of the acknowledge message as integer
        '''
        pass
    
    
    def processMsg(self, timestamp, payload, priority, backlogging=False):
        '''
        Store the message in the backlog and backup database if needed and try to send
        it to GSN.
        
        This function should be used by the plugins to send any data to GSN.
        
        @param timestamp: the timestamp this message has been generated
        @param payload: the raw data to be sent (no more than 4 Gb)
        @param backLog: True if this message should be backlogged in the database, otherwise False.
                        BackLogMessageside has to send an acknowledge to remove this message from
                        the backlog database after successful processing if set to True.
                       
        @return: True if the message has been stored successfully into the backlog database if needed,
                 otherwise False.
        '''
        return self._backlogMain.gsnpeer.processMsg(self.getMsgType(), timestamp, payload, priority, backlogging)
    
    
    def registerTOSListener(self):
        '''
        Register a plugin as a TOS listener.
        
        This function should be used by the plugins to register themselves as TOS listeners.
        After registering all incoming TOS messages will be received with tosMsgReceived(...).
        
        @raise Exception: if the TOSPeerClass can not be started.
        '''
        self._backlogMain.registerTOSListener(self)
    
    
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
        pass
        
    
    def stop(self):
        '''
        This function have to stop the thread.
        '''
        pass
        
    
    def connectionToGSNestablished(self):
        '''
        This function is called if a new connection to GSN has been established
        '''
        pass
        
    
    def connectionToGSNlost(self):
        '''
        This function is called if a the connection to GSN has been lost
        '''
        pass
    
    
    def getTimeStamp(self):
        return int(time.time()*1000)
    
    
    def resend(self):
        '''
        Tells the BackLogDB class to resend all unacknowledged packets.
        '''
        self._backlogMain.backlog.resend()


    def getBackLogStatus(self):
        '''
        Returns the status of the backlog database as tuple:
        (number of database entries, database file size)
        
        @return: status of the backlog database as tuple (number of database entries, database file size)
        '''
        return self._backlogMain.backlog.getStatus()


    def getGSNPeerStatus(self):
        return self._backlogMain.gsnpeer.getStatus()
    
        
    def isBusy(self):
        '''
        This function should return True if the plugin has still things to do.
        If nothing more has to be processed it should return False.
        
        This function will get polled by the ScheduleHandler on shutdown. It will
        wait a given time for all plugins to return False before shutdown. Thus, this
        function should only return True if it changes back to returning False again
        after a given time.
        
        @return: True if this plugin has still work to do
        
        @raise NotImplementedError: if this function is not implemented by the plugin
        '''
        raise NotImplementedError('isBusy is not implemented')


    def isGSNConnected(self):
        '''
        Returns True if GSN is connected otherwise False
        
        @return: True if GSN is connected otherwise False
        '''
        return self._backlogMain.gsnpeer.isConnected()


    def getExceptionCounter(self):
        '''
        Returns the number of errors occurred since the last program start
        '''
        return self._backlogMain.getExceptionCounter()


    def getErrorCounter(self):
        '''
        Returns the number of errors occurred since the last program start
        '''
        return self._backlogMain.getErrorCounter()

    
    def exception(self, exception):
        self._backlogMain.incrementExceptionCounter()
        self._logger.exception(str(exception))
    
    
    def error(self, error):
        self._backlogMain.incrementErrorCounter()
        self._logger.error(str(error))
        

    def warning(self, msg):
        self._logger.warning(msg)
        
    
    def info(self, msg):
        self._logger.info(msg)
        
    
    def debug(self, msg):
        self._logger.debug(msg)
        