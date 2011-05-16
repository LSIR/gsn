#! /usr/bin/python
# -*- coding: UTF-8 -*-
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import os
import re
import sys
import signal
import hashlib
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
DEFAULT_PLUGINS = [('BackLogStatusPlugin',1)]
DEFAULT_OPTION_GSN_PORT = 9003
DEFAULT_OPTION_BACKLOG_DB = '/tmp/backlog.db'
DEFAULT_OPTION_BACKLOG_RESEND_SLEEP = 0.1
DEFAULT_TOS_VERSION = 2
DEFAULT_BACKLOG_DB_RESEND = 12

SHUTDOWN_CHECK_FILE = '/media/card/backlog/.backlog_shutdown'
BACKLOG_PYTHON_DIRECTORY = '/usr/lib/python2.6/backlog/'

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
    _last_clean_shutdown
    jobsobserver
    schedulehandler
    powerControl
    gsnpeer
    backlog
    plugins
    _msgtypetoplugin
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
        Thread.__init__(self, name='BackLogMain-Thread')
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

        try:
            # readout options from config
            for name, value in self._config.items('options'):
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
        except ConfigParser.NoSectionError:
            raise TypeError('no [options] section specified in %s' % (config_file,))
                
        if self.device_id == None:
            raise TypeError('device_id has to be specified in the configuration file')
        if self.device_id >= 65535 or self.device_id < 0:
            raise TypeError('device_id has to be in the range of 0 and 65534 (both inclusive)')
                
        if not folder_to_check_size:
            raise TypeError('folder_to_check_size has to be specified in the configuration file')
        else:
            if os.path.isdir(folder_to_check_size):
                self._folder_to_check_size = folder_to_check_size
                self._logger.info('folder_to_check_size: %s' % (folder_to_check_size,))
            else:
                raise TypeError('folder_to_check_size has to be an existing directory')
                
        if not folder_min_free_mb:
            raise TypeError('folder_min_free_mb has to be specified in the configuration file')
        else:
            if folder_min_free_mb > 0:
                self._folder_min_free_mb = folder_min_free_mb
                self._logger.info('folder_min_free_mb: %s' % (folder_min_free_mb,))
            else:
                raise TypeError('folder_min_free_mb has to be a positive number')
            
        if not self.checkFolderUsage():
            raise Exception('Not enough space left on %s (%f<%f)' % (self._folder_to_check_size, self.getFolderAvailableMb(), self._folder_min_free_mb))
        else:
            self._logger.info('folder check succeeded (%s: %f MB available)' % (self._folder_to_check_size, self.getFolderAvailableMb()))
        
        # printout options
        self._logger.info('device_id: %s' % (self.device_id,))
        self._logger.info('gsn_port: %s' % (gsn_port,))
        self._logger.info('backlog_db: %s' % (backlog_db,))
        
        # create the backlog root directory if inexistent
        if not os.path.exists(os.path.dirname(backlog_db)):
            os.makedirs(os.path.dirname(backlog_db))
        # and change working directory
        os.chdir(os.path.dirname(backlog_db))
                
        if backlog_db_resend_hr == None:
            backlog_db_resend_hr = DEFAULT_BACKLOG_DB_RESEND
            self._logger.info('backlog_db_resend_hr is not set using default value: %s' % (backlog_db_resend_hr,))
        else:
            self._logger.info('backlog_db_resend_hr: %s' % (backlog_db_resend_hr,))
        
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

        # get schedule section from config files
        try:
            config_schedule = self._config.items('schedule')
        except ConfigParser.NoSectionError:
            raise TypeError('no [schedule] section specified in %s' % (config_file,))
        
        # check for proper shutdown
        self._last_clean_shutdown = None
        if os.path.exists(SHUTDOWN_CHECK_FILE):
            fd = open(SHUTDOWN_CHECK_FILE, 'r')
            self._last_clean_shutdown = long(fd.readline())
            fd.close()
        
        self._tospeer = None
        self._tos_address = tos_address
        self._tos_version = tos_version
        self._tosPeerLock = Lock()
        self._tosListeners = {}
        
        self.gsnpeer = GSNPeerClass(self, self.device_id, gsn_port)
        self._logger.info('loaded GSNPeerClass')
        self.backlog = BackLogDBClass(self, backlog_db, backlog_db_resend_hr)
        self._logger.info('loaded BackLogDBClass')
            
        self.schedulehandler = ScheduleHandlerClass(self, self.duty_cycle_mode, config_schedule)
        self._msgtypetoplugin = {self.schedulehandler.getMsgType(): [self.schedulehandler]}
        
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
            self._logger.warning('no [plugins] section specified in %s' % (config_file,))
            config_plugins = DEFAULT_PLUGINS
            self._logger.warning('use default plugins: %s' % (config_plugins,))

        # init each plugin
        self.plugins = {}
        for module_name, enabled in config_plugins:
            if enabled == '0': continue
            try:
                module = __import__(module_name)
                pluginclass = getattr(module, module_name + 'Class')
                try:
                    config_plugins_options = dict(self._config.items(module_name + '_options'))
                except ConfigParser.NoSectionError:
                    self._logger.warning('no [%s_options] section specified in %s' % (module_name, config_file,))
                    config_plugins_options = {}
                plugin = pluginclass(self, config_plugins_options)
                
                # update message type to plugin dict
                plugs = self._msgtypetoplugin.get(plugin.getMsgType())
                if plugs == None:
                    self._msgtypetoplugin[plugin.getMsgType()] = [plugin]
                else:
                    plugs.append(plugin)
                    self._msgtypetoplugin.update({plugin.getMsgType(): plugs})
                
                self.plugins.update({module_name: plugin})
                self.jobsobserver.observeJob(plugin, module_name, True, plugin.getMaxRuntime())
                self._logger.info('loaded plugin %s' % (module_name,))
            except Exception, e:
                self._logger.error('could not load plugin %s: %s' % (module_name, e))
                self.incrementErrorCounter()

  
    def run(self):
        self._logger.info('started')
        '''
        Starts the GSN server and all plugins.
        
        @param plugins: all plugins tuple as specified in the plugin configuration file under the [plugins] section
        '''

        self.backlog.start()
        self.gsnpeer.start()
        self.jobsobserver.start()
        self.schedulehandler.start()

        for plugin_name, plugin in self.plugins.items():
            self._logger.info('starting %s' % (plugin_name,))
            try:
                plugin.start()
            except Exception, e:
                self.incrementExceptionCounter()
                self._logger.exception(e)
            
        self._stopEvent.wait()
        
        for plugin in self.plugins.values():
            try:
                plugin.join()
            except Exception, e:
                self.incrementExceptionCounter()
                self._logger.exception(e)
        
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
            try:
                plugin.stop()
            except Exception, e:
                self.incrementExceptionCounter()
                self._logger.exception(e)
            
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
                self._logger.info('tos_source_addr: %s' % (self._tos_address,))
                self._logger.info('tos_version: %s' % (self._tos_version,))
                try:
                    self._tospeer = TOSPeerClass(self, self._tos_address, self._tos_version)
                    self._tospeer.start()
                    self._logger.info('TOSPeerClass instantiated')
                except Exception, e:
                    self._tosPeerLock.release()
                    raise Exception('TOSPeerClass could not be loaded: %s' % (e,))
            else:
                self._tosPeerLock.release()
                raise TypeError('TOSPeer can not be loaded as no tos_source_addr is specified in config file')
        self._tosPeerLock.release()
        
        
    def registerTOSListener(self, listener, types=[], excempted=False):
        self.instantiateTOSPeer()
        if excempted:
            tmp = range(0,256)
            for type in types:
                tmp.remove(type)
        else:
            tmp = types
            
        for type in tmp:
            listeners = self._tosListeners.get(type)
            if listeners == None:
                self._tosListeners[type] = [listener]
            else:
                listeners.append(listener)
                self._tosListeners.update({type: listeners})
        if excempted:
            self._logger.info('%s registered as TOS listener (listening to all types except %s)' % (listener.__class__.__name__, types))
        else:
            self._logger.info('%s registered as TOS listener (listening to types %s)' % (listener.__class__.__name__, types))
        
        
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
            
        self._logger.info('%s deregistered as TOS listener' % (listener.__class__.__name__,))
        if not self._tosListeners:
            self._logger.info('no more TOS listeners around -> stop TOSPeer')
            self._tosPeerLock.acquire()
            self._tospeer.stop()
            self._tospeer.join()
            self._tospeer = None
            self._tosPeerLock.release()
        
        
    def processTOSMsg(self, timestamp, type, packet):
        ret = False
        if self._logger.isEnabledFor(logging.DEBUG):
            self._logger.debug('received TOS message with AM type %s' % (type,))
            
        listeners = self._tosListeners.get(type)
        if not listeners:
            self._logger.warning('There is no listener for TOS message with AM type %s.' % (type,))
            return False
        
        for listener in listeners:
            if self._logger.isEnabledFor(logging.DEBUG):
                self._logger.debug('forwarding TOS message to listener %s' % (listener.__class__.__name__,))
            try:
                if listener.tosMsgReceived(timestamp, packet):
                    ret = True
            except Exception, e:
                self.incrementExceptionCounter()
                self._logger.exception(e)
                
        if not ret:
            self._logger.warning('TOS message with AM type %s could not be processed properly by the plugin(s).' % (type,))

        return ret
    
    
    def getTOSPeerStatus(self):
        if self._tospeer:
            return self._tospeer.getStatus()
        else:
            return [None]*3
    
    
    def pluginAction(self, pluginclassname, parameters, runtimemax):
        pluginactive = False
        plugin = self.plugins.get(pluginclassname)
        if plugin != None:
            if runtimemax:
                self.jobsobserver.observeJob(plugin, pluginclassname, True, runtimemax)
            else:
                self.jobsobserver.observeJob(plugin, pluginclassname, True, plugin.getMaxRuntime())
            thread.start_new_thread(plugin.action, (parameters,))
            pluginactive = True
        else:
            try:
                module = __import__(pluginclassname)
                pluginclass = getattr(module, '%sClass' % (pluginclassname,))
                try:
                    config_plugins_options = dict(self._config.items('%s_options' % (pluginclassname,)))
                except ConfigParser.NoSectionError:
                    self._logger.warning('no [%s_options] section specified in configuration file' % (pluginclassname,))
                    config_plugins_options = {}
                plugin = pluginclass(self, config_plugins_options)
                
                # update message type to plugin dict
                plugs = self._msgtypetoplugin.get(plugin.getMsgType())
                if plugs == None:
                    self._msgtypetoplugin[plugin.getMsgType()] = [plugin]
                else:
                    plugs.append(plugin)
                    self._msgtypetoplugin.update({plugin.getMsgType(): plugs})
                    
                self.plugins.update({pluginclassname: plugin})
                if runtimemax:
                    self.jobsobserver.observeJob(plugin, pluginclassname, True, runtimemax)
                else:
                    self.jobsobserver.observeJob(plugin, pluginclassname, True, plugin.getMaxRuntime())
                self._logger.info('loaded plugin %s' % (pluginclassname))
            except Exception, e:
                raise Exception('could not load plugin %s: %s' % (pluginclassname, e))
            try:
                plugin.start()
                thread.start_new_thread(plugin.action, (parameters,))
            except Exception, e:
                self.incrementExceptionCounter()
                self._logger.exception(e)
        return plugin
        
        
    def pluginStop(self, pluginclassname, stopAnyway=False):
        plugin = self.plugins.get(pluginclassname)
        if plugin != None:
            if ((self.schedulehandler._beacon or not self.schedulehandler._duty_cycle_mode) and not plugin.stopIfNotInDutyCycle() and not stopAnyway):
                self._logger.info('%s should not be stopped if not in duty-cycle mode (or beacon) => keep running' % (pluginclassname,))
                return False
            else:
                # update message type to plugin dict
                plugs = self._msgtypetoplugin.get(plugin.getMsgType())
                for index, p in enumerate(plugs):
                    if p == plugin:
                        del plugs[index]
                        if not plugs:
                            del self._msgtypetoplugin[plugin.getMsgType()]
                        else:
                            self._msgtypetoplugin.update({plugin.getMsgType(): plugs})
                        break
                    
                try:
                    plugin.stop()
                except Exception, e:
                    self.incrementExceptionCounter()
                    self._logger.exception(e)
                    
                del self.plugins[pluginclassname]
                return True
        else:
            self._logger.warning('there is no plugin named %s to be stopped' % (pluginclassname, ))
            return False
        
        
    def gsnMsgReceived(self, msgType, message):
        try:
            plugs = self._msgtypetoplugin.get(msgType)
            data = message.getData()
        except Exception, e:
            self.incrementExceptionCounter()
            self._logger.exception(e)
        if plugs:
            try:
                [plug.msgReceived(data) for plug in plugs]
            except Exception, e:
                self.incrementExceptionCounter()
                self._logger.exception(e)
        else:
            self._logger.error('unknown message type %s received' % (msgType,))
            self.incrementErrorCounter()
        
        
    def ackReceived(self, timestamp, msgType):
        # tell the plugins to have received an acknowledge message
        plugs = self._msgtypetoplugin.get(msgType)
        if plugs:
            try:
                [plug.ackReceived(timestamp) for plug in plugs]
            except Exception, e:
                self.incrementExceptionCounter()
                self._logger.exception(e)
            
        # remove the message from the backlog database using its timestamp and message type
        self.backlog.removeMsg(timestamp, msgType)
        
    def beaconSet(self):
        self._logger.info('beacon set')
        # tell the plugins that beacon has been set
        try:
            [plugin.beaconSet() for plugin in self.plugins.values()]
        except Exception, e:
            self.incrementExceptionCounter()
            self._logger.exception(e)
        
    def beaconCleared(self):
        self._logger.info('beacon cleared')
        # tell the plugins that beacon has been cleared
        try:
            [plugin.beaconCleared() for plugin in self.plugins.values()]
        except Exception, e:
            self.incrementExceptionCounter()
            self._logger.exception(e)
        
    def connectionToGSNestablished(self):
        # start resending
        self.backlog.resend()
        # tell the plugins that the connection to GSN has been established
        try:
            [plugin.connectionToGSNestablished() for plugin in self.plugins.values()]
        except Exception, e:
            self.incrementExceptionCounter()
            self._logger.exception(e)
        self.schedulehandler.connectionToGSNestablished()
        self.backlog.resumeResending()
        
    def connectionToGSNlost(self):
        # tell the plugins that the connection to GSN has been lost
        try:
            [plugin.connectionToGSNlost() for plugin in self.plugins.values()]
        except Exception, e:
            self.incrementExceptionCounter()
            self._logger.exception(e)
        self.backlog.pauseResending()
    
    
    def wlanNeeded(self):
        # check if one of the plugins still needs the wlan
        for plugin_name, plugin in self.plugins.items():
            try:
                if plugin.needsWLAN():
                    self._logger.info('wlan is still needed by %s' % (plugin_name,))
                    return True
            except Exception, e:
                self.incrementExceptionCounter()
                self._logger.exception(e)
        
        
    def checkFolderUsage(self):
        stats = os.statvfs(self._folder_to_check_size)
        return self._folder_min_free_mb < (stats.f_bsize * stats.f_bavail / 1048576.0)
        
        
    def getFolderAvailableMb(self):
        stats = os.statvfs(self._folder_to_check_size)
        return stats.f_bsize * stats.f_bavail / 1048576.0
    
    
    def getUptime(self):
        return int(self.timeMeasurementDiff(self._uptimeId, False))
    
    
    def lastCleanShutdown(self):
        return self._last_clean_shutdown
    
    
    def getCodeRevisionList(self):
        m = re.compile('.+\$Id: ([^$]+)\$')
        ret = []
        for root, dirs, files in os.walk(BACKLOG_PYTHON_DIRECTORY):
            for file in files:
                if file.endswith('.py'):
                    hasRevisionLine = False
                    fd = open(os.path.join(root, file), 'r')
                    for line in fd:
                        if line.strip().startswith('__id__'):
                            md5 = hashlib.md5()
                            fd.seek(0)
                            block_size = 128*md5.block_size
                            while True:
                                data = fd.read(block_size)
                                if not data:
                                    break
                                md5.update(data)
                            tmp = m.match(line.strip())
                            if tmp != None:
                                ret.append([tmp.group(1), md5.hexdigest()])
                            else:
                                self._logger.warning('revision line in file %s is mall formated: %s' % (os.path.join(root, file), line.strip()))
                            hasRevisionLine = True
                            break
                    if not hasRevisionLine:
                        self._logger.warning('there is no revision line in file %s' % (os.path.join(root, file), ))
        return ret



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
        print 'config file (%s) not found' % (options.config_file,)
        sys.exit(1)

    # read config file for logging options
    try:
        logging.config.fileConfig(options.config_file)
        logging.logProcesses = 0
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
    
    fd = open(SHUTDOWN_CHECK_FILE, 'w')
    fd.write(str(long(time.time()*1000)))
    fd.close()
    
    if backlog.shutdown:
        print 'shutdown now'
        subprocess.Popen(['shutdown', '-h', 'now'])


if __name__ == '__main__':
    main()
