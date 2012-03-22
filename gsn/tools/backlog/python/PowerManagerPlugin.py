# -*- coding: UTF-8 -*-
__author__      = "Ben Buchli <bbuchli@ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"
'''
backlog imports
'''

import BackLogMessage
from AbstractPlugin import AbstractPluginClass
import BatteryClass
'''
stdlib imports
'''
import struct
#from time import gmtime, time, strftime
import time
from threading import Event, Thread
import Filter


from ScheduleHandler import SUBPROCESS_BUG_BYPASS
if SUBPROCESS_BUG_BYPASS:
    import SubprocessFake
    subprocess = SubprocessFake
else:
    import subprocess
    
from SpecialAPI import Statistics

'''
defines
'''
DEFAULT_BACKLOG = True

#charge states
UNKNOWN = -1
DISCHARGING = 0
BULK = 1
ABSORPTION = 2
FLOAT = 3

class PowerManagerPluginClass(AbstractPluginClass):
#class PowerManagerPlugin():
    '''
    ##########################################################################################
    __init__
    ##########################################################################################
    '''
    #def __init__(self):
    def __init__(self, parent, config):
        Thread.__init__(self)
        AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG, needPowerControl=False)
        
        self.info('Init PowerManagerPlugin...')
        self._stopped = False
        self._busy = True
        self._initialized = False
        
        self._state = UNKNOWN

        self._historySize = 10
        self._tendencyV = [0 for i in range(0,self._historySize)]
        self._tendencyI = [0 for i in range(0,self._historySize)]
        self._voltages = [0 for i in range(0,self._historySize)]
        self._currents = [0 for i in range(0,self._historySize)]        
                
        # Filter taps
        self._ifilterTaps = int(self.getOptionValue('itaps'))
        self._vfilterTaps = int(self.getOptionValue('vtaps'))
        # The measurement interval in seconds
        self._interval = float(self.getOptionValue('poll_interval'))
        # The measurement time in seconds
        self._measTime = int(self.getOptionValue('measurement_time'))
        # The rated battery capacity in mAh
        self._initCapacity = int(self.getOptionValue('rated_capacity'))*1000
        # Current capacity
        self._cap = 0
        # energy consumed
        self._energy = 0
        # average power
        self._power = 0
        # instantenous power
        self._curPower = 0
        
        self._bat = BatteryClass.BatteryClass()
        
        self._vFilter = Filter.Filter(self._vfilterTaps)
        self._iFilter = Filter.Filter(self._ifilterTaps)
        self._runEv = Event()
        
        # stats vars
        self._stateChange01 = 0
        self._stateChange12 = 0
        self._stateChange23 = 0
        self._stateChange30 = 0
        self._stateChange10 = 0
        self._time0 = 0
        self._time1 = 0
        self._time2 = 0
        self._time3 = 0
        
        self._initialized = True

        self.debug("Done init PowerManagerPlugin")
 
    '''
    ##########################################################################################
    runPlugin():  This function is essentially the run function
    ##########################################################################################
    '''
    def runPlugin(self,param):  
        if (not self._initialized):
            return
        # scheduling my death...
        _endTime = time.time() + self._measTime
        _idx = 1
        
        # Prepare for precise timing
        now = time.time()
        latestTime = now
        while (time.time() <= _endTime and not self._stopped):
            t = self._bat._getTemperature(_idx)   
            self.info("Temperature " + str(t))
            #get Voltage and current
            v,i = self._bat._getVoltage(_idx)
            
            #filter voltage and current
            vFiltered = self._vFilter.filter(v)
            iFiltered = self._iFilter.filter(i)
            
            #get charge voltage approximation
            vta = self._bat._getChargeVoltage(t,"absorption")
            vtf = self._bat._getChargeVoltage(t,"float")
        
            #get charge state as a function of V,I,T
            newState = self._bat._getNextState(vFiltered,iFiltered,vta,vtf,self._state)
            
            if (newState != self._state):
                print ("Changed state from " + str(self._state) + " to " +str(newState) + " at " + " (" + str(_idx) + ")") 
                if (self._state == DISCHARGING and newState == BULK):
                    self._stateChange01 += 1
                    self._time0 += time.time()-latestTime
                if (self._state == BULK and newState == ABSORPTION):
                    self._stateChange12 += 1
                    self._time1 += time.time()-latestTime
                if (self._state == ABSORPTION and newState == FLOAT):
                    self._stateChange23 += 1
                    self._time2 += time.time()-latestTime
                if (self._state == FLOAT and newState == DISCHARGING):
                    self._stateChange30 += 1
                    self._time3 += time.time()-latestTime
                if (self._state == BULK and newState == DISCHARGING):
                    self._stateChange10 += 1
                    self._time1 += time.time()-latestTime
                    
            latestTime = time.time()
            self._state = newState
            
            #get capacity approximate
            self._cap = self._bat._getCapEstimate(vFiltered,iFiltered,t,newState)
            expectedLifetime = self._cap * self._initCapacity/(i*100)
            self._power += vFiltered/1000 * iFiltered
            self._curPower = vFiltered/1000 * iFiltered
            self._energy = self._power/_idx * self.getUptime()/3600
            
            #self._writeToFile([v,i,t,vFiltered,iFiltered,self._state, self._cap,(self._voltages[0] - self._voltages[self._historySize-1])], self._outfile)
            print([v,i,t,vFiltered,iFiltered,self._state,self._cap,self._vfilterTaps, self._ifilterTaps, expectedLifetime, self._curPower, self._energy, self._time0,self._time1,self._time2, self._time3 ])
            
            self.processMsg(self.getTimeStamp(), [int(v),int(i),int(t),int(vFiltered),int(iFiltered),float(self._state),float(self._cap), \
                                                  float(expectedLifetime), float(self._curPower), float(self._energy), float(self._time0),float(self._time1),float(self._time2), float(self._time3), int(self._vfilterTaps), int(self._ifilterTaps)])
            _idx += 1
            
            self._runEv.wait(self._interval-1)
            
        print("Ran " + str(_idx) + " times")
        print("Changed states from DISCHARGE to BULK    " + str(self._stateChange01) + " times")
        print("Changed states from BULK to ABSORPTION   " + str(self._stateChange12) + " times")
        print("Changed states from ABSORPTION to FLOAT  " + str(self._stateChange23) + " times")
        print("Changed states from FLOAT to DISCHARGING " + str(self._stateChange30) + " times")
        print("Changed states from BULK to DISCHARGING " + str(self._stateChange10) + " times")

        end = time.time()

        self.stop()
   
    '''
    ##########################################################################################
    action():   This function will be fired by the schedule handler each time
                this plugin is scheduled. The function is started in a new
                thread.
        
                @param parameters:  The parameters as one string given in the
                                    schedule file.
    ##########################################################################################
    '''
    def action(self,parameters):
        self.info("Action called!")
        self.runPlugin(parameters)
        
    '''
    ##########################################################################################
    run():     This function gets called when the plugin is loaded. In duty-cycle mode the
               action() function takes care of running the plugin
    ##########################################################################################
    '''
    def run(self):
        self.runPlugin('')
    '''
    ##########################################################################################
    stop()
    ##########################################################################################
    '''
    def stop(self):
        self.info('BatteryPlugin stopping...')
        self._runEv.set()
        self._busy = False
        self._stopped = True
        self.info('BatteryPlugin stopped')
    
    '''
    ##########################################################################################
    isBusy(): This function is required to let the parent know we're still alive and busy.
    ##########################################################################################
    '''
    def isBusy(self):
        return self._busy
        
    '''
    ##########################################################################################
    needsWLAN(): This function is required to let backlog know if this plugin requires WLAN
                NOTE: If this plugin modifies WLAN state it MUST return False!
    ##########################################################################################
    '''
    def needsWLAN(self):
        # TODO: implement return value
        return False
    
    def info(self,val):
        print(str(val))
        
    def warning(self,val):
        self.info("WARNING: " + str(val))
        
    def exception(self,val):
        self.info("EXCEPTION: " + str(val))
        
    def debug(self,val):
        self.info(str(val))
        
    
