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

SENSOR_ID = 2

# Reads messages from the OZ47Driver and sends them to GSN
class OZ47Plugin2Class(AbstractPluginClass):
    
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
        self.debug ('self._statisticsReadOffset' + str(self._statisticsReadOffset))
                
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
                msg = self.oz47._setAuto()
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
                msg = self.oz47._setgTimer()
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

        self.debug('OZ47Plugin started...')
        
        if parameters == '' or parameters <= 0:
            parameters = -1

        # Read message
        msg = ''
        readCount = 0
        readFlag = 0
        while len(str(msg)) != 18 and readCount < 10:
            msg = self.oz47._read()
            if str(msg) != 'None' and msg.find('{M') >= 0:
                self.debug(msg)
                if msg.find('{M000') >= 0:
                    self.initAfterPlugout()
                readFlag = 1
                break
            else:
                readCount += 1
        if readFlag == 1:
            dataPackage = [READINGS_NAMING]
            dataPackage += [msg]
            dataPackage += [SENSOR_ID]
            dataPackage += [parameters]

            self.processMsg(self.getTimeStamp(), dataPackage)
            '''
            Code to read the statistics after StatisticsOffset value from backlog.cfg file
            '''
            self.statisticsCounterValue += 1
            if self.statisticsCounterValue == int(self._statisticsReadOffset):
                self.statistics()
                self.statisticsCounterValue = 0
            self.debug('OZ47 reading done')
        else:
            self.warning ('OZ47 read failed')

    def stop(self):
        self._stopped = True
        self.info('stopped')


    def resetPage(self):

        self.debug('OZ47Plugin Page 0 cleared and written with default values...')
        
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
        self.debug('OZ47 reset Page 0 done')


    def statistics(self):

        self.debug('OZ47Plugin statistics...')
        m1 = ''
        m2 = ''
        m3 = ''
        m4 = ''
        m5 = ''
        x0_1 = ''
        x1_1 = ''
        x2_1 = ''
        x3_1 = ''
        kt_1 = ''
        x0_2 = ''
        x1_2 = ''
        x2_2 = ''
        x3_2 = ''
        kt_2 = ''
        statCount = 0
        while len(str(m1)) != 13 and statCount < 10:
            m1 = self.oz47._readPageIndex('0', '0')
            if str(m1) != 'None' and m1.find('{R00') >= 0:
                break
            else:
                statCount += 1
        statCount = 0
        while len(str(x0_1)) != 13 and statCount < 10:
            x0_1 = self.oz47._readPageIndex('0', '1')
            if str(x0_1) != 'None' and x0_1.find('{R01') >= 0:
                break
            else:
                statCount += 1
        statCount = 0
        while len(str(x1_1)) != 13 and statCount < 10:
            x1_1 = self.oz47._readPageIndex('0', '2')
            if str(x1_1) != 'None' and x1_1.find('{R02') >= 0:
                break
            else:
                statCount += 1
        statCount = 0
        while len(str(x2_1)) != 13 and statCount < 10:
            x2_1 = self.oz47._readPageIndex('0', '3')
            if str(x2_1) != 'None' and x2_1.find('{R03') >= 0:
                break
            else:
                statCount += 1
        statCount = 0
        while len(str(x3_1)) != 13 and statCount < 10:
            x3_1 = self.oz47._readPageIndex('0', '4')
            if str(x3_1) != 'None' and x3_1.find('{R04') >= 0:
                break
            else:
                statCount += 1
        statCount = 0
        while len(str(kt_1)) != 13 and statCount < 10:
            kt_1 = self.oz47._readPageIndex('0', '5')
            if str(kt_1) != 'None' and kt_1.find('{R05') >= 0:
                break
            else:
                statCount += 1
        statCount = 0
        while len(str(x0_2)) != 13 and statCount < 10:
            x0_2 = self.oz47._readPageIndex('0', '7')
            if str(x0_2) != 'None' and x0_2.find('{R07') >= 0:
                break
            else:
                statCount += 1
        statCount = 0
        while len(str(x1_2)) != 13 and statCount < 10:
            x1_2 = self.oz47._readPageIndex('0', '8')
            if str(x1_2) != 'None' and x1_2.find('{R08') >= 0:
                break
            else:
                statCount += 1
        statCount = 0
        while len(str(x2_2)) != 13 and statCount < 10:
            x2_2 = self.oz47._readPageIndex('0', '9')
            if str(x2_2) != 'None' and x2_2.find('{R09') >= 0:
                break
            else:
                statCount += 1
        statCount = 0
        while len(str(x3_2)) != 13 and statCount < 10:
            x3_2 = self.oz47._readPageIndex('0', 'A')
            if str(x3_2) != 'None' and x3_2.find('{R0A') >= 0:
                break
            else:
                statCount += 1
        statCount = 0
        while len(str(kt_2)) != 13 and statCount < 10:
            kt_2 = self.oz47._readPageIndex('0', 'B')
            if str(kt_2) != 'None' and kt_2.find('{R0B') >= 0:
                break
            else:
                statCount += 1
        statCount = 0
        while len(str(m2)) != 13 and statCount < 10:
            m2 = self.oz47._readPageIndex('0', '=')
            if str(m2) != 'None' and m2.find('{R0=') >= 0:
                break
            else:
                statCount += 1
        statCount = 0
        while len(str(m3)) != 13 and statCount < 10:
            m3 = self.oz47._readPageIndex('0', '?')
            if str(m3) != 'None' and m3.find('{R0?') >= 0:
                break
            else:
                statCount += 1
        statCount = 0
        while len(str(m4)) != 13 and statCount < 10:
            m4 = self.oz47._readPageIndex('1', '0')
            if str(m4) != 'None' and m4.find('{R10') >= 0:
                break
            else:
                statCount += 1
        statCount = 0
        while len(str(m5)) != 13 and statCount < 10:
            m5 = self.oz47._readPageIndex('2', '0')
            if str(m5) != 'None' and m5.find('{R20') >= 0:
                break
            else:
                statCount += 1
        # Read message
        msg = m1 + m2 + m3 + m4 + m5 + x0_1 + x1_1 + x2_1 + x3_1 + kt_1 + x0_2 + x1_2 + x2_2 + x3_2 + kt_2
        
        self.debug('statistics message' + msg)
        dataPackage = [STATISTICS_NAMING]
        dataPackage += [msg]
        dataPackage += [SENSOR_ID]
        self.processMsg(self.getTimeStamp(), dataPackage)

        self.debug('OZ47 statistics done')
        
    def initAfterPlugout(self):

        msg = ''
        setAutoCounter = 0
        count = 0
        autoValue = 0
        while (setAutoCounter < 3):   
            while len(str(msg)) != 3 and count <=10:
                msg = self.oz47._setAuto()
                if str(msg) != 'None' and msg.find('{S}') >= 0:
                    autoValue = 1
                    break
                else:
                    count = count+1
            count=0
            setAutoCounter=setAutoCounter+1
            time.sleep(0.1)
        if autoValue  == 1:    
            self.debug('OZ47 initialized after re-plugin')
        else:
            self.error('OZ47 initialization failed after re-plugin')        
