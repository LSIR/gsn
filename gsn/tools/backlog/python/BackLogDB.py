# -*- coding: UTF-8 -*-
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import sqlite3
import os
import sys
import logging
from threading import Thread, Lock, Event

from SpecialAPI import Statistics



RESEND_BUNCH_SIZE = 10
MAX_WAIT_FOR_ACK = 30.0
MAX_DB_ENTRIES_FOR_VACUUM = 10000


class BackLogDBClass(Thread, Statistics):
    '''
    Offers the backlog functionality.
    
    Backlogs messages in a sqlite3 database using their timestamp as primary key.
    
    If buffer_size is 0, sqlite3 INSERT action will be asynchronous
    otherwise this action will be executed in synchronous mode.
    The DELETE action is always asynchronous.
    '''

    '''
    data/instance attributes:
    _logger
    _backlogMain
    _dbname
    _con
    _cur
    _dbNumberOfEntriesId
    _storeCounterId
    _storeTimeId
    _removeCounterId
    _removeTimeId
    _dblock
    _resend
    _resendAmount
    _resendtimer
    _stopped
    '''

    def __init__(self, parent, dbname, backlog_db_resend_hr):
        '''
        Inititalizes the backlog database.
        
        @param parent: the BackLogMain object
        @param dbname: the name/path of the slite3 database used for backlogging.
            If it does not yet exist a new database will be created.
        @param backlog_db_resend_hr: countinous backlog database content resend
            interval in hours.
        
        @raise Exception: if there is a problem with the sqlite3 database.
        '''
        Thread.__init__(self, name='BackLogDB-Thread')
        Statistics.__init__(self)
        
        self._storeCounterId = self.createCounter(60)
        self._storeTimeId = self.createCounter(60)
        self._removeCounterId = self.createCounter(60)
        self._removeTimeId = self.createCounter(60)

        self._logger = logging.getLogger(self.__class__.__name__)
        
        # initialize variables
        self._backlogMain = parent
        self._dbname = dbname
        
        # thread lock to coordinate access to the database
        self._dblock = Lock()
        self._resend = Event()
        self._resendAmount = 0
        self._waitForAck = Event()
        self._ackLock = Lock()
        self._resentMsgIdentifier = [None, None]
        
        # try to create/open database
        self._dblock.acquire()
        try:
            # check_same_thread is not necessary, we are use a global lock 
            self._con = sqlite3.connect(self._dbname, check_same_thread=False)
            self._cur = self._con.cursor()
            
            self._logger.info('integrity check of database')
            
            self._cur.execute('PRAGMA integrity_check')
            if self._cur.fetchone()[0] != 'ok':
                raise sqlite3.Error('failed database integrity check')
            
            self._con.execute('PRAGMA synchronous = OFF')
            
            self._con.execute('CREATE table IF NOT EXISTS backlogmsg (timestamp INTEGER PRIMARY KEY ON CONFLICT REPLACE, type INTEGER, message BLOB)')
            
            self._con.execute('CREATE INDEX IF NOT EXISTS type_index ON backlogmsg (type)')
            
            self._cur.execute('SELECT COUNT(1) FROM backlogmsg')
            self._dbNumberOfEntriesId = self.createCounter(initCounterValue=self._cur.fetchone()[0])
            self._dblock.release()
            self._logger.info(str(self.getCounterValue(self._dbNumberOfEntriesId)) + ' entries in database')
            
            if self.getCounterValue(self._dbNumberOfEntriesId) < MAX_DB_ENTRIES_FOR_VACUUM:
                self._logger.info('vacuum database')
                self._con.execute('VACUUM')
            else:
                self._logger.info('too many entries in database -> skip vacuum')
            
            if self.getCounterValue(self._dbNumberOfEntriesId) > 0:
                self._isBusy = True
            else:
                self._isBusy = False
            
            self._con.commit()
        except sqlite3.Error, e:
            self._dblock.release()
            raise TypeError('sqlite3: %s' % (e,))
        except Exception, e:
            self._dblock.release()
            raise TypeError(e.__str__())
    
        self._stopped = False
        
        self._resendtimer = ResendTimer(backlog_db_resend_hr*3600, self.resend)
        
        if self._logger.isEnabledFor(logging.DEBUG):
            self._logger.debug('database %s ready to use' % (self._dbname,))
        
        
    def storeMsg(self, timestamp, msgType, data):
        '''
        Store a message in the buffer/backlog database.
        
        The timestamp is used as primary key and has to be unique.
        
        @param timestamp: the timestamp of the message
        @param type: the message type
        @param data: the whole message
        
        @return: True if the message has been stored in the buffer/database otherwise False
        '''
        id = self.timeMeasurementStart()
        
        try:
            self._dblock.acquire()
            self._con.execute('INSERT INTO backlogmsg VALUES (?,?,?)', (timestamp, msgType, sqlite3.Binary(data)))
            self._con.commit()
            self.counterAction(self._dbNumberOfEntriesId)
            self._dblock.release()
            
            storeTime = self.timeMeasurementDiff(id)
            self.counterAction(self._storeTimeId, storeTime)
            self.counterAction(self._storeCounterId)

            if self._logger.isEnabledFor(logging.DEBUG):
                self._logger.debug('store (%d,%d,%d): %f s' % (msgType, timestamp, len(data), storeTime))
            return True
        except sqlite3.Error, e:
            self._dblock.release()
            if not self._stopped:
                self.exception(e)
            return False
        except Exception, e:
            self._dblock.release()
            self.exception(e)
            return False

        
    def removeMsg(self, timestamp, msgType):
        '''
        Remove a message out of the buffer/database with a given timestamp.
        
        If a message with the given timestamp does exist in the buffer/database,
        it will be deleted.
        
        @param timestamp: the timestamp of the message to be removed
        '''
        id = self.timeMeasurementStart()

        try:
            self._dblock.acquire()
            self._cur.execute('SELECT COUNT(1) FROM backlogmsg WHERE timestamp = ? and type = ?', (timestamp,msgType))
            cnt = self._cur.fetchone()[0]
            if cnt >= 1:
                self._con.execute('DELETE FROM backlogmsg WHERE timestamp = ? and type = ?', (timestamp,msgType))
                self._con.commit()
                
            self._dblock.release()
            removeTime = self.timeMeasurementDiff(id)
            
            if cnt >= 1:
                self._ackLock.acquire()
                if self._resentMsgIdentifier[0] == msgType and self._resentMsgIdentifier[1] == timestamp:
                    if self._logger.isEnabledFor(logging.DEBUG):
                        self._logger.debug('acknowledge for resent msg (%d,%d,?) received' % (msgType, timestamp))
                    self._waitForAck.set()
                self._ackLock.release()
                self.counterAction(self._dbNumberOfEntriesId, -cnt)
                self.counterAction(self._removeTimeId, removeTime)
                self.counterAction(self._removeCounterId)

            if self._logger.isEnabledFor(logging.DEBUG):
                self._logger.debug('del (%d,%d,?): %f s' % (msgType, timestamp, removeTime))
        except sqlite3.Error, e:
            self.timeMeasurementDiff(id)
            self._dblock.release()
            if not self._stopped:
                self.exception(e) 
        except Exception, e:
            self.timeMeasurementDiff(id)
            self._dblock.release()
            self.exception(e)
            
            
    def getStatus(self, intervalSec):
        '''
        Returns the status of the backlog database as list:
        
        @param intervalSec: the passed n seconds over which min/mean/max is calculated.
        
        @return: status of the backlog database [number of database entries,
                                                 database file size,
                                                 store counter, 
                                                 remove counter, 
                                                 minimum store time, 
                                                 average store time, 
                                                 maximum store time, 
                                                 minimum remove time, 
                                                 average remove time, 
                                                 maximum remove time]
        '''
        stat = [self.getCounterValue(self._dbNumberOfEntriesId), \
                int(os.path.getsize(self._dbname)/1024), \
                self.getCounterValue(self._storeCounterId), \
                self.getCounterValue(self._removeCounterId), \
                self._convert(self.getMinCounterInc(self._storeTimeId, [intervalSec])[0]), \
                self._convert(self.getAvgCounterInc(self._storeTimeId, [intervalSec])[0]), \
                self._convert(self.getMaxCounterInc(self._storeTimeId, [intervalSec])[0]), \
                self._convert(self.getMinCounterInc(self._removeTimeId, [intervalSec])[0]), \
                self._convert(self.getAvgCounterInc(self._removeTimeId, [intervalSec])[0]), \
                self._convert(self.getMaxCounterInc(self._removeTimeId, [intervalSec])[0])]
        return stat
    
    
    def _convert(self, value):
        if value == None:
            return None
        elif int(value*1000) > sys.maxint:
            self.exception("value %d out of range -> set it to None type" % (int(value*1000),))
            return None
        else:
            return int(value*1000)
            
                
    def resend(self):
        '''
        Resend all messages which are in the backlog database to GSN.
        '''
        self._isBusy = True
        self._resendAmount = self.getCounterValue(self._dbNumberOfEntriesId)
        self._resend.set()


    def run(self):
        self._logger.info('started')
        self._resendtimer.start()
        while not self._stopped:
            self._resend.wait()
            if self._stopped:
                break

            timestamp = 0
            
            resendCounter = self._resendAmount
            self._backlogMain.sendResendStarted()
            if resendCounter > 0:
                self._logger.info('trying to resend %d messages from backlog database' % (resendCounter,))
                
                while not self._stopped:
                    if resendCounter <= 0:
                        self._logger.info('resend finished')
                        if resendCounter < 0:
                            self.exception('resend counter dropped below zero')
                        self._backlogMain.sendResendStopped()
                        break
                    
                    try:
                        self._dblock.acquire()
                        if resendCounter >= RESEND_BUNCH_SIZE:
                            self._cur.execute('SELECT * FROM backlogmsg WHERE timestamp > ? order by timestamp asc LIMIT ?', (timestamp,RESEND_BUNCH_SIZE))
                        else:
                            self._cur.execute('SELECT * FROM backlogmsg WHERE timestamp > ? order by timestamp asc LIMIT ?', (timestamp,resendCounter))
                        
                        rows = self._cur.fetchall()
                        self._dblock.release()
                    except sqlite3.Error, e:
                        self._dblock.release()
                        self.exception(e)
                        break
                    
                    if not rows:
                        self._logger.warning('empty select request received -> resend finished')
                        self._backlogMain.sendResendStopped()
                        break
                    resendCounter -= len(rows)
    
                    interrupt = False
                    for index, row in enumerate(rows):
                        if self._stopped:
                            break
                        timestamp = row[0]
                        msgType = row[1]
                        message = row[2]
                        # should be blocking until queue is free and ready to send
                        self._logger.debug('rsnd...')
                        if index == 1:
                            self._ackLock.acquire()
                            self._resentMsgIdentifier = [msgType, timestamp]
                            self._waitForAck.clear()
                            self._ackLock.release()
                        if self._backlogMain.gsnpeer.processResendMsg(msgType, timestamp, message):
                            if self._logger.isEnabledFor(logging.DEBUG):
                                self._logger.debug('rsnd (%d,%d,%d)' % (msgType, timestamp, len(message)))
                        else:
                            self._logger.info('resend interrupted')
                            self._backlogMain.sendResendStopped()
                            interrupt = True
                            break
                        
                    if interrupt or self._stopped:
                        break
                    
                    self._waitForAck.wait(MAX_WAIT_FOR_ACK)
                    if self._backlogMain.gsnpeer.isConnected() and not self._waitForAck.isSet():
                        self._logger.debug('resent message (%d,%d,%d) has not been acknowledged whithin %f seconds' % (msgType, timestamp, len(message), MAX_WAIT_FOR_ACK))
                    if not self._backlogMain.gsnpeer.isConnected() or self._stopped:
                        break
            else:
                self._backlogMain.sendResendStopped()

            self._isBusy = False
            self._resend.clear()

        self._logger.info('died')
        
        
    def pauseResending(self):
        self._resendtimer.pause()
        
        
    def resumeResending(self):
        self._resendtimer.resume()


    def __del__(self):
        self._dblock.acquire()
        if '_cur' in locals():
            self._cur.close()
        if '_con' in locals():
            self._con.close()
        self._dblock.release()
        
        
    def isBusy(self):
        return self._isBusy
        

    def stop(self):
        self._isBusy = False
        self._stopped = True
        self._resend.set()
        self._waitForAck.set()
        self._resendtimer.stop()
        self._logger.info('stopped')
        
        
    def exception(self, e):
        self._backlogMain.incrementExceptionCounter()
        self._logger.exception(str(e))



class ResendTimer(Thread):
    
    '''
    data/instance attributes:
    _logger
    _interval
    _action
    _wait
    _timer
    _stopped
    '''
    
    def __init__(self, interval, action):
        Thread.__init__(self, name='ResendTimer-Thread')
        self._logger = logging.getLogger(self.__class__.__name__)
        self._interval = interval
        self._action = action
        self._wait = None
        self._timer = Event()
        self._stopped = False
        
           
    def run(self):
        self._logger.info('started')
        # wait for first resume
        self._timer.wait()
        self._timer.clear()
        while not self._stopped:
            self._timer.wait(self._wait)
            if self._timer.isSet():
                self._timer.clear()
                continue
            self._action()
            
        self._logger.info('died')
    
    
    def pause(self):
        self._wait = None
        self._timer.set()
        self._logger.debug('paused')
    
            
    def resume(self):
        self._wait = self._interval
        self._timer.set()
        self._logger.debug('resumed')
    
    
    def stop(self):
        self._stopped = True
        self._timer.set()
        self._logger.info('stopped')
        