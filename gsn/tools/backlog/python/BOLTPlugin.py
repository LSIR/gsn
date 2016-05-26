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
import BOLTTypes
from AbstractPlugin import AbstractPluginClass

DEFAULT_BACKLOG = True

class BOLTPluginClass(AbstractPluginClass):
    '''
    This plugin forwards all incoming BOLT packets to GSN and vice versa.
    
    data/instance attributes:
    _plugstop
    _QueueClosedEvent
    '''
    
    def __init__(self, parent, config):
        AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG)
        
        self._plugstop = False
        self._QueueClosedEvent = Event()
        
        # register all possible packet types (except for invalid and timesync messages)
        self.registerBOLTListener([BOLTTypes.MSG_TYPE_INVALID, BOLTTypes.MSG_TYPE_TIMESYNC], True)
        
    
    def stop(self):
        self._plugstop = True
        self.deregisterBOLTListener()
        self.info('stopped')
    
    
    def boltMsgReceived(self, timestamp, boltMsg):
        return self.processMsg(timestamp, [boltMsg['device_id'], boltMsg['type'], boltMsg['payload_len'], boltMsg['seqnr'], boltMsg['generation_time'], boltMsg['payload']], self._priority, self._backlog)
    
    
    def msgReceived(self, data):
        self.sendBOLTmsg(dict(device_id=data[0], type=data[1], payload_len=data[2], seqnr=data[3], generation_time=data[4], payload=data[5]))
    
    
    def isBusy(self):
        return False
    
    
    def needsWLAN(self):
        return False
    
    
    def _backlog2bolt(self, message):
        return array.array('B', message[1:]).tolist()
    