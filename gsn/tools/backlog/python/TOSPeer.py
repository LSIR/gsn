__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision: 1945 $"
__date__        = "$Date: 2010-06-10 14:24:14 +0200 (Don, 10. Jun 2010) $"
__id__          = "$Id: TOSPlugin.py 1945 2010-06-10 12:24:14Z rlim $"
__source__      = "$URL: https://gsn.svn.sourceforge.net/svnroot/gsn/branches/permasense/gsn/tools/backlog/python/TOSPlugin.py $"

#import struct
import array
import logging
import time
import Queue
from threading import Thread, Event

import tos
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
    _logger 
    _stopped
    '''
    
    def __init__(self, parent, address):
        Thread.__init__(self)
        self._logger = logging.getLogger(self.__class__.__name__)
        
        # split the address (it should have the form serial@port:baudrate)
        source = address.split('@')
        if source[0] == 'serial':
            try:
                # try to open a connection to the specified serial port
                serial = tos.getSource(address, DEBUG)
                serial.setTimeout(5)
                self._serialsource = tos.AM(serial)
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

        # open accessnode queue, just in case if we closed it before...
        while not self.sendOpenQueueCommand()and not self._stopped:
            self.error('could not send OpenQueue command')
            time.sleep(5)
            
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
            payload = self.tos2backlog(packet);

            self._logger.debug('rcv (%d,%d,%d)' % (BackLogMessage.TOS_MESSAGE_TYPE, timestamp, length))

            # tell PSBackLogMain to send the packet to the plugins
            # using the serial port we can guarantee flow control to the backlog database!
            if self._parent.processTOSMsg(timestamp, payload):
                try:
                    self._serialsource.sendAck()
                except Exception, e:
                    if not self._stopped:
                        self.error('could not send ack: ' + e.__str__())

        self._logger.info('died')

    def tos2backlog(self, packet):
        # TODO: append zero at start should not really happen here -> needs bugfix for tos.py
        return array.array('B', [0] + packet.payload()).tostring()

    def sendCloseQueueCommand(self):
        if self._serialsource.write(array.array('B', [0x00, 0x00, 0x00, 0x00, 0x05, 0x22, 0x50, 0xff, 0xff, 0x80, 0x00, 0x00]).tolist(), 0x00, 0.2, True, 10):
            time.sleep(35)

    def sendOpenQueueCommand(self):
        return self._serialsource.write(array.array('B', [0x00, 0x00, 0x00, 0x00, 0x05, 0x22, 0x50, 0xff, 0xff, 0x80, 0x01, 0x00]).tolist(), 0x00, 0.2, True, 10)
            
    def sendTOSMsg(self, message):
        return self._toswriter.addMsg(message)


    def error(self, msg):
        self._parent.incrementErrorCounter()
        self._logger.error(msg)

        
    def stop(self):
        self._stopped = True
        self._toswriter.stop()
        
        # send close queue cmd to access node
        self.sendCloseQueueCommand()
        
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
                    message = self._sendqueue.get_nowait()[1]
                except Queue.Empty:
                    self._logger.warning('send queue is empty')
                    break
                
                packet = self.backlog2tos(message)
                try:
                    self._parent._serialsource.write(packet, 0x00, 0.2, True)
                    self._logger.debug('snd (%d,?,%d)' % (BackLogMessage.TOS_MESSAGE_TYPE, len(message)))
                except Exception, e:
                    if not self._stopped:
                        self._logger.warning('could not write message to serial port: ' + e.__str__())
                finally:
                    self._sendqueue.task_done()
 
        self._logger.info('died')
        

    def backlog2tos(self, message):
        return array.array('B', message[1:]).tolist()


    def stop(self):
        self._stopped = True
        self._work.set()
        self._logger.info('stopped')


    def addMsg(self, msg):
        if not self._stopped:
            try:
                self._sendqueue.put_nowait(msg)
                self._work.set()
            except Queue.Full:
                self._logger.warning('TOS send queue is full')
                self._work.set()
                return False
        return True
        