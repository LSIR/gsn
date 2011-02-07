#! /usr/bin/python
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import os
import sys
import signal
import ConfigParser
import optparse
import time
import logging
import logging.config
import thread
from threading import Thread, Lock, Event

from SpecialAPI import Statistics, PowerControl
from BackLogDB import BackLogDBClass
from GSNPeer import GSNPeerClass
from TOSPeer import TOSPeerClass
from JobsObserver import JobsObserverClass
from ScheduleHandler import ScheduleHandlerClass, SUBPROCESS_BUG_BYPASS


if SUBPROCESS_BUG_BYPASS:
    import SubprocessFake
    subprocess = SubprocessFake
else:
    import subprocess

DEFAULT_CONFIG_FILE = '/etc/backlog.cfg'
DEFAULT_PLUGINS = [ 'BackLogStatusPlugin' ]
DEFAULT_OPTION_GSN_PORT = 9003
DEFAULT_OPTION_BACKLOG_DB = '/tmp/backlog.db'
DEFAULT_OPTION_BACKLOG_RESEND_SLEEP = 0.1
DEFAULT_TOS_VERSION = 2
DEFAULT_BACKLOG_DB_RESEND = 12

class BackLogMainClass(Thread, Statistics):
    '''
    The main thread class for the backlog functionality.
    
    It starts the GSN server, backlog and all plugins specified in the configuration file.
    Furthermore, the read/write interface for plugin/GSN communication is offered.
    '''

    '''
    data/instance attributes:
    _logger
    _uptimeId
    jobsobserver
    schedulehandler
    powerControl
    gsnpeer
    backlog
    plugins
    duty_cycle_mode
    _exceptionCounterId
    _errorCounterId
    '''
    
    def __init__(self, config_file):
        '''
        Initialize the BackLogMain class
        
        Initializes the backlog class.
        Initializes the GSN server class.
        
        @param options: options from the OptionParser
        '''
        Thread.__init__(self)
        Statistics.__init__(self)
        
        self._uptimeId = self.timeMeasurementStart()

        self._logger = logging.getLogger(self.__class__.__name__)
        
        self.shutdown = False

        self.jobsobserver = JobsObserverClass(self)
        self._exceptionCounterId = self.createCounter()
        self._errorCounterId = self.createCounter()
        self._stopEvent = Event()

        # read config file for other options
        self._config = ConfigParser.SafeConfigParser()
        self._config.optionxform = str # case sensitive
        self._config.read(config_file)

        # get options section from config file
        try:
            config_options = self._config.items('options')
        except ConfigParser.NoSectionError:
            self._logger.warning('no [options] section specified in ' + config_file)
            config_options = []

        # set default options
        gsn_port = DEFAULT_OPTION_GSN_PORT
        backlog_db = DEFAULT_OPTION_BACKLOG_DB
        
        self.device_id = None
        tos_address = None
        tos_version = None
        dutycyclemode = None
        backlog_db_resend_hr = None
        folder_to_check_size = None
        folder_min_free_mb = None

        # readout options from config
        for entry in config_options:
            name = entry[0]
            value = entry[1]
            if name == 'gsn_port':
                gsn_port = int(value)
            elif name == 'backlog_db':
                backlog_db = value
            elif name == 'backlog_db_resend_hr':
                backlog_db_resend_hr = int(value)
            elif name == 'device_id':
                self.device_id = int(value)
            elif name == 'tos_source_addr':
                tos_address = value
            elif name == 'tos_version':
                tos_version = int(value)
            elif name == 'duty_cycle_mode':
                dutycyclemode = int(value)
            elif name == 'folder_to_check_size':
                folder_to_check_size = value
            elif name == 'folder_min_free_mb':
                folder_min_free_mb = int(value)
                
        if self.device_id == None:
            raise TypeError('device_id has to be specified in the configuration file')
        if self.device_id >= 65535 or self.device_id < 0:
            raise TypeError('device_id has to be in the range of 0 and 65534 (both inclusive)')

        # printout info
        self._logger.info(str(__version__))
        self._logger.info(str(__date__))
        self._logger.info(str(__id__))
        self._logger.info(str(__source__))
                
        if not folder_to_check_size:
            raise TypeError('folder_to_check_size has to be specified in the configuration file')
        else:
            if os.path.isdir(folder_to_check_size):
                self._folder_to_check_size = folder_to_check_size
                self._logger.info('folder_to_check_size: ' + folder_to_check_size)
            else:
                raise TypeError('folder_to_check_size has to be an existing directory')
                
        if not folder_min_free_mb:
            raise TypeError('folder_min_free_mb has to be specified in the configuration file')
        else:
            if folder_min_free_mb > 0:
                self._folder_min_free_mb = folder_min_free_mb
                self._logger.info('folder_min_free_mb: ' + str(folder_min_free_mb))
            else:
                raise TypeError('folder_min_free_mb has to be a positive number')
            
        if not self.checkFolderUsage():
            raise Exception('Not enough space left on ' + self._folder_to_check_size + ' (' + str(self.getFolderAvailableMb()) + '<' + str(self._folder_min_free_mb) + ')')
        else:
            self._logger.info('folder check succeeded (' + self._folder_to_check_size + ': ' + str(self.getFolderAvailableMb()) + ' MB available)')
        
        # printout options
        self._logger.info('device_id: ' + str(self.device_id))
        self._logger.info('gsn_port: ' + str(gsn_port))
        self._logger.info('backlog_db: ' + backlog_db)
        
                
        if backlog_db_resend_hr == None:
            backlog_db_resend_hr = DEFAULT_BACKLOG_DB_RESEND
            self._logger.info('backlog_db_resend_hr is not set using default value: ' + str(backlog_db_resend_hr))
        else:
            self._logger.info('backlog_db_resend_hr: ' + str(backlog_db_resend_hr))
        
        if dutycyclemode is None:
            raise TypeError('duty_cycle_mode has to be specified in the configuration file')
        elif dutycyclemode != 1 and dutycyclemode != 0:
            raise TypeError('duty_cycle_mode has to be set to 1 or 0 in config file')
        elif dutycyclemode == 1:
            self._logger.info('running in duty-cycle mode')
            self.duty_cycle_mode = True
        else:
            self._logger.info('not running in duty-cycle mode')
            self.duty_cycle_mode = False
        
        self._tospeer = None
        self._tos_address = tos_address
        self._tos_version = tos_version
        self._tosPeerLock = Lock()
        self._tosListeners = {}
        
        self.gsnpeer = GSNPeerClass(self, self.device_id, gsn_port)
        self._logger.info('loaded GSNPeerClass')
        self.backlog = BackLogDBClass(self, backlog_db, backlog_db_resend_hr)
        self._logger.info('loaded BackLogDBClass')

        # get schedule section from config files
        try:
            config_schedule = self._config.items('schedule')
        except ConfigParser.NoSectionError:
            raise TypeError('no [schedule] section specified in ' + config_file)
            
        self.schedulehandler = ScheduleHandlerClass(self, self.duty_cycle_mode, config_schedule)
        
        self.powerControl = None
        try:
            self.powerControl = PowerControl(self)
        except:
            pass
        else:
            self._logger.info('loaded PowerControl class')

        # get plugins section from config files
        try:
            config_plugins = self._config.items('plugins')
        except ConfigParser.NoSectionError:
            self._logger.warning('no [plugins] section specified in ' + config_file)
            config_plugins = DEFAULT_PLUGINS
            self._logger.warning('use default plugins: ' + config_plugins)

        # init each plugin
        self.plugins = {}
        for plugin_entry in config_plugins:
            if plugin_entry[1] == '0': continue
            module_name = plugin_entry[0]
            try:
                module = __import__(module_name)
                pluginclass = getattr(module, module_name + 'Class')
                try:
                    config_plugins_options = self._config.items(module_name + '_options')
                except ConfigParser.NoSectionError:
                    self._logger.warning('no [' + module_name + '_options] section specified in ' + config_file)
                    config_plugins_options = []
                plugin = pluginclass(self, config_plugins_options)
                self.plugins.update({module_name: plugin})
                self.jobsobserver.observeJob(plugin, module_name, True, plugin.getMaxRuntime())
                self._logger.info('loaded plugin ' + module_name)
            except Exception, e:
                self._logger.error('could not load plugin ' + module_name + ': ' + str(e))
                self.incrementErrorCounter()
                continue

  
    def run(self):
        self._logger.info('started')
        '''
        Starts the GSN server and all plugins.
        
        @param plugins: all plugins tuple as specified in the plugin configuration file under the [plugins] section
        '''

        self.gsnpeer.start()
        self.backlog.start()
        self.schedulehandler.start()
        self.jobsobserver.start()

        for plugin_name, plugin in self.plugins.items():
            self._logger.info('starting ' + plugin_name)
            plugin.start()
            
        self._stopEvent.wait()
        
        for plugin in self.plugins.values():
            plugin.join()
        
        self.jobsobserver.join()
        self.schedulehandler.join()
        if self._tospeer:
            self._tospeer.join()
        self.backlog.join()
        self.gsnpeer.join()
        
        self._logger.info('died')


    def stop(self):
        if self.powerControl:
            self.powerControl.stop()
        self.schedulehandler.stop()
        self.jobsobserver.stop()
        
        for plugin in self.plugins.values():  
            plugin.stop()
            
        self._stopEvent.set()

        if self._tospeer:
            self._tospeer.stop()
        self.backlog.stop()
        self.gsnpeer.stop()
        
        self._logger.info('stopped')
        
        
    def instantiateTOSPeer(self):
        self._tosPeerLock.acquire()
        if not self._tospeer:
            if self._tos_address:
                if not self._tos_version:
                    self._tos_version = DEFAULT_TOS_VERSION
                self._logger.info('tos_source_addr: ' + self._tos_address)
                self._logger.info('tos_version: ' + str(self._tos_version))
                try:
                    self._tospeer = TOSPeerClass(self, self._tos_address, self._tos_version)
                    self._tospeer.start()
                    self._logger.info('TOSPeerClass instantiated')
                except Exception, e:
                    self._tosPeerLock.release()
                    raise Exception('TOSPeerClass could not be loaded: ' + str(e))
            else:
                self._tosPeerLock.release()
                raise TypeError('TOSPeer can not be loaded as no tos_source_addr is specified in config file')
        self._tosPeerLock.release()
        
        
    def registerTOSListener(self, listener, types):
        self.instantiateTOSPeer()
        for type in types:
            listeners = self._tosListeners.get(type)
            if listeners == None:
                self._tosListeners[type] = [listener]
            else:
                listeners.append(listener)
                self._tosListeners.update({type: listeners})
        self._logger.info(listener.__class__.__name__ + ' registered as TOS listener (types ' + str(types) + ')')
        
        
    def deregisterTOSListener(self, listener):
        for type, listeners in self._tosListeners.items():
            for index, listenerfromlist in enumerate(listeners):
                if listener == listenerfromlist:
                    del listeners[index]
                    if not listeners:
                        del self._tosListeners[type]
                    else:
                        self._tosListeners.update({type: listeners})
                    break
            
        self._logger.info(listener.__class__.__name__ + ' deregistered as TOS listener')
        if not self._tosListeners:
            self._logger.info('no more TOS listeners around -> stop TOSPeer')
            self._tosPeerLock.acquire()
            self._tospeer.stop()
            self._tospeer.join()
            self._tospeer = None
            self._tosPeerLock.release()
        
        
    def processTOSMsg(self, timestamp, type, packet):
        ret = False
        self._logger.debug('received TOS message with AM type ' + str(type))
        listeners = self._tosListeners.get('all')
        if listeners != None:
            for listener in listeners:
                self._logger.debug('forwarding TOS message to listener ' + listener.__class__.__name__ + ' (listening to all AM types)')
                try:
                    if listener.tosMsgReceived(timestamp, packet):
                        ret = True
                except Exception, e:
                    self.incrementExceptionCounter()
                    self._logger.exception(e)
            
        listeners = self._tosListeners.get(type)
        if listeners != None:
            for listener in listeners:
                self._logger.debug('forwarding TOS message to listener ' + listener.__class__.__name__ + ' (listening only to some types)')
                try:
                    if listener.tosMsgReceived(timestamp, packet):
                        ret = True
                except Exception, e:
                    self.incrementExceptionCounter()
                    self._logger.exception(e)
        
        if not ret:
            self._logger.warning('TOS message with AM type ' + str(type) + ' has not been processed.')

        return ret
    
    
    def pluginAction(self, pluginclassname, parameters, runtimemax):
        pluginactive = False
        for plugin_name, plugin in self.plugins.items():
            if plugin_name == pluginclassname:
                if runtimemax:
                    self.jobsobserver.observeJob(plugin, pluginclassname, True, runtimemax)
                else:
                    self.jobsobserver.observeJob(plugin, pluginclassname, True, plugin.getMaxRuntime())
                thread.start_new_thread(plugin.action, (parameters,))
                pluginactive = True
                return plugin
                
        if not pluginactive:
            try:
                module = __import__(pluginclassname)
                pluginclass = getattr(module, pluginclassname + 'Class')
                try:
                    config_plugins_options = self._config.items(pluginclassname + '_options')
                except ConfigParser.NoSectionError:
                    self._logger.warning('no [' + pluginclassname + '_options] section specified in configuration file')
                    config_plugins_options = []
                plugin = pluginclass(self, config_plugins_options)
                self.plugins.update({pluginclassname: plugin})
                if runtimemax:
                    self.jobsobserver.observeJob(plugin, pluginclassname, True, runtimemax)
                else:
                    self.jobsobserver.observeJob(plugin, pluginclassname, True, plugin.getMaxRuntime())
                self._logger.info('loaded plugin ' + pluginclassname)
            except Exception, e:
                raise Exception('could not load plugin ' + pluginclassname + ': ' + str(e))
            plugin.start()
            thread.start_new_thread(plugin.action, (parameters,))
            return plugin
        
        
    def pluginStop(self, pluginclassname, stopAnyway=False):
        for plugin_name, plugin in self.plugins.items():
            if pluginclassname == plugin_name:
                if ((self.schedulehandler._beacon or not self.schedulehandler._duty_cycle_mode) and not plugin.stopIfNotInDutyCycle() and not stopAnyway):
                    self._logger.info(pluginclassname + ' should not be stopped if not in duty-cycle mode (or beacon) => keep running')
                    return False
                else:
                    plugin.stop()
                    del self.plugins[plugin_name]
                    return True
        return False
        
    
    def resend(self):
        self.backlog.resend()
        
        
    def gsnMsgReceived(self, msgType, message):
        msgTypeValid = False
        if msgType == self.schedulehandler.getMsgType():
            self.schedulehandler.msgReceived(message.getData())
            msgTypeValid = True
        else:
            # send the packet to all plugins which 'use' this message type
            for plugin in self.plugins.values():
                if msgType == plugin.getMsgType():
                    try:
                        plugin.msgReceived(message.getData())
                        msgTypeValid = True
                    except Exception, e:
                        self.incrementExceptionCounter()
                        self._logger.exception(e)
                    break
        if not msgTypeValid:
            self._logger.error('unknown message type ' + str(msgType) + ' received')
            self.incrementErrorCounter()
        
        
    def ackReceived(self, timestamp, msgType):
        # tell the plugins to have received an acknowledge message
        for plugin in self.plugins.values():
            if plugin.getMsgType() == msgType:
                plugin.ackReceived(timestamp)
            
        # remove the message from the backlog database using its timestamp and message type
        self.backlog.removeMsg(timestamp, msgType)
        
    def beaconSet(self):
        self._logger.info('beacon set')
        # tell the plugins that beacon has been set
        for plugin in self.plugins.values():
            plugin.beaconSet()
        
    def beaconCleared(self):
        self._logger.info('beacon cleared')
        # tell the plugins that beacon has been cleared
        for plugin in self.plugins.values():
            plugin.beaconCleared()
        
    def connectionToGSNestablished(self):
        # tell the plugins that the connection to GSN has been established
        for plugin in self.plugins.values():
            plugin.connectionToGSNestablished()
        self.schedulehandler.connectionToGSNestablished()
        self.backlog.connectionToGSNestablished()
        
    def connectionToGSNlost(self):
        # tell the plugins that the connection to GSN has been lost
        for plugin in self.plugins.values():
            plugin.connectionToGSNlost()
        self.backlog.connectionToGSNlost()
    
    
    def wlanNeeded(self):
        # check if one of the plugins still needs the wlan
        for plugin_name, plugin in self.plugins.items():
            if plugin.needsWLAN():
                self._logger.info('wlan is still needed by ' + plugin_name)
                return True
        
        
    def checkFolderUsage(self):
        stats = os.statvfs(self._folder_to_check_size)
        return self._folder_min_free_mb < (stats.f_bsize * stats.f_bavail / 1048576.0)
        
        
    def getFolderAvailableMb(self):
        stats = os.statvfs(self._folder_to_check_size)
        return stats.f_bsize * stats.f_bavail / 1048576.0
    
    
    def getUptime(self):
        return int(self.timeMeasurementDiff(self._uptimeId, False))


    def incrementExceptionCounter(self):
        self.counterAction(self._exceptionCounterId)

    
    def getExceptionCounter(self):
        '''
        Returns the number of exception occurred since the last program start
        '''
        return self.getCounterValue(self._exceptionCounterId)

    
    def incrementErrorCounter(self):
        self.counterAction(self._errorCounterId)

    
    def getErrorCounter(self):
        '''
        Returns the number of errors occurred since the last program start
        '''
        return self.getCounterValue(self._errorCounterId)



