# -*- coding: UTF-8 -*-
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"

import socket
import struct
import time
import logging
import Queue
import array
from threading import Thread, Event, Lock

import BackLogMessage
from SpecialAPI import Statistics

# Ping request interval in seconds.
PING_INTERVAL_SEC = 30.0

# Time in seconds in which at least one ping acknowledge
# message should have been received. If no acknowledge has
# been received, the connection is considered broken.
PING_ACK_CHECK_INTERVAL_SEC = 300.0

SEND_QUEUE_SIZE = 100

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
    _gsnPeerStop
    '''

    def __init__(self, parent, deviceid, port):
        '''
        Inititalizes the GSN server.
        
        @param parent: the BackLogMain object
        @param port: the local port the server should be listening on
        
        @raise Exception: if there is a problem opening the server socket
        '''
        Thread.__init__(self, name='GSNPeer-Thread')
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
        self._gsnqueuelimitreached = False
        
        # try to open a server socket to which GSN can connect to
        try:
            self._serversocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self._serversocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self._serversocket.bind(('0.0.0.0', port))
        except Exception, e:
            raise TypeError(e.__str__())

        self._pingtimer = PingTimer(PING_INTERVAL_SEC, self.ping)
        self._pingwatchdog = PingWatchDog(PING_ACK_CHECK_INTERVAL_SEC, self.watchdogdisconnect)
        
        
    def run(self):
        try:
            self._logger.info('started')
            
            self._pingwatchdog.start()
            self._pingtimer.start()
            
            self._serversocket.listen(1)
            
            threadnr = 1
            while not self._gsnPeerStop:
                self._gsnlistener = GSNListener(self, self._port, self._serversocket, threadnr)
                threadnr = (threadnr+1)%0xFF
                if not self._gsnPeerStop:
                    self._gsnlistener.start()
                    self._gsnlistener.join()
                
            self._pingwatchdog.join()
            self._pingtimer.join()
     
            self._logger.info('died')
        except Exception, e:
            self._logger.exception(str(e), e)
            self._backlogMain.incrementExceptionCounter()


    def stop(self):
        self._gsnPeerStop = True
        self._pingwatchdog.stop()
        self._pingtimer.stop()
        self._gsnlistener.stop()
        self._serversocket.shutdown(socket.SHUT_RDWR)
        self._serversocket.close()
        self._logger.info('stopped')


    def getStatus(self):
        '''
        Returns the status of the GSN peer as a list:
        
        @return: status of the GSN peer
        '''
        stat = [self.getCounterValue(self._msgInCounterId), \
                self.getCounterValue(self._msgOutCounterId), \
                self.getCounterValue(self._msgAckInCounterId), \
                self.getCounterValue(self._pingOutCounterId), \
                self.getCounterValue(self._pingAckInCounterId), \
                self.getCounterValue(self._pingInCounterId), \
                self.getCounterValue(self._pingAckOutCounterId), \
                self.getCounterValue(self._connectionLossesId)]
        return stat
            
            
    def isConnected(self):
        return self._connected


    def sendToGSN(self, msg, priority, resend=False):
        '''
        Send message to GSN.
        
        @param blMessage: the BackLogMessage to be sent to GSN
        
        @return: True if the message could have been sent to GSN otherwise False
        '''
        if not self._connected or self._gsnPeerStop:
            return False
        if resend:
            return self._gsnlistener._gsnwriter.addResendMsg(msg, priority)        
        else:
            return self._gsnlistener._gsnwriter.addMsg(msg, priority)
        
        
    def pktReceived(self, pkt):
        try:
            # convert the packet to a BackLogMessage
            msg = BackLogMessage.BackLogMessageClass()
            msg.setMessage(pkt)
            # get the message type
            msgType = msg.getType()
            
            if self._logger.isEnabledFor(logging.DEBUG):
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
            elif msgType == BackLogMessage.MESSAGE_QUEUE_LIMIT_MESSAGE_TYPE:
                if not self._gsnqueuelimitreached:
                    self._gsnqueuelimitreached = True
                    self._backlogMain.backlog.pauseResending()
                    self._logger.info('GSN message queue reached its limit => stop sending messages')
            elif msgType == BackLogMessage.MESSAGE_QUEUE_READY_MESSAGE_TYPE:
                if self._gsnqueuelimitreached:
                    self._gsnqueuelimitreached = False
                    # let BackLogMain know that GSN successfully connected
                    self._backlogMain.backlog.resend()
                    self._backlogMain.backlog.resumeResending()
                    self._logger.info('GSN message queue is ready => send messages')
            else:
                self.counterAction(self._msgInCounterId)
                self._backlogMain.gsnMsgReceived(msgType, msg)
        except Exception, e:
            self._logger.exception('Can not process received message: %s' % (str(e),), e)
            self._backlogMain.incrementExceptionCounter()


    def disconnect(self):
        self._logger.info('connection to GSN lost')
        self._connected = False
        self.counterAction(self._connectionLossesId)
        self._backlogMain.connectionToGSNlost()
        self._pingwatchdog.pause()
        self._pingtimer.pause()
        
        
    def wlanTurnedOff(self):
        self._gsnlistener.disconnect()
        
        
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
            
        # if not blocked send the message to the GSN backend
        if not self._gsnqueuelimitreached:
            if self.sendToGSN(msg, priority):
                self.counterAction(self._msgOutCounterId)
                
        return ret


    def processResendMsg(self, msgType, timestamp, msg):
        ret = False
        if not self._gsnqueuelimitreached:
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
    _disconnectLock
    _gsnListenerStop
    _stuff
    _stuffread
    '''

    def __init__(self, parent, port, serversocket, threadnr):
        '''
        Inititalizes the GSN server.
        
        @param parent: the GSNPeer object
        @param port: the local port the server should be listening on
        
        @raise Exception: if there is a problem opening the server socket
        '''
        Thread.__init__(self, name='%s-Thread-%d' % (self.__class__.__name__,threadnr))
        
        self._logger = logging.getLogger(self.__class__.__name__)
        
        # initialize variables
        self._gsnPeer = parent
        self._port = port
        self._serversocket = serversocket
        self._stuff = False
        
        self._gsnwriter = GSNWriter(self, threadnr)

        self.clientsocket = None
        self._clientaddr = None
        self._stuffread = ''

        self._connected = False
        self._disconnectLock = Lock()
        self._gsnListenerStop = False


    def run(self):
        self._logger.debug('started')
        
        pkt_len = None
        pkt = None
        msgType = None
        msgTypeValid = None
        connecting = True
        
        # listen for a connection request by a GSN instance (this is blocking)
        self._logger.debug('listening on port %d' % (self._port,))
        try:
            (self.clientsocket, self._clientaddr) = self._serversocket.accept()
            if self._gsnListenerStop:
                self._logger.debug('died')
                return
            
            self._logger.info('got connection from %s' % (self._clientaddr,))

            self.clientsocket.settimeout(None)
            
            # speed optimizations
            pktReceived = self._gsnPeer.pktReceived

            self._connected = True
            self._gsnwriter.start()
            self._gsnwriter.sendHelloMsg()
        except (IOError, socket.error), e:
            if not self._gsnListenerStop:
                self._gsnPeer._backlogMain.incrementExceptionCounter()
                self._logger.exception('exception while accepting connection: %s' % (e,))
                self.disconnect()
            self._logger.debug('died')
            return

        try:
            while not self._gsnListenerStop:
                self._logger.debug('rcv...')
                
                if connecting:
                    try:
                        helloByte = self.pktReadAndDestuff(1)
                        if self._gsnListenerStop:
                            break;
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
                        if self._gsnListenerStop:
                            break;
                        if not pkt:
                            continue
                    
                        if len(pkt) != 4:
                            raise IOError('packet length does not match')
                    except (IOError, socket.error), e:
                        if not self._gsnListenerStop:
                            raise
                        break
                    
                    pkt_len = struct.unpack('<i', pkt)[0]
                    try:
                        pkt = self.pktReadAndDestuff(pkt_len)
                        if self._gsnListenerStop:
                            break;
                        if not pkt:
                            continue
                    
                        if len(pkt) != pkt_len:
                            raise IOError('packet length does not match')
                    except (IOError,socket.error), e:
                        if not self._gsnListenerStop:
                            raise
                        break
                    
                    pktReceived(pkt)
        except Exception, e:
            self.disconnect()
            if self._logger.isEnabledFor(logging.DEBUG):
                self._logger.debug(str(e))
            
        self._gsnwriter.join()

        self._logger.debug('died')
        
        
    def pktReadAndDestuff(self, length):
        recv = self.clientsocket.recv
        unstuffed = ''
        
        missingLen = length
        while missingLen != 0 and not self._gsnListenerStop:
            if len(self._stuffread) < missingLen:
                if (missingLen-len(self._stuffread)) < 4096:
                    l = missingLen-len(self._stuffread)
                else:
                    l = 4096
                r = recv(l)
                if not r:
                    raise IOError('None returned from socket')
                self._stuffread += r
                continue
            
            for i in range(len(self._stuffread)):
                if ord(self._stuffread[i]) != STUFFING_BYTE and not self._stuff:
                    unstuffed += self._stuffread[i]
                    missingLen -= 1
                elif self._stuff:
                    if ord(self._stuffread[i]) == STUFFING_BYTE:
                        unstuffed += self._stuffread[i]
                        missingLen -= 1
                        self._stuff = False
                    else:
                        self._logger.debug('stuffing mark reached')
                        self._stuff = False
                        self._stuffread = self._stuffread[i:]
                        return None
                else:
                    self._stuff = True
                    
            self._stuffread = ''

        return unstuffed


    def stop(self):
        self._gsnListenerStop = True
        self._gsnwriter.stop()
        if self._connected:
            try:
                try:
                    self.clientsocket.shutdown(socket.SHUT_RDWR)
                except Exception, e1:
                    self._logger.debug(str(e1))
                self.clientsocket.close()
            except Exception, e:
                self._gsnPeer._backlogMain.incrementExceptionCounter()
                self._logger.exception(str(e))
            self._connected = False
        self._logger.debug('stopped')


    def disconnect(self):
        # synchonized method, guarantee that stop is called only once
        self._disconnectLock.acquire()
        if self._connected:
            self.stop()
            self._gsnPeer.disconnect()
        self._disconnectLock.release()



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
        Thread.__init__(self, name='%s-Thread' % (self.__class__.__name__,))
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
            self._logger.debug('action')
            self._action()
            
        self._logger.info('died')
    
    
    def pause(self):
        self._wait = None
        self._timer.set()
        self._logger.debug('paused')
    
            
    def resume(self):
        self._wait = self._interval
        self._timer.set()
        self._logger.debug('resumed')
    
    
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
    _gsnWriterStop
    _emptyLock
    '''
    class HelloMessage:
        def __init__(self, helloMsg):
            self._helloMsg = helloMsg
            self._timestamp = 0
        def getMessage(self):
            return self._helloMsg
        
    

    def __init__(self, parent, threadnr):
        Thread.__init__(self, name='%s-Thread-%d' % (self.__class__.__name__,threadnr))
        self._logger = logging.getLogger(self.__class__.__name__)
        self._gsnListener = parent
        self._sendqueue = Queue.PriorityQueue(SEND_QUEUE_SIZE)
        self._stuff = chr(STUFFING_BYTE)
        self._dblstuff = self._stuff + self._stuff
        self._gsnWriterStop = False
        self._emptyLock = Lock()


    def run(self):
        self._logger.debug('started')
        
        # speed optimizations
        pack = struct.pack
        pktStuffing = self.pktStuffing
        sendall = self._gsnListener.clientsocket.sendall
        isEnabledFor = self._logger.isEnabledFor
        
        while not self._gsnWriterStop:
            msg = self._sendqueue.get()[1]
            if self._gsnWriterStop:
                self._sendqueue.task_done()
                break
        
            if isinstance(msg, BackLogMessage.BackLogMessageClass):
                message = msg.getMessage()
                msglen = len(message)
                pkt = pktStuffing(pack('<i', msglen) + message)
            elif isinstance(msg, self.HelloMessage):
                pkt = msg.getMessage()
            else:
                pkt = pktStuffing(pack('<i', len(msg)) + str(msg))
        
            try:
                sendall(pkt)
                if isEnabledFor(logging.DEBUG):
                    if isinstance(msg, BackLogMessage.BackLogMessageClass):
                        self._logger.debug('snd (%d,%d,%d)' % (msg.getType(), msg.getTimestamp(), msglen)) 
                    elif isinstance(msg, self.HelloMessage):
                        self._logger.debug('hello message sent')
            except (IOError, socket.error), e:
                try:
                    self._sendqueue.task_done()
                except ValueError, e1:
                    self.exception(e1)
                if not self._gsnWriterStop:
                    self._gsnListener.disconnect() # sets connected to false
                    if isEnabledFor(logging.DEBUG):
                        self._logger.debug(str(e))
            else:
                try:
                    self._sendqueue.task_done()
                except ValueError, e:
                    self.exception(e)
                    
        self._emptyLock.acquire()
        # to unblock addResendMsg
        self.emptyQueue()
 
        self._logger.debug('died')
        self._emptyLock.release()
        
        
    def pktStuffing(self, pkt):
        return pkt.replace(self._stuff, self._dblstuff)
        
        
    def sendHelloMsg(self):
        self.emptyQueue()
        helloMsg = chr(STUFFING_BYTE) + chr(HELLO_BYTE)
        helloMsg += self.pktStuffing(struct.pack('<i', self._gsnListener._gsnPeer._deviceid))
        self.addMsg(self.HelloMessage(helloMsg), 0)


    def stop(self):
        self._gsnWriterStop = True
        self._emptyLock.acquire()
        if self.isAlive():
            try:
                self._sendqueue.put_nowait((0, BackLogMessage.BackLogMessageClass()))
            except Queue.Full:
                pass
            except Exception, e:
                self._logger.exception(e)
        self._emptyLock.release()
        self._logger.debug('stopped')


    def emptyQueue(self):
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


    def addMsg(self, msg, priority):
        if not self._gsnWriterStop:
            try:
                self._sendqueue.put_nowait((priority, msg))
            except Queue.Full:
                pass
            except Exception, e:
                self.exception(e)
            else:
                return True
        return False

        
    def addResendMsg(self, msg, priority=100):
        if self._gsnListener._connected and not self._gsnWriterStop:
            # wait until send queue is empty
            self._sendqueue.join()
            assert self._sendqueue.not_empty != True
            if self._gsnWriterStop:
                return False
            try:
                self._sendqueue.put_nowait((priority, msg))
                return True
            except Queue.Full:
                return True
            except Exception, e:
                self.exception(e)
        return False


    def exception(self, error):
        self._gsnListener._gsnPeer._backlogMain.incrementExceptionCounter()
        self._logger.exception(str(error))
    