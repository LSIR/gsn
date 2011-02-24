
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import socket
import struct
import time
import logging
import Queue
from threading import Thread, Event, Lock

import BackLogMessage
from SpecialAPI import Statistics

# Ping request interval in seconds.
PING_INTERVAL_SEC = 10.0

# Time in seconds in which at least one ping acknowledge
# message should have been received. If no acknowledge has
# been received, the connection is considered broken.
PING_ACK_CHECK_INTERVAL_SEC = 60.0

SEND_QUEUE_SIZE = 25

STUFFING_BYTE = 0x7e
HELLO_BYTE = 0x7d

SOL_IP = 0
IP_MTU = 14

class GSNPeerClass(Thread, Statistics):
    '''
    Offers the server functionality for GSN.
    '''
    
    '''
    data/instance attributes:
    _logger
    _backlogMain
    _deviceid
    _port
    _serversocket
    _pingtimer
    _pingwatchdog
    _connected
    _msgAckInCounterId
    _pingAckInCounterId
    _pingInCounterId
    _msgInCounterId
    _pingAckOutCounterId
    _pingOutCounterId
    _msgOutCounterId
    _connectionLossesId
    _work
    _gsnPeerStop
    '''

    def __init__(self, parent, deviceid, port):
        '''
        Inititalizes the GSN server.
        
        @param parent: the BackLogMain object
        @param port: the local port the server should be listening on
        
        @raise Exception: if there is a problem opening the server socket
        '''
        Thread.__init__(self)
        Statistics.__init__(self)
        
        self._logger = logging.getLogger(self.__class__.__name__)
        
        # initialize variables
        self._backlogMain = parent
        self._deviceid = deviceid
        self._port = port

        self._msgInCounterId = self.createCounter()
        self._msgAckInCounterId = self.createCounter()
        self._pingInCounterId = self.createCounter()
        self._pingAckInCounterId = self.createCounter()
        self._msgOutCounterId = self.createCounter()
        self._pingOutCounterId = self.createCounter()
        self._pingAckOutCounterId = self.createCounter()
        self._connectionLossesId = self.createCounter()
        
        self._connected = False
        self._gsnPeerStop = False
        self._work = Event()
        
        # try to open a server socket to which GSN can connect to
        try:
            self._serversocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self._serversocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self._serversocket.bind(('0.0.0.0', port))
            self._serversocket.listen(1)
        except Exception, e:
            raise TypeError(e.__str__())

        self._pingtimer = PingTimer(PING_INTERVAL_SEC, self.ping)
        self._pingwatchdog = PingWatchDog(PING_ACK_CHECK_INTERVAL_SEC, self.watchdogdisconnect)
        
        
    def run(self):
        self._logger.info('started')
        
        self._pingwatchdog.start()
        self._pingtimer.start()
        self._work.set()
            
        while not self._gsnPeerStop:
            self._work.wait()
            if self._gsnPeerStop:
                break
            self._work.clear()
        
            self._gsnlistener = GSNListener(self, self._port, self._serversocket)
            if not self._gsnPeerStop:
                self._gsnlistener.start()
                
        self._pingwatchdog.join()
        self._pingtimer.join()
        self._gsnlistener.join()
 
        self._logger.info('died')


    def stop(self):
        self._pingwatchdog.stop()
        self._pingtimer.stop()
        self._gsnlistener.stop()
        self._gsnPeerStop = True
        self._work.set()
        self._serversocket.shutdown(socket.SHUT_RDWR)
        self._serversocket.close()
        self._logger.info('stopped')


    def getStatus(self):
        '''
        Returns the status of the GSN peer as a list:
        
        @return: status of the GSN peer
        '''
        stat = []
        stat.append(self.getCounterValue(self._msgInCounterId))
        stat.append(self.getCounterValue(self._msgOutCounterId))
        stat.append(self.getCounterValue(self._msgAckInCounterId))
        stat.append(self.getCounterValue(self._pingOutCounterId))
        stat.append(self.getCounterValue(self._pingAckInCounterId))
        stat.append(self.getCounterValue(self._pingInCounterId))
        stat.append(self.getCounterValue(self._pingAckOutCounterId))
        stat.append(self.getCounterValue(self._connectionLossesId))
        return stat
            
            
    def isConnected(self):
        return self._connected


    def sendToGSN(self, msg, priority, resend=False):
        '''
        Send message to GSN.
        
        @param blMessage: the BackLogMessage to be sent to GSN
        
        @return: True if the message could have been sent to GSN otherwise False
        '''
        if not self._connected:
            return False
        if resend:
            return self._gsnlistener._gsnwriter.addResendMsg(msg, priority)        
        else:
            return self._gsnlistener._gsnwriter.addMsg(msg, priority)
        
        
    def pktReceived(self, pkt):
        # convert the packet to a BackLogMessage
        msg = BackLogMessage.BackLogMessageClass()
        msg.setMessage(pkt)
        # get the message type
        msgType = msg.getType()
        
        self._logger.debug('rcv (%d,%d,%d)' % (msgType, msg.getTimestamp(), len(pkt)))
        
        # is it an answer to a ping?
        if msgType == BackLogMessage.PING_ACK_MESSAGE_TYPE:
            self.counterAction(self._pingAckInCounterId)
            self._pingwatchdog.reset()
        # or is it a ping request
        elif msgType == BackLogMessage.PING_MESSAGE_TYPE:
            self.counterAction(self._pingInCounterId)
            # answer with a ping ack
            self.pingAck(msg.getTimestamp())
        elif msgType == BackLogMessage.ACK_MESSAGE_TYPE:
            self.counterAction(self._msgAckInCounterId)
            # if it is an acknowledge, tell BackLogMain to have received one
            self._backlogMain.ackReceived(msg.getTimestamp(), msg.getData()[0])
        else:
            self.counterAction(self._msgInCounterId)
            self._backlogMain.gsnMsgReceived(msgType, msg)


    def disconnect(self):
        self._logger.info('connection to GSN lost')
        self._connected = False
        self.counterAction(self._connectionLossesId)
        self._backlogMain.connectionToGSNlost()
        self._pingwatchdog.pause()
        self._pingtimer.pause()
        self._work.set()
        
        
    def watchdogdisconnect(self):
        self._gsnlistener.disconnect()
        
        
    def connected(self):
        self._connected = True
        self._backlogMain.connectionToGSNestablished()
        self._pingtimer.resume()
        self._pingwatchdog.resume()


    def ping(self):
        if self.sendToGSN(BackLogMessage.BackLogMessageClass(BackLogMessage.PING_MESSAGE_TYPE, int(time.time()*1000)), 0):
            self.counterAction(self._pingOutCounterId)


    def pingAck(self, timestamp):
        if self.sendToGSN(BackLogMessage.BackLogMessageClass(BackLogMessage.PING_ACK_MESSAGE_TYPE, timestamp), 0):
            self.counterAction(self._pingAckOutCounterId)


    def processMsg(self, msgType, timestamp, payload, priority, backlog=False):
        '''
        Store the message in the backlog database if needed and try to send
        it to GSN.
        
        Send the message using the GSNServer class.
        This function should be used by the plugins to send any data to GSN.
        
        @param msgType: the message type. The message type must be listed in BackLogMessage.
        @param timestamp: the timestamp this message has been generated
        @param payload: payload of the message as a byte array or a list.
                         Should not be bigger than MAX_PAYLOAD_SIZE.
        @param backLog: True if this message should be backlogged in the database, otherwise False.
                        BackLogMessageside has to send an acknowledge to remove this message from
                        the backlog database after successful processing if set to True.
        @param backup: True if this message should be stored in the backup database, otherwise False.
                       The message will only be stored in the backup database, if this is set to
                       True AND BackLogMain has been started with the '--backup' option.
                       
        @return: True if the message has been stored successfully into the backlog database if needed,
                 otherwise False.
                 
        @raise ValueError: if something is wrong with the format of the payload.
        '''
        ret = True
        msg = BackLogMessage.BackLogMessageClass(msgType, timestamp, payload)
        
        if backlog:
            # back log the message
            ret = self._backlogMain.backlog.storeMsg(timestamp, msgType, msg.getMessage())
            
        # send the message to the GSN backend
        if self.sendToGSN(msg, priority):
            self.counterAction(self._msgOutCounterId)
                
        return ret


    def processResendMsg(self, msgType, timestamp, msg):
        ret = self.sendToGSN(msg, 99, True)
        if ret:
            self.counterAction(self._msgOutCounterId)
        return ret
    
    
    
    
