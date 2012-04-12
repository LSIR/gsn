#!/usr/bin/python
# -*- coding: UTF-8 -*-
__author__      = "Dani Aschwanden <asdaniel@ee.ethz.ch>"
__copyright__   = "Copyright 2011, ETH Zurich, Switzerland, Dani Aschwanden"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import sys
import os
import time
import math
import itertools
import logging
import logging.config
from threading import Thread, Lock, Event, Timer
from SpecialAPI import PowerControl

#############
# CONSTANTS #
#############
#charge states
MAINS = -2
UNKNOWN = -1
DISCHARGING = 0
BULK = 1
ABSORPTION = 2
FLOAT = 3
TRICKLE = 4
states = dict({MAINS:"mains",UNKNOWN:"unknown",DISCHARGING:"discharging",BULK:"bulk",FLOAT:"float",ABSORPTION:"absorption",TRICKLE:"trickle"})

# Tendencies
DROP = -1
RISE = 1
EVEN = 0

SOC_MARGIN = 5 # If we have suddenly 10% more SOC, we are charging...
V_BULK_THRESHOLD = 12900 #mV
V_CHARGE_CONTROLLER_LIMIT = 11500 #mV
BULK_MAXSOC = 90 # At the end of the Bulk we have this Soc!
FLOAT_MARGIN = 20 #mV
ABSORPTION_MARGIN = 30 #mV

class Callable:
    def __init__(self, anycallable):
        self.__call__ = anycallable


