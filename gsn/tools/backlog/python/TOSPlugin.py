'''
Created on Jul 15, 2009

@author: Tonio Gsell
@author: Mustafa Yuecel
'''

#import struct
import array
import time
import tos
import BackLogMessage
from AbstractPlugin import AbstractPluginClass

DEFAULT_BACKLOG = True

class TOSPluginClass(AbstractPluginClass):
    '''
    This plugin offers the functionality to connect to a serial port
    and listens for incoming TOS packets.
    '''

    '''
    data/instance attributes:
    _serialsource    
    _stopped
    '''
    
    def __init__(self, parent, config):
        AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG)
        
        address = self.getOptionValue('tos_source_addr')
        
        if address is None:
            raise TypeError('tos_source_addr not specified in config file')
        
        debug = self.getOptionValue('debug')
        if debug is not None and debug == '1':
            debug = True
        else:
            debug = False
        
        # split the address (it should have the form serial@port:baudrate)
        source = address.split('@')
        if source[0] == 'serial':
            try:
                # try to open a connection to the specified serial port
                serial = tos.getSource(address, debug)
                serial.setTimeout(5)
                self._serialsource = tos.AM(serial)
            except Exception, e:
                raise TypeError('could not initialize serial source: ' + e.__str__())
        else:
            raise TypeError('address type must be serial')

        self._stopped = False
            
        
    def run(self):
        self.info('started')

        self._serialsource.start()

        # open accessnode queue, just in case if we closed it before...
        self.sendOpenQueueCommand()
        
        while not self._stopped:
            # read packet from serial port (this is blocking)
            try:
                self.debug('rcv...')
                packet = self._serialsource.read()
            except Exception, e:
                if not self._stopped:
                    self.error('could not read from serial source: ' + e.__str__())
                continue
            
            # if the packet is None just continue
            if not packet:
                #self.debug('read packet None')
                continue
        
            timestamp = self.getTimeStamp()

            length = len(packet.payload())
            payload = self.tos2backlog(packet);

            self.debug('rcv (%d,%d,%d)' % (self.getMsgType(), timestamp, length))

            # tell PSBackLogMain to send the packet to GSN and backlog
            # using the serial port we can guarantee flow control to the backlog database!
            if self.processMsg(timestamp, payload, self._priority, self._backlog):
                try:
                    self._serialsource.sendAck()
                except Exception, e:
                    if not self._stopped:
                        self.error('could not send ack: ' + e.__str__())

        self.info('died')

        
    def getMsgType(self):
        return BackLogMessage.TOS_MESSAGE_TYPE

    def tos2backlog(self, packet):
        # TODO: append zero at start should not really happen here -> needs bugfix for tos.py
        return array.array('B', [0] + packet.payload()).tostring()

    def backlog2tos(self, message):
        return array.array('B', message[1:]).tolist()

    def sendCloseQueueCommand(self):
        self._serialsource.write(array.array('B', [0x00, 0x00, 0x00, 0x00, 0x05, 0x22, 0x50, 0xff, 0xff, 0x80, 0x00, 0x00]).tolist(), 0x00, 0.2, True)
        time.sleep(35)

    def sendOpenQueueCommand(self):
        self._serialsource.write(array.array('B', [0x00, 0x00, 0x00, 0x00, 0x05, 0x22, 0x50, 0xff, 0xff, 0x80, 0x01, 0x00]).tolist(), 0x00, 0.2, True)
            
    def msgReceived(self, message):
        packet = self.backlog2tos(message)
        #packet = [ byte for byte in struct.unpack(str(len(message)) + 'B', message) ]
        try:
            self._serialsource.write(packet, 0x00, 0.2, True)
            self.debug('snd (%d,?,%d)' % (self.getMsgType(), len(message)))
        except Exception, e:
            if not self._stopped:
                self.warning('could not write message to serial port: ' + e.__str__())

        
    def stop(self):
        self._stopped = True
        
        # send close queue cmd to access node
        self.sendCloseQueueCommand()
        
        self._serialsource.stop()
        self.info('stopped')
