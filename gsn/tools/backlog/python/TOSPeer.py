# -*- coding: UTF-8 -*-
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import logging
import time
import Queue
from threading import Thread, Event

import tos
import tos1x
import BackLogMessage
from SpecialAPI import Statistics

DEFAULT_BACKLOG = True

SEND_QUEUE_SIZE = 25

class TOSPeerClass(Thread, Statistics):
    '''
    Offers the functionality to communicate with a node running TOS.
    '''

    '''
    data/instance attributes:
    _backlogMain
    _serialsource
    _toswriter
    _version
    _logger 
    _tosPeerStop
    '''
    
    def __init__(self, parent, address, version):
        Thread.__init__(self, name='TOSPeer-Thread')
        self._logger = logging.getLogger(self.__class__.__name__)
        Statistics.__init__(self)
        
        # split the address (it should have the form serial@port:baudrate)
        source = address.split('@')
        if source[0] == 'serial':
            try:
                # try to open a connection to the specified serial port
                if version == 2:
                    serial = tos.getSource(address)
                    serial.setTimeout(5)
                    self._serialsource = tos.AM(serial)
                elif version == 1:
                    serial = tos1x.getSource(address)
                    serial.setTimeout(5)
                    self._serialsource = tos1x.AM(serial)
            except Exception, e:
                raise TypeError('could not initialize serial source: %s' % (e,))
        else:
            raise TypeError('address type must be serial')
        
        self._msgSentCounterId = self.createCounter()
        self._msgReceivedCounterId = self.createCounter()
        self._ackSentCounterId = self.createCounter()
        
        self._toswriter = TOSWriter(self)

        self._backlogMain = parent
        self._tosPeerStop = False
            
        
    def run(self):
        self._logger.info('started')

        self._serialsource.start()
        self._toswriter.start()
        
        # speed optimizations
        read = self._serialsource.read
        sendAck = self._serialsource.sendAck
        processTOSMsg = self._backlogMain.processTOSMsg
        isEnabledFor = self._logger.isEnabledFor
        
        while not self._tosPeerStop:
            # read packet from serial port (this is blocking)
            try:
                self._logger.debug('rcv...')
                packet = read()
            except Exception, e:
                if not self._tosPeerStop:
                    self.exception('could not read from serial source: %s' % (e,))
                continue
            
            # if the packet is None just continue
            if not packet:
                #self._logger.debug('read packet None')
                continue
        
            timestamp = int(time.time()*1000)

            length = len(packet.payload())

            if isEnabledFor(logging.DEBUG):
                self._logger.debug('rcv (?,%d,%d)' % (timestamp, length))

            self.counterAction(self._msgReceivedCounterId)
            # tell BackLogMain to send the packet to the plugins
            # using the serial port we can guarantee flow control to the backlog database!
            if processTOSMsg(timestamp, packet['type'], packet):
                try:
                    sendAck()
                    self.counterAction(self._ackSentCounterId)
                except Exception, e:
                    if not self._tosPeerStop:
                        self.exception('could not send ack: %s' % (e,))
                        
        self._toswriter.join()
        self._serialsource.join()

        self._logger.info('died')
        
            
    def sendTOSMsg(self, packet, amId, timeout=None, blocking=True, maxretries = None):
        return self._toswriter.addMsg(packet, amId, timeout, blocking, maxretries)
            
            
    def getStatus(self):
        '''
        Returns the status of the TOS peer as list:
        
        @return: status of the TOS peer [received message counter,
                                         sent acknowledge counter,
                                         sent message counter]
        '''
        return [self.getCounterValue(self._msgReceivedCounterId), \
                self.getCounterValue(self._ackSentCounterId), \
                self.getCounterValue(self._msgSentCounterId)]


    def exception(self, exception):
        self._backlogMain.incrementExceptionCounter()
        self._logger.exception(str(exception))

        
    def stop(self):
        self._tosPeerStop = True
        self._toswriter.stop()
        self._serialsource.stop()
        self._logger.info('stopped')



class TOSWriter(Thread):

    '''
    data/instance attributes:
    _logger
    _tosPeer
    _sendqueue
    _tosWriterStop
    '''

    def __init__(self, parent):
        Thread.__init__(self, name='%s-Thread' % (self.__class__.__name__,))
        self._logger = logging.getLogger(self.__class__.__name__)
        self._tosPeer = parent
        self._sendqueue = Queue.Queue(SEND_QUEUE_SIZE)
        self._tosWriterStop = False


    def run(self):
        self._logger.info('started')
        
        # speed optimizations
        write = self._tosPeer._serialsource.write
        get = self._sendqueue.get
        isEnabledFor = self._logger.isEnabledFor
        
        while not self._tosWriterStop:
            packet, amId, timeout, blocking, maxretries = get()
            if self._tosWriterStop:
                break
            
            try:
                write(packet, amId, timeout, blocking, maxretries)
                self._tosPeer.counterAction(self._tosPeer._msgSentCounterId)
                if isEnabledFor(logging.DEBUG):
                    self._logger.debug('snd (%d,?,%d)' % (BackLogMessage.TOS_MESSAGE_TYPE, len(packet)))
            except Exception, e:
                if not self._tosWriterStop:
                    self._logger.warning('could not write message to serial port: %s' % (e,))
            finally:
                self._sendqueue.task_done()
 
        self._logger.info('died')


    def stop(self):
        self._tosWriterStop = True
        try:
            self._sendqueue.put_nowait(('end', 0, 0, False, 1))
        except Queue.Full:
            pass
        except Exception, e:
            self._logger.exception(e)
        self._logger.info('stopped')


    def addMsg(self, packet, amId, timeout, blocking, maxretries):
        if not self._tosWriterStop:
            try:
                self._sendqueue.put_nowait((packet, amId, timeout, blocking, maxretries))
            except Queue.Full:
                self._logger.warning('TOS send queue is full')
                return False
        return True
        