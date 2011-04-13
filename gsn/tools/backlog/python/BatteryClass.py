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
#charge states
UNKNOWN = -1
DISCHARGING = 0
BULK = 1
ABSORPTION = 2
FLOAT = 3

RISE = 1
DROP = -1
EVEN = 0


class BatteryClass():
#class BatteryPluginClass():
    '''
    ##########################################################################################
    __init__
    ##########################################################################################
    '''
    #def __init__(self):
    def __init__(self):
        #Thread.__init__(self)
    
    	#AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG, needPowerControl=False)
        self.info('Init BatteryPlugin...')
        self._stopped = False
        self._busy = True
        self._initialized = False
        self._capacity = -1;
        self._state = UNKNOWN
        self._tidx = 0
        self._read = None
        
        self._historySize = 10
        self._tendencyV = [0 for i in range(0,self._historySize)]
        self._tendencyI = [0 for i in range(0,self._historySize)]
        self._voltages = [0 for i in range(0,self._historySize)]
        self._currents = [0 for i in range(0,self._historySize)]        
        self._stateChange01 = 0
        self._stateChange12 = 0
        self._stateChange23 = 0
        self._stateChange30 = 0
        self._stateChange10 = 0
        self._sampleTime = []
        self._filterTaps = 20
        
        self._ad77x8 = False
        self._calibrated = False
        self._conf_calibrate = False #TODO: This must be true on older boards
        self._oldBoard = False
        self._initAD77x8()
        
        # The measurement interval in seconds
        self._interval = 30; #float(self.getOptionValue('poll_interval'))
    	# The measurement time in seconds
        self._measTime = 900; #int(self.getOptionValue('measurement_time'))
        
        '''
        self._vifile = "vii_dh-4_03012011-03122011.csv"
        self._tfile = "t_dh-4_03012011-03122011.csv"
        self._outfile = "output.txt"
        
        self._fpvi = None
        self._fpt = None
        self._fpout = None
        try:
            self._fpvi = open(self._vifile,"r")
        except Exception as e:
            self.exception( "could not open file: " + str(self._vifile) + " " + str(e))
            return
        try:
            self._fpt = open(self._tfile,"r")
        except Exception as e:
            self.exception( "could not open file: " + str(self._tfile) + " " + str(e))
            return
        try:
            self._fpout = open(self._outfile,"w")
        except Exception as e:
            self.exception( "could not open file: " + str(self._outfile) + " " + str(e))
            return
        '''   
        self._runEv = Event()
        self._vFilter = Filter.Filter(self._filterTaps)
        self._iFilter = Filter.Filter(self._filterTaps)

        #self._stats = Statistics()
        self._initialized = True

        self.debug("Done init BatteryPlugin")
 
    '''
    ##########################################################################################
    runPlugin():  This function is essentially the run function
    ##########################################################################################
    '''
    '''
    def runPlugin(self,param):  
        if (not self._initialized):
            return
        # scheduling my death...
        _endTime = time.time() + self._measTime
        _idx = 0
        
        # Prepare for precise timing
        now = time.time()
        while (time.time() <= _endTime and not self._stopped):
            t = self._getTemperature(_idx)
                
            #get Voltage and current
            v= self._getVoltage(_idx) #MUST be called before getCurrent, otherwise getCurrent will return nothing
            i = self._getCurrent(_idx)
            self.info(str(v)+ " " + str(i))
            return
            
            #filter voltage and current
            vFiltered = self._vFilter.filter(v)
            iFiltered = self._iFilter.filter(i)
                       
            
            #get charge voltage approximation
            vta = self._getChargeVoltage(t,"absorption")
            vtf = self._getChargeVoltage(t,"float")
        
            if (i == None or v == None or t == None):
                print("break")
                break
            
            #print (str(_idx) + ": Current: " + str(i) + "mA\nVoltage: " + str(v) + "mV\nTemp: " + str(t) + " C")
            #get charge state as a function of V,I,T
            newState = self._getNextState(vFiltered,iFiltered,vta,vtf)
            
            if (newState != self._state):
                print ("Changed state from " + str(self._state) + " to " +str(newState) + " at " + " (" + str(_idx) + ")" + str(self._sampleTime[3].strip("\n"))) 
                if (self._state == DISCHARGING and newState == BULK):
                    self._stateChange01 += 1
                if (self._state == BULK and newState == ABSORPTION):
                    self._stateChange12 += 1
                if (self._state == ABSORPTION and newState == FLOAT):
                    self._stateChange23 += 1
                if (self._state == FLOAT and newState == DISCHARGING):
                    self._stateChange30 += 1
                if (self._state == BULK and newState == DISCHARGING):
                    self._stateChange10 += 1
            self._state = newState
            
            #get capacity approximate
            self._cap = self._getCapEstimate(vFiltered,iFiltered,t)
            #print("Capacity: " + str(self._cap))
            self._writeToFile([v,i,t,vFiltered,iFiltered,self._state, self._cap,(self._voltages[0] - self._voltages[self._historySize-1])], self._outfile)
            _idx += 1
            
        print("Ran " + str(_idx) + " times")
        print("Changed states from DISCHARGE to BULK    " + str(self._stateChange01) + " times")
        print("Changed states from BULK to ABSORPTION   " + str(self._stateChange12) + " times")
        print("Changed states from ABSORPTION to FLOAT  " + str(self._stateChange23) + " times")
        print("Changed states from FLOAT to DISCHARGING " + str(self._stateChange30) + " times")
        print("Changed states from BULK to DISCHARGING " + str(self._stateChange10) + " times")

        end = time.time()

        self.stop()
    '''
    '''
    ##########################################################################################
    _getNextState(V,I,T): Returns battery state
    ##########################################################################################
    '''
    def _getNextState(self,v,i,vta,vtf,state):
        
        self._updateTendency(v,i)
        tendencyV = self._getTendency("V")
        tendencyI = self._getTendency("I")
        
        slopeV = (self._voltages[0] - self._voltages[self._historySize-1])
        slopeI = (self._currents[0] - self._currents[self._historySize-1])
        #Upon startup the state may be unknown
        if (state == UNKNOWN):
            #if we haven't sampled enough to determine a tendency
            if (tendencyV == None): # or tendencyV == EVEN):
                return UNKNOWN
            #if tendency > 0 --> charging
            if (tendencyV == RISE and v > 13000):# and tendencyI != DROP):
                return BULK
            #if tendency < 0 --> discharging
            if (tendencyV == DROP):
                return DISCHARGING
            return UNKNOWN
        
        if (state == DISCHARGING):
            #print("tendencyV " + str(tendencyV) + " tendencyI " + str(tendencyI) )
            if (tendencyV == RISE and v > vta):
                    return ABSORPTION
            if (tendencyV == RISE and v > 13000): # and slopeI > 0):
                    print("v " + str(v) + " max " + str(max(self._voltages)))
                    return BULK    
            return DISCHARGING
        
        else: # charging
            if (state == BULK):
                if (v < vta and tendencyV == DROP):
                    return DISCHARGING
                if (v >= vta):
                    return ABSORPTION
                return BULK
            if (state == ABSORPTION):
                if (v >= vtf and v < vta):
                    return FLOAT
                if (v < vtf):
                    return DISCHARGING
                return ABSORPTION
            if (state == FLOAT):
                #print(str(v))
                #print(str(vtf))
                if (v < vtf and tendencyV == DROP):
                    #self._capacity = 100
                    return DISCHARGING
                return FLOAT
    
    '''
    ##########################################################################################
    _getTendency()
    ##########################################################################################
    '''
    def _getTendency(self,type):
        up = 0
        if (type == "V"):
            tend = self._tendencyV
        elif (type == "I"):
            tend = self._tendencyI
        else:
            print ("ERROR: _getTendency type missing!!")
            exit
            
        for j in range(0,self._historySize-1):
            if (tend[j] > 0):
                up += 1

        if (up >= self._historySize*.75):
            return RISE
        
