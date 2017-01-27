# -*- coding: UTF-8 -*-
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"


from threading import Event

import BackLogMessage
import DPPTypes
import time
import struct
import StringIO
import binascii
import math
from AbstractPlugin import AbstractPluginClass

DEFAULT_BACKLOG = True

FW_BLOCK_SIZE       = 32
FW_MAX_SIZE         = 65536

FW_PKT_TYPE_DATA    = 0     # firmware binary data with offset
FW_PKT_TYPE_CHECK   = 1     # request FW verification
FW_PKT_TYPE_READY   = 2     # response to a FW validation request
FW_PKT_TYPE_DATAREQ = 3     # request missing FW data packets
FW_PKT_TYPE_UPDATE  = 4     # initiate the FW update

class DPPFirmwarePluginClass(AbstractPluginClass):
    '''
    This plugin offers the functionality to upload a DPP application side firmware
    received from GSN to one or several DPP nodes.
    '''

    '''
    _work
    _stopped
    '''
    
    def __init__(self, parent, config):
        AbstractPluginClass.__init__(self, parent, config, DEFAULT_BACKLOG)
        
        # listen to firmware dpp packets
        self.registerDPPListener([DPPTypes.MSG_TYPE_APP_FIRMWARE])
        
        self._work = Event()
        self._stopped = False
        
        
    def isBusy(self):
        #TODO
        return False
        
        
    def needsWLAN(self):
        #TODO
        return False
    
    
    def dppMsgReceived(self, timestamp, dppMsg):
        type=dppMsg['payload'][0]
        self.info('dpp firmware packet received from DPP board with type %d' % (type,))
        
        #TODO: complete message
        if type == FW_PKT_TYPE_READY:
            return self.processMsg(timestamp, [dppMsg['generation_time'], dppMsg['device_id'], dppMsg['target_id'], dppMsg['seqnr'], None, type, 'Firmware ready to be distributed', None], self._priority, self._backlog)
        else:
            return self.processMsg(timestamp, [dppMsg['generation_time'], dppMsg['device_id'], dppMsg['target_id'], dppMsg['seqnr'], None, type, None, None], self._priority, self._backlog)
        
    
    
    def msgReceived(self, data):
        '''
        Message structure:
            fw_type, ...
        '''
        timestamp = int(time.time()*1000)
        self.info('dpp firmware command received from gsn with type %d' % (data[0],))
        
        #TODO: seqnr
        seqnr=0
        
        if data[0] == FW_PKT_TYPE_DATA:
            '''
                FW_PKT_TYPE_DATA, version, ihex_file
            '''
            version = data[1]
            try:
                bin = IHex.read(StringIO.StringIO(data[2])).binary()
            except Exception, e:
                self.error('could not parse ihex file: %s' % (e,))
                self.processMsg(timestamp, [timestamp*1000, self.getDeviceId(), None, seqnr, version, FW_PKT_TYPE_DATA, 'could not parse ihex file: %s' % (e,), None], self._priority, self._backlog)
                return
            
            bin_length = len(bin)
            if (bin_length > FW_MAX_SIZE):
                self.processMsg(timestamp, [timestamp*1000, self.getDeviceId(), None, seqnr, version, FW_PKT_TYPE_DATA, 'firmware is too big (FW_MAX_SIZE=%d)' % (FW_MAX_SIZE,), None], self._priority, self._backlog)
                self.error('firmware is too big (%d>%d)' % (bin_length,FW_MAX_SIZE))
                return
            
            # send firmware data to DPP
            for i in range(0, int(math.ceil(float(bin_length)/FW_BLOCK_SIZE))):
                if i*FW_BLOCK_SIZE+FW_BLOCK_SIZE>bin_length:
                    chunk = bin[i*FW_BLOCK_SIZE:] + "\0" * (FW_BLOCK_SIZE-len(bin[i*FW_BLOCK_SIZE:]))
                else:
                    chunk = bin[i*FW_BLOCK_SIZE:i*FW_BLOCK_SIZE+FW_BLOCK_SIZE]
                
                payload = struct.pack('<BHH', FW_PKT_TYPE_DATA, version, i) + chunk
                #TODO: seqnr , use Thread.wait
                while not self.sendDPPmsg(dict(device_id=self.getDeviceId(), ext_msg=False, type=DPPTypes.MSG_TYPE_APP_FIRMWARE, payload_len=len(payload), target_id=self.getDeviceId(), seqnr=seqnr, generation_time=timestamp, payload=payload)) and not self._stopped:
                    time.sleep(0.1)
                if self._stopped:
                    return
            #TODO: what if firmware was not accepted by DPP
                
            self._logger.info('firmware data sent to DPP host board (firmware size=%d)' % (bin_length,))
            self.processMsg(timestamp, [timestamp*1000, self.getDeviceId(), None, seqnr, version, FW_PKT_TYPE_DATA, 'firmware data sent to DPP host board (firmware size=%d)' % (bin_length,), bytearray(data[2])], self._priority, self._backlog)
            
            # request firmware validation
            crc32 = binascii.crc32(bin) & 0xFFFFFFFF
            payload = struct.pack('<BHHI', FW_PKT_TYPE_CHECK, version, bin_length, crc32)
            while not self.sendDPPmsg(dict(device_id=self.getDeviceId(), ext_msg=False, type=DPPTypes.MSG_TYPE_APP_FIRMWARE, payload_len=len(payload), target_id=self.getDeviceId(), seqnr=seqnr, generation_time=timestamp, payload=payload)) and not self._stopped:
                time.sleep(0.1)
                
            self._logger.info('firmware validation request sent to DPP host board (crc32=0x%08x)' % (crc32,))
            self.processMsg(timestamp, [timestamp*1000, self.getDeviceId(), None, seqnr, version, FW_PKT_TYPE_DATA, 'firmware validation request sent to DPP host board (crc32=0x%08x)' % (crc32,), None], self._priority, self._backlog)
        elif data[0] == FW_PKT_TYPE_UPDATE:
            '''
                FW_PKT_TYPE_UPDATE, version, target_id
            '''
            target_id = data[2]
            payload = struct.pack('<B', FW_PKT_TYPE_UPDATE)
            while not self.sendDPPmsg(dict(device_id=self.getDeviceId(), ext_msg=False, type=DPPTypes.MSG_TYPE_APP_FIRMWARE, payload_len=len(payload), target_id=target_id, seqnr=seqnr, generation_time=timestamp, payload=payload)) and not self._stopped:
                time.sleep(0.1)
                
            self._logger.info('firmware update command sent to target_id %d' % (target_id,))
            self.processMsg(timestamp, [timestamp*1000, self.getDeviceId(), target_id, seqnr, None, FW_PKT_TYPE_UPDATE, 'firmware update command sent', None], self._priority, self._backlog)
       
        
    def run(self):
        self.name = 'DPPFirmwarePlugin-Thread'
        self.info('started')
        
        while not self._stopped:
            self._work.wait()
            
        self.info('died')
    
    
    def stop(self):
        self._stopped = True
        self._work.set()
        self.info('stopped')