class PowerMonitor(Thread):
    '''
    PowerMonitor
    by Dani Aschwanden <asdaniel@ee.ethz.ch>
    '''
    def __init__(self,backlogMain,config):
        Thread.__init__(self, name='PowerMonitor-Thread')
        self._backlogMain = backlogMain
        
        self._logger = logging.getLogger(self.__class__.__name__)
        self._logger.info('initalizing...')
        self._interval = self.getOptionvalue('update_interval',config)
        if self._interval == '' or self._interval == None:
            raise Exception('PowerMonitor is disabled')
        else:
            self._interval = int(self._interval)
            self._samples = int(self.getOptionvalue('samples',config))
            self._cableLength = int(self.getOptionvalue('cable_length',config))
            # 4m cable ~= 0.14ohm, 14m cable ~=0.42ohm... 0.028ohm/m
            self._cableResistance = 0.028*self._cableLength + float(self.getOptionvalue('voltage_controller_resistance',config))/1000
            # BatteryModell
            self._bat = BatteryState(self,self.getOptionvalue('battery_capacity',config),self.getOptionvalue('history_size',config), self.getOptionvalue('battery_number',config))
        
        self._timer = Event()
        self._measureTimer = Event()
        
        # Temporary Measurements
        self._measure_V = []
        self._measure_I = []
        self._measure_temp = []
        
        if self._logger.isEnabledFor(logging.DEBUG):
            self._logger.debug("Initialized!")
    
   
    def run(self):
        '''Run instance of thread, called directly after initialization, when PowerMonitor Thread is started'''
        self._logger.info('started')
        
        # Enable the measuring
        while not self._timer.isSet():
            self._timer.wait(int(self._interval-self._samples))
            if self._timer.isSet():
                break
            try:
                self._action()
            except Exception, e:
                self._logger.exception(e)
                self._backlogMain.incrementExceptionCounter()
        self._logger.info('died')
    
    
    def _action(self):
        """Performs measurements and updates the batterystate periodically"""
        if self._logger.isEnabledFor(logging.DEBUG):
            self._logger.debug('waking up...')
        
        # Measure x times the sensor
        for _ in itertools.repeat(None, self._samples):
            self._measureTimer.wait(1)
            if self._measureTimer.isSet():
                return
            self._measure()
        
        # Update the battery state
        self._update_batterystate()
        
        # Clear averaging measure arrays, wait till no measure thread is in action
        if self._logger.isEnabledFor(logging.DEBUG):
            self._logger.debug('clearing measurements of last period')
        self._measure_V = []
        self._measure_I = []
        self._measure_temp = []
        if self._logger.isEnabledFor(logging.DEBUG):
            self._logger.debug('going to sleep...')
    
    
    def stop(self):
        """stops the powermonitor"""
        self._timer.set()
        self._measureTimer.set()
        self._logger.info('stopped')
    
    def set_interval(self,value):
        """sets the sampling inverval to param value"""
        self._logger.info('Setting interval to %s', value)
        try:
            if self._samples < value:
                self._interval = int(value)
        except TypeError:
            print "Provide integer value for set_interval"
    
    
    def get_interval(self):
        """returns the sampling inverval value"""
        return int(self._interval)
    
    
    def _update_batterystate(self):
        """ for get_batterystate"""
        if self._logger.isEnabledFor(logging.DEBUG):
            self._logger.debug('Updating the batterystate')
        
        # Get the sensor measurements and average the values..
        v_sys = self._mean(self._measure_V)         # mV
        i_sys = self._mean(self._measure_I)/1000    # mA
        temp = self._mean(self._measure_temp)
        
        # Correct the measured voltage with the value of the cable.. so that we can approx the battery
        v_sys = v_sys + i_sys * self._cableResistance
        
        # Update the battery state with the averaged values.
        self._bat.update(v_sys,i_sys,temp, time.time())
    
    
    def _measure(self):
        '''Get all the sensor values and add them to the temporary averaging list'''
        self._measure_V.append( self._backlogMain.powerControl.getV12DcIn() )
        self._measure_I.append( self._backlogMain.powerControl.getIV12DcIn() + self._backlogMain.powerControl.getIV12DcExt() )
        self._measure_temp.append( self._get_Temp() )
        return
    
    
    def _get_Temp(self):
        '''Returns the current tempertature value'''
        ret = None
        try:
                temp_input = open("/sys/bus/i2c/devices/0-004b/temp1_input", "r")
                line = temp_input.readline()
                temp_input.close()
                val = float(line)
                if val != -240000:
                    ret = val/1000
        except Exception:
            self._logger.warning("Error getting temperature value!")
        return ret
    
    def _mean(self,list):
        '''Returns the mean of the float values of a list'''
        if len(list) == 0:
            return float('nan')
        values = [float(x) for x in list]
        return sum(values) / len(list)
    
    
    def checkConfig(config):
        '''Used by ConfigurationHandler for verifying the powermonitor part of the configuration file'''
        # update_interval
        update_interval = PowerMonitor.getOptionvalue('update_interval',config)
        if update_interval is None or update_interval == '':
            return
        if int(update_interval) <= 0:
            raise TypeError('update_interval has to be a positive integer or empty for disabling PowerMonitor')
        
        # Battery Capacitcy
        battery_capacity = PowerMonitor.getOptionvalue('battery_capacity',config)
        if battery_capacity == None or not battery_capacity.isdigit() or int(battery_capacity) < 0:
            raise TypeError('battery_capactiy has to be a positive integer')
        
        # Power Cable Length
        cable_length = PowerMonitor.getOptionvalue('cable_length',config)
        if cable_length == None or not cable_length.isdigit() or int(cable_length) < 0:
            raise TypeError('cable_length has to be a positive integer')
        
        
        # samples
        samples = PowerMonitor.getOptionvalue('samples',config)
        if samples == None or not samples.isdigit() or int(samples) < 0 or (int(samples) > int(update_interval) and update_interval != '-1'):
            raise TypeError('samples has to be a positive integer smaller than update_interval')
        
        # history_size
        history_size = PowerMonitor.getOptionvalue('history_size',config)
        if history_size == None or not history_size.isdigit() or int(history_size) < 0:
            raise TypeError('history_size has to be a positive integer')
        
        # number of battery attached
        battery_number = PowerMonitor.getOptionvalue('battery_number',config)
        if battery_number == None or not battery_number.isdigit() or int(battery_number) < 0:
            raise TypeError('battery_number has to be a positive integer, 0 means no battery attached.')
        
        # Resistance of Voltage controller in OHM
        voltage_controller_resistance = PowerMonitor.getOptionvalue('voltage_controller_resistance',config)
        if voltage_controller_resistance == None or not voltage_controller_resistance.isdigit() or int(voltage_controller_resistance) < 0:
            raise TypeError('voltage_controller_resistance has to be a positive integer!')
    
    checkConfig = Callable(checkConfig)
    
    def getOptionvalue(key, config):
        for entry in config:
            entry_key = entry[0]
            entry_value = entry[1]
            if key == entry_key:
                return entry_value
        return None
    getOptionvalue = Callable(getOptionvalue)


