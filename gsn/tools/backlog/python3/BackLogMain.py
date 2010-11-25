#! /usr/bin/python
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision: 2380 $"
__date__        = "$Date: 2010-11-15 14:21:29 +0100 (Mon, 15. Nov 2010) $"
__id__          = "$Id: BackLogMain.py 2380 2010-11-15 13:21:29Z tgsell $"
__source__      = "$URL: https://gsn.svn.sourceforge.net/svnroot/gsn/branches/permasense/gsn/tools/backlog/python/BackLogMain.py $"
 
import os
import subprocess
import sys
import signal
import configparser
import optparse
import logging
import logging.config
from threading import Thread, Lock

from BackLogDB import BackLogDBClass
from GSNPeer import GSNPeerClass
from TOSPeer import TOSPeerClass
from ScheduleHandler import ScheduleHandlerClass

PROFILE = False
PROFILE_FILE = '/media/card/backlog.profile'

DEFAULT_CONFIG_FILE = '/etc/backlog.cfg'
DEFAULT_PLUGINS = [ 'BackLogStatusPlugin' ]
DEFAULT_OPTION_GSN_PORT = 9003
DEFAULT_OPTION_BACKLOG_DB = '/tmp/backlog.db'
DEFAULT_OPTION_BACKLOG_RESEND_SLEEP = 0.1
DEFAULT_TOS_VERSION = 2
DEFAULT_BACKLOG_DB_RESEND = 12