class IHex(object):
    @classmethod
    def read(cls, lines):
        ihex = cls()
    
        segbase = 0
        for line in lines:
            line = line.strip()
            if not line: continue
        
            t, a, d = ihex.parse_line(line)
            if t == 0x00:
                ihex.insert_data(segbase + a, d)
        
            elif t == 0x01:
                break # Should we check for garbage after this?
        
            elif t == 0x02:
                ihex.set_mode(16)
                segbase = struct.unpack(">H", d[0:2])[0] << 4
        
            elif t == 0x03:
                ihex.set_mode(16)
        
                cs, ip = struct.unpack(">2H", d[0:4])
                ihex.set_start((cs, ip))
        
            elif t == 0x04:
                ihex.set_mode(32)
                segbase = struct.unpack(">H", d[0:2])[0] << 16
        
            elif t == 0x05:
                ihex.set_mode(32)
                ihex.set_start(struct.unpack(">I", d[0:4])[0])
        
            else:
                raise ValueError("Invalid type byte")
    
        return ihex

    @classmethod
    def read_file(cls, fname):
        f = open(fname, "r")
        ihex = cls.read(f)
        f.close()
        return ihex

    def __init__(self):
        self.areas = {}
        self.start = None
        self.mode = 8
        self.row_bytes = 16

    def set_row_bytes(self, row_bytes):
        """Set output hex file row width (bytes represented per row)."""
        if row_bytes < 1 or row_bytes > 0xff:
            raise ValueError("Value out of range: (%r)" % row_bytes)
        self.row_bytes = row_bytes
  
    def extract_data(self, start=None, end=None):
        if start is None:
            start = 0
    
        if end is None:
            end = 0
            result = ""
      
            for addr, data in self.areas.iteritems():
                if addr >= start:
                    end = max(end, addr + len(data))
                    result = result[:start] + data[start-addr:end-addr] + result[end:]
      
            return result
    
        else:
            result = ""
      
            for addr, data in self.areas.iteritems():
                if addr >= start and addr < end:
                    result = result[:start] + data[start-addr:end-addr] + result[end:]
      
            return result
  
    def set_start(self, start=None):
        self.start = start

    def set_mode(self, mode):
        self.mode = mode

    def get_area(self, addr):
        for start, data in self.areas.iteritems():
            end = start + len(data)
            if addr >= start and addr <= end:
                return start
        return None

    def insert_data(self, istart, idata):
        iend = istart + len(idata)
    
        area = self.get_area(istart)
        if area is None:
            self.areas[istart] = idata
    
        else:
            data = self.areas[area]
            self.areas[area] = data[:istart-area] + idata + data[iend-area:]

    def calc_checksum(self, bytes):
        total = sum(map(ord, bytes))
        return (-total) & 0xFF

    def parse_line(self, rawline):
        if rawline[0] != ":":
            raise ValueError("Invalid line start character (%r)" % rawline[0])
    
        try:
            line = rawline[1:].decode("hex")
        except:
            raise ValueError("Invalid hex data")
    
        length, addr, type = struct.unpack(">BHB", line[:4])
    
        dataend = length + 4
        data = line[4:dataend]
    
        cs1 = ord(line[dataend])
        cs2 = self.calc_checksum(line[:dataend])
    
        if cs1 != cs2:
            raise ValueError("Checksums do not match")
    
        return (type, addr, data)

    def make_line(self, type, addr, data):
        line = struct.pack(">BHB", len(data), addr, type)
        line += data
        line += chr(self.calc_checksum(line))

        return ":" + line.encode("hex").upper() + "\r\n"

    def binary(self):
        output = ""
        for start, data in sorted(self.areas.iteritems()):
            if start < FW_MAX_SIZE:
                offset = len(output)
                if start>offset:
                    output += "\0" * (start-offset)
                output += data
            else:
                print "start=%d" % (start,)
        return output