#! /usr/bin/python
# -*- coding: UTF-8 -*-
__author__      = "David Hasenfratz <hasenfratz@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2011, ETH Zurich, Switzerland, David Hasenfratz"
__license__     = "GPL"
__version__     = "$Revision: 4007 $"
__date__        = "$Date: 2012-03-22 15:25:24 +0100 (Thu, 22 Mar 2012) $"
__id__          = "$Id: CONO2Plugin.py 4007 2012-03-22 14:25:24Z dhasenfratz $"
__source__      = "$URL: http://gsn.svn.sourceforge.net/svnroot/gsn/branches/permasense/gsn/tools/backlog/python/CONO2Plugin.py $"

import BackLogMessage
from AbstractPlugin import AbstractPluginClass

import CONO2Driver
'''
added time to give a break in the initialization process.
'''
import time

DEFAULT_BACKLOG = True

# Reads messages from the CONO2Driver and sends them to GSN
class CONO2PluginClass(AbstractPluginClass):
    
    '''
    Class variable to count the iterations of the read command.
    '''
    
    def __init__(self, parent, config):
        
        AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG)
        self._timer = None
        self._stopped = False
        self._interval = None
        self.statisticsCounterValue = 0

        self.info('Init CONO2Plugin...')
        
        self._deviceStr = self.getOptionValue('cono2_device')
        self.cono2 = CONO2Driver.CONO2Driver([self._deviceStr])

                
        self.debug("Set sensor into automatic mode")
        msg = ''
        '''
        modified the init process max try is 30 times
        '''
        setAutoCounter = 0
        count = 0
        autoValue = 0
        while (setAutoCounter < 3):   
            while len(str(msg)) != 3 and count <=10:
                msg = self.cono2._setAuto()
                if str(msg) != 'None' and msg.find('{S}') >= 0:
                    autoValue = 1
                    break
                else:
                    count = count+1
            count=0
            setAutoCounter=setAutoCounter+1
            time.sleep(0.1)
        
        if autoValue == 1:
            self.debug("Set gTimer_delay to 30s")
            msg = ''
            timerSetCounter = 0
            flagTimer = 0
            while len(str(msg)) != 13 and timerSetCounter < 10:
                msg = self.cono2._setgTimer()
                if str(msg) != 'None' and msg.find('{W0?0:') >= 0:
                    flagTimer = 1
                    break
                else:
                    timerSetCounter += 1
            if flagTimer == 1:
               self.info("Done init")
        else:
            self.error("Failed init")

    def isBusy(self):
        return False

    def action(self, parameters):

        self.debug('CONO2Plugin started...')
        
        if parameters == '' or parameters <= 0:
            parameters = -1

        # Read message
        msg = ''
        readCount = 0
        readFlag = 0
        while len(str(msg)) != 18 and readCount < 10:
            msg = self.cono2._read()
            if str(msg) != 'None' and msg.find('{M') >= 0:
                self.debug(msg)
                if msg.find('{M000') >= 0:
                    self.initAfterPlugout()
                readFlag = 1
                break
            else:
                readCount += 1
        if readFlag == 1:
            dataPackage = [msg]
            dataPackage += [parameters]
            
            self.processMsg(self.getTimeStamp(), dataPackage)
            
            self.debug('CONO2 reading done')
        else:
            self.warning ('CONO2 read failed')

    def recvInterPluginCommand(self, command):
        self.action(command)

    def stop(self):
        self._stopped = True
        self.info('stopped')
        
    def initAfterPlugout(self):

        msg = ''
        setAutoCounter = 0
        count = 0
        autoValue = 0
        while (setAutoCounter < 3):   
            while len(str(msg)) != 3 and count <=10:
                msg = self.cono2._setAuto()
                if str(msg) != 'None' and msg.find('{S}') >= 0:
                    autoValue = 1
                    break
                else:
                    count = count+1
            count=0
            setAutoCounter=setAutoCounter+1
            time.sleep(0.1)
        if autoValue  == 1:    
            self.debug('CONO2 initialized after re-plugin')
        else:
            self.error('CONO2 initialization failed after re-plugin')        
