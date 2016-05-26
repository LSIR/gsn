#! /usr/bin/python
# -*- coding: UTF-8 -*-
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"

import os
import re
import sys
import signal
import hashlib
import optparse
import time
import logging
import logging.config
import thread
from datetime import datetime, timedelta
from threading import Thread, Lock, Event

from SpecialAPI import Statistics
from PowerControl import PowerControlClass
from ConfigurationHandler import ConfigurationHandlerClass
from BackLogDB import BackLogDBClass
from GSNPeer import GSNPeerClass
from TOSPeer import TOSPeerClass
from DPPPeer import DPPPeerClass
from JobsObserver import JobsObserverClass
from ScheduleHandler import ScheduleHandlerClass, SUBPROCESS_BUG_BYPASS
from PowerMonitor import PowerMonitor
from ConfigParser import NoSectionError


if SUBPROCESS_BUG_BYPASS:
    import SubprocessFake
    subprocess = SubprocessFake
else:
    import subprocess

DEFAULT_CONFIG_FILE = '/etc/backlog.cfg'
DEFAULT_PLUGINS = [('BackLogStatusPlugin',1)]
DEFAULT_OPTION_GSN_PORT = 9003
DEFAULT_OPTION_BACKLOG_DB = '/media/card/backlog/backlog.db'
DEFAULT_OPTION_BACKLOG_RESEND_SLEEP = 0.1
DEFAULT_TOS_VERSION = 2
DEFAULT_BACKLOG_DB_RESEND = 12
DEFAULT_SHUTDOWN_CHECK_FILE = '/media/card/backlog/.backlog_shutdown'

