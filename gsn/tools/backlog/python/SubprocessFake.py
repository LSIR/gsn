__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

import os
import types
import logging
import uuid
from threading import Thread, Event, Lock
from pyinotify import WatchManager, ThreadedNotifier, EventsCodes, ProcessEvent

SUBPROCESS_FAKE_FOLDER_NEW = '/tmp/subprocessfake/new/'
SUBPROCESS_FAKE_FOLDER_FINISH = '/tmp/subprocessfake/finish/'


PIPE = -1
STDOUT = -2

class Popen(Thread):
    '''
    This Module implements a similar interface as subprocess.py offers.
    It has been designed to bypass a bug with subprocess.Popen() which
    leads to a illegal instruction. Probably resulting from the os.fork()
    call in subprocess.Popen() in some special thread state...
    
    As soon as this bug has been fixed this module should be replaced by
    subprocess.Popen()!
    '''
    
    def __init__(self, args, bufsize=0, executable=None,
                 stdin=None, stdout=None, stderr=None,
                 preexec_fn=None, close_fds=False, shell=False,
                 cwd=None, env=None, universal_newlines=False,
                 startupinfo=None, creationflags=0):
        
        Thread.__init__(self)
        self._logger = logging.getLogger(self.__class__.__name__)
        
        if startupinfo is not None:
            raise ValueError("startupinfo is only supported on Windows "
                             "platforms")
        if creationflags != 0:
            raise ValueError("creationflags is only supported on Windows "
                             "platforms")

        if stdin != None:
            raise ValueError("stdin is not supported by SubprocessFake")
        
        self.finishEvent = Event()
        self.pidEvent = Event()
        
        self._uniqueFileName = str(uuid.uuid4())

        wm = WatchManager()
        self._notifier = ThreadedNotifier(wm, FinishFolderEventHandler(self))
        # tell the watch manager which folders to watch for newly written files
        wm.add_watch(SUBPROCESS_FAKE_FOLDER_NEW, EventsCodes.FLAG_COLLECTIONS['OP_FLAGS']['IN_DELETE'])
        self._logger.info('enable IN_DELETE_SELF notification for ' + SUBPROCESS_FAKE_FOLDER_NEW+self._uniqueFileName)
        wm.add_watch(SUBPROCESS_FAKE_FOLDER_FINISH, EventsCodes.FLAG_COLLECTIONS['OP_FLAGS']['IN_CLOSE_WRITE'])
        self._logger.info('enable IN_CLOSE_WRITE notification for ' + SUBPROCESS_FAKE_FOLDER_FINISH)
        self._notifier.start()
        
        self.stdout = ''.encode()
        self.stderr = ''.encode()
        self.pid = None
        self.returncode = None
        
        if isinstance(args, types.StringTypes):
            args = args
        else:
            args = " ".join(args)

        if shell:
            args = "/bin/sh -c " + args

        f = open(SUBPROCESS_FAKE_FOLDER_NEW+self._uniqueFileName,'w')
        f.write(args)
        f.close()
        
        self.start()
        self.pidEvent.wait(5)
        if not self.pidEvent.isSet():
            if not self.finishEvent.isSet():
                self._notifier.stop()
            raise Exception('something went wrong in executing >' + args + '<')
        
        
    def run(self):
        self.finishEvent.wait()
        self._notifier.stop()
        self._logger.info('deleted')
        
        
    def poll(self):
        return self.returncode
    
    
    def kill(self):
        pass
    
    
    def wait(self):
        self.finishEvent.wait()
        return self.returncode
        
        
    def communicate(self, input=None):
        if input != None:
            raise ValueError("input is not supported by SubprocessFake")
        
        return (self.stdout, self.stderr)
    
    
    def _fileFinishEvent(self, file):
        if (file == SUBPROCESS_FAKE_FOLDER_NEW+self._uniqueFileName):
            self._logger.info('finish')
            
            self.returncode = 0
            
            f = open(SUBPROCESS_FAKE_FOLDER_FINISH+self._uniqueFileName, 'r')
            
            stdout = ''.join(f.readlines()[1:]).rstrip('\n')
            if stdout:
                self.stdout = stdout.encode()
            
            f.close()
            
            self.finishEvent.set()
            os.remove(SUBPROCESS_FAKE_FOLDER_FINISH+self._uniqueFileName)
            
            
    def _pidEvent(self, file):
        self._logger.info('pid event: ' + file)
        if self.pid == None and os.path.exists(file):
            if file == SUBPROCESS_FAKE_FOLDER_FINISH+self._uniqueFileName:
                f = open(file, 'r')
                pid = f.readline().strip('\n')
                f.close()
                if pid.isdigit():
                    self._logger.info('pid: ' + pid)
                    self.pid = int(pid)
                    self.pidEvent.set()

            


class FinishFolderEventHandler(ProcessEvent):
    
    '''
    data/instance attributes:
    _subprocessFake
    '''

    def __init__(self, parent):
        self._subprocessFake = parent
        
    def process_IN_CLOSE_WRITE(self, event):
        self._subprocessFake._pidEvent(event.pathname)
        
    def process_IN_DELETE(self, event):
        self._subprocessFake._fileFinishEvent(event.pathname)

    def process_default(self, event):
        self._subprocessFake._logger.info(event.pathname + ' changed (event=' + event.name + ')')
        