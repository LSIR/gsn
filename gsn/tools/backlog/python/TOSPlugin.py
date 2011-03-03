# -*- coding: UTF-8 -*-
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import array
import time
import tos
from threading import Event

import BackLogMessage
import TOSTypes
from AbstractPlugin import AbstractPluginClass

DEFAULT_BACKLOG = True

# Dozer Beacon Structure
TOS_DOZER_BEACON_STRUCTURE = [('destination', 'int', 2), ('cmd', 'int', 2), ('repetitionCnt','int',1)]
TOS_AM_COMMANDMSG = 0x50 
ACCESSNODE_QUEUE_CTRL_CMD = 8
ACCESSNODE_QUEUE_CTRL_CMD_VAL_CLOSE = 0
ACCESSNODE_QUEUE_CTRL_CMD_VAL_OPEN = 1
ACCESSNODE_QUEUE_CTRL_CMD_VAL_CLOSED = 2

class TOSPluginClass(AbstractPluginClass):
    '''
    This plugin forwards all incoming TOS packets to GSN and vice versa.
    
    data/instance attributes:
    _ready
    '''
    
    def __init__(self, parent, options):
        AbstractPluginClass.__init__(self, parent, options, DEFAULT_BACKLOG)
        self.registerTOSListener([TOSTypes.AM_ALL])
        
        self._plugstop = False
        self._ready = False
        self._QueueClosedEvent = Event()
    
    def run(self):
        # open accessnode queue, just in case if we closed it before...
        while not self._sendOpenQueueCommand() and not self._plugstop:
            self.error('could not send OpenQueue command')
            time.sleep(5)
            
        self._ready = True
        
    
    def stop(self):
        self._ready = False
        self._plugstop = True
        # send close queue cmd to access node
        self._sendCloseQueueCommand()
        self.deregisterTOSListener()
        self.info('stopped')
    
    
    def tosMsgReceived(self, timestamp, packet):
        if packet['type'] == TOS_AM_COMMANDMSG:
            self._logger.debug('TOS_AM_COMMANDMSG')         
            response = tos.Packet(TOS_DOZER_BEACON_STRUCTURE, packet['data'])
            self._logger.debug('rcv (cmd=' + str(response['cmd']) + ', destination=' + str(response['destination']) + ', repetition count=' +  str(response['repetitionCnt']) +')')
            if (response['cmd'] >> 12) == ACCESSNODE_QUEUE_CTRL_CMD and response['cmd'] & 0x0fff == ACCESSNODE_QUEUE_CTRL_CMD_VAL_CLOSED:
                self._logger.debug('access node queue closed')
                self._QueueClosedEvent.set()
        return self.processMsg(timestamp, self._tos2backlog(packet), self._priority, self._backlog)
        
        
    def getMsgType(self):
        return BackLogMessage.TOS_MESSAGE_TYPE
    
            
    def msgReceived(self, data):
        if self._ready:
            self.sendTOSmsg(self._backlog2tos(data[0]), 0x00, 1, True, 10)
            
            
    def isBusy(self):
        return False
        
        
    def needsWLAN(self):
        return False


    def _sendCloseQueueCommand(self):
        closecmd = tos.Packet(TOS_DOZER_BEACON_STRUCTURE, (self.getDeviceId(), (ACCESSNODE_QUEUE_CTRL_CMD << 12) + ACCESSNODE_QUEUE_CTRL_CMD_VAL_CLOSE, 1))
        if self.sendTOSmsg(closecmd, TOS_AM_COMMANDMSG, 1, True, 10):
            self.info('waiting for close queue command to be complete (timeout 35s)')
            self._QueueClosedEvent.wait(35)
            if self._QueueClosedEvent.isSet():
                self.info('queue closed')
            else:
                self.warning('queue not closed')

    def _sendOpenQueueCommand(self):
        self._QueueClosedEvent.clear();
        opencmd = tos.Packet(TOS_DOZER_BEACON_STRUCTURE, (self.getDeviceId(), (ACCESSNODE_QUEUE_CTRL_CMD << 12) + ACCESSNODE_QUEUE_CTRL_CMD_VAL_OPEN, 1))
        return self.sendTOSmsg(opencmd, TOS_AM_COMMANDMSG, 1, True, 10)
        

    def _backlog2tos(self, message):
        return array.array('B', message[1:]).tolist()


    def _tos2backlog(self, packet):
        # TODO: append zero at start should not really happen here -> needs bugfix for tos.py
        return bytearray(array.array('B', [0] + packet.payload()).tostring())
    