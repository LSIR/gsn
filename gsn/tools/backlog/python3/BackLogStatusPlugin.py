
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import struct

import BackLogMessage
from AbstractPlugin import AbstractPluginClass

DEFAULT_BACKLOG = True

class BackLogStatusPluginClass(AbstractPluginClass):
    '''
    This plugin offers the functionality to receive commands from the GSN Backlog wrapper.
    It also sends BackLogStatus messages.
    
    Any new status information coming from this program should be implemented here.
    '''
    
    
    def getMsgType(self):
        return BackLogMessage.BACKLOG_STATUS_MESSAGE_TYPE
        
        
    def isBusy(self):
        return False
    
    
    def msgReceived(self, message):
        if message[0] == 1:
            self.info('received command resend')
            self._backlogMain.resend()


    def action(self, parameters):
        packet = struct.pack('<i', self.getErrorCounter())
        packet += struct.pack('<i', self.getExceptionCounter())
        backlogstatus = self.getBackLogStatus()
        backlogdbentries = int(backlogstatus[0])
        backlogdbsize = int(backlogstatus[1])
        minstoretime = int(backlogstatus[2])
        maxstoretime = int(backlogstatus[3])
        meanstoretime = int(backlogstatus[4])
        minremovetime = int(backlogstatus[5])
        maxremovetime = int(backlogstatus[6])
        meanremovetime = int(backlogstatus[7])
        packet += struct.pack('<i', backlogdbentries)
        packet += struct.pack('<i', backlogdbsize)
        gsnpeerstatus = self.getGSNPeerStatus()
        incounter = int(gsnpeerstatus[0])
        outcounter = int(gsnpeerstatus[1])
        backlogcounter = int(gsnpeerstatus[2])
        connectionLosses = int(gsnpeerstatus[3])
        uptime = self.getUptime()
        packet += struct.pack('<i', incounter)
        packet += struct.pack('<i', outcounter)
        packet += struct.pack('<i', backlogcounter)
        packet += struct.pack('<i', connectionLosses)
        packet += struct.pack('<i', uptime)
        packet += struct.pack('<i', minstoretime)
        packet += struct.pack('<i', maxstoretime)
        packet += struct.pack('<i', meanstoretime)
        packet += struct.pack('<i', minremovetime)
        packet += struct.pack('<i', maxremovetime)
        packet += struct.pack('<i', meanremovetime)
        
        self.processMsg(self.getTimeStamp(), packet, self._priority, self._backlog)
