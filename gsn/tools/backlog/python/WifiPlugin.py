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
from threading import Timer

from ScheduleHandler import SUBPROCESS_BUG_BYPASS
if SUBPROCESS_BUG_BYPASS:
    import SubprocessFake
    subprocess = SubprocessFake
else:
    import subprocess

import GPSDriverNAV
import WifiScanner
'''
defines
'''

DEFAULT_BACKLOG = True

class WifiPluginClass(AbstractPluginClass):

    def __init__(self, parent, config):

        AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG)
        self._sleeper = Event()
        self._stopped = False
        self._pollInterval = float(self.getOptionValue('poll_interval'))

        self.info('Init WifiPlugin...')

        self._gpsDeviceStr = self.getOptionValue('gps_device')
        self.gps = GPSDriverNAV.GPSDriverNAV([self._gpsDeviceStr])

        self._wifiDeviceStr = self.getOptionValue('wifi_device')

        self._mikrotikPort = self.getOptionValue('mikrotik_port')
        self._mikrotikIP = self.getOptionValue('mikrotik_ip')
        
        self.wifi = WifiScanner.WifiScanner([self._wifiDeviceStr, self._mikrotikIP])
        
        self.reboot_counter = 0

        self.info("Done init")

    def isBusy(self):
        return False

    def run(self):
        try:
          self.name = 'Wifiplugin-Thread'
          self.debug('Wifiplugin started...')
          t = time.time()

          while not self._stopped:
              self._sleeper.wait(self._pollInterval - (time.time() - t))
              if self._sleeper.isSet():
                  continue
              t = time.time()
              self.action()
          self.info('died')
        except Exception as e:
          self.error( "Exception: " + str(e))
          self.error("Could not execute run")
          return

    # Ugly workaround. Call a seperate python process with the fixed subprocess module.
    def scan_workaround(self):
        # Kill the subprocess when it hangs.
        def kill_scan():
            try:
                self.error('killed scan after 100 seconds')
                p.terminate()
            except OSError:
                self.error('no process to kill')

        p = subprocess.Popen(['python', '/media/card/backlog/python2.6/WifiScannerWorkaround.py', self._wifiDeviceStr, self._mikrotikIP],
                stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        # Make sure the new python process gets killed after 100 seconds in case it hangs.
        # Should never happen, since the scanner has a timeout itself.
        t = Timer(100, kill_scan)
        t.start()
        return_code = p.wait()
        t.cancel()
        (stdout, stderr) = p.communicate()

        if return_code != 0:
            self.error('Wifi scan failed with return code ' + str(return_code))
            if stderr != None and stderr != '':
                self.error('stderr: ' + stderr)
            if stdout != None and stdout != '':
                self.error('stdout: ' + stdout)
            return ''
        else:
            if stdout != None and stdout != '':
                self.debug('stdout: ' + stdout)
        return stdout

    def action(self):
        # Power cycle the mikrotik board when it doesn't respond.
        if self.reboot_counter > 3:
            self.warning('reboot mikrotik')
            self.reboot_counter = 0
            p = subprocess.Popen(['bb_extpwr.py', '--ext', self._mikrotikPort, '--state',  'off'],
                    stdout=subprocess.PIPE,
                    stderr=subprocess.PIPE)
            p.wait()
            p = subprocess.Popen(['bb_extpwr.py', '--ext', self._mikrotikPort, '--state',  'on'],
                    stdout=subprocess.PIPE,
                    stderr=subprocess.PIPE)
            p.wait()
            return

        # Read message
        wifiMsg = ''
        self.debug('start Wifi scan')
        if SUBPROCESS_BUG_BYPASS:
            wifiMsg = self.scan_workaround()
        else:
            # This crashes after a while with a segfault.
            [wifiMsg, wifiMsgList] = self.wifi.scan()

        self.debug('Wifi scan completed')

        if wifiMsg != '' and wifiMsg != None:
          self.reboot_counter = 0
          self.debug(wifiMsg)

          # Read GPS message
          gpsMsg = self.gps._read()
          
          if gpsMsg != 0 and gpsMsg != '' and gpsMsg is not None:
          
            # Parse message
            dataPackage = self._parseGPSMsg(gpsMsg)
            
            if dataPackage == '':
                self.warn('Could not parse GPS reading')
                return
            
            self.debug('GPS reading done')

            dataPackage += [wifiMsg]
            self.debug('Send complete msg')
            self.processMsg(self.getTimeStamp(), dataPackage)
          else:
              self.warning('No GPS data')

        else:
          self.reboot_counter += 1
          self.warning('No Wifi data')


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

            self.debug(dataPackage)
            return dataPackage

        else:
            self.debug("WARNING: GPS MSG packet was empty!")

