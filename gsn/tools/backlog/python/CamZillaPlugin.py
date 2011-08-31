# -*- coding: UTF-8 -*-
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import logging
import Queue
from threading import Thread, Event, Lock

import BackLogMessage
from AbstractPlugin import AbstractPluginClass

try: 
    import serial
except ImportError, e: 
    print "Please install PySerial first."
    sys.exit(1)

DEFAULT_BACKLOG = True

class CamZillaPluginClass(AbstractPluginClass):
    '''
    This plugin offers the functionality to control the CamZilla robot.
    '''

    '''
    data/instance attributes:
    _taskqueue
    _plugStop
    _isBusy
    '''
    
    def __init__(self, parent, config):
        AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG)
        self._isBusy = True
        
        device = self.getOptionValue('device_name')
        if device is None:
            raise TypeError('no device_name specified')
        
        try:
            self._serial = serial.Serial(device)
        except serial.SerialException, e:
            raise TypeError('could not initialize serial source: %s' % (e,))
        self.info(self._serial.readline())
        
        self._taskqueue = Queue.Queue()
        
        self._plugStop = False
    
    
    def getMsgType(self):
        return BackLogMessage.CAMZILLA_MESSAGE_TYPE
        
        
    def isBusy(self):
        return self._isBusy
        
        
    def needsWLAN(self):
        return False
    
    
    def msgReceived(self, data):
        pass
       
        
    def run(self):
        self.name = 'CamZillaPlugin-Thread'
        self.info('started')
        
        self._initRobot()

        while not self._plugStop:
            try:
                task = self._taskqueue.get()
                if self._plugStop:
                    try:
                        self._taskqueue.task_done()
                    except ValueError, e:
                        self.exception(e)
                    break
                
                #TODO
                   
                try:
                    self._taskqueue.task_done()
                except ValueError, e:
                    self.exception(e)
            except Exception, e:
                self.exception(str(e))
        
        self.info('died')


    def action(self, parameters):
        self._taskqueue.put(parameters)
    
    
    def stop(self):
        self._write('j=on')
        self._isBusy = False
        self._plugStop = True
        self._taskqueue.put('end')
        
    
    def _initRobot(self):
        self.info(self._write("j=off"))
        self.info(self._write("cal"))
        
        
        
    def _write(self, com):
        if not self._plugStop:
            self.info('write: %s' % (com,))
            if com == 'cal':
                self._serial.write(com + "\n")
                cal = self._serial.readline().strip()
                self.info('cal: %s' % (cal))
                xy = self._serial.readline().strip()
                self.info('cal: %s' % (xy))
                spl = xy.split('=')[1].split('/')
                return (int(spl[0]), int(spl[1]))
            elif com == 'j=on' or com == 'j=off':
                self._serial.write(com + "\n")
                ans = self._serial.readline().strip()
                self.info('j=..: %s' % (ans,))
                if com != ans:
                    raise Exception('return value (%s) does not match command (%s)' % (ans, com))
            elif com.startswith('x=') or com.startswith('y='):
                self._serial.write(com + "\n")
                ans = self._serial.readline().strip()
                self.info('x=..: %s' % (ans,))
                if ans == '!cal':
                    raise Exception('not yet calibrated')
                elif ans.startswith('x/y='):
                    spl = ans.split('=')[1].split('/')
                    return (int(spl[0]), int(spl[1]))
                else:
                    raise Exception('unknown return value for command (%s): %s' % (com, ans))
            else:
                raise TypeError('command (%s) unknown' % (com,))
        else:
            self.warning('plugin has been stopped -> command will not be executed')
        