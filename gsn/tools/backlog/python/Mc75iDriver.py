#! /usr/bin/python
# -*- coding: UTF-8 -*-
__author__      = "David Hasenfratz <hasenfratz@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2011, ETH Zurich, Switzerland, David Hasenfratz"
__license__     = "GPL"
__version__     = "$Revision: 3717 $"
__date__        = "$Date: 2011-10-13 09:16:45 +0200 (Thu, 13 Oct 2011) $"
__id__          = "$Id: GPSPluginNAV.py 3717 2011-10-13 07:16:45Z dhasenfratz $"

import sys
import serial
import logging

# Connect to the GSM modem to get the basestation information
class Mc75iDriver():
    def __init__(self, device='/dev/acm/mc75i'):
        self._logger = logging.getLogger(self.__class__.__name__)
        self._logger.info('Init mc75i Driver...')
        self._gsm_device_string = device

        # serial port timeout
        self._serialTimeout = 0.1

        try:
            self._gsm_device = serial.Serial(self._gsm_device_string, 115200, timeout=self._serialTimeout)
            self._gsm_device.close()
            self._logger.debug('Init successful for device ' + self._gsm_device_string)
        except Exception as e:
            self._logger.error('Serial access exception ' + str(e))
            self._logger.error('Initialization failed for ' + self._gsm_device_string)
            return

    def read(self):
        try:
            response = ''
            self._gsm_device.open()
            while True:
                data = self._gsm_device.read(1)
                if data == '':
                    break
                else:
                    response += data
            self._gsm_device.close()
            return response
        except serial.SerialException as e:
            self._logger.error('Serial access exception ' + str(e))
            self._logger.error('GSM read failed')
            self._gsm_device.close()
            return ''

    def write(self, data):
        try:
            self._gsm_device.open()
            self._gsm_device.write(data)
            self._gsm_device.close()
        except serial.SerialException as e:
            self._logger.error('Serial access exception ' + str(e))
            self._logger.error('GSM write failed')
            self._gsm_device.close()

    def close(self):
        try:
            self._gsm_device.close()
        except serial.SerialException as e:
            self._logger.error('Serial access exception ' + str(e))
            self._logger.error('Closing failed for device ' + self._gsm_device_string)
