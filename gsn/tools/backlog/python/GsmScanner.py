#!/usr/bin/python

import Mc75iDriver
import time

class GsmScanner():
    def __init__(self, device='/dev/acm/mc75i'):
        self.serial_device = Mc75iDriver.Mc75iDriver(device)

    def send_command(self, command):
        self.serial_device.write(command + '\r\n')

    def read_response(self):
        return self.serial_device.read()

    def stop(self):
        self.serial_device.close()

    def parse_response_smonc(self, response):
        if response.count('ERROR') > 0:
            print 'received error'
            return None, None
        elif response.count('OK') > 0:
            processed = response.partition('SMONC:')[2]
            processed = processed.partition('OK')[0]
            processed = processed.strip()
            values = processed.split(',')

            station_nr = len(values)/9

            results_string = ''
            results_list = []
            for i in range(station_nr):
                if results_string != '':
                    results_string += ','
                lac = int(values[i*9+2], 16)
                cid = int(values[i*9+3], 16)
                arfcn = int(values[i*9+5])
                rssi = int(values[i*9+6]) - 110
                results_string += str(lac) + ',' + str(cid) + ',' + str(arfcn) + ',' + str(rssi)
                results_list += [lac, cid, arfcn, rssi]
            return results_string, results_list
        else:
            print 'invalid response'
            return None, None

    def scan(self):
        response = ''
        self.send_command('AT^SMONC')
        response = self.read_response()

        return self.parse_response_smonc(response)
