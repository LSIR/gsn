# -*- coding: UTF-8 -*-
__author__      = "Daniel Burgener <danielbu@students.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Daniel Burgener"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import serial
import Queue
from threading import Event
import logging
import struct
import thread
import re
import time
import os
import pickle

import BackLogMessage
from AbstractPlugin import AbstractPluginClass


DEFAULT_BACKLOG = True

def enum(**enums):
    return type('Enum', (), enums)

# Define enumerations
E_GSN_MESSAGE_TYPE = enum(SAMPLING_RESULT=0, SAMPLER_STATUS=1)
E_MESSAGE_ORIGIN = enum(CRON=0, GSN=1, TOS=2)
D_MESSAGE_ORIGIN = {E_MESSAGE_ORIGIN.CRON:'schedule',
                    E_MESSAGE_ORIGIN.GSN:'GSN',
                    E_MESSAGE_ORIGIN.TOS: 'TinyOS'}
    
E_COMMAND_TYPE = enum(TAKE_SAMPLE=0, REINIT_SAMPLER=1, REPORT_STATUS=2)
E_GSN_MSG_COMMAND_TYPE = enum(TAKE_SAMPLE=2, REPORT_STATUS=3, GET_6712_REPORT=5, REINIT_SAMPLER=6)
E_SAMPLING_RESULT = enum(SAMPLING_DONE=0, 
                         SAMPLING_SKIPPED__SEE_STATUS=1,
                         SAMPLING_SKIPPED__SAMPLER_STATE_PROGRAM_IDLE=2,
                         SAMPLING_SKIPPED__SAMPLER_STATE_AT_STANDBY=3,
                         SAMPLING_SKIPPED__SAMPLER_STATE_PROGRAM_WAITING_STATE=4,
                         SAMPLING_SKIPPED__SAMPLER_STATE_PROGRAM_RUNNING_ENABLED=5,
                         SAMPLING_SKIPPED__SAMPLER_STATE_PROGRAM_HALTED=6,
                         SAMPLING_SKIPPED__BOTTLE_CAPACITY_EXEEDED=7,
                         SAMPLING_SKIPPED__INVALID_BOTTLE_NUMBER=8 )

E_ACTION_PARAMETER_TYPE = enum(TAKE_SAMPLE=1, REPORT_STATUS=2)

class Sampler6712PluginClass(AbstractPluginClass):
    '''
    This plugin is for the connection with a ISCO 6712 water sampler.
    
    Short description:
    The task of this plugin is to initialize the ISCO 6712 water sampler and
    to initiate samplings.
    
    a) sampling:
    Samplings are triggered by the scheduler, GSN messages or TOS messages. 
    Each trigger source can put a new sampling object into the task queue,
    which is handled by the run() function. If the 6712 sampler is in the 
    status PROGRAM_RUNNING and in the program status DISABLED, the sampling
    command is forwarded to the 6712. Otherwise the 6712 is first initialized
    (when the sampler is off) before the sampling command is forwarded or 
    the sampling is skipped (the case for all other 6712 states).
    
    b) initialization
    When the 6712 is in the state SAMPLER_OFF, it is initialized before 
    a sampling is initiated. During the initialization, the following 
    settings are configured:
        - nb of bottles and bottle volume
        - suction line length and suction head
        - nb of rinse cycles and retries
        - 6712 start condition (WAITING FOR PHONE CALL)
        - 6712 report (optional)
    Furthermore, the internal bottle status is initialized.
    After the configuration is complete, the 6712's running program waiting for
    the phone call is disabled and then started such that finally the 6712
    is in the state PROGRAM_RUNNING with program status DISABLED.
    
    The reason why the 6712's status is used is to detect the collection of
    the bottles which is done by the user. That is, the user needs to manually
    stop the running program by a keypad intervention. This will change
    the 6712's status, and the plugin will skip incoming sampling tasks. As
    soon as the user has finished the bottle collection, it needs to turn off
    the 6712 so that the 6712 is initialized by the next sample trigger.
    6712 status overview:
        - PROGRAM_RUNNING and DISABLED: sampling is possible
        - SAMPLER_OFF: 6712 needs to be initialized
        - other: bottles are being collected -> sampling is not possible
    
    '''

    def __init__(self, parent, options):
        AbstractPluginClass.__init__(self, parent, options, DEFAULT_BACKLOG)
        self._isBusy = True
        
        self._taskqueue = Queue.Queue() # queue where incoming GSN messages are stored
        self._wait = Event()
        self._plugstop = False
        
        # read device name (serial port name) from config file
        sDevice = self.getOptionValue('device_name')
        if sDevice is None:
            sDevice = '/dev/ttyUSB0'
            self.warning('no device_name specified in config file')
        
        # read baudrate from config file
        sBaudrate = self.getOptionValue('baudrate')
        if sBaudrate is None:
            baudrate = 19200
            self.warning('no baudrate specified in config file')
        else:
            try:
                baudrate = int(sBaudrate)
            except Exception as e:
                baudrate = 19200
                self.warning('invalid baudrate %s: %s' % (sBaudrate, e))
        
        # init serial port
        try:
            self.sampler = Sampler6712Driver([sDevice, baudrate])
        except Exception as e:
            raise Exception("Could not access 6712 sampler: %s" % e)
        
        # load bottle status
        sBottleStatusFile = self.getOptionValue('bottle_status_file')
        if sBottleStatusFile is None:
            raise Exception('No filename for bottle status defined!')
        else:
            self._bottles = Bottles(sBottleStatusFile)

    def stop(self):
        self._plugstop = True
        self._taskqueue.put('end') # send something to task queue to release blocking wait
        self._wait.set()
        try:
            self.sampler.stop()
        except Exception as e:
            self.warning('Sample couldn\'t been stopped: %s' %(e))
        self.info('stopped')
        
        
    def run(self):
        self.name = 'Sampler6712-Thread'
        
        # set configuration
        #sampler6712ProgramSettingsToSet = Sampler6712ProgramSettings(24, 0.5, 1.0, 'AUTO', 1, 1)
        sampler6712ProgramSettingsToSet = Sampler6712ProgramSettings(nbOfBottles = 24, 
                                                                     bottleVolumeInLit = 1, 
                                                                     suctionLineLengthInM = 10.0, 
                                                                     suctionLineHeadInM = 3.0, 
                                                                     nbOfRinseCycles = 0, 
                                                                     nbOfRetries = 1)

        while not self._plugstop:
            if self._taskqueue.empty(): # verify if tasks in task queue
                self._isBusy = False # queue is empty
            
            nbOfElementsInQueue = self._taskqueue.qsize()
            if nbOfElementsInQueue > 20:
                self.warning('Number of elements in queue is %d!!' % (nbOfElementsInQueue))
            elif nbOfElementsInQueue > 10:
                self.info('Number of elements in queue is %d' % (nbOfElementsInQueue))
                
            lQueueElement = self._taskqueue.get() # blocking wait for tasks
            self._isBusy = True
            
            # stop plugin
            if self._plugstop:
                try:
                    self._taskqueue.task_done()
                except ValueError, e:
                    self.exception(e)
                break # leave while loop
            
            # handle new task
            try:
                # take sample queue element
                if lQueueElement[1] == E_COMMAND_TYPE.TAKE_SAMPLE:
                    self.samplingObject = lQueueElement[2] # read command
                    
                    # check sampler state
                    # get the sampler's menu control status
                    (eMenuControlSamplerStatus, eMenuControlSamplerStatusExtension, eMenuControlProgramStatus) = self.sampler.eMenuControlGetSamplerStatus()
                    self.info('Menu control sampler status: %s' %(Sampler6712Driver.D_MENU_CONTROL_SAMPLER_STATUS[eMenuControlSamplerStatus]))
                    
                    if eMenuControlSamplerStatus == Sampler6712Driver.E_MENU_CONTROL_SAMPLER_STATUS.PROGRAM_RUNNING:
                        self.info('Menu control program status: %r' %(Sampler6712Driver.D_MENU_CONTROL_PROGRAM_STATUS[eMenuControlProgramStatus]))
                        # verify if program settings are correct
                        if eMenuControlProgramStatus == Sampler6712Driver.E_MENU_CONTROL_PROGRAM_STATUS.ENABLED:
                            self.sentSamplingResultToGsn(lQueueElement[0], self.samplingObject, Status6712(), E_SAMPLING_RESULT.SAMPLING_SKIPPED__SAMPLER_STATE_PROGRAM_RUNNING_ENABLED)
                            self.info('Sampling (bottle nb: %d, volume: %dml) skipped because bottles are collected.' % (self.samplingObject.bottleNb, self.samplingObject.waterVolumeInMl))                        
                    
                        elif eMenuControlProgramStatus == Sampler6712Driver.E_MENU_CONTROL_PROGRAM_STATUS.DISABLED:
                            # When program is running and disabled, it is assumed that it has been correctly 
                            # programmed. Therefore no modification are done!
                            self._takeSample(self.samplingObject, lQueueElement[0])
                        else:
                            raise Exception('Invalid menu control program status %r' %(eMenuControlProgramStatus))
                        
                    elif eMenuControlSamplerStatus == Sampler6712Driver.E_MENU_CONTROL_SAMPLER_STATUS.SAMPLER_OFF:
                        # initialize sampler
                        self.sampler.init6712(sampler6712ProgramSettingsToSet)
                        
                        # initialize internal bottle status
                        self._bottles.reinitBottles(nbOfBottles = sampler6712ProgramSettingsToSet.nbOfBottles, 
                                                    bottleVolumeInMl = sampler6712ProgramSettingsToSet.bottleVolumeInLit * 1000)

                        self._takeSample(self.samplingObject, lQueueElement[0])
                        
                    # sampler status is 'waiting to start' (waiting for phone call)
                    elif eMenuControlSamplerStatus == Sampler6712Driver.E_MENU_CONTROL_SAMPLER_STATUS.PROGRAM_WAITING_START:
                        self.sentSamplingResultToGsn(lQueueElement[0], self.samplingObject, Status6712(), E_SAMPLING_RESULT.SAMPLING_SKIPPED__SAMPLER_STATE_PROGRAM_WAITING_STATE)
                        self.info('sampling (bottle nb: %d, volume: %d) skipped because bottles are collected.' % (self.samplingObject.bottleNb, self.samplingObject.waterVolumeInMl))
                    
                    elif eMenuControlSamplerStatus == Sampler6712Driver.E_MENU_CONTROL_SAMPLER_STATUS.AT_STANDBY:
                        self.sentSamplingResultToGsn(lQueueElement[0], self.samplingObject, Status6712(), E_SAMPLING_RESULT.SAMPLING_SKIPPED__SAMPLER_STATE_AT_STANDBY)
                        self.info('sampling (bottle nb: %d, volume: %d) skipped because bottles are collected.' % (self.samplingObject.bottleNb, self.samplingObject.waterVolumeInMl))
                    
                    elif eMenuControlSamplerStatus == Sampler6712Driver.E_MENU_CONTROL_SAMPLER_STATUS.PROGRAM_IDLE:
                        self.sentSamplingResultToGsn(lQueueElement[0], self.samplingObject, Status6712(), E_SAMPLING_RESULT.SAMPLING_SKIPPED__SAMPLER_STATE_PROGRAM_IDLE)
                        self.info('sampling (bottle nb: %d, volume: %d) skipped because bottles are collected.' % (self.samplingObject.bottleNb, self.samplingObject.waterVolumeInMl))
                    
                    elif eMenuControlSamplerStatus == Sampler6712Driver.E_MENU_CONTROL_SAMPLER_STATUS.PROGRAM_HALTED:
                        self.sentSamplingResultToGsn(lQueueElement[0], self.samplingObject, Status6712(), E_SAMPLING_RESULT.SAMPLING_SKIPPED__SAMPLER_STATE_PROGRAM_HALTED)
                        self.info('sampling (bottle nb: %d, volume: %d) skipped because bottles are collected.' % (self.samplingObject.bottleNb, self.samplingObject.waterVolumeInMl))                        
                    
                    elif eMenuControlSamplerStatus == Sampler6712Driver.E_MENU_CONTROL_SAMPLER_STATUS.UNKNOWN:
                        raise Exception('Unknown menu control sampler status:')
                    
                    else:
                        raise Exception('Invalid menu control sampler status: %r' %(eMenuControlSamplerStatus))
                    
                elif lQueueElement[1] == E_COMMAND_TYPE.REINIT_SAMPLER:
                    # initialize sampler
                    self.sampler.init6712(sampler6712ProgramSettingsToSet)
                    
                    # initialize internal bottle status
                    self._bottles.reinitBottles(nbOfBottles = sampler6712ProgramSettingsToSet.nbOfBottles, 
                                                bottleVolumeInMl = sampler6712ProgramSettingsToSet.bottleVolumeInLit * 1000)
                
                elif lQueueElement[1] == E_COMMAND_TYPE.REPORT_STATUS:
                    # report sampler status
                    (eSamplerStatus, eSamplerStatusExtension, eProgramStatus, statusOf6712) = self.sampler.getCompleteStatus()
                    self.sentStatusToGsn(sampler6712ProgramSettingsToSet, eSamplerStatus, eSamplerStatusExtension, eProgramStatus, statusOf6712)
                    self.info('sent status report')
                    
                # unknown queue element
                else:
                    self.warning('Unknown command type of queue element: %d' % (lQueueElement[1]))
                
                self._taskqueue.task_done() # task complete
            except Exception, e:
                self.exception(str(e))

        self.info('died')
    
    def action(self, parameters):
        self.info('new action triggered')
        # actions are triggered by the schedule plugin
        # here the parameters have to be converted into 'take sample' object
        try:
            lAction = self._parseActionParameter(parameters)
            if lAction[0] == E_ACTION_PARAMETER_TYPE.TAKE_SAMPLE:
                self.info('Queue sample message from scheduler (bottle nb: %d, volume: %dml)' % (lAction[1].bottleNb, lAction[1].waterVolumeInMl))
                self._taskqueue.put([E_MESSAGE_ORIGIN.CRON, E_COMMAND_TYPE.TAKE_SAMPLE, lAction[1]])
            elif lAction[0] == E_ACTION_PARAMETER_TYPE.REPORT_STATUS:
                self.info('Queue report status message from scheduler')
                self._taskqueue.put([E_MESSAGE_ORIGIN.CRON, E_COMMAND_TYPE.REPORT_STATUS])
            else:
                self.info('Invalid schedule parameter: %r' % (parameters))
        except Exception as e:
            self.warning(e) 
            
    def msgReceived(self, data):
        # store data into queue (queue is handled in run())
        if data[0] == E_GSN_MSG_COMMAND_TYPE.TAKE_SAMPLE:
            try:
                samplingObject = Sampling(data[1], data[2])
                self.info('Queue sample message from GSN (bottle nb: %d, volume: %dml)' % (samplingObject.bottleNb, samplingObject.waterVolumeInMl))
                self._taskqueue.put([E_MESSAGE_ORIGIN.GSN, E_COMMAND_TYPE.TAKE_SAMPLE, samplingObject])
            except Exception as e:
                self.warning(e)
        elif data[0] == E_GSN_MSG_COMMAND_TYPE.GET_6712_REPORT:
            self.info('Sampler report request received -- Feature is not implemented')
            #self._taskqueue.put([E_MESSAGE_ORIGIN.GSN, E_COMMAND_TYPE.TAKE_SAMPLE, samplingObject])
        elif data[0] == E_GSN_MSG_COMMAND_TYPE.REINIT_SAMPLER:
            self.info('Queue reinit sampler message from GSN')
            self._taskqueue.put([E_MESSAGE_ORIGIN.GSN, E_COMMAND_TYPE.REINIT_SAMPLER])
        elif data[0] == E_GSN_MSG_COMMAND_TYPE.REPORT_STATUS:
            self.info('Queue report status message from GSN')
            self._taskqueue.put([E_MESSAGE_ORIGIN.GSN, E_COMMAND_TYPE.REPORT_STATUS])
        else:
            self.info('Unknown GSN command message type: %d' % (data[0]))
        
        
            
    def isBusy(self):
        return self._isBusy
        
        
    def needsWLAN(self):
        return False
            
    def ackReceived(self, timestamp):
        pass
        # self.info('Ack received from GSN: timestamp %r' %(timestamp))
        
    def _parseActionParameter(self, sParameter):
        '''
        This function parses the parameter of schedule entry. A schedule entry
        should look like as follows:
            bottle(BOTTLE NB) volume(WATER VOLUME) or
            report_status
        Whitespaces inside the brackets are not allowed!
        In case of a bottle/volume parameter, the parsed parameters are stored
        in a Sampling object, containing the bottle number and the water volume.
        @param sParameter: The parameters as one string given in the schedule 
                           file.

        @return: list with:
                1st elt: action type parameter (see E_ACTION_PARAMETER_TYPE)
                2nd elt: 
                    - TAKE_SAMPLE: Sampling object containing bottle number and volume
                    - REPORT_STATUS: 2nd element does not exit

        '''
        
        # search bottle and volume
        lBottleVolumeSearchResult = re.findall('.*bottle\((\d+)\).*volume\((\d+)\).*',sParameter)
        
        # search
        lStatusSearchResult = re.findall('.*(report_status).*', sParameter)
        
        if lBottleVolumeSearchResult != []:
            if len(lBottleVolumeSearchResult[0]) == 2:
                bottleNb = int(float(lBottleVolumeSearchResult[0][0])) # convert string into number
                waterVolumeInMl = int(float(lBottleVolumeSearchResult[0][1])) # convert string into number
                sampling = Sampling(bottleNb, waterVolumeInMl) # create new Sampling object to store
                eActionParameterType = E_ACTION_PARAMETER_TYPE.TAKE_SAMPLE
                return [eActionParameterType, sampling]
            else:
                raise Exception('Invalid bottle volume parameter in schedule job (%r)' %(sParameter))
        elif lStatusSearchResult != []:
            eActionParameterType = E_ACTION_PARAMETER_TYPE.REPORT_STATUS
            return [eActionParameterType] 
        else:
            raise Exception('Invalid schedule job parameter')
            
    def _takeSample(self, samplingObject, samplingTrigger):
        '''
        This function takes a sample and reports the result to the GSN
        
        @param samplingObject: sampling object of type Sampling
        @param samplingTrigger: origin of sampling command (see E_MESSAGE_ORIGIN)
        '''
        # start sampling
        if(isinstance(samplingObject, Sampling)):
            self.info('Attempt to take sample (bottle nb: %d, volume: %dml)' % (samplingObject.bottleNb, samplingObject.waterVolumeInMl))

            # check if bottle number is valid
            if self._bottles.bVerifyBottleNb(samplingObject.bottleNb) == False: 
                self.info('Sampling skipped: Invalid bottle number (%d)' %(samplingObject.bottleNb))
                # invalid bottle number
                samplingResult = E_SAMPLING_RESULT.SAMPLING_SKIPPED__INVALID_BOTTLE_NUMBER
                status = Status6712() # empty status object
            else:
                # verify if bottle capacity is enough to hold new sampling
                if self._bottles.checkCapacity(samplingObject.bottleNb, samplingObject.waterVolumeInMl) == True:
                    # bottle capacity is enough to hold new sampling
                    # take a sample
                    status = self.sampler.oExternalProgramControlTakeSample(self.samplingObject)
                    
                    # the bottle state is only updated when the sampling command
                    # was accepted, that is when the status is 'sample in progress'
                    if status.samplerStatus == Sampler6712Driver.E_EXTERNAL_PROGRAM_CONTROL_SAMPLER_STATUS.SAMPLE_IN_PROGRESS:
                        # take sample command accepted by sampler
                        # wait until sampling complete (reading status)
                        timeoutCounter = 200
                        while status.samplerStatus == Sampler6712Driver.E_EXTERNAL_PROGRAM_CONTROL_SAMPLER_STATUS.SAMPLE_IN_PROGRESS or \
                              status.mostRecentSamplingResult == Sampler6712Driver.E_EXTERNAL_PROGRAM_CONTROL_LAST_SAMPLE_RESULT.SAMPLE_IN_PROGRESS and\
                              timeoutCounter > 0:
                            timeoutCounter -= 1
                            status = self.sampler.getExternalProgramControlStatus() # read current status
                            self.info('Status during sampling: %r, recent sampling result: %r' % (Sampler6712Driver.D_EXTERNAL_PROGRAM_CONTROL_SAMPLER_STATUS[status.samplerStatus], Sampler6712Driver.D_EXTERNAL_PROGRAM_CONTROL_LAST_SAMPLE_RESULT[status.mostRecentSamplingResult]))
                            
                        if timeoutCounter == 0:
                            self.warning('Sampling last to long, status polling timed out')
                        
                        # update internal bottle state
                        self._bottles.fill(bottleNb = samplingObject.bottleNb, 
                                           volumeToAddInMl = samplingObject.waterVolumeInMl, 
                                           state = status.mostRecentSamplingResult)

                        # sampling done, for more details about sampling result
                        # see most recent sampling result
                        samplingResult = E_SAMPLING_RESULT.SAMPLING_DONE
                        
                        if status.mostRecentSamplingBottle != samplingObject.bottleNb or \
                           status.mostRecentSamplingVolume != samplingObject.waterVolumeInMl:
                            raise Exception('Most recent sampling result (B%d, V%dml) does not fit with sampling object (B%d, V%dml)' \
                                        %(status.mostRecentSamplingBottle, status.mostRecentSamplingVolume, samplingObject.bottleNb, samplingObject.waterVolumeInMl))
                        
                    else:
                        # take sample command not accepted by sampler
                        # sampling skipped, for more details see sampler status
                        samplingResult = E_SAMPLING_RESULT.SAMPLING_SKIPPED__SEE_STATUS
                    
                    self.info('B%d - Status after sampling: %r, recent sampling result: %r, bottle level: %dml'\
                        % (samplingObject.bottleNb, Sampler6712Driver.D_EXTERNAL_PROGRAM_CONTROL_SAMPLER_STATUS[status.samplerStatus], Sampler6712Driver.D_EXTERNAL_PROGRAM_CONTROL_LAST_SAMPLE_RESULT[status.mostRecentSamplingResult], self._bottles.getLevel(samplingObject.bottleNb) ))
                else:
                    # bottle capacity is not enough to hold new sampling
                    self.info('Sampling skipped: Capacity of bottle %d exceeded (level: %dml, sampling volume: %dml)' \
                        %(samplingObject.bottleNb, self._bottles.getLevel(samplingObject.bottleNb), samplingObject.waterVolumeInMl))
                    samplingResult = E_SAMPLING_RESULT.SAMPLING_SKIPPED__BOTTLE_CAPACITY_EXEEDED
                    status = Status6712() # empty status object
                    
            # sent sampling result to GSN
            self.sentSamplingResultToGsn(samplingTrigger, samplingObject, status, samplingResult)
        else:
            self.error('Wrong queue object') 

    def sentSamplingResultToGsn(self, samplingTrigger, samplingObject, status, samplingResult):
        # report sampling result
        timestamp = self.getTimeStamp()
        lGsnMsgPayload = []
        lGsnMsgPayload += [E_GSN_MESSAGE_TYPE.SAMPLING_RESULT] # set type of gsn msg
        lGsnMsgPayload += [samplingTrigger] # sampling trigger
        lGsnMsgPayload += [samplingObject.bottleNb, samplingObject.waterVolumeInMl] # bottle nb and volume
        lGsnMsgPayload += [samplingResult] # set sampling result
        lGsnMsgPayload += status.getStatusAsList() # set sampling result
        
        self.info('Sampling result sent to GSN')
        self.processMsg(timestamp, lGsnMsgPayload)

    def sentStatusToGsn(self, sampler6712ProgramSettingsToSet, eSamplerStatus, eSamplerStatusExtension, eProgramStatus, statusOf6712):
        '''
        This function sends the sampler status to the GSN
        @param sampler6712ProgramSettingsToSet: program settings (object of type Sampler6712ProgramSettings)
        @param eSamplerStatus: menu control mode sampler status (E_MENU_CONTROL_SAMPLER_STATUS)
        @param eSamplerStatusExtension: menu control mode sampler status extension (E_MENU_CONTROL_SAMPLER_STATUS_EXTENSION)
        @param eProgramStatus: menu control mode program status (E_MENU_CONTROL_PROGRAM_STATUS)
        @param statusOf6712: extenden program control mode status (STS,1)
        '''
        # report sampling result
        timestamp = self.getTimeStamp()
        lGsnMsgPayload = []
        lGsnMsgPayload += [E_GSN_MESSAGE_TYPE.SAMPLER_STATUS] # set type of gsn msg
        lGsnMsgPayload += sampler6712ProgramSettingsToSet.lGetSettingsAsGsnList() # sampling trigger
        lGsnMsgPayload += [eSamplerStatus]
        lGsnMsgPayload += [eSamplerStatusExtension]
        lGsnMsgPayload += [eProgramStatus]
        lGsnMsgPayload += statusOf6712.getStatusAsList() # set sampling result
        
        self.info('Sampler status sent to GSN')
        self.processMsg(timestamp, lGsnMsgPayload)

