# -*- coding: UTF-8 -*-
import serial
import Queue
from threading import Event
from AbstractPlugin import AbstractPluginClass
import logging
import struct


DEFAULT_BACKLOG = True

def enum(**enums):
    return type('Enum', (), enums)

# Define enumerations
eGsnMessageType = enum(SAMPLING_RESULT=0, SAMPLER_STATUS=1)
eMessageOrigin = enum(CRON=0, GSN=1, TOS=2)
eCommandType = enum(TAKE_SAMPLE=0)
eGsnMsgCommandType = enum(TAKE_SAMPLE=2)

class Sampler6712PluginClass(AbstractPluginClass):
    '''
    This plugin is for testing.
    '''

    def __init__(self, parent, options):
        AbstractPluginClass.__init__(self, parent, options, DEFAULT_BACKLOG)
        
        self._taskqueue = Queue.Queue() # queue where incoming GSN messages are stored
        self._wait = Event()
        self._plugstop = False
    
    def stop(self):
        self._plugstop = True
        self._wait.set()
        try:
            self.sampler.stop()
        except Exception as e:
            self.warning('Sample couldn\'t been stopped: %s' %(e))
        self.info('stopped')
        
        
    def run(self):
        self.name = 'Sampler6712-Thread'
        self.info('started')

        # read device name (serial port name) from config file
        sDevice = self.getOptionValue('device_name')
        if sDevice is None:
            sDevice = '/tmp/myttyUSB0'
            self.warning('no device_name specified')
        
        # read baudrate from config file
        sBaudrate = self.getOptionValue('baudrate')
        if sBaudrate is None:
            baudrate = 19200
            self.warning('no baudrate specified')
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
            self.error("Could not access 6712 sampler: %s" % e)

        # initialize 6712
        try:
            # turn on 6712
            status = self.sampler.turnOn6712()  # turn on 6712
        except Exception as e:
            self.error(e)
        

        while not self._plugstop:
            if self._taskqueue.empty(): # verify if tasks in task queue
                self._isBusy = False
            
            nbOfElementsInQueue = self._taskqueue.qsize()
            if nbOfElementsInQueue > 20:
                self.warning('Number of elements in queue is %d!!' % (nbOfElementsInQueue))
            elif nbOfElementsInQueue > 10:
                self.info('Number of elements in queue is %d' % (nbOfElementsInQueue))
                
            lQueueElement = self._taskqueue.get() # blocking wait for tasks
            self._isBusy = True
            
            if self._plugstop:
                try:
                    self._taskqueue.task_done()
                except ValueError, e:
                    self.exception(e)
                break
            
            # handle new task
            try:
                if lQueueElement[1] == eCommandType.TAKE_SAMPLE:
                    self.samplingObject = lQueueElement[2] # read command
                    self.info('New sampling command received (from %d)' % lQueueElement[0])
                
                    if(isinstance(self.samplingObject, Sampling)):
                        self.info('sampling object received: bottle nb: %d, volume: %d' % (self.samplingObject.bottleNb, self.samplingObject.waterVolume))
                    
                        # trigger sampling
                        try:
                            status = self.sampler.takeSample6712(self.samplingObject)
                            self.info('Status at sampling request: %s, recent sampling result: %s' % (self.sampler.dStatus6712[status.status], self.sampler.dLastSampleResult[status.mostRecentSamplingResult]))
    
                            # verify is sampler is not turned off
                            if status.status == 9:
                                self.sampler.turnOn6712()  # turn on 6712
                                status = self.sampler.takeSample6712(self.samplingObject)

                            # wait until sampling complete (reading status)
                            timeoutCounter = 200
                            while status.status == 12 or status.mostRecentSamplingResult == 12 and timeoutCounter > 0:
                                timeoutCounter -= 1
                                status = self.sampler.getStatus6712()
                                self.info('Status during sampling: %s, recent sampling result: %s' % (self.sampler.dStatus6712[status.status], self.sampler.dLastSampleResult[status.mostRecentSamplingResult]))
                                
                            if timeoutCounter == 0:
                                self.warning('Sampling last to long, status polling timed out')
                        
                        except Exception as e:
                            self.warning(e)
                            status = Status6712(None) # create status containing Nones
                        
                        # report sampling result
                        timestamp = self.getTimeStamp()
                        lGsnMsgPayload = []
                        lGsnMsgPayload += [eGsnMessageType.SAMPLING_RESULT] # set type of gsn msg
                        lGsnMsgPayload += [lQueueElement[0]] # sampling trigger
                        lGsnMsgPayload += [self.samplingObject.bottleNb, self.samplingObject.waterVolume] # bottle nb and volume
                        lGsnMsgPayload += [self.sampler.get6712OveralStatus()] # set overall status
                        lGsnMsgPayload += status.getStatusAsList() # set sampling result
                        
                        self.processMsg(timestamp, lGsnMsgPayload)
                    
                    else:
                        self.error('Wrong queue object') 

                    self._taskqueue.task_done()
                else:
                    self.warning('Unknown command type of queue element: %d' % (lQueueElement[1]))
                
            except Exception, e:
                self.exception(str(e))

        self.info('died')
    
    def action(self, parameters):
        self.info('new action triggered')
        # actions are triggered by the schedule plugin
        # here the parameters have to be converted into 'take sample' object
        try:
            samplingObject = self._parseActionParameter(parameters)
            self.info('Queue sample message from scheduler (bottle nb: %d, volume: %d)' % (samplingObject.bottleNb, samplingObject.waterVolume))
            self._taskqueue.put([eMessageOrigin.CRON, eCommandType.TAKE_SAMPLE, samplingObject])
        except Exception as e:
            self.warning(e) 

        
        
    def getMsgType(self):
        return 128
    
            
    def msgReceived(self, data):
        # store data into queue (queue is handled in run())
        if data[0] == eGsnMsgCommandType.TAKE_SAMPLE:
            try:
                samplingObject = Sampling(data[1], data[2])
                self.info('Queue sample message from GSN (bottle nb: %d, volume: %d)' % (samplingObject.bottleNb, samplingObject.waterVolume))
                self._taskqueue.put([eMessageOrigin.GSN, eCommandType.TAKE_SAMPLE, samplingObject])
            except Exception as e:
                self.warning(e)            
        else:
            self.info('Unknown GSN command message type: %d' % (data[0]))
        
        
            
    def isBusy(self):
        return False
        
        
    def needsWLAN(self):
        return False
            
    
    def _parseActionParameter(self, sParameter):
        '''
        This function parses the parameter of schedule entry. A schedule entry
        should look like as follows:
            bottle(BOTTLE NB) volume(WATER VOLUME)
        Whitespaces inside the brackets are not allowed!
        The parsed parameters are stored in a Sampling object, containing the 
        bottle number and the water volume.
        @param sParameter: The parameters as one string given in the schedule 
                           file.

        @return: Sampling object containing bottle number and volume.

        '''
        tsParameters = sParameter.strip().split() # remove leading and trailing whitespaces, split string where whitspaces

        for sParameter in tsParameters:
            p = sParameter.lower() # convert parameter to lowercase
            if p.startswith('bottle'):
                sBottleNb = p.split('(')[1].split(')')[0] # read bottle number
                try:
                    bottleNb = int(float(sBottleNb)) # convert string into number
                except Exception as e:
                    raise Exception('Invalid water volume in %s: %s' % (sParameter, e))
            elif p.startswith('volume'):
                sWaterVolume = p.split('(')[1].split(')')[0] # read bottle number
                try:
                    waterVolume = int(float(sWaterVolume)) # convert string into number
                except Exception as e:
                    raise Exception('Invalid water volume in %s: %s' % (sParameter, e))
            else:
                raise('unrecognized parameter >%s< in schedule' % (sParameter,))
            
        sampling = Sampling(bottleNb, waterVolume) # create new Sampling object to store
        
        return sampling



