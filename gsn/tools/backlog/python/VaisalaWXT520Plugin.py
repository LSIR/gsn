'''
Created on Jul 15, 2009

@author: Tonio Gsell
@author: Mustafa Yuecel
'''

import re
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

        ser = serial.Serial('/dev/ttyUSB0', 1200, bytesize=serial.SEVENBITS, parity=serial.PARITY_EVEN, stopbits=serial.STOPBITS_TWO, timeout=1)
        ser.open()
        ser.write('?!')
        ser.flushOutput()
        time.sleep(0.1)
        id = str(str(ser.read(ser.inWaiting())).strip().decode('ascii'))
        ser.write(id + 'XZM!')
        ser.readline()
        ser.write(id + 'XZ!')
        ser.flushOutput()
        time.sleep(0.1)

        ser.write(id + 'XXU!')
        self.info(ser.readline().strip())

        ser.write(id + 'XWU,R=1111110011111100,I=60!')
        ser.readline()
        ser.write(id + 'XWU,A=60,G=3,U=M,D=0,N=W,F=2!')
        ser.readline()
        ser.write(id + 'XWU!')
        self.info(ser.readline().strip())
        ser.write(id + 'XTU,R=1111000011110000,I=60!')
        ser.readline()
        ser.write(id + 'XTU,P=H,T=C!')
        ser.readline()
        ser.write(id + 'XTU!')
        self.info(ser.readline().strip())
        ser.write(id + 'XRU,R=1111111111111111,I=60!')
        ser.readline()
        ser.write(id + 'XRU,U=M,S=M,Z=M!')
        ser.readline()
        ser.write(id + 'XRU,X=65535,Y=65535!')
        ser.readline()
        ser.write(id + 'XRU!')   
        self.info(ser.readline().strip())
        ser.write(id + 'XSU,R=1111000011110000,I=60!')
        ser.readline()
        ser.write(id + 'XSU,S=Y,H=N!')
        ser.readline()
        ser.write(id + 'XSU!')   
        self.info(ser.readline().strip())
        
        while not self._stopped:
            self._sleeper.wait(self._interval)
            if self._sleeper.isSet():
                continue

            line = ''
            try:
                ser.write(id + 'I!')
                Sn = ser.readline().strip()
                packet = self.packString(Sn)
                
                ser.write(id + 'M1!')
                line = ser.readline()
                count = int(line[4])
                self._sleeper.wait(int(line[1:4]))
                if self._sleeper.isSet():
                    continue
                ser.readline()
                i = 0
                params = []
                while count > len(params):
                    ser.write(id + 'D' + str(i) + '!')
                    line = ser.readline().strip()
                    params += re.findall('[+-]{1}[0-9\.]+', line)
                    i += 1
                Wu_Dn = params[0]
                packet += self.packString(Wu_Dn)
                Wu_Dm = params[1]
                packet += self.packString(Wu_Dm)
                Wu_Dx = params[2]
                packet += self.packString(Wu_Dx)
                Wu_Sn = params[3]
                packet += self.packString(Wu_Sn)
                Wu_Sm = params[4]
                packet += self.packString(Wu_Sm)
                Wu_Sx = params[5]
                packet += self.packString(Wu_Sx)
                
                ser.write(id + 'M2!')
                line = ser.readline()
                count = int(line[4])
                self._sleeper.wait(int(line[1:4]))
                if self._sleeper.isSet():
                    continue
                ser.readline()
                i = 0
                params = []
                while count > len(params):
                    ser.write(id + 'D' + str(i) + '!')
                    line = ser.readline().strip()
                    params += re.findall('[+-]{1}[0-9\.]+', line)
                    i += 1
                Tu_Ta = params[0]
                packet += self.packString(Tu_Ta)
                Tu_Tp = params[1]
                packet += self.packString(Tu_Tp)
                Tu_Ua = params[2]
                packet += self.packString(Tu_Ua)
                Tu_Pa = params[3]
                packet += self.packString(Tu_Pa)
                
                ser.write(id + 'M3!')
                line = ser.readline()
                count = int(line[4])
                i = 0
                params = []
                while count > len(params):
                    ser.write(id + 'D' + str(i) + '!')
                    line = ser.readline().strip()
                    params += re.findall('[+-]{1}[0-9\.]+', line)
                    i += 1
                Ru_Rc = params[0]
                packet += self.packString(Ru_Rc)
                Ru_Rd = params[1]
                packet += self.packString(Ru_Rd)
                Ru_Ri = params[2]
                packet += self.packString(Ru_Ri)
                Ru_Hc = params[3]
                packet += self.packString(Ru_Hc)
                Ru_Hd = params[4]
                packet += self.packString(Ru_Hd)
                Ru_Hi = params[5]
                packet += self.packString(Ru_Hi)
                Ru_Rp = params[6]
                packet += self.packString(Ru_Rp)
                Ru_Hp = params[7]
                packet += self.packString(Ru_Hp)
                
                ser.write(id + 'M5!')
                line = ser.readline()
                count = int(line[4])
                self._sleeper.wait(int(line[1:4]))
                if self._sleeper.isSet():
                    continue
                ser.readline()
                i = 0
                params = []
                while count > len(params):
                     ser.write(id + 'D' + str(i) + '!')
                     line = ser.readline().strip()
                     params += re.findall('[+-]{1}[0-9\.]+', line)
                     i += 1
                Su_Th = params[0]
                packet += self.packString(Su_Th)
                Su_Vh = params[1]
                packet += self.packString(Su_Vh)
                Su_Vs = params[2]
                packet += self.packString(Su_Vs)
                Su_Vr = params[3]
                packet += self.packString(Su_Vr)
                
                self.processMsg(self.getTimeStamp(), packet, self._backlog)
            except IndexError, e:
                self.warning(e.__str__())
                self.warning('last line was <' + line + '>')
                ser.close()
                ser.open()
                ser.write('?!')
                ser.flushOutput()
                time.sleep(0.1)
            except Exception, e:
                self.error(e.__str__())
                break

        ser.close()         
        self.info('died')


    def packString(self, s):
        return struct.pack(str(len(s)) + 'sx', s)

    def stop(self):
        self._stopped = True
        self._sleeper.set()
        self.info('stopped')
        