class GSNListener(Thread):
    '''
    Offers the listener functionality for GSN.
    '''
    
    '''
    data/instance attributes:
    _logger
    _gsnPeer
    _port
    _serversocket
    _gsnwriter
    clientsocket
    _connected
    _clientaddr
    _lock
    _gsnListenerStop
    _stuff
    _stuffread
    '''

    def __init__(self, parent, port, serversocket):
        '''
        Inititalizes the GSN server.
        
        @param parent: the GSNPeer object
        @param port: the local port the server should be listening on
        
        @raise Exception: if there is a problem opening the server socket
        '''
        Thread.__init__(self)
        
        self._logger = logging.getLogger(self.__class__.__name__)
        
        # initialize variables
        self._gsnPeer = parent
        self._port = port
        self._serversocket = serversocket
        self._stuff = False
        
        self._gsnwriter = GSNWriter(self)

        self.clientsocket = None
        self._clientaddr = None
        self._stuffread = ''

        self._connected = False
        self._lock = Lock()
        self._gsnListenerStop = False


    def run(self):
        self._logger.info('started')
        # thread is waiting for the first resume to continue
        self._gsnwriter.start()
        
        pkt_len = None
        pkt = None
        msgType = None
        msgTypeValid = None
        connecting = True
        
        # listen for a connection request by a GSN instance (this is blocking)
        self._logger.info('listening on port ' + str(self._port))
        try:
            (self.clientsocket, self._clientaddr) = self._serversocket.accept()
            if self._gsnListenerStop:
                self._logger.info('died')
                return
            self._connected = True
            self._gsnwriter.sendHelloMsg()
        except (IOError, socket.error), e:
            if not self._gsnListenerStop:
                self._gsnPeer._backlogMain.incrementExceptionCounter()
                self._logger.exception('exception while accepting connection: ' + str(e))
                self.disconnect()
            self._logger.info('died')
            return

        try:
            self._logger.info('got connection from ' + str(self._clientaddr))

            self.clientsocket.settimeout(None)

            # let BackLogMain know that GSN successfully connected
            self._gsnPeer._backlogMain.backlog.resend(True)

            while not self._gsnListenerStop:
                self._logger.debug('rcv...')
                
                if connecting:
                    try:
                        helloByte = self.pktReadAndDestuff(1)
                        if not helloByte:
                            continue
                    
                        if len(helloByte) != 1:
                            raise IOError('packet length does not match')
                    except (IOError, socket.error), e:
                        if not self._gsnListenerStop:
                            raise
                        break
                    
                    if ord(helloByte) != HELLO_BYTE:
                        raise IOError('hello byte does not match')
                    else:
                        connecting = False
                        self._logger.debug('hello byte received')
                        self._gsnPeer.connected()
                else:
                    # read the length (4 bytes) of the incoming packet (this is blocking)
                    try:
                        pkt = self.pktReadAndDestuff(4)
                        if not pkt:
                            continue
                    
                        if len(pkt) != 4:
                            raise IOError('packet length does not match')
                    except (IOError, socket.error), e:
                        if not self._gsnListenerStop:
                            raise
                        break
                    
                    pkt_len = int(struct.unpack('<i', pkt)[0])
                    try:
                        pkt = self.pktReadAndDestuff(pkt_len)
                        if not pkt:
                            continue
                    
                        if len(pkt) != pkt_len:
                            raise IOError('packet length does not match')
                    except (IOError,socket.error), e:
                        if not self._gsnListenerStop:
                            raise
                        break
                    
                    self._gsnPeer.pktReceived(pkt)
        except Exception, e:
            self.disconnect()
            self._logger.debug(str(e))
            
        self._gsnwriter.join()

        self._logger.info('died')
        
        
    def pktReadAndDestuff(self, length):
        out = self._stuffread
        if length == 1 and out:
            self._stuffread = ''
            return out
        while True:
            c = self.clientsocket.recv(1)
            if not c:
                raise IOError('None returned from socket')
            
            if ord(c) == STUFFING_BYTE and not self._stuff:
                self._stuff = True
            elif self._stuff:
                if ord(c) == STUFFING_BYTE:
                    out += c
                    self._stuff = False
                else:
                    self._logger.debug('stuffing mark reached')
                    self._stuff = False
                    self._stuffread = c
                    return None
            else:
                out += c
            
            if len(out) == length:
                break

        self._stuffread = ''
        return out


    def stop(self):
        self._gsnListenerStop = True
        self._gsnwriter.stop()
        if self._connected:
            try:
                self.clientsocket.close()
            except Exception, e:
                self._gsnPeer._backlogMain.incrementExceptionCounter()
                self._logger.exception(str(e))
            self._connected = False
        self._logger.info('stopped')


    def disconnect(self):
        # synchonized method, guarantee that stop is called only once
        self._lock.acquire()
        if self._connected:
            self.stop()
            self._gsnPeer.disconnect()
        self._lock.release()