#        if (up < self._historySize*.75 and up > self._historySize*.25):
#            return EVEN
        if (up < self._historySize*.75 and up > self._historySize/2):
            return self._state
       
        if (up <= self._historySize/2 and up > self._historySize*.25):
            if (tend[0] > 0):
                return RISE
            if (tend[0] == 0):
                return EVEN
            else:
                return DROP
        
        if (up <= self._historySize*.25):
            return DROP
        
        else:
            print ("ERROR: _getTendency!!")
            exit
    
    '''
    ##########################################################################################
    _updateTendency(v)
    ##########################################################################################
    '''
    def _updateTendency(self,v,i):
        idx = self._historySize-1
        for j in range(0,len(self._voltages)):
            self._voltages[self._historySize-j-1] = self._voltages[self._historySize-j-2]
            self._tendencyV[self._historySize-j-1] = self._tendencyV[self._historySize-j-2]
            self._tendencyI[self._historySize-j-1] = self._tendencyI[self._historySize-j-2]
        self._voltages[0] = v
        self._currents[0] = i
        
        idx = self._historySize
        for j in range(0,self._historySize-1):
            if (self._voltages[j] == 0):
                idx = j
                break 
        self._tendencyV[0] = self._voltages[0] - self._voltages[idx-1]
        
        idx = self._historySize
        for j in range(0,self._historySize-1):
            if (self._currents[j] == 0):
                idx = j
                break 
        self._tendencyI[0] = self._currents[0] - self._currents[idx-1]
    
    '''
    ##########################################################################################
    _getChargeVoltage(t,type)
    ##########################################################################################
    '''
    def _getChargeVoltage(self,t,type):
        
        if (type == "float"):
            if (t > 40):
                return 13000
            else:
                p = [0.0002395, -0.03594, 14.04]
                return (p[0]*t*t + p[1]*t + p[2])*1000*.95
        if (type == "absorption"):
            p = [ 0.0002394, -0.036, 15.06]
            return (p[0]*t*t + p[1]*t + p[2])*1000*.95
    
    '''
    ##########################################################################################
    _getCapEstimate(V,I,T)
    ##########################################################################################
    '''
    def _getCapEstimate(self,v,i,t,state):
        #TODO: Incorporate temperature effect!
        if (state == DISCHARGING):
            if (i <= 450): #mA
                if (v <=12800):
                    return 100 - (-75 * v/1000 + 970)
                else:
                    return 100
            '''if (i > 250 and i <= 450):
                if (v <= 12800):
                    return -75 * v/1000 + 970
                else:
                    return 0
            '''
            if (i > 450 and i <= 2500):
                if (v <= 12750):
                    return 100 - (-78.2609 * v/1000 + 1007.8261)
                else:
                    return 100
        else:
            return 110
    '''
    ##########################################################################################
    _getVoltage(idx): Reads Voltage sensor
    ##########################################################################################
    '''
    def _getVoltage(self,idx):
        ret = [None]*10
        if (self._ad77x8 ):
           try:
                fc = open('/proc/ad77x8/config', 'w')
                fc.write('format mV')
                fc.flush()                
                fc.write('chopping on')
                fc.flush()
                fc.write('negbuf on')
                fc.flush()
                fc.write('sf 13')
                fc.flush()
                fc.write('range 7')
                fc.flush()
                fc.write('calibrate')
                fc.flush()
                fc.close()
                
                f4 = open('/proc/ad77x8/ain4', 'r') #I_V12DC_EXT
                f5 = open('/proc/ad77x8/ain5', 'r') #V12DC_IN
                f6 = open('/proc/ad77x8/ain6', 'r') #I_V12DC_IN
            
                ad77x8_4 = f4.read()
                ad77x8_5 = f5.read()
                ad77x8_6 = f6.read()
                
                f4.close()
                f5.close()
                f6.close()
                    
                ad77x8_4 = float(ad77x8_4.split()[0])
                ad77x8_5 = float(ad77x8_5.split()[0])
                ad77x8_6 = float(ad77x8_6.split()[0])
                
                if self._calibrated and self._conf_calibrate:
                    ad77x8_6 = ad77x8_6 - 0.3
                    ad77x8_4 = ad77x8_4 - self._ain4_cal
                if ad77x8_4 < 0:
                    ad77x8_4 = 0
                if ad77x8_6 < 0:
                    ad77x8_6 = 0
    
                
                if self._oldBoard:
                    ad77x8_4 = int(round(ad77x8_4 * 20000))
                else:
                    ad77x8_4 = int(round(ad77x8_4 * 10000))
                ad77x8_5 = int(round(ad77x8_5 * 23 / 3.0))
                ad77x8_6 = int(round(ad77x8_6 * 2000))
                

                ret = [ ad77x8_5, float(ad77x8_4 + ad77x8_6)/1000]
           except Exception, e:
                self.warning(e.__str__())
            
        return ret
    
    '''
    ##########################################################################################
    _getTemperature(idx): Reads Temperature sensor
    ##########################################################################################
    '''
    def _getTemperature(self,idx):
        '''
            [LM92 temperature (int)]
        '''
        if (not self._checkLM92Temp()):
            self.info("Cannot check LM92")
            return -100
        ret = None
        try:
                file = open("/sys/bus/i2c/devices/0-004b/temp1_input", "r")
                line = file.readline()
                file.close()