class Delay():
    def __init__(self, delayInSec):
        self.delayInSec = delayInSec
        
class Navigation():
    # Define enumerations
    E_SEARCH_TYPE = enum(SCREEN=0, SELECTION=1)
    
    def __init__(self, eSearchType, lsSearchText, foundAction, maxNbOfFoundAction, foundExpirationAction, notFoundAction, maxNbOfNotFoundAction, notFoundExpirationAction):
        '''
        Initialization of navigation object. This object defines how a target 
        screen or menu element can be reached.
        
        @param eSearchType: defines wether the navigation target is a certain screen or menu element
        @param lsSearchText: list with strings defining the target (screen or menu element)
        
        @param foundAction: defines action to perform when search text was found
        @param maxNbOfFoundAction: defines how often the found action is performed
            before the expiration action is called
        @param foundExpirationAction: action to perform when found counter has
            expired
            
        @param navigation: defines mean to navigate to the search text. can be 
            char or another navigation object. When navigation is 'None' navigation
            is skipped
        
        @param maxNbOfNavigation: nb of attempts to reach target
        @param sNavigationSuccessAction: action when target is found (string to send to serial port)
        '''
        self.lsSearchText = lsSearchText # text to search
        self.eSearchType = eSearchType # type of text
        
        self.foundAction = foundAction 
        self.maxNbOfFoundAction = maxNbOfFoundAction
        self.foundExpirationAction = foundExpirationAction
        
        self.notFoundAction = notFoundAction 
        self.maxNbOfNotFoundAction = maxNbOfNotFoundAction
        self.notFoundExpirationAction = notFoundExpirationAction
        
    def sGetFormatedNavigation(self):
        '''
        This function returns the object properties as string
        @return: object properties as string
        '''
        sFormatedNavigation = ('Src type: %r, src text: %r, fa: %r, nb of fa: %r, fea: %r, nfa: %r, nb of nfa: %r, fea: %r,' % (self.eSearchType, self.lsSearchText, self.foundAction, self.maxNbOfFoundAction, self.foundExpirationAction, self.notFoundAction, self.maxNbOfNotFoundAction, self.notFoundExpirationAction))
        return sFormatedNavigation

