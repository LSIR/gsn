
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision: 2381 $"
__date__        = "$Date: 2010-11-15 14:51:38 +0100 (Mon, 15. Nov 2010) $"
__id__          = "$Id: GSNPeer.py 2381 2010-11-15 13:51:38Z tgsell $"
__source__      = "$URL: https://gsn.svn.sourceforge.net/svnroot/gsn/branches/permasense/gsn/tools/backlog/python/GSNPeer.py $"

import socket
import struct
import time
import logging
import queue
from threading import Thread, Event, Lock

import BackLogMessage

# Ping request interval in seconds.
PING_INTERVAL_SEC = 10.0

# Time in seconds in which at least one ping acknowledge
# message should have been received. If no acknowledge has
# been received, the connection is considered broken.
PING_ACK_CHECK_INTERVAL_SEC = 60.0

SEND_QUEUE_SIZE = 25

STUFFING_BYTE = b'\x7e'
HELLO_BYTE = b'\x7d'

SOL_IP = 0
IP_MTU = 14

class GSNPeerClass(Thread):
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
    _inCounter
    _outCounter
    _connectionLosses
    _backlogCounter
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
        
        self._logger = logging.getLogger(self.__class__.__name__)
        
        # initialize variables
        self._backlogMain = parent
        self._deviceid = deviceid
        self._port = port

        self._inCounter = 0
        self._outCounter = 0
        self._connectionLosses = 0
        self._backlogCounter = 0
        self._connected = False
        self._gsnPeerStop = False
        self._work = Event()
        
        # try to open a server socket to which GSN can connect to
        try:
            self._serversocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self._serversocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self._serversocket.bind(('0.0.0.0', port))
            self._serversocket.listen(1)
        except Exception as e:
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
        return (self._inCounter, self._outCounter, self._backlogCounter, self._connectionLosses)
            
            
    def isConnected(self):
        return self._connected


    def sendToGSN(self, msg, priority, resend=False):
        '''
        Send message to GSN.
        
        @param blMessage: the BackLogMessage to be sent to GSN
        
        @return: True if the message could have been sent to GSN otherwise False
        '''        
        self._outCounter += 1
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
        self._inCounter += 1
        
        # is it an answer to a ping?
        if msgType == BackLogMessage.PING_ACK_MESSAGE_TYPE:
            self._pingwatchdog.reset()
        # or is it a ping request
        elif msgType == BackLogMessage.PING_MESSAGE_TYPE:
            # answer with a ping ack
            self.pingAck(msg.getTimestamp())
        elif msgType == BackLogMessage.ACK_MESSAGE_TYPE:
            # if it is an acknowledge, tell BackLogMain to have received one
            self._backlogMain.ackReceived(msg.getTimestamp(), struct.unpack('<I', msg.getPayload())[0])
        else:
            self._backlogMain.gsnMsgReceived(msgType, msg)


    def disconnect(self):
        self._logger.debug('disconnect')
        self._connected = False
        self._connectionLosses += 1
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
        self.sendToGSN(BackLogMessage.BackLogMessageClass(BackLogMessage.PING_MESSAGE_TYPE, int(time.time()*1000)), 0)


    def pingAck(self, timestamp):
        self.sendToGSN(BackLogMessage.BackLogMessageClass(BackLogMessage.PING_ACK_MESSAGE_TYPE, timestamp), 0)


    def processMsg(self, msgType, timestamp, payload, priority, backlog=False):
        '''
        Store the message in the backlog database if needed and try to send
        it to GSN.
        
        Send the message using the GSNServer class.
        This function should be used by the plugins to send any data to GSN.
        
        @param msgType: the message type. The message type must be listed in BackLogMessage.
        @param timestamp: the timestamp this message has been generated
        @param payload: the raw data to be sent (no more than 4 Gb)
        @param backLog: True if this message should be backlogged in the database, otherwise False.
                        BackLogMessageside has to send an acknowledge to remove this message from
                        the backlog database after successful processing if set to True.
        @param backup: True if this message should be stored in the backup database, otherwise False.
                       The message will only be stored in the backup database, if this is set to
                       True AND BackLogMain has been started with the '--backup' option.
                       
        @return: True if the message has been stored successfully into the backlog database if needed,
                 otherwise False.
        '''
        ret = True
        
        if backlog:
            self._backlogCounter += 1
            # back log the message
            ret = self._backlogMain.backlog.storeMsg(timestamp, msgType, payload)
            
        # send the message to the GSN backend
        self.sendToGSN(BackLogMessage.BackLogMessageClass(msgType, timestamp, payload), priority)
                
        return ret


    def processResendMsg(self, msgType, timestamp, payload):
        return self.sendToGSN(BackLogMessage.BackLogMessageClass(msgType, timestamp, payload), 99, True)
    
    
    
    
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
        self._stuffread = b''

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
        except (IOError, socket.error) as e:
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
                    except (IOError, socket.error) as e:
                        if not self._gsnListenerStop:
                            raise
                        break
                    
                    if helloByte != HELLO_BYTE:
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
                    except (IOError, socket.error) as e:
                        if not self._gsnListenerStop:
                            raise
                        break
                    
                    pkt_len = struct.unpack('<I', pkt)[0]
                    try:
                        pkt = self.pktReadAndDestuff(pkt_len)
                        if not pkt:
                            continue
                    
                        if len(pkt) != pkt_len:
                            raise IOError('packet length does not match')
                    except (IOError,socket.error) as e:
                        if not self._gsnListenerStop:
                            raise
                        break
                    
                    self._gsnPeer.pktReceived(pkt)
        except Exception as e:
            self.disconnect()
            self._logger.error(str(e))
            
        self._gsnwriter.join()

        self._logger.info('died')
        
        
    def pktReadAndDestuff(self, length):
        out = self._stuffread
        if length == 1 and out:
            self._stuffread = b''
            return out
        while True:
            c = self.clientsocket.recv(1)
            if not c:
                raise IOError('None returned from socket')
            
            if c == STUFFING_BYTE and not self._stuff:
                self._stuff = True
            elif self._stuff:
                if c == STUFFING_BYTE:
                    out += c
                    self._stuff = False
                else:
                    self._logger.warn('stuffing mark reached')
                    self._stuff = False
                    self._stuffread = c
                    return None
            else:
                out += c
            
            if len(out) == length:
                break

        self._stuffread = b''
        return out


    def stop(self):
        self._gsnListenerStop = True
        self._gsnwriter.stop()
        if self._connected:
            try:
                self.clientsocket.close()
            except Exception as e:
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

    def __init__(self, parent):
        Thread.__init__(self)
        self._logger = logging.getLogger(self.__class__.__name__)
        self._gsnListener = parent
        self._sendqueue = queue.PriorityQueue(SEND_QUEUE_SIZE)
        self._work = Event()
        self._lock = Lock()
        self._stuff = STUFFING_BYTE
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
                except queue.Empty:
                    self._logger.warning('send queue is empty')
                    self._lock.release()
                    break
            
                if msg.__class__.__name__ != BackLogMessage.__name__ + 'Class':
                    pkt = msg
                else:
                    message = msg.getMessage()
                    msglen = len(message)
                    pkt = self.pktStuffing(struct.pack('<I', msglen) + message)
            
                try:
                    self._gsnListener.clientsocket.sendall(pkt)
                    if msg.__class__.__name__ != BackLogMessage.__name__ + 'Class':
                        self._logger.debug('hello message sent')
                    else:
                        self._logger.debug('snd (%d,%d,%d)' % (msg.getType(), msg.getTimestamp(), msglen)) 
                except (IOError, socket.error) as e:
                    try:
                        self._sendqueue.task_done()
                    except ValueError as e1:
                        self.exception(e1)
                    self._lock.release()
                    if not self._gsnWriterStop:
                        self._gsnListener.disconnect() # sets connected to false
                        self._logger.error(str(e))
                else:
                    try:
                        self._sendqueue.task_done()
                    except ValueError as e:
                        self.exception(e)
                    self._lock.release()
 
        self._logger.info('died')
        
        
    def pktStuffing(self, pkt):
        return pkt.replace(self._stuff, self._dblstuff)
        
        
    def sendHelloMsg(self):
        self.emptyQueue()
        helloMsg = STUFFING_BYTE + HELLO_BYTE
        helloMsg += self.pktStuffing(struct.pack('<I', self._gsnListener._gsnPeer._deviceid))
        self.addMsg(helloMsg, 0)


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
            except queue.Empty:
                self._logger.warning('send queue is empty (emptyQueue)')
                break
            try:
                self._sendqueue.task_done()
            except ValueError as e:
                self.exception(e)
        self._lock.release()


    def addMsg(self, msg, priority):
        if self._gsnListener._connected and not self._gsnWriterStop:
            try:
                self._sendqueue.put_nowait((priority, msg))
            except queue.Full:
                self._logger.warning('send queue is full')
            except Exception as e:
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
            except queue.Full:
                self._logger.warning('send queue is full (resend)')
            except Exception as e:
                self.exception(e)
            else:
                self._work.set()
                return True
        return False


    def exception(self, error):
        self._gsnListener._gsnPeer._backlogMain.incrementExceptionCounter()
        self._logger.exception(str(error))
    