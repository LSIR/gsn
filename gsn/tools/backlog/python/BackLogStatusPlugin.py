'''
Created on Jul 15, 2009

@author: Tonio Gsell
@author: Mustafa Yuecel
'''

import struct
from threading import Event

import BackLogMessage
from AbstractPlugin import AbstractPluginClass

DEFAULT_STATUS_INTERVAL = 10.0
DEFAULT_BACKLOG = True

class BackLogStatusPluginClass(AbstractPluginClass):
    '''
    This plugin offers the functionality to receive commands from the GSN Backlog wrapper.
    It also sends BackLogStatus messages.
    
    Any new status information coming from this program should be implemented here.
    '''

    '''
    data/instance attributes:
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

        self._parent = parent
        self._stopped = False
        self._sleeper = Event()
    
    
    def getMsgType(self):
        return BackLogMessage.BACKLOG_STATUS_MESSAGE_TYPE
    
    
    def msgReceived(self, message):
        resend = ord(message[0])
        if resend == 1:
            self.info('received command resend')
            self._parent.resend()


    def run(self):
        self.info('started')
        while not self._stopped:
            self._sleeper.wait(self._interval)
            if self._sleeper.isSet():
                continue
            
            packet = struct.pack('<II', self.getErrorCounter(), self.getExceptionCounter())
            backlogstatus = self.getBackLogStatus()
            backlogdbentries = backlogstatus[0]
            backlogdbsize = backlogstatus[1]
            packet += struct.pack('<II', backlogdbentries, backlogdbsize)
            gsnpeerstatus = self.getGSNPeerStatus()
            incounter = gsnpeerstatus[0]
            outcounter = gsnpeerstatus[1]
            backlogcounter = gsnpeerstatus[2]
            packet += struct.pack('<III', incounter, outcounter, backlogcounter)
            
            self.processMsg(self.getTimeStamp(), packet, self._backlog)
            
        self.info('died')
        
    
    def stop(self):
        self._stopped = True
        self._sleeper.set()
        self.info('stopped')