class Sampler6712Driver():
    # The following constant value defines the time to wait after a question mark
    # has been sent and the banner string is expected
    _DELAY_BETWEEN_QUESTION_MARK_AND_BANNER_STRING = 0.6

    # The following constant value defines the time to wait after the banner string
    # (string containing the 6712 HW and SW revision) and the menu is displayed
    _DELAY_BETWEEN_BANNER_STRING_AND_MENU = 2.0

    # The following constant value defines the time required to display two 
    # consecutive lines of the menu
    _DELAY_DURING_MENU_DISPLAY = 1.5

    # The following constant value defines the time to wait for new data
    # after the caret symbol '> '
    _DELAY_AFTER_CARET = 0.5

    # The following constant value defines the time to wait for new data
    # after a menu transition command has been sent
    _DELAY_AFTER_MENU_TRANSITION_COMMAND = 2

    # The following constant value defines the time required to turn on the
    # water sampler and report its status
    _DELAY_BETWEEN_TURN_ON_SAMPLER_AND_REPLY = 3

    # The following constant value defines the time between the status request
    # and the reply
    _DELAY_BETWEEN_STATUS_REQUEST_AND_REPLY = 2

    # The following constant value defines the time between the sampling request
    # and status reply
    _DELAY_BETWEEN_SAMPLING_REQUEST_AND_STATUS_REPLY = 2

    # The following constant value defines the time required by the 6712
    # to reply on a input when in remote control of sampler keypad mode
    _DELAY_BETWEEN_CONTROL_COMMAND_AND_REPLY = 3

    # The following constant value defines the time required by the 6712
    # to reply on a status request in the 'menu control' mode
    _DELAY_BETWEEN_MENU_CONTROL_STATUS_REQUEST_AND_REPLY = 2

    # The following constant value defines the time required by the 6712
    # to reply on a 'START' in the 'menu control' mode
    _DELAY_BETWEEN_MENU_CONTROL_STATUS_START_AND_REPLY = 2

    # The following constant value defines the time required by the 6712
    # to reply on a 'DISABLE' in the 'menu control' mode
    _DELAY_BETWEEN_MENU_CONTROL_STATUS_DISABLE_AND_REPLY = 2

    # sampler overal stati:
    E_SAMPLER_OVERAL_STATUS = enum(OK=0, COMMUNICATION_ERROR=1, SERIAL_PORT_ERROR=2)
    
    E_REMOTE_OPERATION_MODE = enum(UNKNOWN_MODE=0, 
                                   OFFLINE_MODE=1, 
                                   EXTERNAL_PROGRAM_CONTROL_MODE=2,
                                   MENU_CONTROL_MODE=3,
                                   REMOTE_CONTROL_OF_SAMPLER_KEYPAD_MODE=4,
                                   SAMPLING_REPORTS_MODE=5)

    # dictionary with all possible 6712 remote control mode numbers (keys)
    # OFFLINE_MODE and SAMPLING_REPORTS_MODE are not possible because they
    # are left when the function is called (due to the sending of '?')
    D_REMOTE_OPERATION_MODE ={0:'UNKNOWN_MODE',
                              1:'OFFLINE_MODE',
                              2:'EXTERNAL_PROGRAM_CONTROL_MODE',
                              3:'MENU_CONTROL_MODE',
                              4:'REMOTE_CONTROL_OF_SAMPLER_KEYPAD_MODE',
                              5:'SAMPLING_REPORTS_MODE'}
    
    E_MENU_CONTROL_SAMPLER_STATUS = enum(PROGRAM_WAITING_START=0, 
                                 PROGRAM_RUNNING=1, 
                                 AT_STANDBY=2, 
                                 PROGRAM_IDLE=3, 
                                 SAMPLER_OFF=4, 
                                 PROGRAM_HALTED=5,
                                 UNKNOWN=6)
    
    D_MENU_CONTROL_SAMPLER_STATUS = {E_MENU_CONTROL_SAMPLER_STATUS.PROGRAM_WAITING_START: 'PROGRAM_WAITING_START', 
                                     E_MENU_CONTROL_SAMPLER_STATUS.PROGRAM_RUNNING: 'PROGRAM_RUNNING', 
                                     E_MENU_CONTROL_SAMPLER_STATUS.AT_STANDBY: 'AT_STANDBY', 
                                     E_MENU_CONTROL_SAMPLER_STATUS.PROGRAM_IDLE: 'PROGRAM_IDLE', 
                                     E_MENU_CONTROL_SAMPLER_STATUS.SAMPLER_OFF: 'SAMPLER_OFF', 
                                     E_MENU_CONTROL_SAMPLER_STATUS.PROGRAM_HALTED: 'PROGRAM_HALTED',
                                     E_MENU_CONTROL_SAMPLER_STATUS.UNKNOWN: 'UNKNOWN'}
    
    E_MENU_CONTROL_PROGRAM_STATUS = enum(ENABLED=0, 
                                         DISABLED=1, 
                                         UNKNOWN=2)
    
    D_MENU_CONTROL_PROGRAM_STATUS = {E_MENU_CONTROL_PROGRAM_STATUS.ENABLED: 'ENABLED', 
                                     E_MENU_CONTROL_PROGRAM_STATUS.DISABLED: 'DISABLED', 
                                     E_MENU_CONTROL_PROGRAM_STATUS.UNKNOWN: 'UNKNOWN'}

    E_MENU_CONTROL_SAMPLER_STATUS_EXTENSION = enum(NONE=0, 
                                                   ERRORS=1, 
                                                   UNKNOWN=2)
    
    E_EXTERNAL_PROGRAM_CONTROL_SAMPLER_STATUS = enum(   WAITING_TO_SAMPLE=1,
                                                        POWER_FAILED=4,
                                                        PUMP_JAMMED=5,
                                                        DISTRIBUTOR_JAMMED=6,
                                                        SAMPLER_OFF=9,
                                                        SAMPLE_IN_PROGRESS=12,
                                                        INVALID_COMMAND=20,
                                                        CHECKSUM_MISMATCH=21,
                                                        INVALID_BOTTLE=22,
                                                        VOLUME_OUT_OF_RANGE=23)

    # dictionary with all possible 6712 status numbers(keys)
    # see datasheet p.7-5
    D_EXTERNAL_PROGRAM_CONTROL_SAMPLER_STATUS = {   
        E_EXTERNAL_PROGRAM_CONTROL_SAMPLER_STATUS.WAITING_TO_SAMPLE: 'WAITING TO SAMPLE',
        E_EXTERNAL_PROGRAM_CONTROL_SAMPLER_STATUS.POWER_FAILED: 'POWER FAILED',
        E_EXTERNAL_PROGRAM_CONTROL_SAMPLER_STATUS.PUMP_JAMMED: 'PUMP JAMMED',
        E_EXTERNAL_PROGRAM_CONTROL_SAMPLER_STATUS.DISTRIBUTOR_JAMMED: 'DISTRIBUTOR JAMMED',
        E_EXTERNAL_PROGRAM_CONTROL_SAMPLER_STATUS.SAMPLER_OFF: 'SAMPLER OFF',
        E_EXTERNAL_PROGRAM_CONTROL_SAMPLER_STATUS.SAMPLE_IN_PROGRESS: 'SAMPLE IN PROGRESS',
        E_EXTERNAL_PROGRAM_CONTROL_SAMPLER_STATUS.INVALID_COMMAND: 'INVALID COMMAND',
        E_EXTERNAL_PROGRAM_CONTROL_SAMPLER_STATUS.CHECKSUM_MISMATCH: 'CHECKSUM MISMATCH',
        E_EXTERNAL_PROGRAM_CONTROL_SAMPLER_STATUS.INVALID_BOTTLE: 'INVALID BOTTLE',
        E_EXTERNAL_PROGRAM_CONTROL_SAMPLER_STATUS.VOLUME_OUT_OF_RANGE: 'VOLUME OUT OF RANGE'}
    
    E_EXTERNAL_PROGRAM_CONTROL_LAST_SAMPLE_RESULT = enum(SAMPLE_OK=0,
                                                         NO_LIQUID_FOUND=1,
                                                         LIQUID_LOST=2,
                                                         USER_STOPPED=3,
                                                         POWER_FAILED=4,
                                                         PUMP_JAMMED=5,
                                                         DISTRIBUTOR_JAMMED=6,
                                                         PUMP_LATCH_OPEN=8,
                                                         SAMPLER_SHUT_OFF=9,
                                                         NO_DISTRIBUTOR=11,
                                                         SAMPLE_IN_PROGRESS=12)
    
    # dictionary with all possible 6712 sampling results numbers (keys)
    # see datasheet p.7-5
    D_EXTERNAL_PROGRAM_CONTROL_LAST_SAMPLE_RESULT = {
        E_EXTERNAL_PROGRAM_CONTROL_LAST_SAMPLE_RESULT.SAMPLE_OK: 'SAMPLE OK',
        E_EXTERNAL_PROGRAM_CONTROL_LAST_SAMPLE_RESULT.NO_LIQUID_FOUND: 'NO LIQUID FOUND',
        E_EXTERNAL_PROGRAM_CONTROL_LAST_SAMPLE_RESULT.LIQUID_LOST: 'LIQUID LOST',
        E_EXTERNAL_PROGRAM_CONTROL_LAST_SAMPLE_RESULT.USER_STOPPED: 'USER STOPPED',
        E_EXTERNAL_PROGRAM_CONTROL_LAST_SAMPLE_RESULT.POWER_FAILED: 'POWER FAILED',
        E_EXTERNAL_PROGRAM_CONTROL_LAST_SAMPLE_RESULT.PUMP_JAMMED: 'PUMP JAMMED',
        E_EXTERNAL_PROGRAM_CONTROL_LAST_SAMPLE_RESULT.DISTRIBUTOR_JAMMED: 'DISTRIBUTOR JAMMED',
        E_EXTERNAL_PROGRAM_CONTROL_LAST_SAMPLE_RESULT.PUMP_LATCH_OPEN: 'PUMP LATCH OPEN',
        E_EXTERNAL_PROGRAM_CONTROL_LAST_SAMPLE_RESULT.SAMPLER_SHUT_OFF: 'SAMPLER SHUT OFF',
        E_EXTERNAL_PROGRAM_CONTROL_LAST_SAMPLE_RESULT.NO_DISTRIBUTOR: 'NO DISTRIBUTOR',
        E_EXTERNAL_PROGRAM_CONTROL_LAST_SAMPLE_RESULT.SAMPLE_IN_PROGRESS: 'SAMPLE IN PROGRESS'}

    N_NAVIGATE_TO_MAIN_MENU_SCREEN = Navigation(Navigation.E_SEARCH_TYPE.SCREEN, ['  PROGRAM  '], None, 0, None, 'S', 10, Exception('Navigation to main menu failed'))
    
    def __init__(self, config):
        self._logger = logging.getLogger(self.__class__.__name__)

        self._logger.info('Init Sampler 6712 Driver...')
        
        self._samplerOveralStatus = self.E_SAMPLER_OVERAL_STATUS.OK
        
        self._serialStr = config[0]
        self._serialBaudrate = int(config[1])
        
        self._eInternalRemoteOperationMode = self.E_REMOTE_OPERATION_MODE.UNKNOWN_MODE
        self._lastInternalRemoteOperationModeUpdateTime = 0
        
        # open serial port
        try:
            self._serial = serial.Serial(self._serialStr, self._serialBaudrate, bytesize=serial.EIGHTBITS, parity=serial.PARITY_NONE, stopbits=serial.STOPBITS_ONE, timeout=2)
            self._logger.info("Serial port successfully opened " + str(self._serial))
            self._serial.flushInput()
            self._serial.flushOutput()
        except Exception as e:
            raise Exception ("Serial access exception " + str(e))
            self._samplerOveralStatus = self.E_SAMPLER_OVERAL_STATUS.SERIAL_PORT_ERROR # change overal status
            self._serial = 0
            return
        
        self._vt100Decoder = Vt100Decoder(self._serial, self._logger)
        
        # try to determine the sampler status
        (eMenuControlSamplerStatus, eMenuControlSamplerStatusExtension, eMenuControlProgramStatus) = self.eMenuControlGetSamplerStatus()


    def getCompleteStatus(self):
        '''
        This function returns the 6712 water sampler overal status.
        @return: overall status
        '''
        try:
            (eSamplerStatus, eSamplerStatusExtension, eProgramStatus) = self.eMenuControlGetSamplerStatus()
        except Exception as e:
            eSamplerStatus = self.E_MENU_CONTROL_SAMPLER_STATUS.UNKNOWN
            eSamplerStatusExtension = self.E_MENU_CONTROL_SAMPLER_STATUS_EXTENSION.UNKNOWN
            eProgramStatus = self.E_MENU_CONTROL_PROGRAM_STATUS.UNKNOWN
            self._logger.error(e)
            
        try:
            # set remote operation mode to 'external program control' to
            # allow the status read
            eCurrentMode, sReadReply = self._set6712RemoteOperationMode(self.E_REMOTE_OPERATION_MODE.EXTERNAL_PROGRAM_CONTROL_MODE)
            statusOf6712 = self.getExternalProgramControlStatus()
        except Exception as e:
            statusOf6712 = Status6712()
            self._logger.error(e)
        
        return (eSamplerStatus, eSamplerStatusExtension, eProgramStatus, statusOf6712)

    def set6712ExtendedProgrammingLevel(self):
        '''
        This function sets the sampler into the EXTENDED programming level. The 
        sampler has to be stopped before calling this function, so that
        the main menu is reachable!
        '''
        # send one cr and then wait 10sec. (that is, maxNbOfRepetition needs to be set to 2!)
        N_SELECT_DONE_AND_WAIT = Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['XXX'], None, 0, None, '\r', 1, Delay(10))
        

         
        LN_SET_EXTENDED_PROGRAMMING_LEVEL = [
            Navigation(Navigation.E_SEARCH_TYPE.SCREEN, ['  PROGRAM  '], None, 0, '6712.2', 'S', 10 , Exception('Navigation to main menu failed')),
            Navigation(Navigation.E_SEARCH_TYPE.SCREEN, ['DOWNLOAD DATA NOW','OR LOSE ALL DATA!'], None, 0, N_SELECT_DONE_AND_WAIT, Delay(0.4), 10, None),
            ]
        
        self._logger.info('Set programming level to EXTENDED')
        
        # set extended programming level. If sampler is not already in extended programming
        # level, the device will reboot. Active remote operation mode detection is enabled
        # because after programming level change, operation mode is different
        self.doRemoteControlOfSamplerKeypadNavigation(LN_SET_EXTENDED_PROGRAMMING_LEVEL, forceActiveRemotOperationModeDetectionAfterNavigation = True)
        
    def oExternalProgramControlTurnOn6712(self):
        '''
        This function turns the 6712 water sampler on. Turn on is repeated
        up to three times before an exception is thrown.
        @return: parsed status
        '''
        sCommand = 'STS,2' # command to send to 6712
        # change overal status, is set to OK if turn on successful
        self._samplerOveralStatus = self.E_SAMPLER_OVERAL_STATUS.COMMUNICATION_ERROR 
        
        counter = 4 # number of times to attemp to turn on 6712
        while counter > 0:
            counter -= 1

            # set external control operation mode
            try:
                self._set6712RemoteOperationMode(self.E_REMOTE_OPERATION_MODE.EXTERNAL_PROGRAM_CONTROL_MODE) 
            except Exception as e:
                if counter > 0:
                    self._logger.warning('Setting remote operation to %s failed: %s' % (self.D_REMOTE_OPERATION_MODE[2], e))
                    continue # go to start of while and try again to set mode
                else:
                    raise Exception('Sampler turn on failed') # leave function

            # turn on 6712
            self._logger.info('Attempt to turn on 6712 sampler')
            try:
                statusOf6712 = self._ExternalProgramControlSendCmdAndReadStatusReply(self._DELAY_BETWEEN_TURN_ON_SAMPLER_AND_REPLY, sCommand)
                self._samplerOveralStatus = self.E_SAMPLER_OVERAL_STATUS.OK # change overal status to OK
                
                # stop loop
                if statusOf6712.samplerStatus != Sampler6712Driver.E_EXTERNAL_PROGRAM_CONTROL_SAMPLER_STATUS.SAMPLER_OFF:
                    self._logger.info('6712 sampler turned on')
                    break # leave loop

            except Exception as e:
                if counter > 0:
                    self._logger.warning('Command %r failed: %r' % (sCommand, e))
                else:
                    raise Exception('Sampler turn on failed') # leave function

        return statusOf6712

    def getExternalProgramControlStatus(self):
        '''
        This function reads the status of the 6712. Before calling this function,
        it has to be ensured that the 6712 is online and in the external control
        operation mode! An exception is thrown if the status could not been read.
        @return: parsed status
        '''
        sCommand = 'STS,1'
        # change overal status to error, is set to OK if status reading is successful
        self._samplerOveralStatus = self.E_SAMPLER_OVERAL_STATUS.COMMUNICATION_ERROR 

        # send command and read reply
        try:
            statusOf6712 = self._ExternalProgramControlSendCmdAndReadStatusReply(self._DELAY_BETWEEN_STATUS_REQUEST_AND_REPLY, sCommand)
        except Exception as e:
            raise Exception('Command \'%s\'failed: %s' % (sCommand, e))
        
        self._samplerOveralStatus = self.E_SAMPLER_OVERAL_STATUS.OK # change overal status
        return statusOf6712

    def oExternalProgramControlTakeSample(self, samplingObject):
        '''
        This function initiates a single sampling. An exception is thrown if the 
        status could not been read.
        @param samplingObject: object defining which bottle to use and how much 
        to sample 
        @return: parsed status
        '''
        sCommand = 'BTL,%d,SVO,%d' % (samplingObject.bottleNb, samplingObject.waterVolumeInMl)
        # change overal status, is set to OK if take sample successful
        self._samplerOveralStatus = self.E_SAMPLER_OVERAL_STATUS.COMMUNICATION_ERROR 
    
        # try to set external control operation mode
        try:
            self._set6712RemoteOperationMode(self.E_REMOTE_OPERATION_MODE.EXTERNAL_PROGRAM_CONTROL_MODE) 
        except Exception as e:
            raise Exception('Setting remote operation to %s failed: %s' % (self.D_REMOTE_OPERATION_MODE[2], e))
        
        # try to send command and read reply
        try:
            statusOf6712 = self._ExternalProgramControlSendCmdAndReadStatusReply(self._DELAY_BETWEEN_SAMPLING_REQUEST_AND_STATUS_REPLY, sCommand)
        except Exception as e:
            raise Exception('Command %r failed: %s' % (sCommand, e))
        
        self._samplerOveralStatus = self.E_SAMPLER_OVERAL_STATUS.OK # change overal status
        return statusOf6712

    def init6712(self, sampler6712ProgramSettingsToSet):
        '''
        This function configures the 6712 according to the parameter containing
        the program settings. 
        After the initialization, the sampler is set into the 
        'Program Running (disable)' state.
        
        @param sampler6712ProgramSettingsToSet: program settings
        '''
        
        self._logger.info('Initialize 6712')
        
        # turn on sampler
        counter = 100
        while counter > 0:
            counter -= 1
            try:
                # get the sampler's menu control status
                (eMenuControlSamplerStatus, eMenuControlSamplerStatusExtension, eMenuControlProgramStatus) = self.eMenuControlGetSamplerStatus()
                self._logger.info('Menu control sampler status: %s' %(Sampler6712Driver.D_MENU_CONTROL_SAMPLER_STATUS[eMenuControlSamplerStatus]))
                
                # program is running
                if eMenuControlSamplerStatus == Sampler6712Driver.E_MENU_CONTROL_SAMPLER_STATUS.PROGRAM_WAITING_START or \
                   eMenuControlSamplerStatus == Sampler6712Driver.E_MENU_CONTROL_SAMPLER_STATUS.PROGRAM_RUNNING:
                    # Stop running program
                    self.stopSamplerProgram()
                
                # sampler is in standby or idle
                elif eMenuControlSamplerStatus == Sampler6712Driver.E_MENU_CONTROL_SAMPLER_STATUS.AT_STANDBY or \
                     eMenuControlSamplerStatus == Sampler6712Driver.E_MENU_CONTROL_SAMPLER_STATUS.PROGRAM_IDLE:
                # program is not running, that is, sampler needs to be configured!
                    # set programming level to extended
                    self.set6712ExtendedProgrammingLevel()
                    
                    # init sampler
                    lnNavigationList = []
                    # the software options have to set first, because the other
                    # navigation relies on it (programmin style is set to 
                    # QUICK VIEW)
                    lnNavigationList += sampler6712ProgramSettingsToSet.getSoftwareOptionsNavigationList() 
                    lnNavigationList += sampler6712ProgramSettingsToSet.getProgramStartNavigationList() # configure start condition
                    lnNavigationList += sampler6712ProgramSettingsToSet.getBottleNavigationList()  # configure nb of bottles, bottle volume etc.
                    #lnNavigationList += sampler6712ProgramSettingsToSet.getReportNavigationList()  # configure report
    
                    self.doRemoteControlOfSamplerKeypadNavigation(lnNavigationList)
    
                    # start program (in disable mode)
                    self.startSamplerProgram()
                    break   # leave while loop (without else clause)
                
                # sampler is off
                elif eMenuControlSamplerStatus == Sampler6712Driver.E_MENU_CONTROL_SAMPLER_STATUS.SAMPLER_OFF:
                    status = self.oExternalProgramControlTurnOn6712()  # turn on 6712
                
                # sampler program has just been stopped or is stopping
                elif eMenuControlSamplerStatus == Sampler6712Driver.E_MENU_CONTROL_SAMPLER_STATUS.PROGRAM_HALTED:
                    self._logger.info('Escape halted mode')
                    self.doRemoteControlOfSamplerKeypadNavigation([Sampler6712Driver.N_NAVIGATE_TO_MAIN_MENU_SCREEN])
                
                elif eMenuControlSamplerStatus == Sampler6712Driver.E_MENU_CONTROL_SAMPLER_STATUS.UNKNOWN:
                    raise Exception('Unknown menu control sampler status:')
                
                else:
                    raise Exception('Invalid menu control sampler status: %r' %(eMenuControlSamplerStatus))
                
            except Exception as e:
                self._logger.error(e)
        else:
            self._logger.error('Initialization of 6712 failed')
            
        self._logger.info('*** Initialization of 6712 complete')
        
    
    def stopSamplerProgram(self):
        '''
        This function stops a currently running program. The 6712 program
        needs to be running, otherwise this function will fail and raise
        an error.
        '''
        LN_STOP_PROGRAM_NAVIGATION = [
            Navigation(Navigation.E_SEARCH_TYPE.SCREEN, ['STOP PROGRAM', 'RESUME PROGRAM'], None, 0, None, 'S', 10, Exception('Navigation to stop menu failed')),
            Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['STOP PROGRAM'], None, 0, '\r', 'L', 11, Exception('Navigation to STOP PROGRAM failed'))]
            
        self._logger.info('Attempt to stop 6712 sampler program')
        self.doRemoteControlOfSamplerKeypadNavigation(LN_STOP_PROGRAM_NAVIGATION)
        self._logger.info('6712 sampler program stopped')

    def startSamplerProgram(self):
        '''
        This function starts the 6712 sampler program. The starting procedure 
        consists of three steps:
            1. run program (remote control of sampler keypad mode)
            2. disable running program (menu control mode)
            3. start program waiting for phone command (menu control mode)
        If the menu control commands fail, it is retried to resent the command.
        '''
        LN_START_PROGRAM_NAVIGATION = [
            self.N_NAVIGATE_TO_MAIN_MENU_SCREEN,
            Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['RUN'], None, 0, '\r', 'L', 10, Exception('Navigation to RUN failed')),
            Navigation(Navigation.E_SEARCH_TYPE.SCREEN, ['PLEASE WAIT'], Delay(2), 30, Exception('Testing of distributor system last too long'), None, 0, None),
            Navigation(Navigation.E_SEARCH_TYPE.SCREEN, ['ENTER START BOTTLE:'], None, 0, '1\r', None, 0, None)]
        
        self._logger.info('Attempt to start 6712 sampler program')
        
        # run program
        self.doRemoteControlOfSamplerKeypadNavigation(LN_START_PROGRAM_NAVIGATION)
        
        # first disable program, then start program waiting for phone command
        lsCmd = ['DISABLE', 'START']
        for sCmd in lsCmd:
            nbOfRetryCounter = 4
            while nbOfRetryCounter > 0:
                nbOfRetryCounter -= 1
                try:
                    lsMenuControlCommandList = self.lsMenuControlGetCommandList()

                    if sCmd in lsMenuControlCommandList:
                        self._serial.flushInput # flush input
                        self._serial.write('%s\r' %(sCmd))
                        sReply = self._read6712Reply(self._DELAY_BETWEEN_MENU_CONTROL_STATUS_DISABLE_AND_REPLY) # wait for reply
                        # verify if command echo received
                        if '%s\r' %(sCmd) in sReply:
                            # command echo found
                            break # leave while loop
                        else:
                            # command echo not found
                            raise Exception('Command echo not received on command %r: %r' %(sCmd, sReply))
                    else:
                        raise Exception('Command %r not in list: %r' %(sCmd, lsMenuControlCommandList))
                except Exception as e:
                    if nbOfRetryCounter > 0:
                        self._logger.warning(e)
                        time.sleep(1) # wait before retry
                    else:
                        raise Exception(e)
        self._logger.info('6712 sampler program started')
            
    def doRemoteControlOfSamplerKeypadNavigation(self, lnNavigationList, forceActiveRemotOperationModeDetectionAfterNavigation = False):
        '''
        This function navigates through the 6712 menu according to the
        navigation list.
        
        @param lnNavigationList: navigation list with Navigation objects
        @param forceActiveRemotOperationModeDetectionAfterNavigation: If True,
            after navigation, the remote operation mode is actively determined,
            independent whether the internal remote operation mode has
            expired or not.
        '''
        counter = 3
        while counter > 0:
            counter -= 1
            try:
                # Set remote control of sampler keypad mode. Set first to menu control 
                # mode to start remote control mode with defined serial data 
                (eCurrentMode, serialData) = self._set6712RemoteOperationMode(self.E_REMOTE_OPERATION_MODE.MENU_CONTROL_MODE)
                (eCurrentMode, serialData) = self._set6712RemoteOperationMode(self.E_REMOTE_OPERATION_MODE.REMOTE_CONTROL_OF_SAMPLER_KEYPAD_MODE)
                
                # clear 6712 display mirror and handle already received serial data
                # it is expected that the 'CONTROL' command send by the function
                # _set6712RemoteOperationMode, is echoed by the 6712 with
                # 'CONTROL' and two times '\r\n' followed by the display data.
                sDataToRemove = 'CONTROL\r\n'
                lsScreenData = serialData.partition(sDataToRemove) # remove CONTROL\r\n command
                if lsScreenData[1] != '':
                    # search for first occurrence of '\r\n' and remove it
                    lsScreenDataWithoutControlCmd = lsScreenData[2].partition('\r\n')
                    # data to remove found, init display mirror with already
                    # received data
                    self._vt100Decoder.initSamplerDisplayMirror(lsScreenDataWithoutControlCmd[0] + lsScreenDataWithoutControlCmd[2])
                    break # leave while loop 
                else:
                    # data to remove not found
                    raise Exception('Control command %r not found in %r' %(sDataToRemove, lsScreenData))
            except Exception as e:
                if counter > 0:
                    self._logger.warning(e)
                    time.sleep(1)
                else:
                    raise Exception(e)
        
        #print 'Display at beginning: %r' %(self._vt100Decoder.getSamplerDisplayMirror())
        
        # navigate through the menu according to the navigation lists
        try:
            for n in lnNavigationList:
                self._vt100Decoder.navigate(n)
        except Exception as e:
            raise Exception('Remote keypad navigation of 6712 failed: %r' %(e))
        
        # set remote operation mode        
        self._set6712RemoteOperationMode(self.E_REMOTE_OPERATION_MODE.MENU_CONTROL_MODE, forceActiveRemotOperationModeDetectionAfterNavigation)

    def stop(self):
        '''
        This function stops the 6712 driver
        '''
        try:
            self._serial.close() # close serial port
        except Exception as e:
            raise Exception('Closing serial port failed: %s' % (e))

    def eMenuControlGetSamplerStatus(self):
        '''
        This function determines the sampler status and when in 'program running' 
        mode also the program status. The sampler status is determined in the 
        'menu control' mode, not in the 'external program control' mode. If
        determination fails, it is retried a certain times.
        To determine the sampler status, the remote operation mode is first
        set to MENU_CONTROL_MODE. Afterwards the sampler's status is requested
        by sending 'ST'. The sampler's reply is then analyzed based on the 
        keys in the dictionary D_MENU_CONTROL_STATUS. If the sampler is in the
        PROGRAM_RUNNING status, the program status is also determined.
        
        @return: tuple with sampler and program status: 
            (E_MENU_CONTROL_SAMPLER_STATUS, E_MENU_CONTROL_SAMPLER_STATUS_EXTENSION,
            E_MENU_CONTROL_PROGRAM_STATUS status)
        '''
        
        D_MENU_CONTROL_STATUS = {'Program Waiting Start': self.E_MENU_CONTROL_SAMPLER_STATUS.PROGRAM_WAITING_START,
                                 'Program Running': self.E_MENU_CONTROL_SAMPLER_STATUS.PROGRAM_RUNNING,
                                 'at Standby': self.E_MENU_CONTROL_SAMPLER_STATUS.AT_STANDBY,
                                 'Program Idle': self.E_MENU_CONTROL_SAMPLER_STATUS.PROGRAM_IDLE,
                                 'Sampler Off': self.E_MENU_CONTROL_SAMPLER_STATUS.SAMPLER_OFF,
                                 'Program Halted': self.E_MENU_CONTROL_SAMPLER_STATUS.PROGRAM_HALTED}
        
        D_MENU_CONTROL_STATUS_EXTENSION = {'ERRORS': self.E_MENU_CONTROL_SAMPLER_STATUS_EXTENSION.ERRORS}
        
        eMenuControlProgramStatus = None
        eMenuControlSamplerStatus = None
        
        nbOfResentCounter = 4
        while nbOfResentCounter > 0:
            nbOfResentCounter -= 1 # decrement counter
            try:
                # set menu control mode
                eCurrentMode, sReadReply = self._set6712RemoteOperationMode(self.E_REMOTE_OPERATION_MODE.MENU_CONTROL_MODE)
                
                if eCurrentMode != self.E_REMOTE_OPERATION_MODE.MENU_CONTROL_MODE:
                    raise Exception('Unable to determine sampler status because failed to set remote operation mode to MENU CONTROL mode')
                
                # read status
                self._serial.flushInput
                self._serial.write('ST\r') # send status command
                sReadReply = self._read6712Reply(self._DELAY_BETWEEN_MENU_CONTROL_STATUS_REQUEST_AND_REPLY)
                
                # determine sampler status (search in key list)
                # the sampler status could be extended with the error information
                # (in case of power fail the sampler status is as follows:
                # 'Sampler Status: Program Running (ERRORS)')
                for x in D_MENU_CONTROL_STATUS.keys():
                    if x in sReadReply:
                        eMenuControlSamplerStatus = D_MENU_CONTROL_STATUS[x]
                        
                        # determine sampler status extension (follows in brackets after
                        # sampler status)
                        sResult = re.findall('.*%s.*\((.*)\).*' %(x), sReadReply)
                        
                        if sResult == []:
                            # no brackets found
                            eMenuControlSamplerStatusExtension = self.E_MENU_CONTROL_SAMPLER_STATUS_EXTENSION.NONE
                        else:
                            # check text inside brackets
                            for y in D_MENU_CONTROL_STATUS_EXTENSION.keys():
                                if y in sResult:
                                    eMenuControlSamplerStatusExtension = D_MENU_CONTROL_STATUS_EXTENSION[y]
                                    break
                            else:
                                # sampler status extension not found in dictionary
                                eMenuControlSamplerStatusExtension = self.E_MENU_CONTROL_SAMPLER_STATUS_EXTENSION.UNKNOWN
                                raise Exception('Invalid menu control status extension %r received: %r' %(sResult, sReadReply))

                        break # leave for loop
                else:
                    # not found
                    eMenuControlSamplerStatus = self.E_MENU_CONTROL_SAMPLER_STATUS.UNKNOWN
                    raise Exception('Invalid menu control status received: %r' %(sReadReply))
               
                # determine program status if program is running        
                if eMenuControlSamplerStatus == self.E_MENU_CONTROL_SAMPLER_STATUS.PROGRAM_RUNNING:
                    lsProgramStatus = re.findall('.*Program Status:.*\((.*)\)', sReadReply, re.DOTALL)
                    
                    # determine program status
                    if lsProgramStatus == []:
                        eMenuControlProgramStatus = self.E_MENU_CONTROL_PROGRAM_STATUS.UNKNOWN
                        raise Exception('Program status not found in: %r' %(sReadReply))
                    elif 'Enabled' in lsProgramStatus[0]:
                        eMenuControlProgramStatus = self.E_MENU_CONTROL_PROGRAM_STATUS.ENABLED
                    elif 'Disabled' in lsProgramStatus[0]:
                        eMenuControlProgramStatus = self.E_MENU_CONTROL_PROGRAM_STATUS.DISABLED
                    else:
                        eMenuControlProgramStatus = self.E_MENU_CONTROL_PROGRAM_STATUS.UNKNOWN
                        raise Exception('Invalid program status in %r: %r' %(sReadReply, lsProgramStatus[0]))
                break # leave while loop
            except Exception as e:
                if nbOfResentCounter > 0:
                    self._logger.warning(e)
                    time.sleep(1) # wait before retry
                else:
                    raise Exception(e)
                    
        return (eMenuControlSamplerStatus, eMenuControlSamplerStatusExtension, eMenuControlProgramStatus)

    def lsMenuControlGetCommandList(self):
        '''
        This function reads the menu in the menu control mode and returns 
        the possible commands in a list. If the reading fails, it is repeated
        a limited number of times.
        
        @return: list with menu commands (string)
        '''
        nbOfRetryCounter = 4
        while nbOfRetryCounter > 0:
            nbOfRetryCounter -= 1
            try:
                # set menu control mode
                self._set6712RemoteOperationMode(self.E_REMOTE_OPERATION_MODE.MENU_CONTROL_MODE)
                
                # read status
                self._serial.flushInput
                sCmd = 'MENU\r'
                self._serial.write(sCmd) # send status command
                sMenuReply = self._read6712Reply(self._DELAY_BETWEEN_MENU_CONTROL_STATUS_REQUEST_AND_REPLY)
                
                # verify if command echo received
                if sCmd in sMenuReply:
                    # command echo received
                    # extract commands in between <...>
                    lsMenuReply = sMenuReply.splitlines() # convert string to list
                    lsMenuCommands = []
                    for sLine in lsMenuReply:
                        # lines without '-' are ignored
                        if sLine.find(' - ') > 0:
                            temp = sLine.split('-')[0].replace('<','').replace('>','').partition(',') # remove '<>'
                        
                            if temp[2] != '':
                                lsMenuCommands.append(temp[2].strip())
                            elif temp[0] != '':
                                lsMenuCommands.append(temp[0].strip())
                     
                    break # leave while loop
                else:
                    # command echo not received
                    raise Exception('Command echo not received on command %r: %r' %(sCmd, sMenuReply))
            
            except Exception as e:
                if nbOfRetryCounter > 0:
                    self._logger.warning(e)
                    time.sleep(1) # wait before retry
                else:
                    raise Exception(e)
        
        return lsMenuCommands

    def sGetReport(self):
        '''
        CAUTION: THIS FUNCTION HAS NOT BEEN TESTED YET!!!
        '''
        sReport = ''
        
        # set external program control mode
        (eCurrentMode, serialData) = self._set6712RemoteOperationMode(self.E_REMOTE_OPERATION_MODE.EXTERNAL_PROGRAM_CONTROL_MODE)
        if eCurrentMode == self.E_REMOTE_OPERATION_MODE.EXTERNAL_PROGRAM_CONTROL_MODE:
            # flush serial buffers
            self._serial.flushInput()
            self._serial.flushOutput()
            self._serial.timeout = 2
            
            # read report
            self._serial.write('REPORT\r')
            sCommandEcho = self._serial.readline() # read command echo
            
            if 'REPORT\r\n' in sCommandEcho:
                # read report
                sReply = 'x'
                while '\x04' not in sReply and sReply != '':
                    sReply = self._serial.readline() # read command echo
                    sReport += sReply
                    
                print 'stop character: \'' +str(sReply) +'\''
            else:
                raise Exception('Requesting 6712 report failed')
            
        else:
            raise Exception('Setting external program control mode failed')
        
        return sReport

    def _appendChecksum(self, sData):
        '''
        This function appends the checksum, followed by CR/LF to the string. 
        CR and LF in the string are removed. 
        eg: STS,1 becoms STS,1,CS,581\r
        
        @param sData: string
        @return: string with appended checksum
        '''
        sTemp = sData.replace('\r', '') # remove cr
        sTemp = sTemp.replace('\n', '') # remove lf
        sTemp += ',CS,' # append checksum text
        sTemp += str(_calculateChecksum(sTemp)) + str('\r') # append checksum value and cr
    
        return sTemp

    
    def _ExternalProgramControlSendCmdAndReadStatusReply(self, timeout, sCommand):
        '''
        This function sends a command and verifies its reply which has to be
        of type status. That is, the following commands are allowed:
        - STS,1
        - STS,2
        - BTL,x,SVO,y
        The command does not have to be terminated with cr/lf. It is extended 
        with the checksum. If the status reply is not received or incorrect
        (that is, INVALID COMMAND or CHECKSUM MISMATCH) the command is 
        send again up to three times. If after three times no valid status
        reply was received, an exception is raised.
        
        Caution: Before calling this function, it has to be ensured that
        the 6712 is in the EXTERNAL PROGRAM CONTROL mode!
    
        @param timeout: timeout of serial interface
        @param sCommand: command string to send
        @return: parsedStatus (status reply)
        '''
        sCommand = self._appendChecksum(sCommand) # append checksum to command
        
        nbOfRemainingResend = 4 # number of time to retry in case of failure
        while nbOfRemainingResend > 0:
            nbOfRemainingResend -= 1
            try:
                # flush serial buffers
                self._serial.flushInput()
                self._serial.flushOutput()
            
                # send command (request status)
                self._serial.write(sCommand)
        
                # read reply
                self._serial.timeout = timeout
                sNewLine = 'x'
                sCommandReply = ''
                counter = 50
                lFindResult = []
                # read data until: i) no more data received or ii) '> ' received or iii) counter expired
                while sNewLine != '' and counter > 0:
                    counter -= 1
                    sNewLine = self._serial.readline() # read new line
                    sCommandReply += sNewLine 
                    
                    if lFindResult == []:
                        # search status reply (STI, BTL, SVO and SOR or not 
                        # verified because they may be not present if
                        # no sampling has ever taken before (e.g. after
                        # reinitialize of the 6712 controller)
                        lFindResult = re.findall('.*MO,.*,ID,.*,TI,.*,STS,.*,CS,.*',sNewLine)
                    else:
                        # search caret
                        if sNewLine.find('> ') >= 0:
                            break # stop while loop
                
                # check if counter expired
                if counter <= 0:
                    self._logger.info('External program control command reply counter expired (command: %r, reply: %r)' %(sCommand, sCommandReply))
                
                # check if status reply has been found
                if lFindResult == []:
                    # status reply not found
                    raise Exception('Status reply of command %r not found in %r)' % (sCommand, sCommandReply))
                else:
                    parsedStatus = Status6712(lFindResult[0]) # parse status reply
                    
                    #check status
                    if parsedStatus.samplerStatus == Sampler6712Driver.E_EXTERNAL_PROGRAM_CONTROL_SAMPLER_STATUS.INVALID_COMMAND or \
                       parsedStatus.samplerStatus == Sampler6712Driver.E_EXTERNAL_PROGRAM_CONTROL_SAMPLER_STATUS.CHECKSUM_MISMATCH:
                        raise Exception ('Status reply of command %r: %r; Reply: %r' %(sCommand, Sampler6712Driver.D_EXTERNAL_PROGRAM_CONTROL_SAMPLER_STATUS[parsedStatus.samplerStatus], sCommandReply))
                    else:
                        # status ok
                        break # leave resend while loop
            except Exception as e:
                if nbOfRemainingResend > 0:
                    self._logger.warning(e)
                    time.sleep(1) # wait before retry
                else:
                    raise Exception(e)
                    
        return parsedStatus

    def _waitForCaret(self, lastReadData=None):
        '''
        This function waits for an incoming caret symbol '>'
    
        @param lastReadData: data read to be checked on caret (optional)
        @return: True if '>' was found
        '''
        counter = 10
        while '>' not in lastReadData and lastReadData != '' and counter > 0:
            counter -= 1
            lastReadData = self._serial.read()
    
        if '>' in lastReadData:
            return True
        else:
            return False

