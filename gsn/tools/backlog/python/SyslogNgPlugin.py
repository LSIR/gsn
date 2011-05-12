# -*- coding: UTF-8 -*-
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision: 2907 $"
__date__        = "$Date: 2011-03-22 15:34:20 +0100 (Die, 22. Mär 2011) $"
__id__          = "$Id: BackLogStatusPlugin.py 2907 2011-03-22 14:34:20Z tgsell $"
__source__      = "$URL: https://gsn.svn.sourceforge.net/svnroot/gsn/branches/permasense/gsn/tools/backlog/python/BackLogStatusPlugin.py $"

import os
import socket
from logging.handlers import SysLogHandler

import BackLogMessage
from AbstractPlugin import AbstractPluginClass

DEFAULT_BACKLOG = True

class SyslogNgPluginClass(AbstractPluginClass):
    '''
    This plugin offers the functionality to receive commands from the GSN Backlog wrapper.
    It also sends BackLogStatus messages.
    
    Any new status information coming from this program should be implemented here.
    '''

    '''
    _logSocket
    _clientSocket
    _stopped
    '''
    
    def __init__(self, parent, config):
        AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG)
        
        logSocketName = self.getOptionValue('log_socket')

        if logSocketName is None:
            raise TypeError('no log_socket specified')
        elif os.path.exists(logSocketName):
            self.info('removing existing socket %s' % (logSocketName,))
            os.remove(logSocketName)

        self._logSocket = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        self._logSocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self._logSocket.bind(logSocketName)
        self._logSocket.listen(1)
        
        self._clientSocket = None
        self._stopped = False
    
    
    def getMsgType(self):
        return BackLogMessage.SYSLOG_NG_MESSAGE_TYPE
        
        
    def isBusy(self):
        return False
        
        
    def needsWLAN(self):
        return False
       
        
    def run(self):
        self.name = 'SyslogNgPlugin-Thread'
        self.info('started')
        
        while not self._stopped:
            try:
                self._clientSocket, addr = self._logSocket.accept()
                self.debug('syslog-ng connected')
            except socket.error, e:
                self.exception(e)
                break
                
            
            logbuf = ''
            while not self._stopped:
                try:
                    rcv = self._clientSocket.recv(4096)
                except socket.error:
                    self.debug('syslog-ng disconnected')
                    self._clientSocket.close()
                    break
                
                if not rcv:
                    self.debug('syslog-ng disconnected')
                    self._clientSocket.close()
                    break
                
                logbuf += rcv
                if logbuf.endswith('\n'):
                    for line in logbuf.splitlines():
                        lnspl = line.split(None, 1)
                        self.processMsg(self.getTimeStamp(), [long(lnspl[0]), lnspl[1]])
                    logbuf = ''
                else:
                    spl = logbuf.splitlines()
                    if len(spl) > 1:
                        for index in range(len(spl)-1):
                            lnspl = spl[index].split(None, 1)
                            self.processMsg(self.getTimeStamp(), [long(lnspl[0]), lnspl[1]])
                    logbuf = spl[len(spl)-1]
            
        self.info('died')
    
    
    def stop(self):
        self._stopped = True
        if self._clientSocket:
            self._clientSocket.shutdown(socket.SHUT_RDWR)
            self._clientSocket.close()
        self._logSocket.shutdown(socket.SHUT_RDWR)
        self._logSocket.close()
        self.info('stopped')


class SyslogNgHandler(SysLogHandler):
    def emit(self, record):
        """
        Emit a record.
        
        The record is formatted, and then sent to the syslog server. If
        exception information is present, it is NOT sent to the server.
        """
        msg = self.format(record)
        """
        We need to convert record level to lowercase, maybe this will
        change in the future.
        """
        msg = self.log_format_string % (
            self.encodePriority(self.facility,
                                self.mapPriority(record.levelname)),
                                msg)
        
        try:
            if self.unixsocket:
                try:
                    self.socket.send(msg)
                except socket.error:
                    self._connect_unixsocket(self.address)
                    self.socket.send(msg)
            else:
                self.socket.sendto(msg, self.address)
        except (KeyboardInterrupt, SystemExit):
            raise
        except:
            self.handleError(record)