#                print '#############################################################################'
#                print '/sys/bus/i2c/devices/0-004b/temp1_input'
#                print '[LM92 (int)]'
#                print ''
#                print line
#                print ''
                val = int(line)
                if val != -240000:
                    ret = val/1000
        except Exception, e:
                self.exception(e)
        return ret
    
    def info(self,val):
        print(str(val))
        
    def warning(self,val):
        self.info("WARNING: " + str(val))
        
    def exception(self,val):
        self.info("EXCEPTION: " + str(val))
        
    def debug(self,val):
        self.info(str(val))
        
        
    def _initAD77x8(self):
        self._ad77x8 = True
        p = subprocess.Popen(['modprobe', 'ad77x8'])
        self.info('wait for modprobe ad77x8 to finish')
        ret = p.wait()
        output = p.communicate()
        if output[0]:
            if output[1]:
                self.info('modprobe ad77x8: (STDOUT=%s STDERR=%s)' % (output[0], output[1]))
            else:
                self.info('modprobe ad77x8: (STDOUT=%s)' % (output[0],))
        elif output[1]:
                self.info('modprobe ad77x8: (STDERR=%s)' % (output[1],))
                
        if ret != 0:
            self.warning('module ad77x8 is not available (modprobe ad77x8 returned with code %d)' % (ret,))
            self._ad77x8 = False
    
    def _checkLM92Temp(self):
        self._lm92Temp = True
        try:
            file = open("/sys/bus/i2c/devices/0-004b/temp1_input", "r")
            line = file.readline()
            file.close()
            val = int(line)
            if val == -240000:
                self._lm92Temp = False
                return False
        except Exception, e:
            self.warning(str(e))
            return False
        return True
