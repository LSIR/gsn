'''
Created on Jul 15, 2009

@author: Tonio Gsell
@author: Mustafa Yuecel
'''

#import struct
import array

import BackLogMessage
import tos
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
                self._serialsource = tos.AM(serial)
            except Exception, e:
                raise TypeError('could not initialize serial source: ' + e.__str__())
        else:
            raise TypeError('address type must be serial')

        self._stopped = False
            
        
    def run(self):
        self.info('started')

        self._serialsource.start()
        
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

            if isinstance(self._serialsource, tos.AM):
                #self.debug('tos2')
                length = len(packet.payload())
                # TODO: append zero at start should not really happen here -> needs bugfix for tos.py
                payload = array.array('B', [0] + packet.payload()).tostring()
            else:
                #self.debug('tos1')
                length = len(packet)
                payload = array.array('B', packet).tostring()

            self.debug('rcv (%d,%d,%d)' % (self.getMsgType(), timestamp, length))

            # tell PSBackLogMain to send the packet to GSN and backlog
            # using the serial port we can guarantee flow control to the backlog database!
            if self.processMsg(timestamp, payload, self._backlog):
                try:
                    self._serialsource.sendAck()
                except Exception, e:
                    if not self._stopped:
                        self.error('could not send ack: ' + e.__str__())

        self.info('died')

        
    def getMsgType(self):
        return BackLogMessage.TOS_MESSAGE_TYPE
        
            
    def msgReceived(self, message):
        packet = array.array('B', message).tolist()
        #packet = [ byte for byte in struct.unpack(str(len(message)) + 'B', message) ]
        try:
            self._serialsource.write(packet, 0x00, None, True)
            self.debug('snd (%d,?,%d)' % (self.getMsgType(), len(message)))
        except Exception, e:
            if not self._stopped:
                self.warning('could not write message to serial port: ' + e.__str__())

        
    def stop(self):
        self._stopped = True
        self._serialsource.stop()
        self.info('stopped')
