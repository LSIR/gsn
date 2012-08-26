#! /usr/bin/python
# -*- coding: UTF-8 -*-
__author__      = "David Hasenfratz <hasenfratz@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2011, ETH Zurich, Switzerland, David Hasenfratz"
__license__     = "GPL"
__version__     = "$Revision: 3717 $"
__date__        = "$Date: 2011-10-13 09:16:45 +0200 (Thu, 13 Oct 2011) $"
__id__          = "$Id: GPSPluginNAV.py 3717 2011-10-13 07:16:45Z dhasenfratz $"

import os
import sys
import StringIO
import time
import math
import re
import socket
#import logging

pexpect_path = os.path.abspath('/media/card/backlog/python2.6/pexpect-2.3')
sys.path.append(pexpect_path)

import pexpect

class WifiScanner():
    def __init__(self, config):
        #self._logger = logging.getLogger(self.__class__.__name__)
        #self._logger.info('Init Wifi Scanner...')

        if config[0] != None:
            self._wifiDeviceStr = config[0]
        else:
            self._wifiDeviceStr = 'wlan1'
            
        if config[1] != None:
            self.mikrotik_address = config[1]
        else:
            self.mikrotik_address = '192.168.20.1'

        self.DURATION_2GHZ = 5
        self.DURATION_5GHZ = 10
        self.BUCKETS_2GHZ = 10
        self.BUCKETS_5GHZ = 10
        self.BAND_2GHZ = '2.4ghz'
        self.BAND_5GHZ = '5ghz'
        self.SCAN_TIMEOUT = 30
        # Regular expression to strip ANSI escape sequences from the scan.
        self.strip_ANSI_escape_sequences_sub = re.compile(r"""
            \x1b     # literal ESC
            \[       # literal [
            [;\d]*   # zero or more digits or semicolons
            [A-Za-z] # a letter
            """, re.VERBOSE).sub

    def strip_ANSI_escape_sequences(self, s):
        return self.strip_ANSI_escape_sequences_sub("", s)

    # Convert the dBm reading to milliwatts for averaging.
    def power_to_milli_watt(self, power_dbm):
        return 10**(float(power_dbm)/10)

    # Convert the milliwatts back to dBm.
    def power_to_dbm(self, power_milli_watt):
        return 10*math.log(float(power_milli_watt), 10)

    def parse_data(self, data):
        #self._logger.debug('strip escape sequences')
        data = self.strip_ANSI_escape_sequences(data)
        # Log the whole output to a file.
        #log_file = open('wifi.log', 'a')
        #log_file.write(data)
        #log_file.close()

        averaged_list = []
        averaged_string = ''
        if data == '':
            #self._logger.debug('no Wifi data')
            return averaged_string, averaged_list
        # The two scans are separeted by the string 'scan_delimeter'
        for band, scan in enumerate(data.split('scan_delimeter')):
            results = []
            scan_count_complete = 0
            scan_count_all = 0

            # The scan is repeated several times, so average the readings.
            # The structure of a single scan is as follows:
            # 
            # FREQ DBM  GRAPH
            # 2200 -89  :::::::::::::..
            # 2236 -87  ::::::::::::::.
            # 2273 -87  :::::::::::::::..
            # 2310 -86  ::::::::::::::::.
            # 2346 -91  :::::::::::.
            # 2383 -79  :::::::::::::::::::::::::.
            # 2420 -66  ::::::::::::::::::::::::::::::::::::::::..
            # 2456 -70  :::::::::::::::::::::::::::::::::::.........................
            # 2493 -71  ::::::::::::::::::::::::::::::::::....
            # 2530 -97  ::::..
            # -- [Q quit|D dump|C-z pause]
            # 
            # First we split it at 'DBM', then strip everything before 'GRAPH' and after '--'
            for sub_scan in scan.split('DBM'):
                sub_scan = sub_scan.partition('GRAPH')[2]
                sub_scan = sub_scan.partition('--')[0]
                sub_scan = sub_scan.strip()
                scan_split = sub_scan.split('\n')
                
                # Ignore it when the lines don't match the expected number of buckets.
                if band == 0 and len(scan_split) < self.BUCKETS_2GHZ:
                    continue
                elif band == 1 and len(scan_split) < self.BUCKETS_5GHZ:
                    continue
                else:
                    scan_count_all += 1
                    # The first few scans can be empty, so ignore anything with fewer than 2 elements.
                    if len(scan_split[0].split())<2:
                        continue
                    else:
                        scan_count_complete += 1
                        for index, line in enumerate(scan_split):
                            elements = line.split()
                            if len(elements) >= 2:
                                if len(results) <= index:
                                    results.append([int(elements[0]), self.power_to_milli_watt(elements[1])])
                                else:
                                    results[index][1] += self.power_to_milli_watt(elements[1])

            for line in results:
                if averaged_string != '':
                    averaged_string += ','
                averaged_string += str(line[0]) + ','
                averaged_list.append(line[0])
                average = self.power_to_dbm(line[1]/scan_count_complete)
                averaged_string += str(average)
                averaged_list.append(average)

        #self._logger.debug('finished parsing')
        return averaged_string, averaged_list

    def get_raw_data(self):
        # Create a file in memory to write the ssh output to.
        log_mem_file = StringIO.StringIO()
        # Execute the scans via ssh.
        # 2.4 GHz and 5 GHz scans have to be executed seperately.
        # To distinguish between the scans while parsing, 'scan_delimeter' is written in between.
        cmd = 'ssh -t ' + self.mikrotik_address + \
                ' "/interface wireless spectral-scan buckets=' + str(self.BUCKETS_2GHZ) + \
                ' duration=' + str(self.DURATION_2GHZ) + \
                ' range=' + self.BAND_2GHZ + \
                ' ' + self._wifiDeviceStr + ';' + \
                ' put "scan_delimeter";' + \
                ' /interface wireless spectral-scan buckets=' + str(self.BUCKETS_5GHZ) + \
                ' duration=' + str(self.DURATION_5GHZ) + \
                ' range=' + self.BAND_5GHZ + \
                ' ' + self._wifiDeviceStr + ';' + \
                ' /quit;"'

        try:
            #self._logger.debug('spawn ssh')
            p = pexpect.spawn(cmd, timeout=5)
            #self._logger.debug('sleep')
            time.sleep(1)
            # Increase the terminal size. Default is 24 (maybe), with this up to 80 scan buckets are possible.
            #self._logger.debug('set winsize')
            p.setwinsize(500,80)
            p.logfile_read = log_mem_file
            # The timeout should be large enough for both scan durations.
            #self._logger.debug('wait for interrupted')
            p.expect('interrupted', timeout=self.SCAN_TIMEOUT)
            p.close()

            log = log_mem_file.getvalue()
            log_mem_file.close()
            #self._logger.debug('ssh done')
        except pexpect.ExceptionPexpect as e:
            log = log_mem_file.getvalue()
            log_mem_file.close()
            p.close()
            # Strip the output, or the debug output gets garbled.
            log = self.strip_ANSI_escape_sequences(log)
            #self._logger.warning('Wifi scan failed')
            #self._logger.debug('pexpect log: ' + log)
            return ''

        return log

    def scan(self):
        data = self.get_raw_data()
        results_string, results_list = self.parse_data(data)
        return results_string, results_list
