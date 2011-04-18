#! /usr/bin/python
# -*- coding: UTF-8 -*-
__author__      = "David Hasenfratz <hasenfratz@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2011, ETH Zurich, Switzerland, David Hasenfratz"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import BackLogMessage
from AbstractPlugin import AbstractPluginClass

import OZ47Driver
'''
added time to give a break in the initialization process.
'''
import time

DEFAULT_BACKLOG = True

STATISTICS_NAMING = 1
READINGS_NAMING = 2

# Reads messages from the OZ47Driver and sends them to GSN
class OZ47PluginClass(AbstractPluginClass):
    
    '''
    Class variable to count the iterations of the read command.
    '''
    
    def __init__(self, parent, config):
	    
        AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG)
        self._timer = None
        self._stopped = False
        self._interval = None
	self.statisticsCounterValue = 0

        self.info('Init OZ47Plugin...')
        
    	self._deviceStr = self.getOptionValue('oz47_device')
    	self.oz47 = OZ47Driver.OZ47Driver([self._deviceStr])

        '''
        adding a new parameter for statistics
        '''
        self._statisticsReadOffset = self.getOptionValue('oz47_statisticsOffset')
        self.info ('self._statisticsReadOffset' + str(self._statisticsReadOffset))
                
        self.info("Set sensor into automatic mode")
        msg = ''
        '''
        modified the init process max try is 30 times
        '''
        setAutoCounter = 0
        count = 0
        autoValue = 0
        while (setAutoCounter < 3):   
            while len(str(msg)) != 3 and count <=10:
                msg = self.oz47._setAuto()
                if str(msg) != 'None': 
                    if msg.find('{S}') >= 0:
                        autoValue = 1
                        break
                else:
                    count = count+1
            count=0
            setAutoCounter=setAutoCounter+1
            time.sleep(0.1)
        
        if autoValue == 1:
            self.info("Set gTimer_delay to 30s")
            msg = ''
            timerSetCounter = 0
            flagTimer = 0
            while len(str(msg)) != 13 and timerSetCounter < 10:
                msg = self.oz47._setgTimer()
                if str(msg) != 'None':
                    if msg.find('{W0?0:') >= 0:
                        flagTimer = 1
                        break
                else:
                    timerSetCounter += 1
            if flagTimer == 1:
    	       self.info("Done init")
        else:
            self.error("Failed init")
                
        
    def getMsgType(self):
        return BackLogMessage.OZ47_MESSAGE_TYPE

    def isBusy(self):
        return False

    def action(self, parameters):

        self.info('OZ47Plugin started...')

        # Read message
        msg = ''
        readCount = 0
        readFlag = 0
        while len(str(msg)) != 18 and readCount < 10:
            msg = self.oz47._read()
            if str(msg) != 'None':
                if msg.find('{M') >= 0:
                    self.info(msg)
                    if msg.find('{M000') >= 0:
                        self.initAfterPlugout()
                    readFlag = 1
                    break
            else:
                readCount += 1
        if readFlag == 1:
            dataPackage = [READINGS_NAMING]
            dataPackage += [msg]

            self.processMsg(self.getTimeStamp(), dataPackage)
            '''
            Code to read the statistics after StatisticsOffset value from backlog.cfg file
            '''
            self.statisticsCounterValue += 1
            if self.statisticsCounterValue == int(self._statisticsReadOffset):
                self.statistics()
                self.statisticsCounterValue = 0
            self.info('OZ47 reading done')
        else:
            self.warning ('OZ47 read failed')

    def stop(self):
        self._stopped = True
        self.info('stopped')


    def resetPage(self):

        self.info('OZ47Plugin Page 0 cleared and written with default values...')
        
        m = self.oz47._erasePage('0')
        m0 = self.oz47._writePageIndex('0', '0', '0002020;')
        m0 = self.oz47._writePageIndex('0', '1', ';3:8181;')
        m0 = self.oz47._writePageIndex('0', '2', '396?0<>3')
        m0 = self.oz47._writePageIndex('0', '3', '3=9280<3')
        m0 = self.oz47._writePageIndex('0', '4', '3?65;0<<')
        m0 = self.oz47._writePageIndex('0', '5', '3<:3=70:')
        m0 = self.oz47._writePageIndex('0', '6', '3<:3=70:')
        m0 = self.oz47._writePageIndex('0', '7', '3<:3=70:')
        m0 = self.oz47._writePageIndex('0', '8', '3<:3=70:')
        m0 = self.oz47._writePageIndex('0', '9', '3<:3=70:')
        m0 = self.oz47._writePageIndex('0', ':', '3<:3=70:')
        m0 = self.oz47._writePageIndex('0', ';', '44372492')
        m0 = self.oz47._writePageIndex('0', '<', '44372492')
        m0 = self.oz47._writePageIndex('0', '=', '3<00????')
        m0 = self.oz47._writePageIndex('0', '>', '3<7=2492')
        m0 = self.oz47._writePageIndex('0', '?', '3<003C0:')

        # Read message
        self.info('OZ47 reset Page 0 done')


    def statistics(self):

        self.info('OZ47Plugin statistics...')
        m1 = ''
        m2 = ''
        m3 = ''
        m4 = ''
        m5 = ''
        statCount = 0
        while len(str(m1)) != 13 and statCount < 10:
            m1 = self.oz47._readPageIndex('0', '0')
            if str(m1) != 'None':
                if m1.find('{W00') >= 0:
                    break
            else:
                statCount += 1
        statCount = 0
        while len(str(m2)) != 13 and statCount < 10:
            m2 = self.oz47._readPageIndex('0', '=')
            if str(m2) != 'None':
                if m2.find('{W0=') >= 0:
                    break
            else:
                statCount += 1
        statCount = 0
        while len(str(m3)) != 13 and statCount < 10:
            m3 = self.oz47._readPageIndex('0', '?')
            if str(m3) != 'None':
                if m3.find('{W0?') >= 0:
                    break
            else:
                statCount += 1
        statCount = 0
        while len(str(m4)) != 13 and statCount < 10:
            m4 = self.oz47._readPageIndex('1', '0')
            if str(m4) != 'None':
                if m4.find('{W10') >= 0:
                    break
            else:
                statCount += 1
        statCount = 0
        while len(str(m5)) != 13 and statCount < 10:
            m5 = self.oz47._readPageIndex('2', '0')
            if str(m5) != 'None':
                if m5.find('{W20') >= 0:
                    break
            else:
                statCount += 1
        # Read message
        msg = m1 + m2 + m3 + m4 + m5
        self.info('statistics message' + msg)
        dataPackage = [STATISTICS_NAMING]
        dataPackage += [msg]
        self.processMsg(self.getTimeStamp(), dataPackage)

        self.info('OZ47 statistics done')
        
    def initAfterPlugout(self):

        msg = ''
        setAutoCounter = 0
        count = 0
        autoValue = 0
        while (setAutoCounter < 3):   
            while len(str(msg)) != 3 and count <=10:
                msg = self.oz47._setAuto()
                if str(msg) != 'None': 
                    if msg.find('{S}') >= 0:
                        autoValue = 1
                        break
                else:
                    count = count+1
            count=0
            setAutoCounter=setAutoCounter+1
            time.sleep(0.1)
        if autoValue  == 1:    
            self.info('OZ47 initialized after re-plugin')
        else:
            self.error('OZ47 initialization failed after re-plugin')        
