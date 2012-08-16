#!/usr/bin/python

import sys
import serial

# Connect to the GSM modem to get the basestation information
class Mc75iDriver():
    def __init__(self, device='/dev/acm/mc75i'):
        self._gsm_device_string = device

        # serial port timeout
        self._serialTimeout = 0.1

        print 'open serial port'

        try:
            self._gsm_device = serial.Serial(self._gsm_device_string, 115200, timeout=self._serialTimeout)
            self._gsm_device.close()
        except Exception as e:
            print 'serial access exception: ' + e.message
            exit()

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
            print 'read error: ' + e.message
            self._gsm_device.close()
            return ''

    def write(self, data):
        try:
            self._gsm_device.open()
            self._gsm_device.write(data)
            self._gsm_device.close()
        except serial.SerialException as e:
            print 'write error: ' + e.message
            self._gsm_device.close()

    def close(self):
        print 'close serial port'
        try:
            self._gsm_device.close()
        except serial.SerialException as e:
            print 'close error: ' + e.message
