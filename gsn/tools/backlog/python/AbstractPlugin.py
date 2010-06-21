
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

class AbstractPluginClass(Thread):
    '''
    A plugin should extend this class.
    
    The abstract functions have to be implemented by all plugins.
    '''

    '''
    data/instance attributes:
    _logger
    _parent
    _config
    _backlog
    '''

    def __init__(self, parent, config, backlog_default=False, priority_default=99):
        Thread.__init__(self)
        self._logger = logging.getLogger(self.__class__.__name__)
        self._parent = parent
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
        return self._parent.gsnpeer.processMsg(self.getMsgType(), timestamp, payload, priority, backlogging)
    
    
    def tosMsgReceived(self, timestamp, payload):
        '''
        This function will be executed if a TOS message has been received from the serial
        port. If the plugin is interested in TOS messages it can process it.
                   
        @return: This function should ONLY return True if the message has been processed
                 successfully. Thus, it will be acknowledged over the serial port.
        '''
        pass
    
    
    def sendTOSmsg(self, message):
        '''
        Send a TOS message over the serial port.
        
        This function should be used by the plugins to send any data to a node running
        TinyOS connected over the serial port.
        
        @param message: The TOS message to be sent.
                       
        @return: True if the message has been put into sendbuffer successfully.
        
        @raise TypeError: if the TOSPeerClass has not been started.
        '''
        if not self._parent._tospeer:
            raise TypeError('TOSPeerClass has not been started')
            return False
        else:
            return self._parent._tospeer.sendTOSMsg(message)
       
        
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
        self._parent.backlog.resend()


    def getBackLogStatus(self):
        '''
        Returns the status of the backlog database as tuple:
        (number of database entries, database file size)
        
        @return: status of the backlog database as tuple (number of database entries, database file size)
        '''
        return self._parent.backlog.getStatus()


    def getGSNPeerStatus(self):
        return self._parent.gsnpeer.getStatus()
    
        
    def isBusy(self):
        '''
        This function should return True if the plugin has still things to do.
        If nothing more has to be processed it should return False.
        
        This function will get polled by the SchedulePlugin on shutdown. It will
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
        return self._parent.gsnpeer.isConnected()


    def getExceptionCounter(self):
        '''
        Returns the number of errors occurred since the last program start
        '''
        return self._parent.getExceptionCounter()


    def getErrorCounter(self):
        '''
        Returns the number of errors occurred since the last program start
        '''
        return self._parent.getErrorCounter()

    
    def exception(self, e):
        self._parent.incrementExceptionCounter()
        self._logger.exception(e.__str__())
    
    
    def error(self, msg):
        self._parent.incrementErrorCounter()
        self._logger.error(msg)
        

    def warning(self, msg):
        self._logger.warning(msg)
        
    
    def info(self, msg):
        self._logger.info(msg)
        
    
    def debug(self, msg):
        self._logger.debug(msg)
        