#! /usr/bin/python
'''
Created on 11.05.2009

@author: Tonio Gsell
@author: Mustafa Yuecel
'''
 
import os
import sys
import signal
import ConfigParser
import optparse
import logging
import logging.config
from threading import Thread, Lock

from BackLogDB import BackLogDBClass
from GSNPeer import GSNPeerClass

DEFAULT_CONFIG_FILE = '/etc/backlog.cfg'
DEFAULT_PLUGINS = [ 'BackLogStatusPlugin' ]
DEFAULT_OPTION_GSN_PORT = 9002
DEFAULT_OPTION_BACKLOG_DB = '/tmp/backlog.db'
DEFAULT_OPTION_BACKLOG_RESEND_SLEEP = 0.1

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
    
    def __init__(self, config_file=DEFAULT_CONFIG_FILE):
        '''
        Initialize the BackLogMain class
        
        Initializes the backlog class.
        Initializes the GSN server class.
        
        @param options: options from the OptionParser
        '''

        Thread.__init__(self)
        
        # config file?
        if not os.path.isfile(config_file):
            raise TypeError('config file not found')

        # read config file for logging options
        try:
            logging.config.fileConfig(config_file)
        except ConfigParser.NoSectionError, e:
            logging.warning(e.__str__())

        self._logger = logging.getLogger(self.__class__.__name__)

        # read config file for other options
        config = ConfigParser.SafeConfigParser()
        config.optionxform = str # case sensitive
        config.read(config_file)

        # get options section from config file
        try:
            config_options = config.items('options')
        except ConfigParser.NoSectionError:
            self._logger.warning('no [options] section specified in ' + config_file)
            config_options = []

        # set default options
        gsn_port = DEFAULT_OPTION_GSN_PORT
        backlog_db = DEFAULT_OPTION_BACKLOG_DB

        # readout options from config
        for entry in config_options:
            name = entry[0]
            value = entry[1]
            if name == 'gsn_port':
                gsn_port = int(value)
            elif name == 'backlog_db':
                backlog_db = value

        # printout options
        self._logger.info('gsn_port: ' + str(gsn_port))
        self._logger.info('backlog_db: ' + backlog_db)

        self.gsnpeer = GSNPeerClass(self, gsn_port)
        self.backlog = BackLogDBClass(self, backlog_db)

        # get plugins section from config files
        try:
            config_plugins = config.items('plugins')
        except ConfigParser.NoSectionError:
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
                except ConfigParser.NoSectionError:
                    self._logger.warning('no [' + module_name + '_options] section specified in ' + config_file)
                    config_plugins_options = []
                plugin = pluginclass(self, config_plugins_options)
                self.plugins.append((module_name, plugin))
                self._logger.info('loaded plugin ' + module_name)
            except Exception, e:
                self._logger.error('could not load plugin ' + module_name + ': ' + e.__str__())
                continue

        self._exceptionCounter = 0
        self._exceptionCounterLock = Lock()
        self._errorCounter = 0
        self._errorCounterLock = Lock()

  
    def run(self):
        self._logger.info('started')
        '''
        Starts the GSN server and all plugins.
        
        @param plugins: all plugins tuple as specified in the plugin configuration file under the [plugins] section
        '''

        self.gsnpeer.start()
        self.backlog.start()

        for plugin_entry in self.plugins:
            module_name = plugin_entry[0]
            plugin = plugin_entry[1]
            plugin.start()
        
        for plugin_entry in self.plugins:
            module_name = plugin_entry[0]
            plugin = plugin_entry[1]
            plugin.join()
        
        self.backlog.join()
        self.gsnpeer.join()
        
        self._logger.info('died')


    def stop(self):
        for plugin_entry in self.plugins:
            module_name = plugin_entry[0]
            plugin = plugin_entry[1]           
            plugin.stop()

        self.backlog.stop()
        self.gsnpeer.stop()
        
        self._logger.info('stopped')
        
    
    def resend(self):
        self.backlog.resend()
        
    def ackReceived(self, timestamp):
        # tell the plugins to have received an acknowledge message
        for plugin_entry in self.plugins:
            module_name = plugin_entry[0]
            plugin = plugin_entry[1]
            plugin.ackReceived(timestamp)
            
        # remove the message from the backlog database using its timestamp
        self.backlog.removeMsg(timestamp)
            


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
    
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname).1s %(name)-.12s - %(message)s", stream=sys.stdout)
    logger = logging.getLogger('BackLogMain.main')
    
    parser = optparse.OptionParser('usage: %prog [options]')
    
    parser.add_option('-c', '--config', type='string', dest='config_file', default=DEFAULT_CONFIG_FILE,
                      help='Configuration file. Default: ' + DEFAULT_CONFIG_FILE, metavar='FILE')
    
    (options, args) = parser.parse_args()
    
    try:
        backlog = BackLogMainClass(options.config_file)
        backlog.start()
        signal.pause()
    except KeyboardInterrupt, e1:
        logger.exception(e1)
        if backlog and backlog.isAlive():
            backlog.stop()
            backlog.join()
    except Exception, e:
        logger.exception(e)
        logging.shutdown()
        sys.exit(1)

    sys.exit(0)


if __name__ == '__main__':
    main()
