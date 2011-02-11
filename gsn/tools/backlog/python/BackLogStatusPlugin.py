
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import struct
import sys
import resource
import threading

import BackLogMessage
from AbstractPlugin import AbstractPluginClass

DEFAULT_BACKLOG = True

class BackLogStatusPluginClass(AbstractPluginClass):
    '''
    This plugin offers the functionality to receive commands from the GSN Backlog wrapper.
    It also sends BackLogStatus messages.
    
    Any new status information coming from this program should be implemented here.
    '''

    '''
    _timer
    _interval
    '''
    
    def __init__(self, parent, options):
        AbstractPluginClass.__init__(self, parent, options, DEFAULT_BACKLOG)
        self._timer = None
        
        self._sleeper = threading.Event()
        self._stopped = False
        
        value = self.getOptionValue('poll_interval')
        if value is None:
            self._interval = None
        else:
            self._interval = float(value)
        
        self.info('interval: ' + str(self._interval))
    
    
    def getMsgType(self):
        return BackLogMessage.BACKLOG_STATUS_MESSAGE_TYPE
        
        
    def isBusy(self):
        return False
        
        
    def needsWLAN(self):
        return False
    
    
    def msgReceived(self, data):
        if data[0] == 1:
            self.info('received command resend')
            self._backlogMain.resend()
       
        
    def run(self):
        self.info('started')
        if self._interval != None:
            while not self._stopped:
                self._sleeper.wait(self._interval)
                if self._sleeper.isSet():
                    continue
                self.action('')
            self.info('died')


    def action(self, parameters):
        if self._timer:
            self._timer.cancel()
        
        paramlist = parameters.split()
        if paramlist:
            if paramlist[0].isdigit():
                self._timer = threading.Timer(int(paramlist[0]), self.action, [''])
                self._timer.start()
            else:
                self.error('parameter has to be a digit (parameter=' + parameters + ')')
        
        # TODO: parameter?
        backlogstatus = self.getBackLogDBStatus(30)
        
        backlogdbentries = backlogstatus[0]
        backlogdbsize = backlogstatus[1]
        storepersec = backlogstatus[2]
        removepersec = backlogstatus[3]
        storecounter = backlogstatus[4]
        removecounter = backlogstatus[5]
        minstoretime = backlogstatus[6]
        meanstoretime = backlogstatus[7]
        maxstoretime = backlogstatus[8]
        minremovetime = backlogstatus[9]
        meanremovetime = backlogstatus[10]
        maxremovetime = backlogstatus[11]

        # TODO: parameter?
        gsnpeerstatus = self.getGSNPeerStatus(30)
        
        msgInPerSec = gsnpeerstatus[0]
        msgOutPerSec = gsnpeerstatus[1]
        msgInCounter = gsnpeerstatus[2]
        msgOutCounter = gsnpeerstatus[3]
        msgAckInCounter = gsnpeerstatus[4]
        pingOutCounter = gsnpeerstatus[5]
        pingAckInCounter = gsnpeerstatus[6]
        pingInCounter = gsnpeerstatus[7]
        pingAckOutCounter = gsnpeerstatus[8]
        connectionLosses = gsnpeerstatus[9]
        
        usage = [None]*16
        
        try:
            r = resource.getrusage(resource.RUSAGE_SELF)
            for i in range(len(r)):
                usage[i] = r[i]
        except Exception, e:
            self.exception(e)
        
        payload = [self.getUptime(), self.getErrorCounter(), self.getExceptionCounter(), threading.active_count()]
        payload += [msgInPerSec, msgOutPerSec, msgInCounter, msgOutCounter, msgAckInCounter, pingOutCounter, pingAckInCounter, pingInCounter, pingAckOutCounter, connectionLosses]
        payload += [backlogdbentries, backlogdbsize, storepersec, removepersec, storecounter, removecounter, minstoretime, meanstoretime, maxstoretime, minremovetime, meanremovetime, maxremovetime]
        payload += usage
        
        self.processMsg(self.getTimeStamp(), payload)
            
    
    
    def _sendInitStats(self):
        # TODO: what exactly to read
        sys.version
    
    
    def stop(self):
        self._stopped = True
        self._sleeper.set()
        if self._timer:
            self._timer.cancel()
        self.info('stopped')