################################################################################
# The following functions are used to determine and set the operation mode
#
#
################################################################################
    def _get6712RemoteOperationMode(self):
        '''
        This function finds out the current remote operation mode of the 6712 water 
        sampler. The determination of the remote operation mode is done by sending 
        '?' to the 6712. The operation mode is determined depending on its reply.
        OFFLINE_MODE and SAMPLING_REPORTS_MODE are not possible because they are
        left when '?' are sent to the 6712. To speed up determination, the timeout
        is adapted depending on the 6712's replies. The buffers of the serial
        interface are flushed before the determination of the operation mode.
        In case of determine the REMOTE_CONTROL_OF_SAMPLER_KEYPAD_MODE mode
        the display representation may be modified due to the sending of 'O' or 'S'.

        @return: remote operation mode (key, see dOperationMode above)
        '''
        eRemoteOperationMode = self.E_REMOTE_OPERATION_MODE.UNKNOWN_MODE # unknown mode
        counter = 10 # used for the detection of the REMOTE_CONTROL_OF_SAMPLER_KEYPAD_MODE
        serialData = '' # data received from the 6712

        # flush serial buffers
        self._serial.flushInput()
        self._serial.flushOutput()

        self._logger.info('Attempt to determine 6712 remote operation mode')

        while eRemoteOperationMode == self.E_REMOTE_OPERATION_MODE.UNKNOWN_MODE and counter > 0:
            
            counter -= 1 # decrement counter

            self._serial.write('?') # send question mark to 6712

            # read reply until 6712 stops sending data
            serialData = self._read6712Reply(self._DELAY_BETWEEN_QUESTION_MARK_AND_BANNER_STRING)

            # verify reply
            if '*** Model 6712' in serialData:
            # either external program control or menu control are active
               if 'Exit MENU' in serialData:
                   eRemoteOperationMode = self.E_REMOTE_OPERATION_MODE.MENU_CONTROL_MODE # menu control mode
               else:
                   eRemoteOperationMode = self.E_REMOTE_OPERATION_MODE.EXTERNAL_PROGRAM_CONTROL_MODE # external program control mode

        # if no '?' is replied by the 6712 further tests are required to verify
        # if 6712 is in the remote control of sampler keypad mode
        if eRemoteOperationMode == self.E_REMOTE_OPERATION_MODE.UNKNOWN_MODE:
            # verification if device is in remote control of sampler keypad mode
            # is done by sending first an 'S' (corresponds to STOP key). If
            # 6712 does not react, it may be turned off. Therefore, an 'Q' is 
            # sent next to leave the 'remote control of sampler keypad' mode 
            # and enter the 'menu control' mode. If 6712 does not answer
            # on 'Q' the device is not connected and the mode therefore unknown
        
            # tuple with string to send to 6712. First string is 'S' character
            # corresponding to STOP key and second character is 'Q' to
            # exit 'remote control of sampler keypad' mode
            lsControlCommand = ('S', 'Q') 
            self._serial.timeout = self._DELAY_BETWEEN_CONTROL_COMMAND_AND_REPLY 
        
            counter = 0
            for sControlCommand in lsControlCommand:
                self._serial.flushInput()
                self._serial.flushOutput()
                self._serial.write(sControlCommand) # send character
                serialData = self._serial.readline(eol='\x1b') # read reply
                if '\x1b' in serialData and sControlCommand == 'S':
                    eRemoteOperationMode = self.E_REMOTE_OPERATION_MODE.REMOTE_CONTROL_OF_SAMPLER_KEYPAD_MODE # remote control of sampler keypad mode
                    break # leave while loop
                elif '\x1b' in serialData and sControlCommand == 'Q':
                    # read the following two lines
                    serialData = self._serial.readline(eol='\n')
                    serialData += self._serial.readline(eol='\n')
                    if 'Exiting' in serialData:
                        eRemoteOperationMode = self.E_REMOTE_OPERATION_MODE.MENU_CONTROL_MODE # menu control mode
                        break # leave while loop

        self._logger.info('Determined 6712 remote operation mode: %s' % (self.D_REMOTE_OPERATION_MODE[eRemoteOperationMode]))
        return eRemoteOperationMode

    def _set6712RemoteOperationMode(self, eModeToSet, forceActiveModeDetermination = False):
        '''
        This function set the remote operation mode of the 6712 water sampler. 
        Before the new operation mode is set, the current mode is determined.
        After the mode transition by means of dTransition, the 6712's reply is
        verified.
        @param eModeToSet: mode to set (see self.E_REMOTE_OPERATION_MODE)
        @param forceActiveModeDetermination: if set, the internal remote operation mode
            is not considered and the current mode is actively determined
        @return: set mode
        '''
        # The following constant value defines how long the internal remote
        # opertion mode is valid until it is updated [s].
        INTERNAL_REMOTE_OPERATION_MODE_UPDATE_TIME = 180 
        
        #                 __current mode
        #                |  _target mode
        #                | |  command to sent to 6712
        dTransition = { (2, 1):'Q\r',
                        (2, 3):'MENU\r',
                        (2, 4):'MENU\r',
                        (3, 1):'Q\r',
                        (3, 2):'Q\r',
                        (3, 4):'CONTROL\r',
                        (4, 1):'Q',
                        (4, 2):'Q',
                        (4, 3):'Q'}
    
        # blank display of 6712 water sampler
        BLANK_6712_DISPLAY = '                    \r\n                    \r\n                    \r\n                    \r\n'

        sReadReply = None
        
        self._logger.info('Set remote operation mode to %s' % (self.D_REMOTE_OPERATION_MODE[eModeToSet]))

        # determine current mode (if in offline mode, offline mode is left by this
        # function). Current remote operation mode is only actively determined
        # if its last determination has expired or the operation mode is unknown
        # expiration is in seconds
        delta = time.time() - self._lastInternalRemoteOperationModeUpdateTime
        if delta > INTERNAL_REMOTE_OPERATION_MODE_UPDATE_TIME or \
           self._eInternalRemoteOperationMode == self.E_REMOTE_OPERATION_MODE.UNKNOWN_MODE or \
           forceActiveModeDetermination == True:
            eCurrentMode = self._get6712RemoteOperationMode()
            self._eInternalRemoteOperationMode = eCurrentMode
            self._lastInternalRemoteOperationModeUpdateTime = time.time()
        else:
            eCurrentMode = self._eInternalRemoteOperationMode
        
        timeoutCounter = 3
        while eCurrentMode == self.E_REMOTE_OPERATION_MODE.UNKNOWN_MODE and timeoutCounter > 0:
            timeoutCounter -= 1
            eCurrentMode = self._get6712RemoteOperationMode()
            self._eInternalRemoteOperationMode = eCurrentMode
            self._lastInternalRemoteOperationModeUpdateTime = time.time()
        
        # raise exception if remote operation mode is still unknown
        if eCurrentMode == self.E_REMOTE_OPERATION_MODE.UNKNOWN_MODE:
            # unknown mode, that is, no connection with 6712!
            raise Exception('No connection with 6712')       
        
        timeoutCounter = 6
        while eCurrentMode != eModeToSet and timeoutCounter > 0:
