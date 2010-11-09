
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import sqlite3
import time
import os
import logging
from threading import Thread, Lock, Event


SLEEP_BEFORE_RESEND_ON_RECONNECT = 30


class BackLogDBClass(Thread):
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
    _parent
    _dbname
    _con
    _cur
    _dbNumberOfEntries
    _minStoreTime
    _maxStoreTime
    _meanStoreTime
    _storeCounter
    _minRemoveTime
    _maxRemoveTime
    _meanRemoveTime
    _removeCounter
    _dblock
    _counterlock
    _sleep
    _resend
    _stopped
    '''

    def __init__(self, parent, dbname):
        '''
        Inititalizes the backlog database.
        
        @param parent: the BackLogMain object
        @param dbname: the name/path of the slite3 database used for backlogging.
            If it does not yet exist a new database will be created.
        @param buffer_size: the in memory buffer size to be used as volatile backlog.
            If the buffer reaches this limit, it will be written to the backlog
            sqlite3 database. Setting it to 0 is equal to storing the message
            directly to the sqlite3 database. Using this buffer can drastically
            speed up the backlog functionality.
        @param resend_sleep: time to sleep between resending messages.
        
        @raise Exception: if there is a problem with the sqlite3 database.
        '''
        Thread.__init__(self)
        self._minStoreTime = -1
        self._maxStoreTime = -1
        self._meanStoreTime = -1
        self._storeCounter = 1
        self._minRemoveTime = -1
        self._maxRemoveTime = -1
        self._meanRemoveTime = -1
        self._removeCounter = 1

        self._logger = logging.getLogger(self.__class__.__name__)
        
        # initialize variables
        self._parent = parent
        self._dbname = dbname
        
        # thread lock to coordinate access to the database
        self._dblock = Lock()
        self._counterlock = Lock()
        self._resend = Event()
        self._sleepEvent = Event()
        
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
            
            self._logger.info('vacuum database')
            
            self._con.execute('VACUUM')
            
            self._con.execute('PRAGMA synchronous = OFF')
            
            self._con.execute('CREATE table IF NOT EXISTS backlogmsg (timestamp INTEGER PRIMARY KEY ON CONFLICT REPLACE, type INTEGER, message BLOB)')
            
            self._con.execute('CREATE INDEX IF NOT EXISTS type_index ON backlogmsg (type)')
            
            self._cur.execute('SELECT COUNT(1) FROM backlogmsg')
            self._dbNumberOfEntries = self._cur.fetchone()[0]
            self._dblock.release()
            self._logger.info(str(self._dbNumberOfEntries) + ' entries in database')
            
            if self._dbNumberOfEntries > 0:
                self._isBusy = True
            else:
                self._isBusy = False
            
            self._con.commit()
        except sqlite3.Error, e:
            self._dblock.release()
            raise TypeError('sqlite3: ' + e.__str__())
        except Exception, e:
            self._dblock.release()
            raise TypeError(e.__str__())
    
        self._stopped = False
        self._sleep = False
        
        self._logger.debug('database ' + self._dbname + ' ready to use')
        
        
    def storeMsg(self, timestamp, msgType, data):
        '''
        Store a message in the buffer/backlog database.
        
        The timestamp is used as primary key and has to be unique.
        
        @param timestamp: the timestamp of the message
        @param type: the message type
        @param data: the payload of the message
        
        @return: True if the message has been stored in the buffer/database otherwise False
        '''
        t = time.time()
        
        try:
            self._dblock.acquire()
            self._con.execute('INSERT INTO backlogmsg VALUES (?,?,?)', (timestamp, msgType, sqlite3.Binary(data)))
            self._con.commit()
            self._dbNumberOfEntries += 1
            self._dblock.release()
            storeTime = time.time() - t
            self._counterlock.acquire()
            if self._minStoreTime == -1 or storeTime*1000 < self._minStoreTime:
                self._minStoreTime = storeTime*1000
            if self._maxStoreTime == -1 or storeTime*1000 > self._maxStoreTime:
                self._maxStoreTime = storeTime*1000
            if self._meanStoreTime == -1:
                self._meanStoreTime = storeTime*1000
            else:
                self._meanStoreTime += storeTime*1000
                self._storeCounter += 1
            self._counterlock.release()
            self._logger.debug('store (%d,%d,%d): %f s' % (msgType, timestamp, len(data), storeTime))
            return True
        except sqlite3.Error, e:
            self._dblock.release()
            if not self._stopped:
                self.exception(e)
            return False

        
    def removeMsg(self, timestamp, msgType):
        '''
        Remove a message out of the buffer/database with a given timestamp.
        
        If a message with the given timestamp does exist in the buffer/database,
        it will be deleted.
        
        @param timestamp: the timestamp of the message to be removed
        '''
        t = time.time()

        try:
            self._dblock.acquire()
            self._cur.execute('SELECT COUNT(1) FROM backlogmsg WHERE timestamp = ? and type = ?', (timestamp,msgType))
            cnt = self._cur.fetchone()[0]
            if cnt >= 1:
                self._con.execute('DELETE FROM backlogmsg WHERE timestamp = ? and type = ?', (timestamp,msgType))
                self._con.commit()
                self._dbNumberOfEntries -= cnt
            self._dblock.release()
            removeTime = time.time() - t
            self._counterlock.acquire()
            if self._minRemoveTime == -1 or removeTime*1000 < self._minRemoveTime:
                self._minRemoveTime = removeTime*1000
            if self._maxRemoveTime == -1 or removeTime*1000 > self._maxRemoveTime:
                self._maxRemoveTime = removeTime*1000
            if self._meanRemoveTime == -1:
                self._meanRemoveTime = removeTime*1000
            else:
                self._meanRemoveTime += removeTime*1000
                self._removeCounter += 1
            self._counterlock.release()
            self._logger.debug('del (%d,%d,?): %f s' % (msgType, timestamp, removeTime))
        except sqlite3.Error, e:
            self._dblock.release()
            if not self._stopped:
                self.exception(e) 
            
            
    def getStatus(self):
        '''
        Returns the status of the backlog database as tuple:
        (number of database entries, database file size in KB)
        
        @return: status of the backlog database as tuple (number of database entries, database file size)
        '''
        self._counterlock.acquire()
        ret = (self._dbNumberOfEntries, os.path.getsize(self._dbname)/1024.0, self._minStoreTime, self._maxStoreTime, self._meanStoreTime/self._storeCounter, self._minRemoveTime, self._maxRemoveTime, self._meanRemoveTime/self._removeCounter)
        
        self._minStoreTime = -1
        self._maxStoreTime = -1
        self._meanStoreTime = -1
        self._storeCounter = 1
        self._minRemoveTime = -1
        self._maxRemoveTime = -1
        self._meanRemoveTime = -1
        self._removeCounter = 1
        self._counterlock.release()
        
        return ret            
            
                
    def resend(self, sleep=False):
        '''
        Resend all messages which are in the backlog database to GSN.
        '''
        self._isBusy = True
        self._sleep = sleep
        self._resend.set()


    def run(self):
        self._logger.info('started')
        while not self._stopped:
            self._resend.wait()
            if self._stopped:
                break
            if self._sleep:
                self._sleepEvent.wait(SLEEP_BEFORE_RESEND_ON_RECONNECT)
            if self._stopped:
                break

            timestamp = 0

            self._logger.info('resend')

            while not self._stopped:
                try:
                    self._dblock.acquire()
                    self._cur.execute('SELECT * FROM backlogmsg WHERE timestamp > ? order by timestamp asc LIMIT 1', (timestamp,))
                    row = self._cur.fetchone()
                    self._dblock.release()
                except sqlite3.Error, e:
                    self._dblock.release()
                    self.exception(e)
                    break
                    
                if row is None:
                    self._logger.info('all packets are sent')
                    self._isBusy = False
                    break

                timestamp = row[0]
                msgType = row[1]
                message = row[2]
                # should be blocking until queue is free and ready to send
                self._logger.debug('rsnd...')
                if self._parent.gsnpeer.processResendMsg(msgType, timestamp, message):
                    self._logger.debug('rsnd (%d,%d,%d)' % (msgType, timestamp, len(message)))
                else:
                    self._logger.info('resend interrupted')
                    self._isBusy = False
                    break

            self._resend.clear()

        self._logger.info('died')


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
        self._sleepEvent.set()
        self._logger.info('stopped')
        
        
    def exception(self, e):
        self._parent.incrementExceptionCounter()
        self._logger.exception(e.__str__())
        