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
        wm.add_watch(SUBPROCESS_FAKE_FOLDER_FINISH, EventsCodes.FLAG_COLLECTIONS['OP_FLAGS']['IN_CLOSE_WRITE'])
        self._notifier.start()
        
        self.stdLock = Lock()
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
            self.finishEvent.set()
            if os.path.exists(SUBPROCESS_FAKE_FOLDER_NEW+self._uniqueFileName):
                os.remove(SUBPROCESS_FAKE_FOLDER_NEW+self._uniqueFileName)
            raise Exception('SubprocessFake could not execute >' + args + '< (is subprocessfaked.sh running?)')
        
        
    def run(self):
        self.finishEvent.wait()
        self._notifier.stop()
        
        
    def poll(self):
        self.stdLock.acquire()
        ret = self.returncode
        self.stdLock.release()
        return ret
    
    
    def kill(self):
        pass
    
    
    def wait(self):
        self.finishEvent.wait()
        return self.returncode
        
        
    def communicate(self, input=None):
        if input != None:
            raise ValueError("input is not supported by SubprocessFake")
        
        self.stdLock.acquire()
        stdout = self.stdout
        stderr = self.stderr
        self.stdLock.release()
        
        return (stdout, stderr)
    
    
    def _fileFinishEvent(self, file):
        if (file == SUBPROCESS_FAKE_FOLDER_NEW+self._uniqueFileName):
            
            stdout = ''
            if os.path.exists(SUBPROCESS_FAKE_FOLDER_FINISH+self._uniqueFileName+'.out'):
                fout = open(SUBPROCESS_FAKE_FOLDER_FINISH+self._uniqueFileName+'.out', 'r')
                stdout = ''.join(fout.readlines()).rstrip('\n')
                fout.close()
            
            stderr = ''
            if os.path.exists(SUBPROCESS_FAKE_FOLDER_FINISH+self._uniqueFileName+'.err'):
                ferr = open(SUBPROCESS_FAKE_FOLDER_FINISH+self._uniqueFileName+'.err', 'r')
                stderr = ''.join(ferr.readlines()).rstrip('\n')
                ferr.close()
            
            ret = None
            if os.path.exists(SUBPROCESS_FAKE_FOLDER_FINISH+self._uniqueFileName+'.ret'):
                fret = open(SUBPROCESS_FAKE_FOLDER_FINISH+self._uniqueFileName+'.ret', 'r')
                ret = fret.readline().strip('\n')
                fret.close()
            
            self.stdLock.acquire()
            if stdout:
                self.stdout = stdout.encode()
            if stderr:
                self.stderr = stderr.encode()
            if ret != None:
                self.returncode = int(ret)
            self.stdLock.release()
            
            self.finishEvent.set()
            if os.path.exists(SUBPROCESS_FAKE_FOLDER_FINISH+self._uniqueFileName+'.pid'):
                os.remove(SUBPROCESS_FAKE_FOLDER_FINISH+self._uniqueFileName+'.pid')
            if os.path.exists(SUBPROCESS_FAKE_FOLDER_FINISH+self._uniqueFileName+'.ret'):
                os.remove(SUBPROCESS_FAKE_FOLDER_FINISH+self._uniqueFileName+'.ret')
            if os.path.exists(SUBPROCESS_FAKE_FOLDER_FINISH+self._uniqueFileName+'.out'):
                os.remove(SUBPROCESS_FAKE_FOLDER_FINISH+self._uniqueFileName+'.out')
            if os.path.exists(SUBPROCESS_FAKE_FOLDER_FINISH+self._uniqueFileName+'.err'):
                os.remove(SUBPROCESS_FAKE_FOLDER_FINISH+self._uniqueFileName+'.err')
            
            
    def _pidEvent(self, file):
        if self.pid == None and os.path.exists(file):
            if file == SUBPROCESS_FAKE_FOLDER_FINISH+self._uniqueFileName+'.pid':
                f = open(file, 'r')
                pid = f.readline().strip('\n')
                f.close()
                if pid.isdigit():
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
        