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

DEFAULT_BACKLOG = True

SEND_QUEUE_SIZE = 25

DEBUG = False

class TOSPeerClass(Thread):
    '''
    Offers the functionality to communicate with a node running TOS.
    '''

    '''
    data/instance attributes:
    _serialsource
    _toswriter
    _version
    _logger 
    _stopped
    '''
    
    def __init__(self, parent, address, version):
        Thread.__init__(self)
        self._logger = logging.getLogger(self.__class__.__name__)
        
        # split the address (it should have the form serial@port:baudrate)
        source = address.split('@')
        if source[0] == 'serial':
            try:
                # try to open a connection to the specified serial port
                if version == 2:
                    serial = tos.getSource(address, DEBUG)
                    serial.setTimeout(5)
                    self._serialsource = tos.AM(serial)
                elif version == 1:
                    serial = tos1x.getSource(address, DEBUG)
                    serial.setTimeout(5)
                    self._serialsource = tos1x.AM(serial)
            except Exception, e:
                raise TypeError('could not initialize serial source: ' + e.__str__())
        else:
            raise TypeError('address type must be serial')
        
        self._toswriter = TOSWriter(self)

        self._parent = parent
        self._stopped = False
            
        
    def run(self):
        self._logger.info('started')

        self._serialsource.start()
        self._toswriter.start()
        
        while not self._stopped:
            # read packet from serial port (this is blocking)
            try:
                self._logger.debug('rcv...')
                packet = self._serialsource.read()
            except Exception, e:
                if not self._stopped:
                    self.error('could not read from serial source: ' + e.__str__())
                continue
            
            # if the packet is None just continue
            if not packet:
                #self._logger.debug('read packet None')
                continue
        
            timestamp = int(time.time()*1000)

            length = len(packet.payload())

            self._logger.debug('rcv (?,%d,%d)' % (timestamp, length))

            # tell PSBackLogMain to send the packet to the plugins
            # using the serial port we can guarantee flow control to the backlog database!
            if self._parent.processTOSMsg(timestamp, packet):
                try:
                    self._serialsource.sendAck()
                except Exception, e:
                    if not self._stopped:
                        self.error('could not send ack: ' + e.__str__())

        self._logger.info('died')
            
    def sendTOSMsg(self, packet, amId, timeout=None, blocking=True, maxretries = None):
        return self._toswriter.addMsg(packet, amId, timeout, blocking, maxretries)


    def error(self, msg):
        self._parent.incrementErrorCounter()
        self._logger.error(msg)

        
    def stop(self):
        self._stopped = True
        self._toswriter.stop()
        self._serialsource.stop()
        self._logger.info('stopped')



class TOSWriter(Thread):

    '''
    data/instance attributes:
    _logger
    _parent
    _sendqueue
    _work
    _stopped
    '''

    def __init__(self, parent):
        Thread.__init__(self)
        self._logger = logging.getLogger(self.__class__.__name__)
        self._parent = parent
        self._sendqueue = Queue.Queue(SEND_QUEUE_SIZE)
        self._work = Event()
        self._stopped = False


    def run(self):
        self._logger.info('started')
        while not self._stopped:
            self._work.wait()
            if self._stopped:
                break
            self._work.clear()
            # is there something to do?
            while not self._sendqueue.empty() and not self._stopped:
                try:
                    list = self._sendqueue.get_nowait()
                except Queue.Empty:
                    self._logger.warning('send queue is empty')
                    break
                
                try:
                    self._parent._serialsource.write(list[0], list[1], list[2], list[3], list[4])
                    self._logger.debug('snd (%d,?,%d)' % (BackLogMessage.TOS_MESSAGE_TYPE, len(list[0])))
                except Exception, e:
                    if not self._stopped:
                        self._logger.warning('could not write message to serial port: ' + e.__str__())
                finally:
                    self._sendqueue.task_done()
 
        self._logger.info('died')


    def stop(self):
        self._stopped = True
        self._work.set()
        self._logger.info('stopped')


    def addMsg(self, packet, amId, timeout, blocking, maxretries):
        if not self._stopped:
            try:
                self._sendqueue.put_nowait((packet, amId, timeout, blocking, maxretries))
                self._work.set()
            except Queue.Full:
                self._logger.warning('TOS send queue is full')
                self._work.set()
                return False
        return True
        