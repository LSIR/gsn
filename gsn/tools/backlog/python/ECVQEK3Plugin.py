
__author__      = "David Hasenfratz <hasenfratz@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2011, ETH Zurich, Switzerland"
__license__     = "GPL"
__version__     = "$Revision:  $"
__date__        = "$Date: 2011-02-14 10:58:41 +0100 (Mon, 14 Feb 2011) $"
__id__          = "$Id: ECVQEK3Plugin.py 2011-02-14 10:58:41Z hdavid $"
__source__      = "$URL: http://gsn.svn.sourceforge.net/svnroot/gsn/branches/permasense/gsn/tools/backlog/python/ECVQEK3Plugin.py $"

import BackLogMessage
from AbstractPlugin import AbstractPluginClass

import ECVQEK3Driver

DEFAULT_BACKLOG = True

# Reads messages from the ECVQEK3Driver and sends them to GSN

class ECVQEK3PluginClass(AbstractPluginClass):
    
    def __init__(self, parent, config):
	    
    	AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG)
	self._timer = None
	self._stopped = False
	self._interval = None

        self.info('Init ECVQEK3Plugin...')
    
    	self._deviceStr = self.getOptionValue('ecvqek3_device')
    	self.ecvqek3 = ECVQEK3Driver.ECVQEK3Driver([self._deviceStr])
    
    	self.info("Done init")

    def getMsgType(self):
	return BackLogMessage.ECVQEK3_MESSAGE_TYPE

    def isBusy(self):
	return False

    def needsWLAN(self):
	return False

    def run(self):
	
        self.info('ECVQEK3Plugin running...')

    def action(self, parameters):

	self.info('ECVQEK3Plugin started...')
	
        # Read message
        msg = ''
	while len(msg) != 69:
	    msg = self.ecvqek3._read()
	self.info(msg)
        
	self.processMsg(self.getTimeStamp(), [msg])

	self.info('ECVQEK3 reading done')

    def stop(self):
	self._stopped = True
	self.info('stopped')