#            print 'current mode: %r, mode to set: %r' %(self.D_REMOTE_OPERATION_MODE[eCurrentMode], self.D_REMOTE_OPERATION_MODE[eModeToSet])
            timeoutCounter -= 1 # decrement counter
            transitionKey = (eCurrentMode, eModeToSet)

            # flush serial buffers
            self._serial.flushInput()
            self._serial.flushOutput()

            # send msg to change mode and read reply until 6712 stops sending data
            self._serial.write(dTransition[transitionKey])
            sReadReply = self._read6712Reply(self._DELAY_AFTER_MENU_TRANSITION_COMMAND)

            # verify reply
            if  transitionKey == (2, 3) or transitionKey == (2, 4) or transitionKey[0] == 4:
                if 'Exit MENU' in sReadReply:
                    eCurrentMode = self.E_REMOTE_OPERATION_MODE.MENU_CONTROL_MODE # menu control mode
                else:
                    eCurrentMode = self.E_REMOTE_OPERATION_MODE.UNKNOWN_MODE # unknown mode (behaviour is not as expected)

            elif transitionKey == (3, 4):
                if BLANK_6712_DISPLAY in sReadReply or '\x1b\x5b' in sReadReply:
                # '\x1b\x5b' is a specific pattern only used when 6712 is on and
                # remote control of sampler keypad mode is active
                    eCurrentMode = self.E_REMOTE_OPERATION_MODE.REMOTE_CONTROL_OF_SAMPLER_KEYPAD_MODE
                else:
                    eCurrentMode = self.E_REMOTE_OPERATION_MODE.UNKNOWN_MODE # unknown mode (behaviour is not as expected)

            elif transitionKey == (3, 1) or transitionKey == (3, 2):
                if 'Q\r\n\r\n\r\n> ' in sReadReply:
                    eCurrentMode = self.E_REMOTE_OPERATION_MODE.EXTERNAL_PROGRAM_CONTROL_MODE
                else:
                    eCurrentMode = self.E_REMOTE_OPERATION_MODE.UNKNOWN_MODE # unknown mode (behaviour is not as expected)

            elif transitionKey == (2, 1):
                if 'Q\r\n\r\n' in sReadReply:
                    eCurrentMode = self.E_REMOTE_OPERATION_MODE.OFFLINE_MODE
                else:
                    eCurrentMode = self.E_REMOTE_OPERATION_MODE.UNKNOWN_MODE # unknown mode (behaviour is not as expected)

            else:
                self._logger.warning('invalid transition key: %d' % (transitionKey))

            # try to determine remote operation mode when it is not known
            if eCurrentMode == self.E_REMOTE_OPERATION_MODE.UNKNOWN_MODE:
                eCurrentMode = self._get6712RemoteOperationMode()
                if eCurrentMode == self.E_REMOTE_OPERATION_MODE.UNKNOWN_MODE:
                    raise Exception('Unable to determine operation mode')

            # update internal remote operation mode
            self._eInternalRemoteOperationMode = eCurrentMode
            self._lastInternalRemoteOperationModeUpdateTime = time.time()
            
        #debug
        self._logger.debug('Remote operation mode set to: %r' % (self.D_REMOTE_OPERATION_MODE[eCurrentMode]))
          
        return (eCurrentMode, sReadReply)


    def _read6712Reply(self, initialTimeout, maxReadTimeInSec = 10, maxNbOfLines = 50):
        '''
        This function reads the reply of the 6712 water sampler. Data is read
        until 6712 stops sending data or at the latest after maxNbOfLines have 
        been read or maxReadTimeInSec has expired
        @param initialTimeout: read timeout
        @param maxReadTimeInSec: 
        '''
        serialData = '' # data received from 6712

        self._serial.timeout = initialTimeout;
        startTime = time.time()

        # read reply until 6712 stops sending data
        newLine = self._serial.readline() # read data from 6712
        while newLine != '' and \
              maxNbOfLines > 0: # verify if reply received
            maxNbOfLines -= 1
            serialData += newLine # store read line
            if '*** Model 6712' in newLine:
               self._serial.timeout = self._DELAY_BETWEEN_BANNER_STRING_AND_MENU   # set timeout to wait for menu, possibly sent by 6712
            if '<ST>' in newLine: 
               self._serial.timeout = self._DELAY_DURING_MENU_DISPLAY # set timeout to display menu
            if '> ' in newLine:
               self._serial.timeout = self._DELAY_AFTER_CARET # set timeout
            
            if time.time() - startTime > maxReadTimeInSec:
                break
            
            #print "serial data (line): " + newLine # debug
            newLine = self._serial.readline() # read data
    
        return serialData

