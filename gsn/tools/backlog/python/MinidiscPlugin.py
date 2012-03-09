#! /usr/bin/python
# -*- coding: UTF-8 -*-
__author__      = "David Hasenfratz <hasenfratz@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2011, ETH Zurich, Switzerland, David Hasenfratz"
__license__     = "GPL"
__version__     = "$Revision: 3717 $"
__date__        = "$Date: 2011-10-13 09:16:45 +0200 (Thu, 13 Oct 2011) $"
__id__          = "$Id: GPSPluginNAV.py 3717 2011-10-13 07:16:45Z dhasenfratz $"

'''
backlog imports
'''
import BackLogMessage
from AbstractPlugin import AbstractPluginClass
import time
from threading import Event

import GPSDriverNAV
import MinidiscDriver
'''
defines
'''

DEFAULT_BACKLOG = True

class MinidiscPluginClass(AbstractPluginClass):

    def __init__(self, parent, config):

        AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG)
        self._sleeper = Event()
        self._stopped = False
        self._pollInterval = 5.0

        self.info('Init MinidiscPlugin...')

        self._gpsDeviceStr = self.getOptionValue('gps_device')
        self.gps = GPSDriverNAV.GPSDriverNAV([self._gpsDeviceStr])

        self._mdDeviceStr = self.getOptionValue('md_device')
        self.md = MinidiscDriver.MinidiscDriver([self._mdDeviceStr])

        self.info("Done init")

    def getMsgType(self):
        return BackLogMessage.MINIDISC_MESSAGE_TYPE

    def isBusy(self):
        return False

    def run(self):
        self.name = 'MinidiscPlugin-Thread'
        self.info('MinidiscPlugin started...')
        t = time.time()

        while not self._stopped:
            self._sleeper.wait(self._pollInterval - (time.time() - t))
            if self._sleeper.isSet():
                continue
            t = time.time()
            self.action()
        self.info('died')

    def action(self):

        # Read message
        mdMsg = ''
        mdMsg = self.md._read()
        self.info('Minidisc reading done')

        if mdMsg != '' and mdMsg is not None and mdMsg.rfind('>') != -1:

          mdMsg = mdMsg[mdMsg.rfind('>')+1:]
          self.info(mdMsg)

          # Read GPS message
          gpsMsg = self.gps._read()
          
          if gpsMsg != 0 and gpsMsg != '' and gpsMsg is not None:
          
            # Parse message
            dataPackage = self._parseGPSMsg(gpsMsg)
            
            if dataPackage == '':
                self.warn('Could not parse GPS reading')
                return
            
            self.info('GPS reading done')

            dataPackage += [mdMsg]
            self.info('Send complete msg')
            self.processMsg(self.getTimeStamp(), dataPackage)
          else:
              self.warning('No GPS data')

        else:
          self.warning('No Minidisc data')


    def remoteAction(self, parameters):
        return

    def stop(self):
        self._stopped = True
        self.info('stopped')

    def _parseGPSMsg(self, msg):
        if msg != 0:
            dataPackage = []
            for i in range(1, len(msg)):
                dataPackage += [msg[i]]

            self.info(dataPackage)
            return dataPackage

        else:
            self.info("WARNING: GPS MSG packet was empty!")