class PingTimer(Thread):
    
    '''
    data/instance attributes:
    _logger
    _interval
    _action
    _wait
    _timer
    _pingTimerStop
    '''
    
    def __init__(self, interval, action):
        Thread.__init__(self)
        self._logger = logging.getLogger(self.__class__.__name__)
        self._interval = interval
        self._action = action
        self._wait = None
        self._timer = Event()
        self._pingTimerStop = False
        
           
    def run(self):
        self._logger.info('started')
        # wait for first resume
        self._timer.wait()
        self._timer.clear()
        while not self._pingTimerStop:
            self._timer.wait(self._wait)
            if self._timer.isSet():
                self._timer.clear()
                continue
            self._action()
            self._logger.debug('action')
            
        self._logger.info('died')
    
    
    def pause(self):
        self._wait = None
        self._timer.set()
        self._logger.info('paused')
    
            
    def resume(self):
        self._wait = self._interval
        self._timer.set()
        self._logger.info('resumed')
    
    
    def stop(self):
        self._pingTimerStop = True
        self._timer.set()
        self._logger.info('stopped')



class PingWatchDog(PingTimer):
    
    def reset(self):
        self._timer.set()
        self._logger.debug('reset')



class GSNWriter(Thread):

    '''
    data/instance attributes:
    _logger
    _gsnListener
    _sendqueue
    _work
    _gsnWriterStop
    '''
    class HelloMessage:
        def __init__(self, helloMsg):
            self._helloMsg = helloMsg
        def getMessage(self):
            return self._helloMsg
        
    

    def __init__(self, parent):
        Thread.__init__(self)
        self._logger = logging.getLogger(self.__class__.__name__)
        self._gsnListener = parent
        self._sendqueue = Queue.PriorityQueue(SEND_QUEUE_SIZE)
        self._work = Event()
        self._lock = Lock()
        self._stuff = chr(STUFFING_BYTE)
        self._dblstuff = self._stuff + self._stuff
        self._gsnWriterStop = False


    def run(self):
        self._logger.info('started')
        while not self._gsnWriterStop:
            self._work.wait()
            if self._gsnWriterStop:
                break
            self._work.clear()
            # is there something to do?
            while self._gsnListener._connected and not self._sendqueue.empty() and not self._gsnWriterStop:
                self._lock.acquire()
                try:
                    msg = self._sendqueue.get_nowait()[1]
                except Queue.Empty:
                    self._logger.warning('send queue is empty')
                    self._lock.release()
                    break
            
                if isinstance(msg, BackLogMessage.BackLogMessageClass):
                    message = msg.getMessage()
                    msglen = len(message)
                    pkt = self.pktStuffing(struct.pack('<i', msglen) + message)
                elif isinstance(msg, self.HelloMessage):
                    pkt = msg.getMessage()
                else:
                    pkt = self.pktStuffing(struct.pack('<i', len(msg)) + str(msg))
            
                try:
                    self._gsnListener.clientsocket.sendall(pkt)
                    if isinstance(msg, BackLogMessage.BackLogMessageClass):
                        self._logger.debug('snd (%d,%d,%d)' % (msg.getType(), msg.getTimestamp(), msglen)) 
                    elif isinstance(msg, self.HelloMessage):
                        self._logger.debug('hello message sent')
                except (IOError, socket.error), e:
                    try:
                        self._sendqueue.task_done()
                    except ValueError, e1:
                        self.exception(e1)
                    self._lock.release()
                    if not self._gsnWriterStop:
                        self._gsnListener.disconnect() # sets connected to false
                        self._logger.error(str(e))
                else:
                    try:
                        self._sendqueue.task_done()
                    except ValueError, e:
                        self.exception(e)
                    self._lock.release()
 
        self._logger.info('died')
        
        
    def pktStuffing(self, pkt):
        return pkt.replace(self._stuff, self._dblstuff)
        
        
    def sendHelloMsg(self):
        self.emptyQueue()
        helloMsg = chr(STUFFING_BYTE) + chr(HELLO_BYTE)
        helloMsg += self.pktStuffing(struct.pack('<i', self._gsnListener._gsnPeer._deviceid))
        self.addMsg(self.HelloMessage(helloMsg), 0)


    def stop(self):
        self._gsnWriterStop = True
        self._work.set()
        self.emptyQueue() # to unblock addResendMsg
        self._logger.info('stopped')


    def emptyQueue(self):
        self._lock.acquire()
        while not self._sendqueue.empty():
            try:
                self._sendqueue.get_nowait()
            except Queue.Empty:
                self._logger.warning('send queue is empty (emptyQueue)')
                break
            try:
                self._sendqueue.task_done()
            except ValueError, e:
                self.exception(e)
        self._lock.release()


    def addMsg(self, msg, priority):
        if self._gsnListener._connected and not self._gsnWriterStop:
            try:
                self._sendqueue.put_nowait((priority, msg))
            except Queue.Full:
                self._logger.warning('send queue is full')
            except Exception, e:
                self.exception(e)
            else:
                self._work.set()
                return True
        return False

        
    def addResendMsg(self, msg, priority=100):
        # wait until send queue is empty
        self._sendqueue.join()
        assert self._sendqueue.not_empty != True
        if self._gsnListener._connected and not self._gsnWriterStop:
            try:
                self._sendqueue.put_nowait((priority, msg))
            except Queue.Full:
                self._logger.warning('send queue is full (resend)')
            except Exception, e:
                self.exception(e)
            else:
                self._work.set()
                return True
        return False


    def exception(self, error):
        self._gsnListener._gsnPeer._backlogMain.incrementExceptionCounter()
        self._logger.exception(str(error))
    