class Status6712():
    '''
    This status class contains information about the status of the 6712.
    '''
    def __init__(self, sStatus=None):
        '''
        This function parses the status string of the 6712.
    
        @param sStatus: data to parse (string)
        '''
        self.modelNb = None
        self.id = None
        self.timeOf6712 = None
        self.samplerStatus = None
        self.mostRecentSamplingTime = None
        self.mostRecentSamplingBottle = None
        self.mostRecentSamplingVolume = None
        self.mostRecentSamplingResult = None
        self.checksum = None

        if sStatus != None:
            try:
                # search identifiers MO, ID, TI, STS and CS (the others may
                # be not available because a most recent sampling does not exists) 
                lResultStatus = re.findall('.*MO,(\d+),ID,(\d+),TI,(\d+.\d+),STS,(\d+),(.*)CS,(\d+).*', sStatus)
                
                if lResultStatus == []:
                    raise Exception('Status reply %r is invalid' %(sStatus))
                else:
                    # extract and convert MO, ID, TI, STS and CS and store
                    # them into status object
                    self.modelNb = int(lResultStatus[0][0])
                    self.id = int(lResultStatus[0][1])
                    self.timeOf6712 = float(lResultStatus[0][2])
                    self.samplerStatus = int(lResultStatus[0][3])
                    self.checksum = int(lResultStatus[0][5])
                    
                    # check if most recent sampling is available
                    if lResultStatus[0][4] != '': 
                        # most recent sampling is available
                        lResultLastSampling = re.findall('.*STI,(\d+.\d+),BTL,(\d+),SVO,(\d+),SOR,(\d+).*', lResultStatus[0][4])
                        
                        if lResultLastSampling != []:
                            self.mostRecentSamplingTime = float(lResultLastSampling[0][0])
                            self.mostRecentSamplingBottle = int(lResultLastSampling[0][1])
                            self.mostRecentSamplingVolume = int(lResultLastSampling[0][2])
                            self.mostRecentSamplingResult = int(lResultLastSampling[0][3])
                    
                    # verify checksum
                    if _calculateChecksum(sStatus) == self.checksum:
                        self._checksumOk = True
                    else: 
                        self._checksumOk = False
                        raise Exception('Checksum of %r is wrong: %r' % (sStatus, e))
            except Exception as e:
                raise Exception('Status parsing of %r failed: %r' % (sStatus, e))
    
    def getStatusAsList(self):
        '''
        This function returns the status values as a list. The order is as follows:
        [MO,ID,TI,STS,STI,BTL,SVO,SOR,CS]
    
        @return: status as a list
        '''
        lStatus = [ self.modelNb,
                    self.id,
                    self.timeOf6712,
                    self.samplerStatus,
                    self.mostRecentSamplingTime,
                    self.mostRecentSamplingBottle,
                    self.mostRecentSamplingVolume,
                    self.mostRecentSamplingResult,
                    self.checksum]
        return lStatus

################################################################################
# The following functions are used to communication in the 'remote control
# of sampler keypad' mode
#
#################################################################################
class Vt100Decoder():

    def __init__(self, serial, logger):
        self._NB_OF_LINES = 4
        self._LINE_LENGTH = 20
        self._ESCAPE_CHAR = '\x1B'
        # self._lDisplay[yPosition][xPosition]:
        self._lSamplerDisplayMirror = [[(' ',0) for col in range(self._LINE_LENGTH)] for row in range(self._NB_OF_LINES)] # create n*m list
        
        self._lInputBuffer = []
        self._rInputBufferHistory = RingBuffer(100) 
        self._cursor = Cursor()
        self._characterAttribute = 0
        
        self._logger = logger
        
        self._serial = serial
        
        self._cmdScreenHistory = RingBuffer(100) # history containing cmd send and resulting displays
    
    def initSamplerDisplayMirror(self, sScreenData):
        '''
        This function initializes the mirror of the sampler's display
        @param sScreenData: data to initialize the display mirror (string)
        '''
        # set cursor to initial position
        self._cursor.setPosition(0, 0)
        
        # clear display
        for i in self._lSamplerDisplayMirror:
            i = (' ', 0)
        
        # clear input buffer
        self._lInputBuffer = []
        self._rInputBufferHistory.clear()         
        
        #print 'sScreenData for init: %r' % (sScreenData)
        
        # handle string data
        for c in sScreenData:
            try:
                self.newChar(c)
            except Exception as e:
                raise Exception('Mirror display initialization failed: %r' %(e))
            
    
    def getSamplerDisplayMirror(self, characterAttribute=None):
        ''' 
        This function returns a representation of the current display. If
        characterAttribute=None, the complete display is returned, independent
        of the individual character attribute. If characterAttribute is set
        to a specific value, only the characters of that value are returned.
        @param characterAttribute: type of character to return (known types are:
            0: Turn off character attributes
            5: Turn blinking mode on
            7: Turn reverse video on
        @return: current display as string
        '''
        sDisplay = ''
        lDisplay = []
        
        if characterAttribute == None:
            # return complete display, independent on character attribute
            for line in self._lSamplerDisplayMirror: # for each line
               # sDisplay += ''.join([y[0] for y in line]) + '\r\n' # create display
               lDisplay.append(''.join([y[0] for y in line]))
        else:
            # only return character of tpye 
            for line in self._lSamplerDisplayMirror: # for each line characterAttribute
                sTempData = ''.join([x[0] for x in line if x[1] == characterAttribute])
                if sTempData != '':
                    lDisplay.append(sTempData)
                    #sDisplay += sTempData + '\r\n'
        
        sDisplay = '\r\n'.join(lDisplay)
        
        return sDisplay

    def newChar(self, char):
        '''
        This function evaluates a received character. A valid VT100 command is
        for example: '\x1b[10A'
        At the latest the 4th character after the escape has to 
        be a alphabetic number
        
        @param char: received character
        '''
        lCharData = []
        lVt100Cmd = []
    
        self._lInputBuffer.append(char)
        self._rInputBufferHistory.append(char) # update history (only used for debugging)
        
        # find first escape character
        try:
            # search first escape character
            idxFirstEscapeChar = self._lInputBuffer.index(self._ESCAPE_CHAR)
        except:
            # no escape character found
            idxFirstEscapeChar = None

        if idxFirstEscapeChar == None:
            # no escape character found -> update display
            lCharData = self._lInputBuffer
            self._lInputBuffer = []
        else:
            # search next alphabetic character
            try:
                idxFirstAlpha = [x.isalpha() for x in self._lInputBuffer].index(True, idxFirstEscapeChar)
            except:
                #first character after escape character not yet received
                idxFirstAlpha = None

            if idxFirstAlpha == None:
                pass
            else:
                # extract VT100 command
                lVt100Cmd = self._lInputBuffer[idxFirstEscapeChar:idxFirstAlpha + 1]

                # extract data until first escape character
                lCharData = self._lInputBuffer[:idxFirstEscapeChar]

                # remove extracted data
                self._lInputBuffer = self._lInputBuffer[idxFirstAlpha + 1:]
        
        # evaluate character and VT100 command
        # character have to be evaluated first
        try:
            self._evaluateCharacter(lCharData) 
        except Exception as e:
            self._logger.warning('Character data error in %r. Error: %r' %(self._rInputBufferHistory.get(''), e))

        try:
            self._evaluateVt100Cmd(lVt100Cmd)
        except Exception as e:
            self._logger.warning('Invalid VT100 command, command skipped: %r' %(e))
        
    
    def _evaluateVt100Cmd(self, lVt100Cmd):
        '''
        This function evaluates a received VT100 command. Commands start with \x1b\x5b
        and are followed by a number and a alphabetic character.
        
        @param lVt100Cmd: parameter as a list (e.g. ['\x1b','\x5b','1','A']),
            empty lists are ignored
        '''
        if lVt100Cmd != []:
            # convert list to string
            sVt100Cmd = ''.join(lVt100Cmd)
        
            # partition VT100 command (e.g. '\x1B[19A' -> ('','\x1B[','19A')
            lVt100CmdPartitioned = sVt100Cmd.partition('\x1B\x5B')
        
            # check if VT100 command starts with '\x1b[' (where '\x1b' is the escape char)
            if lVt100CmdPartitioned[1] != '':
                 # evaluate data after '\x1B\x5B'
                character = lVt100CmdPartitioned[2][-1] # alphabetic character is at last position
                #print 'character: ' + str(character)
                try:
                    sNumber = lVt100CmdPartitioned[2].partition(character)[0]
                    number = int(sNumber)
                #    print 'number: ' + str(number)
                except:
                    raise Exception('Invalid number %r in VT100 command: %r' %(sNumber, sVt100Cmd))
            
                if character == 'A':
                    # move cursor up
                    self._cursor.yPosition = (self._cursor.yPosition - number) % self._NB_OF_LINES
                elif character == 'B':
                    # move cursor down
                    self._cursor.yPosition = (self._cursor.yPosition + number) % self._NB_OF_LINES
                elif character == 'C':
                    # move cursor right
                    self._cursor.xPosition = (self._cursor.xPosition + number) % self._LINE_LENGTH
                elif character == 'D':
                    # move cursor left
                    self._cursor.xPosition = (self._cursor.xPosition - number) % self._LINE_LENGTH
                elif character == 'm':
                    self._characterAttribute = number # store current character attribute
                    if number != 0 and number != 5 and number != 7:
                        raise Exception('Invalid character attribute: %r' %(sVt100Cmd) )
                else:
                    raise Exception('Invalid VT100 command: %r' %(sVt100Cmd) )

    def _evaluateCharacter(self, lCharData):
        '''
        This function evaluates a received character. 
        @param lCharData: list of received characters
        '''
        for x in lCharData:
            if x == '\x0d':
                #carriage return
                self._cursor.xPosition = 0
            elif x == '\x0a':
                # line feed
                self._cursor.yPosition = (self._cursor.yPosition + 1) % self._NB_OF_LINES
            else:
                if self._cursor.xPosition < self._LINE_LENGTH:
                    self._lSamplerDisplayMirror[self._cursor.yPosition][self._cursor.xPosition] = (x, self._characterAttribute)
                    self._cursor.xPosition = (self._cursor.xPosition + 1) % self._LINE_LENGTH
                else:
                    temp = self._cursor.xPosition
                    self._cursor.xPosition = 0
                    raise Exception('Invalid cursor position: %d' % (temp))

    def navigate(self, navigationObject):
        '''
        This function navigates to a defined screen or menu. Where to navigate and
        how to navigate is defined in the navigationObject
        @param navigationObject: defines where and how to navigate
        '''
        
        self._logger.info('Navi: %r' %(navigationObject.sGetFormatedNavigation()))
        
        self._serial.timeout = 0.2
        
        foundCounter = navigationObject.maxNbOfFoundAction
        notFoundCounter = navigationObject.maxNbOfNotFoundAction
        
        while foundCounter >= 0 and notFoundCounter >= 0:

            # read serial data. The number of serial data can be
            # high if the sampler is in a state where lots of 
            # data is sent (e.g. when time information is displayed and 
            # periodically updated)
            nbOfChar = self._serial.inWaiting() # read number of waiting characters
            sSerialData = self._serial.read(nbOfChar) # read characters
            readCounter = 100
            # read as long as data is received and counter has not expired
            while sSerialData != '' and readCounter > 0:
                readCounter -= 1 # decrement counter
                for char in sSerialData:
                    self.newChar(char)  # interpret read data
                sSerialData = self._serial.read(10) # read nb of characters or until timeout occurs

            if readCounter < 0:
                self._logger.warning('Not all data read in navigation!')

            # read current display
            if navigationObject.eSearchType == Navigation.E_SEARCH_TYPE.SCREEN:
                # read complete display, indpendent of character attributes
                sText = self.getSamplerDisplayMirror()
                self._cmdScreenHistory.append('Screen text: %r' %(sText)) # store display
            elif navigationObject.eSearchType == Navigation.E_SEARCH_TYPE.SELECTION:
                # read only characters with blinking attribute (5)
                sText = self.getSamplerDisplayMirror(5) 
                self._cmdScreenHistory.append('Selection text: %r' %(sText)) # store display
            else:
                raise Exception('Invalid search type: %r' %(navigationObject.eSearchType))

            self._logger.info('Relevant display info: %r' %(sText))

            # check whether desired screen/selection found
            allFound = True
            for x in navigationObject.lsSearchText:
                allFound &= x in sText
        
            if allFound == True:
                if foundCounter <= 0:
                    #  screen/selection found
                    action = navigationObject.foundExpirationAction
                    sActionType = 'found expiration action'
                else:
                    #  screen/selection found
                    action = navigationObject.foundAction
                    sActionType = 'found action'
                foundCounter -= 1
            
            else:
                #  screen/selection not found
                if notFoundCounter <= 0:
                    #  screen/selection found
                    action = navigationObject.notFoundExpirationAction
                    sActionType = 'not found expiration action'
                else:
                    #  screen/selection found
                    action = navigationObject.notFoundAction
                    sActionType = 'not found action'
                notFoundCounter -= 1
            
            # do action
            if isinstance(action, str):
                # send string
                self._logger.info('Send remote keypad command %r' %(action))
                self._serial.write(action) # send cmd
                self._cmdScreenHistory.append('%r: %r' %(sActionType, action)) # store display
                time.sleep(1)                
            elif isinstance(action, Navigation):
                # do another navigation
                self.navigate(action)
            elif isinstance(action, Delay):
                # do delay
                self._cmdScreenHistory.append('%r: sleep %fs' %(sActionType, action.delayInSec)) # store display
                time.sleep(action.delayInSec)
            elif isinstance(action, type(None)):
                pass # don't do anything
            elif isinstance(action, Exception):
                # action is exception
                self._cmdScreenHistory.append('Exception: %r' %(action)) # store display
                raise action
            else:
                raise Exception('Invalid %r %r of type %r' %(sActionType, action, type(action)))

