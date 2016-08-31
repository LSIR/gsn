# -*- coding: UTF-8 -*-
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"

import time
import struct
from threading import Event

import BackLogMessage
import DPPTypes
from AbstractPlugin import AbstractPluginClass

DEFAULT_BACKLOG = True

class DPPPluginClass(AbstractPluginClass):
    '''
    This plugin forwards all incoming DPP packets to GSN and vice versa.
    
    data/instance attributes:
    _plugstop
    _QueueClosedEvent
    '''
    
    def __init__(self, parent, config):
        AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG)
        
        self._plugstop = False
        self._QueueClosedEvent = Event()
        
        # register all possible packet types (except for invalid and timesync messages)
        self.registerDPPListener([DPPTypes.MSG_TYPE_INVALID, DPPTypes.MSG_TYPE_TIMESYNC], True)
        
    
    def stop(self):
        self._plugstop = True
        self.deregisterDPPListener()
        self.info('stopped')
    
    
    def dppMsgReceived(self, timestamp, dppMsg):
        return self.processMsg(timestamp, [dppMsg['device_id'], dppMsg['type'], dppMsg['payload_len'], dppMsg['seqnr'], dppMsg['generation_time'], dppMsg['payload']], self._priority, self._backlog)
    
    
    def msgReceived(self, data):
        self.sendDPPmsg(dict(device_id=data[0], type=data[1], payload_len=data[2], seqnr=data[3], payload=data[4]))
    
    
    def isBusy(self):
        return False
    
    
    def needsWLAN(self):
        return False
    