
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import os
import time
import serial
import struct
from threading import Event

import BackLogMessage
from AbstractPlugin import AbstractPluginClass

DEFAULT_STATUS_INTERVAL = 60.0
DEFAULT_BACKLOG = True

class VaisalaWXT520PluginClass(AbstractPluginClass):
    '''
    This plugin sends weather sensor values from Vaisala WXT520 to GSN.
    '''

    '''
    _interval
    _stopped
    _sleeper
    '''

    def __init__(self, parent, config):
        AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG)
        
        value = self.getOptionValue('poll_interval')
        if value is None:
            self._interval = DEFAULT_STATUS_INTERVAL
        else:
            self._interval = float(value)
        
        self.info('interval: ' + str(self._interval))
    
        self._stopped = False
        self._sleeper = Event()
        
    
    def getMsgType(self):
        return BackLogMessage.VAISALA_WXT520_MESSAGE_TYPE
            
        
    def run(self):
        self.info('started')

        os.system('echo GPIO out clear > /proc/gpio/GPIO65')
        self.info('power down sensor...')

        self._sleeper.wait(5)
        if self._sleeper.isSet():
            self.debug('died')
            return
        
        os.system('echo GPIO out set > /proc/gpio/GPIO65')
        self.info('power up sensor...')

        self._sleeper.wait(5)
        if self._sleeper.isSet():
            self.debug('died')
            return

        ser = serial.Serial('/dev/ttyUSB0', 19200, bytesize=serial.EIGHTBITS, parity=serial.PARITY_NONE, stopbits=serial.STOPBITS_ONE, timeout=5)
        ser.open()
        ser.flushInput()
        ser.flushOutput()

        ser.write('?\r\n')
        id = ser.readline().strip()
        self.info('device address: ' + id)
        if len(id) != 1 or not id.isalnum():
            self.error('received invalid device address')
            return
        
        ser.write(id + 'XZM\r\n')
        ser.readline()
        ser.write(id + 'XZ\r\n')
        ser.readline()

        ser.write(id + 'XU\r\n')
        self.info(ser.readline().strip())

        ser.write(id + 'WU,R=1111110000000000,I=' + str(int(self._interval/2)) + '\r\n')
        ser.readline()
        ser.write(id + 'WU,A=3,G=1,U=K,D=0,N=W,F=4\r\n')
        ser.readline()
        ser.write(id + 'WU\r\n')
        self.info(ser.readline().strip())
        ser.write(id + 'TU,R=1111000000000000,I=' + str(int(self._interval/2)) + '\r\n')
        ser.readline()
        ser.write(id + 'TU,P=H,T=C\r\n')
        ser.readline()
        ser.write(id + 'TU\r\n')
        self.info(ser.readline().strip())
        ser.write(id + 'RU,R=1111111100000000,I=' + str(int(self._interval/2)) + '\r\n')
        ser.readline()
        ser.write(id + 'RU,U=M,S=M,Z=M\r\n')
        ser.readline()
        ser.write(id + 'RU,X=65535,Y=65535\r\n')
        ser.readline()
        ser.write(id + 'RU\r\n')
        self.info(ser.readline().strip())
        ser.write(id + 'SU,R=1111000000000000,I=' + str(int(self._interval/2)) + '\r\n')
        ser.readline()
        ser.write(id + 'SU,S=N,H=Y\r\n')
        ser.readline()
        ser.write(id + 'SU\r\n')
        self.info(ser.readline().strip())
        
        while not self._stopped:
            self._sleeper.wait(self._interval)
            if self._sleeper.isSet():
                continue

            try:
                ser.write(id + 'R1\r\n')
                line = ser.readline().strip()
                self.debug(line)
                packet = self.packString(line)
                
                ser.write(id + 'R2\r\n')
                line = ser.readline().strip()
                self.debug(line)
                packet += self.packString(line)
                
                ser.write(id + 'R3\r\n')
                line = ser.readline().strip()
                self.debug(line)
                packet += self.packString(line)
                
                ser.write(id + 'R5\r\n')
                line = ser.readline().strip()
                self.debug(line)
                packet += self.packString(line)
                
                self.processMsg(self.getTimeStamp(), packet, self._priority, self._backlog)
            except Exception, e:
                self.exception(e)
                continue

        ser.close()         
        self.info('died')


    def packString(self, s):
        return struct.pack(str(len(s)) + 'sx', s)


    def stop(self):
        self._stopped = True
        self._sleeper.set()
        self.info('stopped')
        