class RingBuffer():
    def __init__(self, size):
        self.data = [None for i in range(size)]

    def append(self, x):
        self.data.pop(0)
        self.data.append(x)

    def clear(self):
        self.data = [None for i in range(len(self.data))]

    def get(self, sSeparation = '\r\n'):
        sData = ''
        for i in self.data:
            if i != None:
                sData += i + sSeparation
        return sData


class Cursor():
    def __init__(self):
        self.xPosition = 0
        self.yPosition = 0
        
    def setPosition(self, x, y):
        self.xPosition = x
        self.yPosition = y

class Sampling():
    def __init__(self, bottleNb=None, waterVolumeInMl=None):
        try:
            self.bottleNb = int(bottleNb)
            self.waterVolumeInMl = int(waterVolumeInMl)
        except Exception as e:
            raise Exception('Creation of sampling object failed: %r' % e)


    
class Report():
    
    LS_BOTTLE_SCREEN = ['BTLS', 'SUCTION LINE', 'SUCTION HEAD', 'RINSES', 'RETRIES']

    def __init__(self, sReport):
        self.sReport = sReport
        
    def getProgramSettings(self):
        '''
        This function extract the program settings from the report file.
        @param sReport: report from the 6712 (string)
        @return: sampler configuration (Sampler6712ProgramSettings), None if configuration was
        not part of the report string. 
        '''
        
        # extract program settings from report string
        self.sProgramSettings = self._extractProgramSettings()
        if self.sProgramSettings == '':
            # try to re-read report
            return None
        
        # analyse program settings
        lsProgramSettings = self.sProgramSettings.split('----------')
        
        # read bottle screen data
        for screen in lsProgramSettings:
            # verify if all bottle screen elements are on the screen
            for b in self.LS_BOTTLE_SCREEN:
                if b not in screen:
                    break
            else:
                # for loop has not been stopped before (by break), that is
                # all elements of LS_BOTTLE_SCREEN where in p
                lBottleScreenData = re.findall(' *(\d+), *(\d+\.\d+|\d+) *(ml|l) *BTLS.*(\d+\.\d+) *m SUCTION LINE.*(\d+\.\d+|AUTO) *m? *SUCTION HEAD.* *(\d) *RINSES, *(\d) *RETRIES.*', screen, re.DOTALL)
                print 'result: ' + ('%r' %(lBottleScreenData))
                if lBottleScreenData == []:
                    raise Exception('Program settings conversion failed: %r' %(screen))
                
                # set bottle volume
                if lBottleScreenData[0][2] == 'l':
                    bottleVolumeInLit = float(lBottleScreenData[0][1])
                elif lBottleScreenData[0][2] == 'ml':
                    bottleVolumeInLit = float(lBottleScreenData[0][1])/1000
                else:
                    raise Exception('Invalid bottel volume unit: %r' %(lBottleScreenData[2]))
                
                # set number of bottles
                nbOfBottles = int(lBottleScreenData[0][0])
                
                # set suction line length
                suctionLineLengthInM = float(lBottleScreenData[0][3])
                
                # set suction head
                if 'AUTO' in lBottleScreenData[0][4]:
                    suctionLineHeadInM = 'AUTO'
                else:
                    suctionLineHeadInM = float(lBottleScreenData[0][4])
                    
                # set nb of rinse cycles and retries
                nbOfRinseCycles = int(lBottleScreenData[0][5])
                nbOfRetries = int(lBottleScreenData[0][6])
                
                break
        else:
            raise Exception('Bottle screen not found')
                                
        # create sampler configuration object  
        sampler6712ProgramSettings = Sampler6712ProgramSettings(nbOfBottles, bottleVolumeInLit, suctionLineLengthInM, suctionLineHeadInM, nbOfRinseCycles, nbOfRetries)
        
        return sampler6712ProgramSettings
    
    def _extractProgramSettings(self):
        '''
        This function search the 'PROGRAM SETTINGS' section in the report string.
        If report string is invalid, an exception is raised. If the 
        'PROGRAM SETTINGS' section is not found, no exception is raised but
        an empty string is returned.
        
        @param sReport: report as string
        @return: an empty string is returned if the 'PROGRAM SETTINGS' section
            has not been found, else the section is returned
        '''
        # search beginning of program settings section
        cnt = self.sReport.count('** PROGRAM SETTINGS **')
        if cnt == 0:
            return '' # program settings not found --> reconfigure 6712 report
        elif cnt > 1: 
            # program settings not found
            raise Exception('\'** PROGRAM SETTINGS **\' found %d times!' %(cnt))
        
        # search end of programm settings
        start = self.sReport.find('** PROGRAM SETTINGS **')
        if start < 0:
            raise Exception('Program settings not found!')
        
        # search title 'SAMPLER ID#...'
        start = self.sReport.rfind('SAMPLER', 0, start)
        if start < 0:
            raise Exception('\'SAMPLER\' before program settings not found!')
            
        end = self.sReport.find('--------------------------', start)
        if end < 0:
            raise Exception('End of programm settings not found!')
                
        return self.sReport[start:end] # return 'PROGRAM SETTINGS' section 