class BatteryState:
    '''
    Model of battery part
    '''
    def __init__(self,parent,capacity,historySize,batNum):
        self._powermonitor = parent
        self._logger = logging.getLogger(self.__class__.__name__)
        self._state = UNKNOWN
        self._prevState = UNKNOWN
        self._tendencyV = UNKNOWN
        self._tendencyI = UNKNOWN
        
        self._soc = None
        self._remainingTime = None
        self._remainingTimeDiff = None
        # History Size of the SOC
        if historySize == None:
            self._historySize = 0
        else:
            self._historySize = int(historySize)
        self._soc_hist = []
        # History Size of vsys and isys, used for moving average!
        self._updateHistorySize = 10
        self._hist_timestamps = []
        self._vsys = []
        self._vsys_avg = None # Weighted Moving Average
        self._isys = []
        self._isys_avg = None # Weighted Moving Average
        
        # Reminders of the SoC at state transitions
        self._first_discharge_soc = 0
        self._first_discharge_soc_time = None
        self._last_discharge_soc = 0
        self._last_bulk_soc = 0
        
        # The approximated charging current
        self._i_bulk_rated = None
        self._v_bulk = []
        self._bulk_measures = 0
        self._bulk_timestamps = []
        self._absorption_time = None
        self._absorption_lasttime = None
        
        if capacity == None and batNum == None:
            self._capacity = 0
            self._batNum = 0
        else:
            self._capacity = float(capacity)
            self._batNum = int(batNum)
        self._usable_capacity = 0.80 # [0-1] %
    
    def update(self, v_sys, i_tot, temp, time):
        """Updates the battery modell with the given system voltage, current and temperature"""
        self._logger.debug('Computing Battery State with v = %.1f, i = %.1f, temp = %.2f',v_sys,i_tot,temp)
        # Add the v_sys and i_tot to the history arrays
        # Updating the weighted moving average WMA_t+1 = WMA_t +n*measurement - sum(measurement(t-n):measurement(t))
        
        vsys_sum = sum(self._vsys)
        length_old = float(len(self._vsys))
        self._vsys.append(v_sys)
        while len(self._vsys) > self._updateHistorySize:
        # remove the too long stuff
            try:
                del self._vsys[0]
            except IndexError:
                break
        
        length_new = float(len(self._vsys))
        # Updating the weighted moving average WMA_t+1 = WMA_t +n*measurement*2/((n+1)*n) - 2/((n+1)*n)*sum(measurement(t-n):measurement(t))
        # Check if we already have a full history, else the WMA is a bit more complicated...
        if length_new != length_old:
        # We are increasing the history.. so use a different equation for the WMA...
            if self._vsys_avg != None:
                try:
                    self._vsys_avg = length_old/(length_new+1)*self._vsys_avg + (2/(length_new+1))*float(v_sys)
                except ZeroDivisionError:
                    self._vsys_avg = v_sys
            else:
                self._vsys_avg = v_sys
        else:
            if self._vsys_avg != None:
                self._vsys_avg = self._vsys_avg + (2/(length_new+1))*float(v_sys) - (2/((length_new+1)*length_new))*float(vsys_sum)
            else:
                self._vsys_avg = v_sys
        
        # Update the tendency of v_sys
        if v_sys > self._vsys_avg:
            self._tendencyV = RISE
        elif v_sys < self._vsys_avg:
            self._tendencyV = DROP
        else:
            self._tendencyV = EVEN
        
        # and the same fun for the current
        isys_sum = sum(self._isys)
        length_old = float(len(self._isys))
        self._isys.append(i_tot)
        while len(self._isys) > self._updateHistorySize:
        # remove the too long stuff
            try:
                del self._isys[0]
            except IndexError:
                break
        
        length_new = float(len(self._isys))
        # Updating the weigthed moving average WMA_t+1 = WMA_t +n*measurement*2/((n+1)*n) - 2/((n+1)*n)*sum(measurement(t-n):measurement(t))
        # Check if we already have a full history, else the WMA is a bit more complicated...
        if length_new != length_old:
        # We are increasing the history.. so use a different equation for the WMA...
            if self._isys_avg != None:
                try:
                    self._isys_avg = length_old/(length_new+1)*self._isys_avg + (2/(length_new+1))*float(i_tot)
                except ZeroDivisionError:
                    self._isys_avg = i_tot
            else:
                self._isys_avg = i_tot
        else:
            if self._isys_avg != None:
                self._isys_avg = self._isys_avg + (2/(length_new+1))*float(i_tot) - (2/((length_new+1)*length_new))*float(isys_sum)
            else:
                self._isys_avg = i_tot
        
        # Update the Tendency of the current
        if i_tot > self._isys_avg:
            self._tendencyI = RISE
        elif i_tot < self._isys_avg:
            self._tendencyI = DROP
        else:
            self._tendencyI = EVEN
        
        # Do the approximation
        self._stateApprox(v_sys,i_tot,temp,time)
        self._approxRemainingTime()
        if self._logger.isEnabledFor(logging.DEBUG):
            self._logger.debug('Remaining Time (@Load %.2fmA): %.2f', self._isys_avg, self._remainingTime)
        self._logger.debug('Updating Battery State to: %.2f', self._soc)
    
    def _dischargeApprox(self,voltage,current,temp):
        """
        Assuming we are in discharging state, this function approximates the state of charge by using the
        actuall current, voltage and temperature measurements.
        """
        # shift the voltage to the approxmation level... [0..1400]
        voltage = voltage - V_CHARGE_CONTROLLER_LIMIT
        
        # rate the current in terms of the capacity!
        current = current / (self._capacity*1000)
        if self._logger.isEnabledFor(logging.DEBUG):
            self._logger.debug('Rated Current %.5f', current)
        
        # If there is no battery attached, return 100 %
        if self._batNum == 0:
            return 100
        
        if voltage < 0:
            voltage = 0
            self._logger.warning('Discharge approximation with lower voltage than 11500!')
        
        # Calculate the coefficients depending of the actual system current
        # a_fit_quad(x) = p1*x^2 + p2*x + p3
        #         Coefficients:
        #           p1 =     0.08133
        #           p2 =   -0.002257
        #           p3 =  -8.321e-09
        a_coef = 0.08133*(current**2) -0.002257*current/self._batNum -8.321e-09
        # b_fit_quad =
        #      Linear model Poly2:
        #      b_fit_quad(x) = p1*x^2 + p2*x + p3
        #      Coefficients:
        #        p1 =      -204.7
        #        p2 =       4.492
        #        p3 =    -0.07018
        b_coef = -204.7*(current**2) + 4.492*current/self._batNum -0.07018
        c_coef = 100
        
        # Now we can approximate the depth of discharge
        dod = a_coef*(voltage**2) + b_coef*voltage + c_coef
        
        # and set the limits properly...
        if dod > 100:
            dod = 100
        elif dod < 0:
            dod = 0
        return 100-dod
    
    def _approxRemainingTime(self):
        ''' Approximates the remaining time of the battery assuming the load stats constant'''
        if self._soc == None:
            return None
        
        # First try.. incluedes the CAP.. thus not very good regarding aging/temp effect
        self._remainingTime = (float(self._soc)/100)*(60*1000*float(self._capacity)*float(self._usable_capacity))/float(self._isys_avg)
        if self._logger.isEnabledFor(logging.DEBUG):
            self._logger.debug('Remaining Time CAP APPROX (@Load %.2fmA): %.2f', self._isys_avg, self._remainingTime)
        
        if self._state == DISCHARGING:
            # Try to use the differential of the SoC..
            try:
                delta_t = self._hist_timestamps[-1] - self._hist_timestamps[0]
                delta_soc = - self.linreg(self._hist_timestamps, self._soc_hist)*delta_t
            except IndexError:
                delta_soc = 0
                delta_t = 0
            
            # make sure that we have a delta_soc which is significantly bigger than 0 for a safe computation of the remaining time
            if delta_soc > 0 and delta_t > 0 :
                remTime = float(self._soc)/float(delta_soc)*delta_t / 60
                if self._logger.isEnabledFor(logging.DEBUG):
                    self._logger.debug('Remaining Time GRADIENT(HSIZE) APPROX (@Load %.2fmA, delta_soc: %.2f, delta_t: %.1f, ): %.2f', self._isys_avg, delta_soc, delta_t, remTime)
            else:
                # The delta SoC is to small to compute a reasonable accurate remainingTime, so just use the less accurate capacity approximation
                remTime = (float(self._soc)/100)*(60*1000*float(self._capacity)*float(self._usable_capacity))/float(self._isys_avg)
                if self._logger.isEnabledFor(logging.DEBUG):
                    self._logger.debug('Remaining Time CAP APPROX (@Load %.2fmA, delta_soc: %.2f, delta_t: %.1f, ): %.2f', self._isys_avg, delta_soc, delta_t, remTime)
            
            self._remainingTimeDiff = remTime
        else: # we can not use the differential approach in other states than DISCHARGE, so use the capacity approach!
            self._remainingTimeDiff = self._remainingTime
    
    def predictRemainingTime(self):
        '''Predicts the current remaining Time with the current short-term averaged load.'''
        return self._remainingTime
    
    def predictRemainingTimeAtLoad(self, current):
        '''Predicts the current remaining Time with the current short-term averaged load.'''
        # rate the current
        if current == None or self._soc == None:
            return None
        
        current = current / (self._capacity*1000)
        remTime = (float(self._soc)/100)*(60*1000*float(self._capacity)*float(self._usable_capacity))/float(current)
        return remTime
    
    def _bulkApprox(self,v_sys,time):
        """Approximates the State of Charge during BULK charge process"""
        if self._logger.isEnabledFor(logging.DEBUG):
            self._logger.debug('BULK APPROX')
        
        # To enhance stability.. only start approximating bulk soc after 10 measure points.. voltage slope is more stable!
        if self._bulk_measures < 10:
            self._bulk_measures = self._bulk_measures+1
            return self._soc
        
        soc = self._soc
        
        
        self._v_bulk.append(v_sys)
        self._bulk_timestamps.append(time)
        
        # compute the bulk current by the slope of the voltage
        slope = self.linreg(self._bulk_timestamps,self._v_bulk)
        if self._logger.isEnabledFor(logging.DEBUG):
            self._logger.debug('BULK APPROX: slope: %.5f', slope)
        # i_bulk_rated is the averaged predicted charge current rated with the capacity of the battery...
        # x is the slope of the voltage / time plot
        # I_BULK_VoltSlopeFit(x) = p1*x + p2
        # Coefficients:
        #   p1 =       1.296
        #   p2 =    0.005573
        self._i_bulk_rated = 1.296*slope + 0.005573
        if self._logger.isEnabledFor(logging.DEBUG):
            self._logger.debug('BULK APPROX: i_bulk_rated: %.5f', self._i_bulk_rated)
        try:
            time_bulk = self._bulk_timestamps[-1]-self._bulk_timestamps[0]
            # the time we are in bulk and the averaged rated bulk current yields the gain of the soc
            delta_soc = time_bulk*self._i_bulk_rated
            if delta_soc >= 0:
                soc = self._last_discharge_soc + delta_soc
            else:
                soc = delta_soc
            if self._logger.isEnabledFor(logging.DEBUG):
                self._logger.debug('BULK APPROX ( delta_soc: %.2f, t_bulk: %.1f, I_bulk_rated: %.4f ): %.2f',  delta_soc , time_bulk, self._i_bulk_rated, soc)
        except IndexError:
            if self._logger.isEnabledFor(logging.DEBUG):
                self._logger.warning('BULK APPROX: INDEX ERROR')
            if soc > 100:
                soc = 100
        
        self._bulk_measures = self._bulk_measures+1
        return soc
    
    def _absorptionApprox(self,v_sys,time):
        """Approximates the State of Charge during ABSORPTION charge process"""
        # determine the exponential decay factor
        if self._i_bulk_rated == None:
            return self._soc
        landa = -0.1091*self._i_bulk_rated - 0.003891
        
        # determine the time between the measurements..
        if self._absorption_time != None and self._absorption_lasttime != None:
            delta_t = self._absorption_time  - self._absorption_lasttime
            self._absorption_lasttime =  self._absorption_time
            self._absorption_time = time
        else:
            delta_t = 0
            if self._absorption_time != None:
                self._absorption_lasttime =  self._absorption_time
                self._absorption_time = time
            else:
                self._absorption_time = time
        
        
        # delta_soc is the increase of soc since the last measurement..
        if self._soc != None and landa < 0:
            soc = 0
            try:
                delta_soc = delta_t*self._i_bulk_rated * math.exp(delta_t*landa)
            except OverflowError:
                delta_soc = 0
            if delta_soc > 0 and delta_soc < 100:
                soc = self._soc + delta_soc
                if soc >= 100:
                    soc = 100
                
                if soc < 0:
                    soc = 0
                return soc
        else:
            return self._soc
    
    def _update_soc(self, cur_soc,time):
        """Updates the weighted moving average of SOC and the history list of the SOC values, all SoC values are annotated with a timestamp"""
        if cur_soc == None or cur_soc > 100 or cur_soc < 0:
            return
        soc_sum = sum(self._soc_hist)
        length_old = float(len(self._soc_hist))
        self._soc_hist.append(cur_soc)
        self._hist_timestamps.append(time)
        while len(self._soc_hist) > self._updateHistorySize:
        # remove the too long stuff
            try:
                del self._soc_hist[0]
                del self._hist_timestamps[0]
            except IndexError:
                break
        
        length_new = float(len(self._soc_hist))
        # Updating the weighted moving average WMA_t+1 = WMA_t +n*measurement*2/((n+1)*n) - 2/((n+1)*n)*sum(measurement(t-n):measurement(t))
        # Check if we already have a full history, else the WMA is a bit more complicated...
        if length_new != length_old and self._soc != None:
        # We are increasing the history.. so use a different equation for the WMA...
            try:
                self._soc = length_old/(length_new+1)*self._soc + (2/(length_new+1))*float(cur_soc)
            except ZeroDivisionError:
                self._soc = cur_soc
        else:
            if self._soc != None:
                self._soc = self._soc + (2/(length_new+1))*float(cur_soc) - (2/((length_new+1)*length_new))*float(soc_sum)
            else:
                 self._soc = cur_soc
    
    def _stateApprox(self,v_sys, i_tot, temp, time):
        """approximates the current state of the battery by v_sys,i_sys and temp"""
        
        # If we operate in mains operation we can skip the rest of stateApprox...
        if self._state == MAINS:
            if self._logger.isEnabledFor(logging.DEBUG):
                self._logger.debug('State: MAINS')
            
            # If we have at least a voltage level of 11500-300 = 11200, we are definitively in MAINS..
            if self._vsys <= V_CHARGE_CONTROLLER_LIMIT-300:
                self._state = MAINS
                self._soc = 100
                return
            else:
                self._state = UNKNOWN
                self._logger.warning('Transistion from MAINS to UNKNOWN')
        
        # get the absorption_voltage and float_voltage thresholds
        absorption_voltage = self._get_absorption_voltage(temp)
        float_voltage = self._get_float_voltage(temp)
        
        # Computing the approximated SOC
        cur_soc = self._dischargeApprox(v_sys, i_tot, temp)
        if self._soc != None:
            soc_threshold = self._soc + SOC_MARGIN
        else:
            soc_threshold = 0
        if soc_threshold > 100:
            soc_threshold = 100
        
        # UNKNOWN STATE: try to get as fast as possible into a known state
        if self._state == UNKNOWN:
            if self._logger.isEnabledFor(logging.DEBUG):
                self._logger.debug('State: UNKNOWN')
            
            if self._vsys_avg <= V_CHARGE_CONTROLLER_LIMIT-200:
                # Most likely we have a 9V DC Power supply
                self._state = MAINS
                self._soc = 100
                return
            
            if len(self._soc_hist) < 5:
                # If we have less then 5 measurements make sure we stay in UNKNOWN. So that we can base our decisions on
                # the tendency...
                self._state = UNKNOWN
                self._update_soc(cur_soc,time)
                return
            # UNKNOWN => BULK
            if self._tendencyV == RISE and self._tendencyI != DROP and cur_soc < BULK_MAXSOC:
                self._state = BULK
                # Approximate the bulk soc and update the global soc
                cur_soc = self._bulkApprox(v_sys,time)
                self._update_soc(cur_soc,time)
            
            # UNKNOWN => DISCHARGING
            elif self._tendencyV != RISE and self._vsys_avg <= float_voltage-FLOAT_MARGIN:
                self._state = DISCHARGING
                # Approximate the discharge soc and update the global soc
                self._update_soc(cur_soc,time)
                self._first_discharge_soc = self._soc
                self._first_discharge_soc_time = time
            
            # UNKNOWN => ABSORPTION
            elif self._vsys_avg >= absorption_voltage - ABSORPTION_MARGIN:
                self._state = ABSORPTION
                # Approximate the soc in absorption and update the global soc
                self._last_bulk_soc = self._soc
                cur_soc = self._absorptionApprox(v_sys,time)
                self._update_soc(cur_soc,time)
            
            
            # UNKNOWN => FLOAT
            elif self._vsys_avg >= float_voltage - FLOAT_MARGIN and v_sys < float_voltage + FLOAT_MARGIN:
                self._state = FLOAT
                self._soc = 100
            
            self._prevState = UNKNOWN
        
        # DISCHARGING STATE
        elif self._state == DISCHARGING:
            if self._logger.isEnabledFor(logging.DEBUG):
                self._logger.debug('State: DISCHARGING')
            
            # From DISCHARGING we have transitions to BULK
            # Check for Bulk transition, if we have suddenly a X% higher SoC...
            if cur_soc > soc_threshold:
                self._state = BULK
                self._prevState = DISCHARGING
                self._v_bulk = []
                self._bulk_timestamps = []
                self._bulk_measures = 0
                # Approximate the bulk SoC and update the global SoC
                self._last_discharge_soc = self._soc
            else:
                self._state = DISCHARGING
                # Make sure we forget the old RISE tendencyV behavior, because it not a real bulk behavior
            
            # Do only approximate the SOC when current state is DISCHARGING.
            if self._state == DISCHARGING:
                self._update_soc(cur_soc,time)
        
        
        elif self._state == BULK:
            if self._logger.isEnabledFor(logging.DEBUG):
                self._logger.debug('State: BULK')
            
            # BULK => TRICKLE (think of some clever transition condition)
            #if cur_soc < self._soc and self._tendencyV = EVEN:
            #    self._state = TRICKLE
            #    self._prevState = BULK
            
            # BULK => ABSORPTION
            if self._vsys_avg >= absorption_voltage - ABSORPTION_MARGIN and cur_soc >= BULK_MAXSOC:
                self._state = ABSORPTION
                self._prevState = BULK
                # Approximate the soc in absorption and update the global soc
                self._last_bulk_soc = self._soc
                cur_soc = self._absorptionApprox(v_sys,time)
                self._update_soc(cur_soc,time)
            
            # BULK => DISCHARGING
            elif self._tendencyV == DROP:
                self._state = DISCHARGING
                self._prevState = BULK
                # Update the SOC with the current discharge approx
                self._update_soc(cur_soc,time)
                self._first_discharge_soc = self._soc
                self._first_discharge_soc_time = time
            
            # BULK => BULK
            else:
                self._state = BULK
                cur_soc = self._bulkApprox(v_sys,time)
                self._update_soc(cur_soc,time)
        
        elif self._state == ABSORPTION:
            if self._logger.isEnabledFor(logging.DEBUG):
                self._logger.debug('State: ABSOPTION')
            
            # ABSORPTION => ABSORPTION
            if self._vsys_avg >= absorption_voltage - ABSORPTION_MARGIN and self._vsys_avg <= absorption_voltage + ABSORPTION_MARGIN:
                self._state = ABSORPTION
                # Approximate the soc in absorption and update the global soc
                cur_soc = self._absorptionApprox(v_sys,time)
                self._update_soc(cur_soc,time)
            
            
            # ABSORPTION => FLOAT
            elif  self._vsys_avg >= float_voltage - FLOAT_MARGIN and self._vsys_avg <= float_voltage + FLOAT_MARGIN:
                self._state = FLOAT
                self._prevState = ABSORPTION
                # When we are go to float.. we are fully charged! Update it!
                cur_soc = 100
                self._update_soc(cur_soc,time)
            
            
            # ABSORPTION => DISCHARGING
            elif self._tendencyV == DROP and self._vsys_avg < absorption_voltage - ABSORPTION_MARGIN:
                self._state = DISCHARGING
                self._prevState = ABSORPTION
                # Update the SOC with the current discharge approx
                self._update_soc(cur_soc,time)
                self._first_discharge_soc = self._soc
                self._first_discharge_soc_time = time
            
            
            # ABSORPTION => TRICKLE
            else:
                self._state = TRICKLE
                self._prevState = ABSORPTION
        
        
        elif self._state == FLOAT:
            if self._logger.isEnabledFor(logging.DEBUG):
                self._logger.debug('State: FLOAT')
            # When we are in float, the battery is fully charged.. so throw away the WMA and set soc to 100
            self._soc = 100
            
            # FLOAT => DISCHARGING
            if self._vsys_avg < float_voltage:
                self._state = DISCHARGING
                self._prevState = FLOAT
                # Update the SOC with the current discharge approx
                self._update_soc(cur_soc,time)
                self._first_discharge_soc = self._soc
                self._first_discharge_soc_time = time
            
            # FLOAT => FLOAT
            else:
                self._state = FLOAT
        
        
        elif self._state == TRICKLE:
            if self._logger.isEnabledFor(logging.DEBUG):
                self._logger.debug('State: TRICKLE')
            
            # TRICKLE IS INTERMEDIATE STATE FOR UNCERTAIN CHARGE/DISCHARGING ACTIONS
            if self._tendencyV == DROP and self._prevState == ABSORPTION:
                self._state = DISCHARGING
                self._prevState = TRICKLE
            
            # FIXME: this should be extended in a second step..
        
        else:
            self._logger.warning('Non-Existing State')
        self._logger.debug('Current State: %s', states.get(self._state, 'unknown'))
        
        # Make sure that the bulk history is empty, when we are not in BULK
        if self._state != BULK and len(self._v_bulk) > 0:
            self._v_bulk = []
            self._bulk_timestamps = []
            if self._logger.isEnabledFor(logging.DEBUG):
                self._logger.debug('Clearing the BULK History!')
    
    
    def get_state(self):
        """returns the current state of the battery"""
        return int(self._state)
    
    def get_soc(self):
        """returns the current state of charge of the battery (int value between 0 and 100)"""
        if self._soc is None:
            return None
        else:
            return float(self._soc)
    
    def get_remainingTimeDiff_LT(self):
        """returns the remaining time of the battery assuming the current load is constant"""
        if self._soc == None or self._first_discharge_soc == None or self._first_discharge_soc_time == None:
            return None
        
        try:
            remTime = float(self._soc)/(float(self._first_discharge_soc)-float(self._soc))*(time.time()-self._first_discharge_soc_time) / 60
        except ZeroDivisionError:
            remTime = None
        if remTime == None or int(remTime) < 0:
            return None
        else:
            return int(remTime)
    
    def get_remainingTimeDiff(self):
        """returns the remaining time of the battery assuming the current load is constant"""
        if self._remainingTimeDiff == None or self._remainingTimeDiff < 0:
            return None
        else:
            return int(self._remainingTimeDiff)
    
    def get_remainingTime(self):
        """returns the remaining time of the battery assuming the current load is constant"""
        if self._remainingTime == None:
            return None
        else:
            return int(self._remainingTime)
    
    def get_voltage(self):
        """returns the current battery voltage"""
        try:
            voltage = int(self._vsys[-1])
        except IndexError:
            voltage = None
        return voltage
    
    def get_current(self):
        """returns the current system load in terms of current"""
        try:
            current = int(self._isys[-1])
        except IndexError:
            current = None
        return current
    
    def _get_absorption_voltage(self,temp):
        '''Returns the current absorption voltage threshold'''
        # These equation is a direct approximation of the values given in the datasheet
        return 0.239*(temp**2)-35.97*temp +15060
    
    def _get_float_voltage(self,temp):
        '''Returns the current float voltage threshold'''
        # These equation is a direct approximation of the values given in the datasheet
        if temp >= 45:
            return 13000
        return 0.239*(temp**2)-35.94*temp+14040
    
    def _mean(self,list):
        '''Returns the mean of the float values of a list'''
        if len(list) == 0:
            return float('nan')
        values = [float(x) for x in list]
        return sum(values) / len(list)
    
    def linreg(self, X, Y):
        '''Returns a linear regression of x and y..'''
        if len(X) != len(Y) or len(X) == 0:
            return 0
        
        N = len(X)
        Sx = Sy = Sxx = Syy = Sxy = 0.0
        for x, y in map(None, X, Y):
            Sx = Sx + x
            Sy = Sy + y
            Sxx = Sxx + x*x
            Syy = Syy + y*y
            Sxy = Sxy + x*y
        det = Sxx * N - Sx * Sx
        try:
            a = (Sxy * N - Sy * Sx)/det
        except ZeroDivisionError:
            a = 0
        return a


