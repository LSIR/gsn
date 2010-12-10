
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import os
import serial
import struct
from threading import Event

import BackLogMessage
from AbstractPlugin import AbstractPluginClass

DEFAULT_BACKLOG = True

class VaisalaWXT520PluginClass(AbstractPluginClass):
    '''
    This plugin sends weather sensor values from Vaisala WXT520 to GSN.
    '''

    '''
    _interval
    _sleeper
    _serial
    '''

    def __init__(self, parent, config):
        AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG)
    
        self._ready = False
        self._sleeper = Event()
        
    
    def getMsgType(self):
        return BackLogMessage.VAISALA_WXT520_MESSAGE_TYPE
            
        
    def run(self):
        self.info('started')

        os.system('echo GPIO out clear > /proc/gpio/GPIO65')
        self.info('power down sensor...')

        self._sleeper.wait(5)
        if self._sleeper.isSet():
            return
        
        os.system('echo GPIO out set > /proc/gpio/GPIO65')
        self.info('power up sensor...')

        self._sleeper.wait(5)
        if self._sleeper.isSet():
            return

        self._serial = serial.Serial('/dev/ttyUSB0', 19200, bytesize=serial.EIGHTBITS, parity=serial.PARITY_NONE, stopbits=serial.STOPBITS_ONE, timeout=5)
        self._serial.open()
        self._serial.flushInput()
        self._serial.flushOutput()

        self._serial.write('?\r\n')
        self._id = self._serial.readline().strip()
        self.info('device address: ' + self._id)
        if len(self._id) != 1 or not self._id.isalnum():
            self.error('received invalid device address')
        else:
            self._serial.write(self._id + 'XZM\r\n')
            self._serial.readline()
            self._serial.write(self._id + 'XZ\r\n')
            self._serial.readline()
    
            self._serial.write(self._id + 'XU\r\n')
            output = self._serial.readline().strip()
            if output:
                self.info(output)
    
            self._serial.write(self._id + 'WU,R=1111110000000000,I=30\r\n')
            self._serial.readline()
            self._serial.write(self._id + 'WU,A=3,G=1,U=K,D=0,N=W,F=4\r\n')
            self._serial.readline()
            self._serial.write(self._id + 'WU\r\n')
            output = self._serial.readline().strip()
            if output:
                self.info(output)
            self._serial.write(self._id + 'TU,R=1111000000000000,I=30\r\n')
            self._serial.readline()
            self._serial.write(self._id + 'TU,P=H,T=C\r\n')
            self._serial.readline()
            self._serial.write(self._id + 'TU\r\n')
            output = self._serial.readline().strip()
            if output:
                self.info(output)
            self._serial.write(self._id + 'RU,R=1111111100000000,I=30\r\n')
            self._serial.readline()
            self._serial.write(self._id + 'RU,U=M,S=M,Z=M\r\n')
            self._serial.readline()
            self._serial.write(self._id + 'RU,X=65535,Y=65535\r\n')
            self._serial.readline()
            self._serial.write(self._id + 'RU\r\n')
            output = self._serial.readline().strip()
            if output:
                self.info(output)
            self._serial.write(self._id + 'SU,R=1111000000000000,I=30\r\n')
            self._serial.readline()
            self._serial.write(self._id + 'SU,S=N,H=Y\r\n')
            self._serial.readline()
            self._serial.write(self._id + 'SU\r\n')
            output = self._serial.readline().strip()
            if output:
                self.info(output)
            
            self._ready = True


    def action(self, parameters):
        if not self._ready:
            self.error('weather station not ready for action')
            return
        else:
            try:
                packet = None
                self._serial.write(self._id + 'R1\r\n')
                line = self._serial.readline().strip()
                if line:
                    self.debug(line)
                    packet = self.packString(line)
                
                self._serial.write(self._id + 'R2\r\n')
                line = self._serial.readline().strip()
                if line:
                    self.debug(line)
                    packet += self.packString(line)
                
                self._serial.write(self._id + 'R3\r\n')
                line = self._serial.readline().strip()
                if line:
                    self.debug(line)
                    packet += self.packString(line)
                
                self._serial.write(self._id + 'R5\r\n')
                line = self._serial.readline().strip()
                if line:
                    self.debug(line)
                    packet += self.packString(line)
                
                if packet:
                    self.processMsg(self.getTimeStamp(), packet, self._priority, self._backlog)
                else:
                    self.error('Vaisala weather station not connected or wrongly configured')
            except Exception as e:
                self.exception(e)


    def packString(self, s):
        return struct.pack(str(len(s)) + 'sx', s)


    def stop(self):
        self._sleeper.set()
        self._serial.close()
        self.info('stopped')
        