class Sampler6712ProgramSettings():
    '''
    This class contains all information required to init the 6712 sampler
    '''
    L_VALID_NB_OF_BOTTLES = (1, 2, 4, 8, 12, 24)
    L_BOTTLE_VOLUME_RANGE = (((1,2,4,8), 0.3, 100.0), # list of bottles and theire 
                             ((12, 24), 0.3, 30.0))   # min and max bottle volume in liter 
    L_SUCTION_LINE_LENGTH_RANGE_IN_M = (0.9, 30.2) # suction line length range in meter
    L_SUCTION_HEAD_LENGTH_RANGE_IN_M = (0.3, 8.5) # suction head length range in meter
    L_RINSE_CYCLES_RANGE = (0, 1, 2, 3)
    L_RETRIES_RANGE = (0, 1, 2, 3)
    
    N_NEXT_MENU_PAGE = Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['>'], None, 0, '\r', 'R', 10, Exception('Navigation to next menu page failed'))
    N_NAVIGATE_TO_MAIN_MENU_SCREEN = Navigation(Navigation.E_SEARCH_TYPE.SCREEN, ['  PROGRAM  '], None, 0, None, 'S', 10, Exception('Navigation to main menu failed'))
    N_SELECT_YES = Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['YES'], None, 0, '\r', 'R', 5, Exception('Navigation to YES failed'))
    N_SELECT_NO = Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['NO'], None, 0, '\r', 'R', 5, Exception('Navigation to NO failed'))
    
    def __init__(self, nbOfBottles, bottleVolumeInLit, suctionLineLengthInM, 
                 suctionLineHeadInM, nbOfRinseCycles, nbOfRetries):
        '''
        This function initializes the configuration object.
        @param nbOfBottles: number of bottles (integer)
        @param bottleVolumeInLit: bottle volume in liter
        @param suctionLineLengthInM: suction line length in meter (integer or float)
        @param suctionLineHeadInM: suction line head in meter (integer or float) or 'AUTO' string
        @param nbOfRinseCycles: number of rinse cycles as an integer (integer between 0..3)
        @param nbOfRetries: number of retires (integer between 0..3)
        '''
        self.nbOfBottles = None
        self.bottleVolumeInLit = None
        self.suctionLineLengthInM = None
        self.suctionLineHeadInM = None
        self.nbOfRinseCycles = None
        self.nbOfRinseCycles = None
        
        # verify and set nb of bottles
        if nbOfBottles in self.L_VALID_NB_OF_BOTTLES:
            self.nbOfBottles = nbOfBottles
        else:
            raise Exception('Invalid number of bottles: %r' %(nbOfBottles))
        
        # verify and set bottle volume
        for x in self.L_BOTTLE_VOLUME_RANGE:
            if nbOfBottles in x[0]:
                if bottleVolumeInLit >= x[1] and bottleVolumeInLit <= x[2]:
                    self.bottleVolumeInLit = bottleVolumeInLit
                else:
                    raise Exception('Invalid sampling volume: %r' %(bottleVolumeInLit))
                break # leave for loop without else clause
        else: # else is entered when for loop terminates without break
            raise Exception('Nb of bottles not in list: %r' %(nbOfBottles))
        
        # verify and set suction line length
        if suctionLineLengthInM >= self.L_SUCTION_LINE_LENGTH_RANGE_IN_M[0] and \
           suctionLineLengthInM <= self.L_SUCTION_LINE_LENGTH_RANGE_IN_M[1]:
            self.suctionLineLengthInM = suctionLineLengthInM
        else:
            raise Exception('Suction line length out of range: %r' %(suctionLineLengthInM))

        # verify and set suction line head
        if (suctionLineHeadInM >= self.L_SUCTION_HEAD_LENGTH_RANGE_IN_M[0] and \
            suctionLineHeadInM <= self.L_SUCTION_HEAD_LENGTH_RANGE_IN_M[1] and 
            suctionLineHeadInM <= suctionLineLengthInM) or \
            suctionLineHeadInM == 'AUTO': # corresponds to auto suction head
            self.suctionLineHeadInM = suctionLineHeadInM
        else:
            raise Exception('Suction head length out of range: %r' %(suctionLineHeadInM))
        
        # verify and set nb of rinse cycles
        if nbOfRinseCycles in self.L_RINSE_CYCLES_RANGE: 
            self.nbOfRinseCycles = nbOfRinseCycles
        else:
            raise Exception('Rinse cycles out of range: %r' %(nbOfRinseCycles))
        
        # verify and set nb of retries
        if nbOfRetries in self.L_RETRIES_RANGE: 
            self.nbOfRetries = nbOfRetries
        else:
            raise Exception('Retries out of range: %r' %(nbOfRetries))
        
    def getString(self):
        # get suction line head as string (can be a float or 'AUTO' string)
        if isinstance(self.suctionLineHeadInM, str):
            if 'AUTO' in self.suctionLineHeadInM:
                sSuctionLineHead = 'AUTO'
            else:
                raise Exception('Invalid suction line head string: %r' %(self.suctionLineHeadInM))
        else:
            sSuctionLineHead = '%.2f' % (self.suctionLineHeadInM)
        
        # generate string containing configuration info
        sConfiguration = 'Nb of bottles %d, Bottle volume [l]: %.2f, Suction line length [m]: %.2f, Suction head [m]: %r, Nb of rinse cycles: %d, Nb of retries: %d' \
            %(self.nbOfBottles, self.bottleVolumeInLit, self.suctionLineLengthInM, sSuctionLineHead, self.nbOfRinseCycles, self.nbOfRetries)
        
        return sConfiguration

    def __eq__(self, other):
        '''
        This function compares two configuration objects
        '''
        if self.nbOfBottles == other.nbOfBottles and \
           self.bottleVolumeInLit == other.bottleVolumeInLit and \
           self.suctionLineLengthInM == other.suctionLineLengthInM and \
           self.nbOfRinseCycles == other.nbOfRinseCycles and \
           self.nbOfRetries == other.nbOfRetries:
            result = True
        else:
            result = False
        return result
        
    def getProgramStartNavigationList(self):
        '''
        This function returns a navigation list to set the program start condition
        @return: navigation list 
        '''
        # create navigation list
        lnNavigationList = [
             self.N_NAVIGATE_TO_MAIN_MENU_SCREEN,
             Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['PROGRAM'], None, 0, '\r', 'R', 10, Exception('Navigation failed')),
             Navigation(Navigation.E_SEARCH_TYPE.SCREEN, ['START'], None, 0, None, self.N_NEXT_MENU_PAGE, 15, Exception('Navigation to START failed')),
             Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['START'], None, 0, '\r', 'R', 5, Exception('Setting START failed')),
             Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['WAIT FOR PHONE CALL'], None, 0, '\r', 'R', 10, Exception('Navigation to WAIT FOR PHONE CALL failed'))
             ]
        return lnNavigationList
    
    def getSoftwareOptionsNavigationList(self):
        '''
        This function returns the navigation list to configure the 6712 software
        options (see 6712 datasheet chapter 5.18 Software Options)
        
        @return: navigation list
        '''
        N_SELECT_QUICK_VIEW = Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['QUICK VIEW/CHANGE'], None, 0, '\r', 'R', 5, Exception('Navigation to QUICK VIEW/CHANGE failed'))
        
        LN_SOFTWARE_OPTIONS_NAVIGATION = [
            self.N_NAVIGATE_TO_MAIN_MENU_SCREEN,
            Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['OTHER FUNCTIONS'], None, 0, '\r', 'R', 10, Exception('Navigation to OTHER FUNCTIONS failed')),
            Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['SOFTWARE OPTIONS'], None, 0, '\r', 'R', 10, Exception('Navigation to SOFTWARE OPTIONS failed')),
            Navigation(Navigation.E_SEARCH_TYPE.SCREEN, ['USE LIQUID DETECTOR?'], None, 0, self.N_SELECT_YES, None, 0, None),
            Navigation(Navigation.E_SEARCH_TYPE.SCREEN, ['PROGRAMMING STYLE'], None, 0, N_SELECT_QUICK_VIEW, None, 0, None),
            Navigation(Navigation.E_SEARCH_TYPE.SCREEN, ['LIQUID DETECT'], None, 0, None, self.N_NEXT_MENU_PAGE, 10, Exception('Navigation to LIQUID DETECT failed')),
            Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['LIQUID DETECT'], None, 0, '\r', 'L', 10, Exception('Navigation to LIQUID DETECT failed')),
            Navigation(Navigation.E_SEARCH_TYPE.SCREEN, ['USE LIQUID DETECTOR?'], None, 0, self.N_SELECT_YES, Delay(1), 1, Exception('Navigation to USE LIQUID DETECTOR failed')),
            Navigation(Navigation.E_SEARCH_TYPE.SCREEN, ['BACKLIGHT', 'BTL FULL DETECT'], None, 0, None, self.N_NEXT_MENU_PAGE, 10, Exception('Navigation to BACKLIGHT failed')),
            Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['BACKLIGHT'], None, 0, '\r', 'R', 10, Exception('Navigation to BACKLIGHT failed')),
            Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['TIMED BACKLIGHT'], None, 0, '\r', 'R', 10, Exception('Setting TIMED BACKLIGHT failed')),
            Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['BTL FULL DETECT'], None, 0, '\r', 'R', 10, Exception('Navigation to BTL FULL DETECT failed')),
            Navigation(Navigation.E_SEARCH_TYPE.SCREEN, ['BOTTLE FULL DETECT?'], None, 0, self.N_SELECT_NO, Delay(1), 1, Exception('Setting BOTTLE FULL DETECT? failed')),
            Navigation(Navigation.E_SEARCH_TYPE.SCREEN, ['PUMP COUNTS FOR', 'EACH PURGE CYCLE:'], None, 0, None, self.N_NEXT_MENU_PAGE, 10, Exception('Navigation to PUMP COUNTS FOR EACH PURGE CYCLE failed')),
            Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['PRE-SAMPLE'], None, 0, '\r', 'R', 10, Exception('Navigation to PRE-SAMPLE failed')),
            Navigation(Navigation.E_SEARCH_TYPE.SCREEN, ['PRE-SAMPLE PURGE'], None, 0, '200\r', None, 1, Exception('Setting PRE-SAMPLE PURGE failed')),
            Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['POST-SAMPLE'], None, 0, '\r', 'R', 10, Exception('Navigation to POST-SAMPLE PURGE failed')),
            Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['DEPENDENT ON HEAD'], None, 0, '\r', 'R', 5, Exception('Setting POST-SAMPLE PURGE failed'))
            ]
        return LN_SOFTWARE_OPTIONS_NAVIGATION
    
    def getBottleNavigationList(self):
        '''
        This function returns a navigation list to set the bottle settings
        @return: navigation list 
        '''
        N_WAIT_FOR_SUCTION_HEAD_SCREEN = Navigation(Navigation.E_SEARCH_TYPE.SCREEN, ['BTLS', 'SUCTION LINE'], None, 0, None, Delay(1), 10, Exception('Waiting form completion of pump table generation last too long'))
        
        # build strings containing the settings
        sNbOfBottles = '%d' %(self.nbOfBottles)
        sBottleVolumeInMl = '%d\r' % (self.bottleVolumeInLit * 1000)
        sBottleVolumeInLit = '%.2f\r' % (self.bottleVolumeInLit)
        sSuctionLineLengthInM = '%.1f\r' % (self.suctionLineLengthInM)
        sNbOfRinseCycles = '%d\r' % (self.nbOfRinseCycles)
        sNbOfRetries = '%d\r' % (self.nbOfRetries)
        
        # create navigation list
        lNavigationList = []
        lNavigationList.append(self.N_NAVIGATE_TO_MAIN_MENU_SCREEN)
        lNavigationList.append(Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['PROGRAM'], None, 0, '\r', 'R', 10, Exception('Navigation to PROGRAM failed')))
        lNavigationList.append(Navigation(Navigation.E_SEARCH_TYPE.SCREEN, ['BTLS', 'SUCTION LINE'], None, 0, None, self.N_NEXT_MENU_PAGE, 10, Exception('Navigation bottle menu failed')))
        lNavigationList.append(Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['BTLS'], None, 0, '\r', 'R', 10, Exception('Navigation to BTLS failed')))
        lNavigationList.append(Navigation(Navigation.E_SEARCH_TYPE.SELECTION, [sNbOfBottles], None, 0, '\r', 'R', 10, Exception('Navigation to %r failed' %(sNbOfBottles))))
        lNavigationList.append(Navigation(Navigation.E_SEARCH_TYPE.SCREEN, ['BOTTLE VOLUME IS','ml'], None, 0, sBottleVolumeInMl, None, 0, None))
        lNavigationList.append(Navigation(Navigation.E_SEARCH_TYPE.SCREEN, ['BOTTLE VOLUME IS','lit'], None, 0, sBottleVolumeInLit, None, 0, None))
        lNavigationList.append(Navigation(Navigation.E_SEARCH_TYPE.SCREEN, ['WARNING!'], None, 0, self.N_SELECT_YES, None, 0, None)) # select 'yes' if warning is shown
            
        lNavigationList.append(Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['SUCTION LINE'], None, 0, '\r', 'R', 10, Exception('Navigation to SUCTION LINE failed')))
        lNavigationList.append(Navigation(Navigation.E_SEARCH_TYPE.SCREEN, ['SUCTION LINE LENGTH'], None, 0, sSuctionLineLengthInM, None, 0, Exception('Setting suction line length to %r failed' %(sSuctionLineLengthInM)))) # one digit after comma
        lNavigationList.append(Navigation(Navigation.E_SEARCH_TYPE.SCREEN, ['PUMP TABLES'], None, 0, N_WAIT_FOR_SUCTION_HEAD_SCREEN, None, 0, None)) # one digit after comma
           
        lNavigationList.append(Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['SUCTION HEAD'], None, 0, '\r', 'R', 10, Exception('Navigation to SUCTION HEAD failed')))
        if self.suctionLineHeadInM == 'AUTO':
            lNavigationList.append(Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['AUTO SUCTION HEAD'], None, 0, '\r','R', 10, Exception('Navigation to AUTO SUCTION HEAD failed')))
        else:
            sSuctionLineHeadInM = '%.1f\r' % (self.suctionLineHeadInM)
            lNavigationList.append(Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['ENTER HEAD'], None, 0, '\r', 'R', 10, Exception('Navigation to ENTER HEAD failed')))
            lNavigationList.append(Navigation(Navigation.E_SEARCH_TYPE.SCREEN, ['SUCTION HEAD OF'], None, 0, sSuctionLineHeadInM, None, 0, Exception('Setting suction line length to %r failed' %(sSuctionLineHeadInM)))) # one digit after comma
            
        lNavigationList.append(Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['RINSES','RETRIES'], None, 0, '\r', 'R', 10, Exception('Navigation to RINSES menu failed')))
        lNavigationList.append(Navigation(Navigation.E_SEARCH_TYPE.SCREEN, ['RINSE CYCLES'], None, 0, sNbOfRinseCycles, None, 1, Exception('Setting RINSE CYCLES to %r failed' %(sNbOfRinseCycles)))) # one digit after comma
        lNavigationList.append(Navigation(Navigation.E_SEARCH_TYPE.SCREEN, ['RETRY UP TO'], None, 0, sNbOfRetries, None, 1, Exception('Setting RETRY UP TO to %r failed' %(sNbOfRetries)))) # one digit after comma

        return lNavigationList
    
    def getReportNavigationList(self):

        lnNavigationList = [
            self.N_NAVIGATE_TO_MAIN_MENU_SCREEN,
            Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['VIEW REPORT'], None, 0, '\r', 'R', 10, Exception('Navigation to VIEW REPORT failed')),
            Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['CONFIGURE REPORT'], None, 0, '\r', 'R', 10, Exception('Navigation to CONFIGURE REPORT failed')),
            Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['CUSTOM REPORT'], None, 0, '\r', 'R', 10, Exception('Navigation to CUSTOM REPORT failed')),
            Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['BOTH'], None, 0, '\r', 'R', 10, Exception('Selecting BOTH failed')),
            Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['NONE'], None, 0, '\r', 'R', 10,  Exception('Selecting NONE failed')),
            Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['NONE'], None, 0, '\r', 'R', 10, Exception('Selecting NONE failed')),
            Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['NONE'], None, 0, '\r', 'R', 10, Exception('Selecting NONE failed')),
            Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['NO'], None, 0, '\r', 'R', 10, Exception('Selecting NO failed')),
            Navigation(Navigation.E_SEARCH_TYPE.SELECTION, ['NO'], None, 0, '\r', 'R', 10, Exception('Selecting NO failed'))
        ]
        
        return lnNavigationList
    def lGetSettingsAsGsnList(self):
        # set suctionLineHeadInM to None if it is set to 'AUTO'
        # explicit float type cast for java DOUBLE values is necessary! 
        if isinstance(self.suctionLineHeadInM, str):
            suctionLineHeadInM = None
        else:
            suctionLineHeadInM = float(self.suctionLineHeadInM)

        lGsnList = [
                    self.nbOfBottles,
                    float(self.bottleVolumeInLit),
                    float(self.suctionLineLengthInM),
                    suctionLineHeadInM,
                    self.nbOfRinseCycles,
                    self.nbOfRetries
                    ]
        return lGsnList
    
class Bottles():
    def __init__(self, backupFileName):
        self._lBottles = [] # list with bottle objects
        self._backupFileName = backupFileName
        
        if os.path.isfile(self._backupFileName):
            try:
                # Try to load the bottel status
                bottle_status_file = open(self._backupFileName, 'r')
                self._lBottles = pickle.load(bottle_status_file)
                bottle_status_file.close()
            except Exception, e:
                self.exception(str(e))
        else:
            self._updateBottleStatusFile()
            
    def reinitBottles(self, nbOfBottles, bottleVolumeInMl):
        '''
        This function reinitializes the bottles. Their init levels are
        set to zero.
        @param nbOfBottles: nb of bottles
        @param bottleVolumeInMl: bottle volume in [ml]
        '''
        # create list with bottles
        self._lBottles = [Bottle(volumeInMl = bottleVolumeInMl, levelInMl = 0) for x in range(1, nbOfBottles + 1)]
        self._updateBottleStatusFile()
    
    def bVerifyBottleNb(self, bottleNb):
        '''
        This function verifies if the bottle number exist
        @param bottleNb: bottle number to verify
        @return: True if bottle number is valid
        '''
        return bottleNb in range(1, len(self._lBottles) +1)
    
    def checkCapacity(self, bottleNb, volumeToAddInMl):
        return self._lBottles[bottleNb - 1].checkCapacity(volumeToAddInMl)
    
    def fill(self, bottleNb, volumeToAddInMl, state):
        self._lBottles[bottleNb - 1].fill(volumeToAddInMl, state)
        self._updateBottleStatusFile()
    
    def getLevel(self, bottleNb):
        '''
        This function returns the current level of the bottle:
        @param bottleNb: bottle number 
        @return: current level in [ml]
        '''
        return self._lBottles[bottleNb - 1].getLevel()
    
    def _updateBottleStatusFile(self):
        '''
        This function update the bottle status file
        '''
        bottle_status_file = open(self._backupFileName, 'w')
        pickle.dump(self._lBottles, bottle_status_file)
        bottle_status_file.close()

class Bottle():
    def __init__(self, volumeInMl, levelInMl):
        self._volumeInMl = volumeInMl # bottle capacity in [ml]
        self._levelInMl = levelInMl # current level in the bottle
        self._lFillingHistory = [] # list containing filling history
    
    def checkCapacity(self, volumeToAddInMl):
        '''
        This function checks if there is enough space to add a certain volume
        @param volumeToAddInMl:  volume to add in [ml]
        @return: True if bottle has capacity
        '''
        if self._volumeInMl >= self._levelInMl + volumeToAddInMl:
            return True
        else:
            return False
        
    def fill(self, volumeToAddInMl, state):
        '''
        This function add a certain volume of water to the bottle. Overfilling
        a bottle is possible but raises an exception.
        @param volumeToAddInMl: volume to add to the bottle
        '''
        self._lFillingHistory.append((volumeToAddInMl, state))
        
        self._levelInMl += volumeToAddInMl
        
        if self._levelInMl > self._volumeInMl:
            raise Exception('Bottle number %r with volume %d l was overfilled')
    
    def emptyBottle(self):
        '''
        This function resets the bottle level.
        '''
        self._levelInMl = 0
        
    def getState(self):
        '''
        This function returns the current state of the bottle:
        @return: 1. current level in [ml]
                 2. filling history (list with tuples containing individual
                    filling volumes and states)
        '''
        return self._levelInMl, self._lFillingHistory

    def getLevel(self):
        '''
        This function returns the current level of the bottle:
        @return: current level in [ml]
        '''
        return self._levelInMl

################################################################################
# General functions
#################################################################################
def _calculateChecksum(sData):
    '''
    This function calcuates the checksum of the data received from the water 
    sampler. The checksum is the sum of the ASCII value between the beginning
    of the string and 'CS,'.
    @param sData: string
    @return: checksum
    '''
    # find 'CS,'
    posCs = sData.index('CS,')

    # convert string to list with ASCII values
    lDataAscii = struct.unpack('%sB' % len(sData), sData)

    # calculate checksum
    checksum = sum(lDataAscii[:posCs + 3])

    return checksum