class BackLogMainClass(Thread):
    '''
    The main thread class for the backlog functionality.
    
    It starts the GSN server, backlog and all plugins specified in the configuration file.
    Furthermore, the read/write interface for plugin/GSN communication is offered.
    '''

    '''
    data/instance attributes:
    _logger
    gsnpeer
    backlog
    plugins
    _exceptionCounter
    _exceptionCounterLock
    _errorCounter
    _errorCounterLock
    '''
    
    def __init__(self, config_file):
        '''
        Initialize the BackLogMain class
        
        Initializes the backlog class.
        Initializes the GSN server class.
        
        @param options: options from the OptionParser
        '''

        Thread.__init__(self)

        self._logger = logging.getLogger(self.__class__.__name__)
        
        self.shutdown = False

        self._exceptionCounter = 0
        self._exceptionCounterLock = Lock()
        self._errorCounter = 0
        self._errorCounterLock = Lock()

        # read config file for other options
        config = configparser.SafeConfigParser()
        config.optionxform = str # case sensitive
        config.read(config_file)

        # get options section from config file
        try:
            config_options = config.items('options')
        except configparser.NoSectionError:
            self._logger.warning('no [options] section specified in ' + config_file)
            config_options = []

        # set default options
        gsn_port = DEFAULT_OPTION_GSN_PORT
        backlog_db = DEFAULT_OPTION_BACKLOG_DB
        
        id = None
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
                id = int(value)
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
                
        if id == None:
            raise TypeError('device_id has to be specified in the configuration file')
        if id >= 65535 or id < 0:
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
        self._logger.info('device_id: ' + str(id))
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
            duty_cycle_mode = True
        else:
            self._logger.info('not running in duty-cycle mode')
            duty_cycle_mode = False

        self.gsnpeer = GSNPeerClass(self, id, gsn_port)
        self._logger.info('loaded GSNPeerClass')
        self.backlog = BackLogDBClass(self, backlog_db, backlog_db_resend_hr)
        self._logger.info('loaded BackLogDBClass')
        
        self.tospeer = None
        if tos_address:
            if not tos_version:
                tos_version = DEFAULT_TOS_VERSION
            self._logger.info('tos_source_addr: ' + tos_address)
            self._logger.info('tos_version: ' + str(tos_version))
            try:
                self.tospeer = TOSPeerClass(self, tos_address, tos_version)
                self._logger.info('loaded TOSPeerClass')
            except Exception as e:
                self._logger.exception('TOSPeerClass could not be loaded: ' + str(e))
        else:
            self._logger.info('TOSPeer will not be loaded as no tos_source_addr is specified in config file')

        # get schedule section from config files
        try:
            config_schedule = config.items('schedule')
        except configparser.NoSectionError:
            raise TypeError('no [schedule] section specified in ' + config_file)
            
        self.schedulehandler = ScheduleHandlerClass(self, duty_cycle_mode, config_schedule)

        # get plugins section from config files
        try:
            config_plugins = config.items('plugins')
        except configparser.NoSectionError:
            self._logger.warning('no [plugins] section specified in ' + config_file)
            config_plugins = DEFAULT_PLUGINS
            self._logger.warning('use default plugins: ' + config_plugins)

        # init each plugin
        self.plugins = []
        for plugin_entry in config_plugins:
            if plugin_entry[1] == '0': continue
            module_name = plugin_entry[0]
            try:
                module = __import__(module_name)
                pluginclass = getattr(module, module_name + 'Class')
                try:
                    config_plugins_options = config.items(module_name + '_options')
                except configparser.NoSectionError:
                    self._logger.warning('no [' + module_name + '_options] section specified in ' + config_file)
                    config_plugins_options = []
                plugin = pluginclass(self, config_plugins_options)
                self.plugins.append((module_name, plugin))
                self._logger.info('loaded plugin ' + module_name)
            except Exception as e:
                self._logger.exception('could not load plugin ' + module_name + ': ' + str(e))
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
        if self.tospeer:
            self.tospeer.start()
        self.schedulehandler.start()

        for plugin_entry in self.plugins:
            self._logger.info('starting ' + plugin_entry[0])
            plugin_entry[1].start()
        
        for plugin_entry in self.plugins:
            plugin_entry[1].join()
        
        self.schedulehandler.join()
        if self.tospeer:
            self.tospeer.join()
        self.backlog.join()
        self.gsnpeer.join()
        
        self._logger.info('died')


    def stop(self):
        for plugin_entry in self.plugins:    
            plugin_entry[1].stop()

        self.schedulehandler.stop()
        if self.tospeer:
            self.tospeer.stop()
        self.backlog.stop()
        self.gsnpeer.stop()
        
        self._logger.info('stopped')
        
        
    def processTOSMsg(self, timestamp, payload):
        ret = False
        for plugin_entry in self.plugins:
            if plugin_entry[1].tosMsgReceived(timestamp, payload):
                ret = True
        if self.schedulehandler.tosMsgReceived(timestamp, payload):
            ret = True
        return ret
        
        
    def pluginsBusy(self):
        for plugin_entry in self.plugins:
            try:
                if plugin_entry[1].isBusy():
                    return True
            except NotImplementedError as e:
                self._logger.error(plugin_entry[0] + ': ' + str(e))
                self.incrementErrorCounter()
        return self.backlog.isBusy()
        
    
    def resend(self):
        self.backlog.resend()
        
    def gsnMsgReceived(self, msgType, message):
        msgTypeValid = False
        if msgType == self.schedulehandler.getMsgType():
            self.schedulehandler.msgReceived(message.getPayload())
            msgTypeValid = True
        else:
            # send the packet to all plugins which 'use' this message type
            for plug in self.plugins:
                if msgType == plug[1].getMsgType():
                    plug[1].msgReceived(message.getPayload())
                    msgTypeValid = True
                    break
        if not msgTypeValid:
            self._logger.error('unknown message type ' + str(msgType) + ' received')
            self.incrementErrorCounter()
        
        
    def ackReceived(self, timestamp, msgType):
        # tell the plugins to have received an acknowledge message
        for plugin_entry in self.plugins:
            if plugin_entry[1].getMsgType() == msgType:
                plugin_entry[1].ackReceived(timestamp)
            
        # remove the message from the backlog database using its timestamp and message type
        self.backlog.removeMsg(timestamp, msgType)
        
    def connectionToGSNestablished(self):
        # tell the plugins that the connection to GSN has been established
        for plugin_entry in self.plugins:
            plugin_entry[1].connectionToGSNestablished()
        self.schedulehandler.connectionToGSNestablished()
        self.backlog.connectionToGSNestablished()
        
    def connectionToGSNlost(self):
        # tell the plugins that the connection to GSN has been lost
        for plugin_entry in self.plugins:
            plugin_entry[1].connectionToGSNlost()
        self.backlog.connectionToGSNlost()
        
        
    def checkFolderUsage(self):
        stats = os.statvfs(self._folder_to_check_size)
        return self._folder_min_free_mb < (stats.f_bsize * stats.f_bavail / 1048576.0)
        
        
    def getFolderAvailableMb(self):
        stats = os.statvfs(self._folder_to_check_size)
        return stats.f_bsize * stats.f_bavail / 1048576.0
            


    def incrementExceptionCounter(self):
        self._exceptionCounterLock.acquire()
        self._exceptionCounter += 1
        self._exceptionCounterLock.release()

    
    def getExceptionCounter(self):
        '''
        Returns the number of exception occurred since the last program start
        '''
        self._exceptionCounterLock.acquire()
        counter = self._exceptionCounter
        self._exceptionCounterLock.release()
        return counter

    
    def incrementErrorCounter(self):
        self._errorCounterLock.acquire()
        self._errorCounter += 1
        self._errorCounterLock.release()

    
    def getErrorCounter(self):
        '''
        Returns the number of errors occurred since the last program start
        '''
        self._errorCounterLock.acquire()
        counter = self._errorCounter
        self._errorCounterLock.release()
        return counter


def main():
    parser = optparse.OptionParser('usage: %prog [options]')
    
    parser.add_option('-c', '--config', type='string', dest='config_file', default=DEFAULT_CONFIG_FILE,
                      help='Configuration file. Default: ' + DEFAULT_CONFIG_FILE, metavar='FILE')
    
    (options, args) = parser.parse_args()
    
        # config file?
    if not os.path.isfile(options.config_file):
        print('config file not found')
        sys.exit(1)

    # read config file for logging options
    try:
        logging.config.fileConfig(options.config_file)
    except configparser.NoSectionError as e:
        print(e.__str__())
        
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
    except Exception as e:
        logger.error(e)
        if backlog and backlog.isAlive():
            backlog.stop()
        logging.shutdown()
        sys.exit(1)
        
    logging.shutdown()
    if backlog.shutdown:
        print('shutdown now')
        subprocess.Popen(['shutdown', '-h', 'now'])


if __name__ == '__main__':
    if PROFILE:
        import cProfile
        cProfile.run('main()', PROFILE_FILE)
    else:
        main()