def main():
    parser = optparse.OptionParser('usage: %prog [options]')
    
    parser.add_option('-c', '--config', type='string', dest='config_file', default=DEFAULT_CONFIG_FILE,
                      help='Configuration file. Default: ' + DEFAULT_CONFIG_FILE, metavar='FILE')
    
    (options, args) = parser.parse_args()
    
        # config file?
    if not os.path.isfile(options.config_file):
        print 'config file (' + options.config_file + ') not found'
        sys.exit(1)

    # read config file for logging options
    try:
        logging.config.fileConfig(options.config_file)
    except ConfigParser.NoSectionError, e:
        print e.__str__()
        
    logger = logging.getLogger('BackLogMain.main')
        
    backlog = None
    try:
        backlog = BackLogMainClass(options.config_file)
        backlog.start()
        signal.pause()
    except KeyboardInterrupt:
        logger.warning('KeyboardInterrupt')
        if backlog and backlog.isAlive():
            backlog.stop()
            backlog.join()
    except Exception, e:
        logger.error(e)
        if backlog and backlog.isAlive():
            backlog.stop()
        logging.shutdown()
        sys.exit(1)
        
    logging.shutdown()
    
    if backlog.shutdown:
        print 'shutdown now'
        subprocess.Popen(['shutdown', '-h', 'now'])


if __name__ == '__main__':
    main()