BACKLOG_PYTHON_DIRECTORY = '/usr/lib/python2.6/backlog/'
BASEBOARD_R1_PATH = '/sys/devices/platform/baseboard_old/'
BASEBOARD_R2_PATH = '/sys/devices/platform/baseboard/'

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
    confighandler
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
        self.powerControl = None
        Thread.__init__(self, name='BackLogMain-Thread')
        Statistics.__init__(self)
        
        self._uptimeId = self.timeMeasurementStart()

        self._logger = logging.getLogger(self.__class__.__name__)
        
        self.confighandler = ConfigurationHandlerClass(self, config_file, DEFAULT_OPTION_BACKLOG_DB, DEFAULT_BACKLOG_DB_RESEND, DEFAULT_SHUTDOWN_CHECK_FILE)
        self._msgtypetoplugin = {self.confighandler.getMsgType(): [self.confighandler]}
        
        self._backlogStopped = False
        self.shutdown = False

        self.jobsobserver = JobsObserverClass(self)
        self._exceptionCounterId = self.createCounter()
        self._errorCounterId = self.createCounter()
        self._stopEvent = Event()
        
        self.device_id = self.confighandler.getParsedConfig()['device_id']
                
        self._folder_to_check_size = self.confighandler.getParsedConfig()['folder_to_check_size']
        self._logger.info('folder_to_check_size: %s' % (self._folder_to_check_size,))
             
        self._folder_min_free_mb = self.confighandler.getParsedConfig()['folder_min_free_mb']
        self._logger.info('folder_min_free_mb: %s' % (self._folder_min_free_mb,))
            
        if not self.checkFolderUsage():
            raise Exception('Not enough space left on %s (%f<%f)' % (self._folder_to_check_size, self.getFolderAvailableMb(), self._folder_min_free_mb))
        else:
            self._logger.info('folder check succeeded (%s: %f MB available)' % (self._folder_to_check_size, self.getFolderAvailableMb()))
        
        # printout options
        self._logger.info('device_id: %s' % (self.device_id,))
        self._logger.info('gsn_port: %s' % (self.confighandler.getParsedConfig()['gsn_port'],))
        self._logger.info('backlog_db: %s' % (self.confighandler.getParsedConfig()['backlog_db'],))
        
        # create the backlog root directory if inexistent
        if not os.path.exists(os.path.dirname(self.confighandler.getParsedConfig()['backlog_db'])):
            os.makedirs(os.path.dirname(self.confighandler.getParsedConfig()['backlog_db']))
        # and change working directory
        os.chdir(os.path.dirname(self.confighandler.getParsedConfig()['backlog_db']))
                
        self._logger.info('backlog_db_resend_hr: %s' % (self.confighandler.getParsedConfig()['backlog_db_resend_hr'],))
        self._logger.info('service_wakeup_schedule: %s' % (self.confighandler.getParsedConfig()['service_wakeup_schedule'],))
        self._logger.info('service_wakeup_minutes: %s' % (self.confighandler.getParsedConfig()['service_wakeup_minutes'],))
        self._serviceWindow = {'wakeup_minutes': timedelta(minutes=self.confighandler.getParsedConfig()['service_wakeup_minutes']), 'hour': int(self.confighandler.getParsedConfig()['service_wakeup_schedule'].split(':')[0]), 'minute': int(self.confighandler.getParsedConfig()['service_wakeup_schedule'].split(':')[1])}
        
        self.duty_cycle_mode = self.confighandler.getParsedConfig()['duty_cycle_mode']
        if self.duty_cycle_mode:
            self._logger.info('running in duty-cycle mode')
        else:
            self._logger.info('not running in duty-cycle mode')
        
        if os.path.isdir(BASEBOARD_R1_PATH):
            platform = 1
            self._logger.info('BackLog is running on an old BaseBoardv2 r1.0 platform')
        elif os.path.isdir(BASEBOARD_R2_PATH):
            platform = 2
            self._logger.info('BackLog is running on a BaseBoardv2 r2.0 platform')
        else:
            platform = self.confighandler.getParsedConfig()['platform']
            if platform == 1:
                self._logger.info('BackLog is running on an old BaseBoardv2 r1.0 platform')
            elif platform == 2:
                self._logger.info('BackLog is running on a BaseBoardv2 r2.0 platform')
            else:
                self._logger.warning('BackLog is running on a unknown platform')
        
        dutycyclewhileresending = self.confighandler.getParsedConfig()['wlan_duty_cycle_while_resending']
        if dutycyclewhileresending:
            self._logger.info('wlan can be duty-cycled while messages are resent from DB')
        
        # check for proper shutdown
        self._last_clean_shutdown = None
        if os.path.exists(self.confighandler.getParsedConfig()['shutdown_check_file']):
            fd = open(self.confighandler.getParsedConfig()['shutdown_check_file'], 'r')
            try:
                self._last_clean_shutdown = long(fd.readline())
            except Exception, e:
                self._logger.error('could not check %s: %s' % (self.confighandler.getParsedConfig()['shutdown_check_file'], str(e)))
                fd.close()
                os.remove(self.confighandler.getParsedConfig()['shutdown_check_file'])
                self._last_clean_shutdown = None
            fd.close()
        
        self._tospeer = None
        self._tos_address = self.confighandler.getParsedConfig()['tos_address']
        self._tos_version = self.confighandler.getParsedConfig()['tos_version']
        self._tosPeerLock = Lock()
        self._tosListeners = {}
        
        self._dpppeer = None
        self._dpp_i2c_device = self.confighandler.getParsedConfig()['dpp_i2c_device']
        self._dpp_i2c_address = self.confighandler.getParsedConfig()['dpp_i2c_address']
        self._dpp_i2c_polling_interval_sec = self.confighandler.getParsedConfig()['dpp_i2c_polling_interval_sec']
        self._dppPeerLock = Lock()
        self._dppListeners = {}
        
        self.powerControl = PowerControlClass(self, self.confighandler.getParsedConfig()['wlan_port'], dutycyclewhileresending, platform)
        self._logger.info('loaded PowerControl class')
        self.gsnpeer = GSNPeerClass(self, self.device_id, self.confighandler.getParsedConfig()['gsn_port'])
        self._logger.info('loaded GSNPeerClass')
        self.schedulehandler = ScheduleHandlerClass(self, self.duty_cycle_mode, self.confighandler.getParsedConfig()['config_schedule'])
        self._msgtypetoplugin.update({self.schedulehandler.getMsgType(): [self.schedulehandler]})
        self._logger.info('loaded ScheduleHandlerClass')
        self.backlog = BackLogDBClass(self, self.confighandler.getParsedConfig()['backlog_db'], self.confighandler.getParsedConfig()['backlog_db_resend_hr'])
        self._logger.info('loaded BackLogDBClass')
        
        self.powerMonitor = None
        try:
            self.powerMonitor = PowerMonitor(self, self.confighandler.getParsedConfig()['config_powermonitor'])
            self._logger.info('loaded PowerMonitor class')
        except Exception, e:
            self._logger.warning(e)

        # get plugins section from config files
        try:
            config_plugins = self.confighandler.getParsedConfig()['config'].items('plugins')
        except NoSectionError:
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
                    config_plugins_options = dict(self.confighandler.getParsedConfig()['config'].items(module_name + '_options'))
                except NoSectionError:
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

        self.powerControl.start()
        self.backlog.start()
        self.gsnpeer.start()
        self.jobsobserver.start()
        if self._tospeer and not self._tospeer.isAlive():
            self._tospeer.start()
        if self._dpppeer and not self._dpppeer.isAlive():
            self._dpppeer.start()
        self.confighandler.start()
        if self.powerMonitor:
            self.powerMonitor.start()

        for plugin_name, plugin in self.plugins.items():
            self._logger.info('starting %s' % (plugin_name,))
            try:
                plugin.start()
                self.jobsobserver.observeJob(plugin, plugin_name, True, plugin.getMaxRuntime(), plugin.getMinRuntime())
            except Exception, e:
                self.incrementExceptionCounter()
                self._logger.exception(e)
                
        self.schedulehandler.start()
            
        self._stopEvent.wait()
        
        for plugin_name, plugin in self.plugins.items():
            try:
                plugin.join()
                self._logger.info('%s joined' % (plugin_name,))
            except Exception, e:
                self.incrementExceptionCounter()
                self._logger.exception(e)
        
        self.jobsobserver.join()
        self._logger.info('JobsObserverClass joined')
        self.schedulehandler.join()
        self._logger.info('ScheduleHandlerClass joined')
        if self._tospeer:
            self._tospeer.join()
            self._logger.info('TOSPeerClass joined')
        if self._dpppeer:
            self._dpppeer.join()
            self._logger.info('DPPPeerClass joined')
        self.backlog.join()
        self._logger.info('BackLogDBClass joined')
        self.gsnpeer.join()
        self._logger.info('GSNPeerClass joined')
        self.powerControl.join()
        self._logger.info('PowerControlClass joined')
        
        self._logger.info('died')


    def stop(self):
        self._backlogStopped = True
        
        try:
            self.confighandler.stop()
        except Exception, e:
            self.incrementExceptionCounter()
            self._logger.exception(e)
        
        if self.powerMonitor:
            try:
                self.powerMonitor.stop()            
            except Exception, e:
                self.incrementExceptionCounter()
                self._logger.exception(e)
        
        try:
            self.schedulehandler.stop()
        except Exception, e:
            self.incrementExceptionCounter()
            self._logger.exception(e)
            
        try:
            self.jobsobserver.stop()
        except Exception, e:
            self.incrementExceptionCounter()
            self._logger.exception(e)
        
        for plugin in self.plugins.values():
            try:
                plugin.stop()
            except Exception, e:
                self.incrementExceptionCounter()
                self._logger.exception(e)
            
        self._stopEvent.set()

        if self._tospeer:
            try:
                self._tospeer.stop()
            except Exception, e:
                self.incrementExceptionCounter()
                self._logger.exception(e)

        if self._dpppeer:
            try:
                self._dpppeer.stop()
            except Exception, e:
                self.incrementExceptionCounter()
                self._logger.exception(e)
                
        try:
            self.backlog.stop()
        except Exception, e:
            self.incrementExceptionCounter()
            self._logger.exception(e)
            
        try:
            self.gsnpeer.stop()
        except Exception, e:
            self.incrementExceptionCounter()
            self._logger.exception(e)
        
        try:
            self.powerControl.stop()
        except Exception, e:
            self.incrementExceptionCounter()
            self._logger.exception(e)
        
        self._logger.info('stopped')
        
        
    def instantiateTOSPeer(self):
        self._tosPeerLock.acquire()
        if not self._tospeer:
            if self._tos_address is not None:
                if self._tos_version is None:
                    self._tos_version = DEFAULT_TOS_VERSION
                self._logger.info('tos_source_addr: %s' % (self._tos_address,))
                self._logger.info('tos_version: %s' % (self._tos_version,))
                try:
                    self._tospeer = TOSPeerClass(self, self._tos_address, self._tos_version)
                    if self.isAlive() or self.duty_cycle_mode:
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
            try:
                self._tospeer.stop()
                self._tospeer.join()
            except Exception, e:
                self.incrementExceptionCounter()
                self._logger.exception(e)
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
        
        
    def instantiateDPPPeer(self):
        self._dppPeerLock.acquire()
        if not self._dpppeer:
            if self._dpp_i2c_device is not None:
                self._logger.info('dpp_i2c_device: %s' % (self._dpp_i2c_device,))
                self._logger.info('dpp_i2c_addresse: %s' % (self._dpp_i2c_address,))
                self._logger.info('dpp_i2c_polling_interval_sec: %s' % (self._dpp_i2c_polling_interval_sec,))
                try:
                    self._dpppeer = DPPPeerClass(self, self._dpp_i2c_device, self._dpp_i2c_address, self._dpp_i2c_polling_interval_sec)
                    if self.isAlive() or self.duty_cycle_mode:
                        self._dpppeer.start()
                    self._logger.info('DPPPeerClass instantiated')
                except Exception, e:
                    self._dppPeerLock.release()
                    raise Exception('DPPPeerClass could not be loaded: %s' % (e,))
            else:
                self._dppPeerLock.release()
                raise TypeError('DPPPeer can not be loaded as no dpp_i2c_device is specified in config file')
        self._dppPeerLock.release()
        
        
    def registerDPPListener(self, listener, types=[], excempted=False):
        self.instantiateDPPPeer()
        if excempted:
            tmp = range(0,256)
            for type in types:
                tmp.remove(type)
        else:
            tmp = types
            
        for type in tmp:
            listeners = self._dppListeners.get(type)
            if listeners == None:
                self._dppListeners[type] = [listener]
            else:
                listeners.append(listener)
                self._dppListeners.update({type: listeners})
        if excempted:
            self._logger.info('%s registered as DPP listener (listening to all types except %s)' % (listener.__class__.__name__, types))
        else:
            self._logger.info('%s registered as DPP listener (listening to types %s)' % (listener.__class__.__name__, types))
        
        
    def deregisterDPPListener(self, listener):
        for type, listeners in self._dppListeners.items():
            for index, listenerfromlist in enumerate(listeners):
                if listener == listenerfromlist:
                    del listeners[index]
                    if not listeners:
                        del self._dppListeners[type]
                    else:
                        self._dppListeners.update({type: listeners})
                    break
            
        self._logger.info('%s deregistered as DPP listener' % (listener.__class__.__name__,))
        if not self._dppListeners:
            self._logger.info('no more DPP listeners around -> stop DPPPeer')
            self._dppPeerLock.acquire()
            try:
                self._dpppeer.stop()
                self._dpppeer.join()
            except Exception, e:
                self.incrementExceptionCounter()
                self._logger.exception(e)
            self._dpppeer = None
            self._dppPeerLock.release()
        
        
    def processDPPMsg(self, timestamp, dppMsg):
        ret = False
        if self._logger.isEnabledFor(logging.DEBUG):
            self._logger.debug('received DPP message with type %s' % (dppMsg['type'],))
            
        listeners = self._dppListeners.get(dppMsg['type'])
        if not listeners:
            self._logger.warning('There is no listener for DPP message with type %s.' % (dppMsg['type'],))
            return False
        
        for listener in listeners:
            if self._logger.isEnabledFor(logging.DEBUG):
                self._logger.debug('forwarding DPP message to listener %s' % (listener.__class__.__name__,))
            try:
                if listener.dppMsgReceived(timestamp, dppMsg):
                    ret = True
            except Exception, e:
                self.incrementExceptionCounter()
                self._logger.exception(e)
                
        if not ret:
            self._logger.warning('DPP message with type %s could not be processed properly by the plugin(s).' % (dppMsg['type'],))

        return ret
    
    
    def getDPPPeerStatus(self):
        if self._dpppeer:
            return self._dpppeer.getStatus()
        else:
            return [None]*3
    
    
    def pluginAction(self, pluginclassname, parameters, runtimemax, runtimemin):
        if self._backlogStopped:
            return None
        
        pluginactive = False
        plugin = self.plugins.get(pluginclassname)
        if plugin != None:
            if runtimemax is None:
                runtimemax = plugin.getMaxRuntime()
            if runtimemin is None:
                runtimemin = plugin.getMinRuntime()
            self.jobsobserver.observeJob(plugin, pluginclassname, True, runtimemax, runtimemin)
            thread.start_new_thread(plugin.action, (parameters,))
            pluginactive = True
        else:
            try:
                module = __import__(pluginclassname)
                pluginclass = getattr(module, '%sClass' % (pluginclassname,))
                try:
                    config_plugins_options = dict(self.confighandler.getParsedConfig()['config'].items('%s_options' % (pluginclassname,)))
                except NoSectionError:
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
                if runtimemax is None:
                    runtimemax = plugin.getMaxRuntime()
                if runtimemin is None:
                    runtimemin = plugin.getMinRuntime()
                self.jobsobserver.observeJob(plugin, pluginclassname, True, runtimemax, runtimemin)
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
        
        
    def pluginStop(self, pluginclassname):
        plugin = self.plugins.get(pluginclassname)
        if plugin != None:
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
            if not self._backlogStopped:
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
        
        
    def newScheduleSet(self, origin, schedule):
        try:
            [plugin.scheduleEvent(origin, schedule) for plugin in self.plugins.values()]
        except Exception, e:
            self.incrementExceptionCounter()
            self._logger.exception(e)
    
    
    def getNextServiceWindowRange(self):
        now = datetime.utcnow()
        service_time = datetime(now.year, now.month, now.day, self._serviceWindow['hour'], self._serviceWindow['minute'])
        if (service_time + self._serviceWindow['wakeup_minutes']) < now:
            twentyfourhours = timedelta(hours=24)
            return (service_time + twentyfourhours, service_time + twentyfourhours + self._serviceWindow['wakeup_minutes'])
        else:
            return (service_time, service_time + self._serviceWindow['wakeup_minutes'])
    
    
    def wlanNeededByPlugins(self):
        # check if one of the plugins still needs the wlan
        for plugin_name, plugin in self.plugins.items():
            try:
                if plugin.needsWLAN():
                    self._logger.info('wlan is still needed by %s' % (plugin_name,))
                    return True
            except Exception, e:
                self.incrementExceptionCounter()
                self._logger.exception(e)

    
    def sendInterPluginCommand(self, pluginName, command):
        if self._backlogStopped:
            return
        
        plugin = self.plugins.get(pluginName)
        if plugin != None:
            thread.start_new_thread(plugin.recvInterPluginCommand, (command,))
        else:
            raise Exception('%s has not been started yet -> can not send inter plugin command' % (pluginName,))
        
        
    def sendResendStopped(self):
        if self._backlogStopped:
            return
        
        try:
            [plugin.resendStopped() for plugin in self.plugins.values()]
        except Exception, e:
            self.incrementExceptionCounter()
            self._logger.exception(e)
            
        self.schedulehandler.backlogResendStopped()
            
    def sendResendStarted(self):
        if self._backlogStopped:
            return
        
        try:
            [plugin.resendStarted() for plugin in self.plugins.values()]
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
    config_file = os.path.abspath(options.config_file)
    
    # read config file for logging options
    try:
        logging.config.fileConfig(config_file)
        logging.logProcesses = 0
    except NoSectionError, e:
        print e.__str__()
        
    logger = logging.getLogger('BackLogMain.main')

    loop = True
    while loop:
            
        backlog = None
        try:
            backlog = BackLogMainClass(config_file)
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
        
        if backlog:
            loop = backlog.confighandler.restart()
            if loop:
                logger.warning('restarting BackLog now')
        else:
            loop = False
            
        logging.shutdown()
        
        fd = open(backlog.confighandler.getParsedConfig()['shutdown_check_file'], 'w')
        fd.write(str(long(time.time()*1000)))
        fd.close()
    
    if backlog:
        if backlog.shutdown:
            print 'shutdown now'
            subprocess.Popen(['shutdown', '-h', 'now'])
        elif backlog.powerControl and not backlog.powerControl.getWlanStatus():
            print 'turn wlan on'
            backlog.powerControl.wlanOn()
            


if __name__ == '__main__':
    main()