class Sampler6712Driver():

    def __init__(self, config):
        self._logger = logging.getLogger(self.__class__.__name__)

        self._logger.info('Init Sampler 6712 Driver...')

        # The following constant value defines the time to wait after a question mark
        # has been sent and the banner string is expected
        self._DELAY_BETWEEN_QUESTION_MARK_AND_BANNER_STRING = 0.4

        # The following constant value defines the time to wait after the banner string
        # (string containing the 6712 HW and SW revision) and the menu is displayed
        self._DELAY_BETWEEN_BANNER_STRING_AND_MENU = 1.4

        # The following constant value defines the time required to display two 
        # consecutive lines of the menu
        self._DELAY_DURING_MENU_DISPLAY = 1.0

        # The following constant value defines the time to wait for new data
        # after the caret symbol '> '
        self._DELAY_AFTER_CARET = 0.2

        # The following constant value defines the time to wait for new data
        # after a menu transition command has been sent
        self._DELAY_AFTER_MENU_TRANSITION_COMMAND = 2

        # The following constant value defines the time required to turn on the
        # water sampler and report its status
        self._DELAY_BETWEEN_TURN_ON_SAMPLER_AND_REPLY = 3

        # The following constant value defines the time between the status request
        # and the reply
        self._DELAY_BETWEEN_STATUS_REQUEST_AND_REPLY = 2

        # The following constant value defines the time between the sampling request
        # and status reply
        self._DELAY_BETWEEN_SAMPLING_REQUEST_AND_STATUS_REPLY = 2

        # The following constant value defines the time required by the 6712
        # to reply on a input when in remote control of sampler keypad mode
        self._DELAY_BETWEEN_CONTROL_COMMAND_AND_REPLY = 3

        # dictionary with all possible 6712 status numbers(keys)
        # see datasheet p.7-5
        self.dStatus6712 = {1: 'WAITING TO SAMPLE',
                            4: 'POWER FAILED',
                            5: 'PUMP JAMMED',
                            6: 'DISTRIBUTOR JAMMED',
                            9: 'SAMPLER OFF',
                           12: 'SAMPLE IN PROGRESS',
                           20: 'INVALID COMMAND',
                           21: 'CHECKSUM MISMATCH',
                           22: 'INVALID BOTTLE',
                           23: 'VOLUME OUT OF RANGE'}

        # dictionary with all possible 6712 sampling results numbers (keys)
        # see datasheet p.7-5
        self.dLastSampleResult = { 0: 'SAMPLE OK',
                                   1: 'NO LIQUID FOUND',
                                   2: 'LIQUID LOST',
                                   3: 'USER STOPPED',
                                   4: 'POWER FAILED',
                                   5: 'PUMP JAMMED',
                                   6: 'DISTRIBUTOR JAMMED',
                                   8: 'PUMP LATCH OPEN',
                                   9: 'SAMPLER SHUT OFF',
                                  11: 'NO DISTRIBUTOR',
                                  12: 'SAMPLE IN PROGRESS'}

        # dictionary with all possible 6712 remote control mode numbers (keys)
        # OFFLINE_MODE and SAMPLING_REPORTS_MODE are not possible because they
        # are left when the function is called (due to the sending of '?')
        self.dOperationMode = {0:'UNKNOWN_MODE',
                              1:'OFFLINE_MODE',
                              2:'EXTERNAL_PROGRAM_CONTROL_MODE',
                              3:'MENU_CONTROL_MODE',
                              4:'REMOTE_CONTROL_OF_SAMPLER_KEYPAD_MODE',
                              5:'SAMPLING_REPORTS_MODE'}
        
        # sampler overal stati:
        self.E_SAMPLER_OVERAL_STATUS = enum(OK=0, COMMUNICATION_ERROR=1, SERIAL_PORT_ERROR=2)
    
        self._samplerOveralStatus = self.E_SAMPLER_OVERAL_STATUS.OK
        
        self._serialStr = config[0]
        self._serialBaudrate = int(config[1])

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


    def get6712OveralStatus(self):
        '''
        This function returns the 6712 water sampler overal status.
        @return: overall status
        '''
        return self._samplerOveralStatus

    def turnOn6712(self):
        '''
        This function turns the 6712 water sampler on. Turn on is repeated
        up to three times before an exception is thrown.
        @return: parsed status
        '''
        sCommand = 'STS,2' # command to send to 6712
        # change overal status, is set to OK if turn on successful
        self._samplerOveralStatus = self.E_SAMPLER_OVERAL_STATUS.COMMUNICATION_ERROR 
        
        counter = 3 # number of times to attemp to turn on 6712
        while counter > 0:
            counter -= 1

            # set external control operation mode
            try:
                self._set6712RemoteOperationMode(2) 
            except Exception as e:
                if counter > 0:
                    self._logger.warning('Setting remote operation to %s failed: %s' % (self.dOperationMode[2], e))
                    continue # go to start of while and try again to set mode
                else:
                    raise Exception('Sampler turn on failed') # leave function

            # turn on 6712
            self._logger.info('Attemp to turn on 6712 sampler')
            try:
                statusOf6712 = self._sendCmdAndReadStatusReply(self._DELAY_BETWEEN_TURN_ON_SAMPLER_AND_REPLY, sCommand)
                self._samplerOveralStatus = self.E_SAMPLER_OVERAL_STATUS.OK # change overal status to OK
                
                # stop loop
                if statusOf6712.status != 9:
                    self._logger.info('6712 sampler turned on')
                    break # leave loop

            except Exception as e:
                if counter > 0:
                    self._logger.warning('Command \'%s\'failed: %s' % (sCommand, e))
                else:
                    raise Exception('Sampler turn on failed') # leave function

        return statusOf6712

    def getStatus6712(self):
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
            statusOf6712 = self._sendCmdAndReadStatusReply(self._DELAY_BETWEEN_STATUS_REQUEST_AND_REPLY, sCommand)
        except Exception as e:
            raise Exception('Command \'%s\'failed: %s' % (sCommand, e))
        
        self._samplerOveralStatus = self.E_SAMPLER_OVERAL_STATUS.OK # change overal status
        return statusOf6712

    def takeSample6712(self, samplingObject):
        '''
        This function initiates a single sampling. An exception is thrown if the 
        status could not been read.
        @return: parsed status
        '''
        sCommand = 'BTL,%d,SVO,%d' % (samplingObject.bottleNb, samplingObject.waterVolume)
        # change overal status, is set to OK if take sample successful
        self._samplerOveralStatus = self.E_SAMPLER_OVERAL_STATUS.COMMUNICATION_ERROR 
    
        # try to set external control operation mode
        try:
            self._set6712RemoteOperationMode(2) 
        except Exception as e:
            raise Exception('Setting remote operation to %s failed: %s' % (self.dOperationMode[2], e))
        
        self._logger.info('Take a sample: bottle nb: %d, volume: %d' % (samplingObject.bottleNb, samplingObject.waterVolume))
        
        # try to send command and read reply
        try:
            statusOf6712 = self._sendCmdAndReadStatusReply(self._DELAY_BETWEEN_SAMPLING_REQUEST_AND_STATUS_REPLY, sCommand)
        except Exception as e:
            raise Exception('Command \'%s\'failed: %s' % (sCommand, e))
        
        self._samplerOveralStatus = self.E_SAMPLER_OVERAL_STATUS.OK # change overal status
        return statusOf6712

    def stop(self):
        try:
            self._serial.close() # close serial port
        except Exception as e:
            raise Exception('Closing serial port failed: %s' % (e))
            

    def _appendChecksum(self, sData):
        '''
        This function appends the checksum to the string.
        @param sData: string
        @return: string with appended checksum
        '''
        sTemp = sData.replace('\r', '') # remove cr
        sTemp = sTemp.replace('\n', '') # remove lf
        sTemp += ',CS,' # append checksum text
        sTemp += str(_calculateChecksum(sTemp)) + str('\r\n') # append checksum value and cr/lf
    
        return sTemp

    
    def _sendCmdAndReadStatusReply(self, timeout, sCommand):
        '''
        This function sends a command and verifies its reply which has to be
        of type status. That is, the following commands are allowed:
        - STS,1
        - STS,2
        - BTL,x,SVO,y
        The command does not have to be terminated with cr/lf. It is extended with the 
        checksum
    
        @param timeout: timeout of serial interface
        @param sCommand: command string to send
        @return: parsedStatus (status reply), its values are set to None if reading
        of the status failed.
        '''
        # flush serial buffers
        self._serial.flushInput()
        self._serial.flushOutput()
    
       # parsedStatus = None
        
        sCommand = self._appendChecksum(sCommand) # append checksum to command

        # request status
        self._serial.write(sCommand)

        # read reply
        self._serial.timeout = timeout
        sCommandEcho = self._serial.readline() # read command echo
        sStatus = self._serial.readline() # read status line

        # wait for caret
        if self._waitForCaret(sStatus) == False:
            raise Exception('Caret \'>\' not in \'%s\'' % (sStatus))
            #self._logger.warning('Caret \'>\' not in \'%s\'' %(sStatus))

        # verify command echo
        if sCommand in sCommandEcho:
            parsedStatus = Status6712(sStatus)
        else:
            raise Exception('Invalid command echo (\'%s\' instead of \'%s\')' % (sCommandEcho, sCommand))

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
# The following functions are used to determin and set the operation mode
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

        @return: remote operation mode (key, see dOperationMode above)
        '''
        mode = 0 # unknown mode
        counter = 10 # used for the detection of the REMOTE_CONTROL_OF_SAMPLER_KEYPAD_MODE
        serialData = '' # data received from the 6712

        # flush serial buffers
        self._serial.flushInput()
        self._serial.flushOutput()

        self._logger.info('Attemp to determine 6712 remote operation mode')

        while mode == 0 and counter > 0:
            
            counter -= 1 # decrement counter

            self._serial.write('?') # send question mark to 6712

            # read reply until 6712 stops sending data
            serialData = self._read6712Reply(self._DELAY_BETWEEN_QUESTION_MARK_AND_BANNER_STRING)

            # verify reply
            if '*** Model 6712' in serialData:
            # either external program control or menu control are active
               if 'Exit MENU' in serialData:
                   mode = 3 # menu control mode
               else:
                   mode = 2 # external program control mode

        # if no '?' is replied by the 6712 further tests are required to verify
        # if 6712 is in the remote control of sampler keypad mode
        if mode == 0:
            # verification if device is in remote control of sampler keypad mode
            # is done by sending first an 's' (corresponds to STOP key). If
            # 6712 does not react, it may be turned of. Therefore, an 'o' is 
            # sent next to power off the device. If 6712 does not answer
            # on 'o' the device is not connected and the mode therefore unknown
        
            # tuple with string to send to 6712. First string is 's' character
            # corresponding to STOP key and second character is 'o' corresponding
            # to the ON key.
            tControlStrings = ('s', 'o') 
            self._serial.timeout = self._DELAY_BETWEEN_CONTROL_COMMAND_AND_REPLY
        
            counter = 0
            while counter < len(tControlStrings):
                self._serial.flushInput()
                self._serial.flushOutput()
                self._serial.write(tControlStrings[counter]) # send character
                serialData = self._serial.readline(eol='\x1b') # read reply
                if '\x1b' in serialData:
                    mode = 4 # remote control of sampler keypad mode
                    counter = len(tControlStrings) # stop while loop
                else:
                    counter += 1

        self._logger.info('6712 remote operation mode: %s' % (self.dOperationMode[mode]))
        return mode

    def _set6712RemoteOperationMode(self, modeToSet):
        '''
        This function set the remote operation mode of the 6712 water sampler. 
        Before the new operation mode is set, the current mode is determined.
        After the mode transition by means of dTransition, the 6712's reply is
        verified.
        '''
        #                 __current mode
        #                |  _target mode
        #                | |  command to sent to 6712
        dTransition = { (2, 1):'Q\r\n',
                        (2, 3):'MENU\r\n',
                        (2, 4):'MENU\r\n',
                        (3, 1):'Q\r\n',
                        (3, 2):'Q\r\n',
                        (3, 4):'CONTROL\r\n',
                        (4, 1):'Q',
                        (4, 2):'Q',
                        (4, 3):'Q'}
    
        # blank display of 6712 water sampler
        BLANK_6712_DISPLAY = '                    \r\n                    \r\n                    \r\n                    \r\n'
        TIMEOUT_COUNTER_A = 3
        TIMEOUT_COUNTER_B = 5

        currentMode = 0
        timeoutCounterA = TIMEOUT_COUNTER_A
        
        self._logger.info('Set remote operation mode to %s' % (self.dOperationMode[modeToSet]))
        
        while currentMode == 0 and timeoutCounterA > 0:
            timeoutCounterA -= 1 # decrement counter

            # determin current mode (if in offine mode, offline mode is left by this
            # function)
            currentMode = self._get6712RemoteOperationMode()
            
            # verify if mode is known
            if currentMode == 0:
                # unknown mode, that is, no connection with 6712!
                raise Exception('No connection with 6712')
            
#            self._logger.info('Current operation mode (at start): %s' %(self.dOperationMode[currentMode]))

            timeoutCounterB = TIMEOUT_COUNTER_B
            while currentMode != modeToSet and currentMode != 0 and timeoutCounterB > 0:
                timeoutCounterB -= 1 # decrement counter
                transitionKey = (currentMode, modeToSet)

                #debug
#                self._logger.info('Transition key: (%d,%d)' %(transitionKey[0], transitionKey[1]))

                # flush serial buffers
                self._serial.flushInput()
                self._serial.flushOutput()
                self._serial.write(dTransition[transitionKey]) # send msg to change mode

                # read reply until 6712 stops sending data
                serialData = self._read6712Reply(self._DELAY_AFTER_MENU_TRANSITION_COMMAND)

                # verify reply
                if  transitionKey == (2, 3) or transitionKey == (2, 4) or transitionKey[0] == 4:
                    if 'Exit MENU' in serialData:
                        currentMode = 3 # menu control mode
                    else:
                        currentMode = 0 # unknown mode (behaviour is not as expected)

                elif transitionKey == (3, 4):
                    if BLANK_6712_DISPLAY in serialData or '\x1b\x5b' in serialData:
                    # '\x1b\x5b' is a specific pattern only used when 6712 is on and
                    # remote control of sampler keypad mode is active
                        currentMode = 4
                    else:
                        currentMode = 0 # unknown mode (behaviour is not as expected)

                elif transitionKey == (3, 1) or transitionKey == (3, 2):
                    if 'Q\r\n\r\n\r\n> ' in serialData:
                        currentMode = 2
                    else:
                        currentMode = 0 # unknown mode (behaviour is not as expected)

                elif transitionKey == (2, 1):
                    if 'Q\r\n\r\n' in serialData:
                        currentMode = 1
                    else:
                        currentMode = 0 # unknown mode (behaviour is not as expected)

                else:
                    self._logger.warning('invalid transition key: %d' % (transitionKey))

                #debug
                self._logger.info('Current operation mode: %s' % (self.dOperationMode[currentMode]))
        
        return currentMode


    def _read6712Reply(self, initialTimeout):
        '''
        This function reads the reply of the 6712 water sampler. Data is read
        until 6712 stops sending data.
        '''
        serialData = '' # data received from 6712
        maxNbOfLines = 50

        self._serial.timeout = initialTimeout;

        # read reply until 6712 stops sending data
        newLine = self._serial.readline() # read data from 6712
        while newLine != '' and maxNbOfLines > 0: # verify if reply received
            maxNbOfLines -= 1
            if '*** Model 6712' in newLine:
               self._serial.timeout = self._DELAY_BETWEEN_BANNER_STRING_AND_MENU   # set timeout to wait for menu, possibly sent by 6712
            if '<ST>' in newLine: 
               self._serial.timeout = self._DELAY_DURING_MENU_DISPLAY # set timeout to display menu
            if '> ' in newLine:
               self._serial.timeout = self._DELAY_AFTER_CARET # set timeout
            serialData += newLine # store read line
            #print "serial data (line): " + newLine # debug
            newLine = self._serial.readline() # read data
    
        #TODO: raise error if maxNbOfLines == 0
        if maxNbOfLines == 0:
            raise Exception('To much data received from 6712')
    
        return serialData

class Status6712():
    '''
    This status class contains information about the status of the 6712.
    '''
    def __init__(self, sStatus):
        '''
        This function parses the status string of the 6712 and returns the parsed
        data as a Status6712 object. If sStatus is None, an object containing
        all Nones is returned.
    
        @param sStatus: data to parse
        @return: parsed data if parsing was successful or None if parsing failed
        '''
        self.modelNb = None
        self.id = None
        self.timeOf6712 = None
        self.status = None
        self.mostRecentSamplingTime = None
        self.mostRecentSamplingBottle = None
        self.mostRecentSamplingVolume = None
        self.mostRecentSamplingResult = None
        self.checksum = None

        if sStatus != None:
            lStatus = sStatus.split(',') # split data into list, commas are removed
    
            try:
                # parse data and store it into status object
                self.modelNb = int(lStatus[lStatus.index('MO') + 1])
                self.id = int(lStatus[lStatus.index('ID') + 1])
                self.timeOf6712 = float(lStatus[lStatus.index('TI') + 1])
                self.status = int(lStatus[lStatus.index('STS') + 1])
                self.mostRecentSamplingTime = float(lStatus[lStatus.index('STI') + 1])
                self.mostRecentSamplingBottle = int(lStatus[lStatus.index('BTL') + 1])
                self.mostRecentSamplingVolume = int(lStatus[lStatus.index('SVO') + 1])
                self.mostRecentSamplingResult = int(lStatus[lStatus.index('SOR') + 1])
                self.checksum = int(lStatus[lStatus.index('CS') + 1])
    
                # verify checksum
                if _calculateChecksum(sStatus) == self.checksum:
                    self._checksumOk = True
                else: 
                    self._checksumOk = False
                    raise Exception('Checksum of \'%s\' is wrong: %s' % (sStatus, e))
            except Exception as e:
                raise Exception('Parsing of \'%s\' failed: %s' % (sStatus, e))
    
    def getStatusAsList(self):
        '''
        This function returns the status values as a list. The order is as follows:
        [MO,ID,TI,STS,STI,BTL,SVO,SOR,CS]
    
        @return: status as a list
        '''
        lStatus = [ self.modelNb,
                    self.id,
                    self.timeOf6712,
                    self.status,
                    self.mostRecentSamplingTime,
                    self.mostRecentSamplingBottle,
                    self.mostRecentSamplingVolume,
                    self.mostRecentSamplingResult,
                    self.checksum]
        return lStatus

class Sampling():
    def __init__(self, bottleNb=None, waterVolume=None):
        try:
            self.bottleNb = int(bottleNb)
            self.waterVolume = int(waterVolume)
        except Exception as e:
            raise Exception('Creation of sampling object failed: %s' % e)

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
    
