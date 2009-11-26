'''
Created on Jul 15, 2009

@author: Tonio Gsell
@author: Mustafa Yuecel
'''

import struct
import os
import logging
from pyinotify import WatchManager, ThreadedNotifier, EventsCodes, ProcessEvent

import BackLogMessage
from AbstractPlugin import AbstractPluginClass

DEFAULT_BACKLOG = False

class BinaryPluginClass(AbstractPluginClass):
    '''
    Reads a file from disk and sends it to GSN.
    
    The following values have to specified in the plugin configuration file:
        file:        the location of the file
    '''

    '''
    data/instance attributes:
    _notifier
    _stopped
    '''
    
    def __init__(self, parent, options):
        AbstractPluginClass.__init__(self, parent, options, DEFAULT_BACKLOG)
        
        files = self.getOptionValues('file')

        if files is None:
            raise TypeError('no files specified to watch')

        wm = WatchManager()
        self._notifier = ThreadedNotifier(wm, BinaryChangedProcessing(self))
        for pathname in files:
            if not os.path.isdir(pathname):
                open(pathname, 'a').close()  # touch file
                wm.add_watch(pathname, EventsCodes.FLAG_COLLECTIONS['OP_FLAGS']['IN_CLOSE_WRITE'])
                self.info('enable close-after-write notification for file ' + pathname)
                
        self._stopped = False


    def getMsgType(self):
        return BackLogMessage.BINARY_MESSAGE_TYPE
       
        
    def run(self):
        self.info('started')
        try:
            self._notifier.start()
            self._notifier.join()
        except Exception, e:
            if not self._stopped:
                self.exception(e)

        self.info('died')
    
    
    def stop(self):
        self._stopped = True
        self._notifier.stop()
        self.info('stopped')


class BinaryChangedProcessing(ProcessEvent):

    def __init__(self, parent):
        self._logger = logging.getLogger(self.__class__.__name__)
        self._parent = parent

    def process_default(self, event):
        try:
            self._logger.debug(event.path + ' changed')
            fd = open(event.path, 'rb')
            file = fd.read() # we assume here that the whole file is readed...
            fd.close()

            #assert len(file) == os.path.getsize(event.path)
            
            if len(file) == 0:
                self._logger.debug('ignore empty file')
                return
            
            pathlen = len(event.path)
            if pathlen > 255:
                pathlen = 255 
            
            packet = struct.pack(str(pathlen) + 'sx', event.path)
            packet += file
            
            # tell PSBackLogMain to send the packet to GSN
            self._parent.processMsg(self._parent.getTimeStamp(), packet, self._parent._backlog)
        except Exception, e:
            self._logger.exception(e.__str__())
