__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision: 2453 $"
__date__        = "$Date: 2010-12-07 14:09:01 +0100 (Die, 07. Dez 2010) $"
__id__          = "$Id: SubprocessFake.py 2453 2010-12-07 13:09:01Z tgsell $"
__source__      = "$URL: https://gsn.svn.sourceforge.net/svnroot/gsn/branches/permasense/gsn/tools/backlog/python3/SubprocessFake.py $"

import commands
from threading import Thread, Event, Lock


class SubprocessFakeClass(Thread):
    '''
    This Module implements a similar interface as subprocess.py offers.
    It has been designed to bypass a bug with subprocess.Popen() which
    leads to a illegal instruction. Probably resulting from the os.fork()
    call in subprocess.Popen() in some special thread state...
    
    As soon as this bug has been fixed this module should be replaced by
    subprocess.Popen()!
    '''
    
    def __init__(self, command):
        Thread.__init__(self)
        self.finishEvent = Event()
        self._returncodelock = Lock()
        self._stdoutdatalock = Lock()
        self._cmd = command
        self.returncode = None
        self.stdoutdata = b''
        self.stderrdata = b''
        self.start()
        
        
    def run(self):
        status, output = commands.getstatusoutput(self._cmd)
        
        self._returncodelock.acquire()
        self.returncode = status
        self._returncodelock.release()
        
        self._stdoutdatalock.acquire()
        self.stdoutdata = output
        self._stdoutdatalock.release()
        
        self.finishEvent.set()
        
        
    def poll(self):
        self._returncodelock.acquire()
        ret = self.returncode
        self._returncodelock.release()
        return ret
    
    
    def kill(self):
        pass
    
    
    def wait(self):
        self.finishEvent.wait()
        self._returncodelock.acquire()
        ret = self.returncode
        self._returncodelock.release()
        return ret
        
        
    def communicate(self):
        self._stdoutdatalock.acquire()
        stdoutdata = self.stdoutdata
        self._stdoutdatalock.release()
        
        return (stdoutdata, self.stderrdata)