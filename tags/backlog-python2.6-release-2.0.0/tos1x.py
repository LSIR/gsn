
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

# Copyright (c) 2008 Johns Hopkins University.
# All rights reserved.
#
# Permission to use, copy, modify, and distribute this software and its
# documentation for any purpose, without fee, and without written
# agreement is hereby granted, provided that the above copyright
# notice, the (updated) modification history and the author appear in
# all copies of this source code.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS `AS IS'
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS
# BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, LOSS OF USE, DATA,
# OR PROFITS) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
# CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
# ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
# THE POSSIBILITY OF SUCH DAMAGE.

# @author Razvan Musaloiu-E. <razvanm@cs.jhu.edu>
# @author David Purdy <david@radioretail.co.za>
# @author Tonio Gsell <tgsell@ee.ethz.ch>

"""
A library that implements the T1.x serial communication.

This library has two parts: one that deals with sending and receiving
packets using the serial format from T1.x and a second one that
tries to simplifies the work with arbitrary packets.
"""

import sys, struct, time, socket, operator, os
import logging
import array
import traceback
from threading import Thread, Event, Lock

try: 
    import serial
except ImportError, e: 
    print "Please install PySerial first."
    sys.exit(1)

__version__ = "$Id$"

__all__ = ['Serial', 'AM',
           'Packet', 'RawPacket',
           'AckFrame', 'DataFrame', 'NoAckDataFrame',
           'ActiveMessage']

HDLC_FLAG_BYTE = 0x7e
HDLC_CTLESC_BYTE = 0x7d

TOS_SERIAL_ACTIVE_MESSAGE_ID = 0
TOS_SERIAL_CC1000_ID = 1
TOS_SERIAL_802_15_4_ID = 2
TOS_SERIAL_UNKNOWN_ID = 255

SERIAL_PROTO_ACK = 64
SERIAL_PROTO_PACKET_ACK = 65
SERIAL_PROTO_PACKET_NOACK = 66
SERIAL_PROTO_PACKET_UNKNOWN = 255

def list2hex(v):
    return " ".join(["%02x" % p for p in v])

class Timeout(Exception):
    pass

def getSource(comm):
    source = comm.split('@')
    params = source[1].split(':')
#    debug = '--debug' in sys.argv
    if source[0] == 'serial':
        try:
            return Serial(params[0], int(params[1]), flush=True)
        except:
            print "ERROR: Unable to initialize a serial connection to", comm
            raise Exception
    elif source[0] == 'network':
        try:
            return SerialMIB600(params[0], int(params[1]))
        except:
            print "ERROR: Unable to initialize a network connection to", comm
            print "ERROR:", traceback.format_exc()
            raise Exception
    raise Exception

class Serial:
    def __init__(self, port, baudrate, flush=False, readTimeout=None, ackTimeout=0.02):
        self._logger = logging.getLogger(self.__class__.__name__)
        
        self.readTimeout = readTimeout
        self.ackTimeout = ackTimeout
        self._ts = None

        self._s = serial.Serial(port, int(baudrate), rtscts=0, timeout=0.5)
        self._s.flushInput()
        if flush:
            self._logger.info("Flushing the serial port")
            endtime = time.time() + 1
            while time.time() < endtime:
                self._s.read()
                sys.stdout.write(".")
            sys.stdout.write("\n")
        self._s.close()
        self._s = serial.Serial(port, baudrate, rtscts=0, timeout=readTimeout)

    def getByte(self):
        c = self._s.read()
        if not c:
            raise Timeout
        #print 'Serial:getByte: 0x%02x' % ord(c)
        return ord(c)

    def putBytes(self, data):
        #print "DEBUG: putBytes:", data
        self._s.write(array.array('B', data).tostring())

    def getTimeout(self):
        return self._s.timeout

    def setTimeout(self, timeout):
        self._s.timeout = timeout
        
    def close(self):
        self._s.close()

class SerialMIB600:
    def __init__(self, host, port=10002, readTimeout=None, ackTimeout=0.5):
        self._logger = logging.getLogger(self.__class__.__name__)
        self.readTimeout = readTimeout
        self.ackTimeout = ackTimeout
        self._ts = None
        self._s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self._s.connect((host, port))
        self._logger.info("Connected")

    def getByte(self):
        try:
            c = self._s.recv(1)
        except socket.timeout:
            c = ''
        if c == '':
            raise Timeout
        #print 'Serial:getByte: 0x%02x' % ord(c)
        return ord(c)

    def putBytes(self, data):
        #print "DEBUG: putBytes:", data
        for b in data:
            self._s.send(struct.pack('B', b))

    def getTimeout(self):
        return self._s.gettimeout()

    def setTimeout(self, timeout):
        self._s.settimeout(timeout)
        
    def close(self):
        self._s.close()

class HDLC:
    """
    An HDLC object offers a way to send and receive data on a byte
    source using a HDLC-like formating.
    """
    def __init__(self, source):
        self._logger = logging.getLogger(self.__class__.__name__)
        self._s = source
        self._hdlcStop = False

    # Returns the next incoming serial packet
    def read(self, timeout=None):
        """Wait for a packet and return it as a RawPacket."""

        # Developer notes:
        #
        # Packet data read is in this format:
        #     [HDLC_FLAG_BYTE][Escaped data][HDLC_FLAG_BYTE]
        #
        # [Escaped data] is encoded so that [HDLC_FLAG_BYTE] byte
        # values cannot occur within it. When [Escaped data] has been
        # unescaped, the last 2 bytes are a 16-bit CRC of the earlier
        # part of the packet (excluding the initial HDLC_FLAG_BYTE
        # byte)
        #
        # It's also possible that the serial device was half-way
        # through transmitting a packet when this function was called
        # (app was just started). So we also neeed to handle this
        # case:
        #
        #     [Incomplete escaped data][HDLC_FLAG_BYTE][HDLC_FLAG_BYTE][Escaped data][HDLC_FLAG_BYTE]
        #
        # In this case we skip over the first (incomplete) packet.
        #

        if self._s.getTimeout() != timeout and timeout != None:
            self._logger.debug("Set the timeout to %s, previous one was %s" % (timeout, self._s.getTimeout()))
            self._s.setTimeout(timeout)

        #    +--- FLAG -----+
        #    |              |  ___________
        #    v              | /           |
        #  >(1)-- !FLAG -->(2)<-- !FLAG --+
        #    |
        #   FLAG
        #    |  ___________
        #    v /           |
        #   (3)<-- FLAG ---+
        #    |
        #  !FLAG
        #    |  ___________
        #    v /           |
        #   (4)<-- !FLAG --+
        #    |
        #   FLAG
        #    |
        #    v
        #   (5)

        try:
            if self._hdlcStop:
                return None
            # Read bytes until we get to a HDLC_FLAG_BYTE value
            # (either the end of a packet, or the start of a new one)
            d = self._s.getByte()
            ts = time.time()
            if d != HDLC_FLAG_BYTE:
                self._logger.debug("Skipping byte %d" % d)
                while d != HDLC_FLAG_BYTE:
                    if self._hdlcStop:
                        return None
                    d = self._s.getByte()
                    self._logger.debug("Skipping byte %d" % d)
                    ts = time.time()

            # Store HDLC_FLAG_BYTE at the start of the retrieved packet
            # data:
            packet = [d]

            if self._hdlcStop:
                return None
            # Is the next byte also HDLC_FLAG_BYTE?
            d = self._s.getByte()
            while d == HDLC_FLAG_BYTE:
                if self._hdlcStop:
                    return None
                d = self._s.getByte()
                ts = time.time()

            # We are now on the 2nd byte of the packet. Add it to
            # our retrieved packet data:
            packet.append(d)

            # Read bytes from serial until we read another HDLC_FLAG_BYTE
            # value (end of the current packet):
            while d != HDLC_FLAG_BYTE:
                if self._hdlcStop:
                    return None
                d = self._s.getByte()
                packet.append(d)

            # Done reading a whole packet from serial
            self._logger.debug("SimpleSerial:_read: unescaped %s" % packet)

            # Decode the packet, and check CRC:
            packet = self._unescape(packet)

            crc = self._crc16(0, packet[1:-3])
            packet_crc = self._decode(packet[-3:-1])

            if crc != packet_crc:
                self._logger.warning("Warning: wrong CRC! %x != %x %s" % (crc, packet_crc, ["%2x" % i for i in packet]))
                return None
            if not self._s._ts:
                self._s._ts = ts
            self._logger.debug("Serial:_read: %.4f (%.4f) Recv: %s" % (ts, ts - self._s._ts, self._format(packet[1:-3])))
            self._ts = ts

            # Packet was successfully retrieved, so return it in a
            # RawPacket wrapper object (but leave out the HDLC_FLAG_BYTE
            # and CRC bytes)
            return RawPacket(ts, packet[1:-3])
        except Timeout:
            return None

    def write(self, payload, seqno):
        """
        Write a packet. If the payload argument is a list, it is
        assumed to be exactly the payload. Otherwise the payload is
        assume to be a Packet and the real payload is obtain by
        calling the .payload().
        """

        if isinstance(payload, Packet):
            payload = payload.payload()

        packet = DataFrame();
        # We need to always request for acks
        packet.protocol = SERIAL_PROTO_PACKET_ACK
        packet.seqno = seqno
        packet.data = payload
        packet = packet.payload()
        crc = self._crc16(0, packet)
        packet.append(crc & 0xff)
        packet.append((crc >> 8) & 0xff)
        packet = [HDLC_FLAG_BYTE] + self._escape(packet) + [HDLC_FLAG_BYTE]

        self._logger.debug("Serial: write %s" % packet)
        self._s.putBytes(packet)
        
    def sendAck(self, seqno):
        packet = AckFrame();
        # We need to always request for acks
        packet.protocol = SERIAL_PROTO_ACK
        packet.seqno = seqno
        packet = packet.payload()
        crc = self._crc16(0, packet)
        packet.append(crc & 0xff)
        packet.append((crc >> 8) & 0xff)
        packet = [HDLC_FLAG_BYTE] + self._escape(packet) + [HDLC_FLAG_BYTE]

        self._logger.debug("Serial: write ACK %s" % packet)
        self._s.putBytes(packet)

    def _format(self, payload):
        f = NoAckDataFrame(payload)
        if f.protocol == SERIAL_PROTO_ACK:
            rpacket = AckFrame(payload)
            return "Ack seqno: %d" % (rpacket.seqno)
        else:
            rpacket = ActiveMessage(f.data)
            return "D: %04x S: %04x L: %02x G: %02x T: %02x | %s" % \
                   (rpacket.destination, rpacket.source,
                    rpacket.length, rpacket.group, rpacket.type,
                    list2hex(rpacket.data))

    def _crc16(self, base_crc, frame_data):
        crc = base_crc
        for b in frame_data:
            crc = crc ^ (b << 8)
            for i in range(0, 8):
                if crc & 0x8000 == 0x8000:
                    crc = (crc << 1) ^ 0x1021
                else:
                    crc = crc << 1
                crc = crc & 0xffff
        return crc

    def _encode(self, val, dim):
        output = []
        for i in range(dim):
            output.append(val & 0xFF)
            val = val >> 8
        return output

    def _decode(self, v):
        r = long(0)
        for i in v[::-1]:
            r = (r << 8) + i
        return r

    def _unescape(self, packet):
        r = []
        esc = False
        for b in packet:
            if esc:
                r.append(b ^ 0x20)
                esc = False
            elif b == HDLC_CTLESC_BYTE:
                esc = True
            else:
                r.append(b)
        return r

    def _escape(self, packet):
        r = []
        for b in packet:
            if b == HDLC_FLAG_BYTE or b == HDLC_CTLESC_BYTE:
                r.append(HDLC_CTLESC_BYTE)
                r.append(b ^ 0x20)
            else:
                r.append(b)
        return r

    def stop(self):
        self._hdlcStop = True

class SimpleAM(Thread):
    def __init__(self, source, oobHook=None):
        self._logger = logging.getLogger(self.__class__.__name__)
        self._lastseqno = -1
        self._source = source
        self._hdlc = HDLC(source)
        self.seqno = 0
        self.oobHook = oobHook
        self._ackSeqno = None
        self._AckPacket = None
        self._AckLock = Lock()
        self._AckEvent = Event()
        self._DataPacket = None
        self._DataLock = Lock()
        self._DataEvent = Event()
        self._WriteLock = Lock()
        Thread.__init__(self)
        self._simpleAMStop = False

    def run(self):
        while not self._simpleAMStop:
            try:
                f = self._hdlc.read()
            except Exception, e:
                if not self._simpleAMStop:
                    self._logger.exception(e)
            if self._simpleAMStop:
                break
            p = AckFrame(f)
            if p.protocol == SERIAL_PROTO_ACK:
                self._AckLock.acquire()
                if self._AckPacket:
                    self._logger.warning("WARN: unhandled ACK frame.")
                self._AckPacket = AckFrame(f)
                self._AckEvent.set()
                self._AckLock.release()
            elif p.protocol == SERIAL_PROTO_PACKET_ACK:
                self._DataLock.acquire()
                if self._DataPacket:
                    self._logger.warning("WARN: unhandled PACKET_ACK frame.")
                self._DataPacket = DataFrame(f)
                self._DataEvent.set()
                self._DataLock.release()
            elif p.protocol == SERIAL_PROTO_PACKET_NOACK:
                data = NoAckDataFrame(f)
                if data.seqno != self._lastseqno:
                    self._DataLock.acquire()
                    if self._DataPacket:
                        self._logger.warning("WARN: unhandled PACKET_NOACK frame.")
                    self._DataPacket = data
                    self._lastseqno = data.seqno
                    self._DataEvent.set()
                    self._DataLock.release()
                else:
                    self._logger.warning('WARN: sequence number >%d< already received: drop packet.' % data.seqno)


    def readAck(self, timeout=None):
        p = None
        self._AckEvent.wait(timeout)
        if self._AckEvent.isSet():
            self._AckLock.acquire()
            p = self._AckPacket
            self._AckPacket = None
            self._AckEvent.clear()
            self._AckLock.release()
        return p

    def readData(self, timeout=None):
        p = None
        self._DataEvent.wait(timeout)
        if self._DataEvent.isSet() and not self._simpleAMStop:
            self._DataLock.acquire()
            p = self._DataPacket
            if p:
                if p.protocol ==  SERIAL_PROTO_PACKET_ACK:
                    self._ackSeqno = p.seqno
                else:
                    self._ackSeqno = None
            self._DataPacket = None
            self._DataEvent.clear()
            self._DataLock.release()
            p = ActiveMessage(p)
        return p

    def read(self, timeout=None):
        return self.readData(timeout)

    def sendAck(self):
        if self._ackSeqno:
            self._WriteLock.acquire()
            self._hdlc.sendAck(self._ackSeqno)
            self._WriteLock.release()

    def write(self, packet, amId, timeout=5, blocking=True, inc=1):
        self.seqno = (self.seqno + inc) % 256
        ack = None
        end = None
        self._WriteLock.acquire()
        self._hdlc.write(ActiveMessage(packet, amId=amId), seqno=self.seqno)
        self._WriteLock.release()
        if not blocking:
            return True
        ack = self.readAck(timeout)
        #print 'SimpleAM:write: got an ack:', ack, ack.seqno == self.seqno
        return (ack != None and ack.seqno == self.seqno)

    def setOobHook(self, oobHook):
        self.oobHook = oobHook

    def stop(self):
        self._simpleAMStop = True
        self._DataEvent.set()
        self._hdlc.stop()
        self._source.close()
        self._logger.info("SimpleAM - stopped")

def printfHook(packet):
#    if packet == None:
#        return
#    if packet.type == 100:
#        s = "".join([chr(i) for i in packet.data]).strip('\0')
#        lines = s.split('\n')
#        for line in lines:
#            if line: print "PRINTF:", line
#        #packet = None # No further processing for the printf packet
    return packet    

class AM(SimpleAM):
    def __init__(self, s=None, oobHook=None):
        self._logger = logging.getLogger(self.__class__.__name__)
        if s == None:
            try:
                s = getSource(sys.argv[1])
            except:
                try:
                    for (i, j) in zip(sys.argv[1::2], sys.argv[2::2]):
                        if i == '-comm':
                            s = getSource(j)
                    if s == None:
                        raise Exception
                except:
                    try:
                        s = getSource(os.environ['MOTECOM'])
                    except:
                        raise Exception("Please indicate a way to connect to the mote")
        if oobHook == None:
            oobHook = printfHook
        super(AM, self).__init__(s, oobHook)
        
    def sendAck(self):
        super(AM, self).sendAck()

    def read(self, timeout=None):
        return self.oobHook(super(AM, self).read(timeout))

    def write(self, packet, amId, timeout=None, blocking=True, maxretries = None):
        retries = 0
        if not timeout:
           timeout=self._source.ackTimeout
        r = super(AM, self).write(packet, amId, timeout, blocking)
        while not r and not self._simpleAMStop and (maxretries == None or retries < maxretries):
            retries += 1
            r = super(AM, self).write(packet, amId, timeout, blocking, inc=0)
        return r


# class SFClient:
#     def __init__(self, host, port, qsize=10):
#         self._in_queue = Queue(qsize)
#         self._s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
#         self._s.connect((host, port))
#         data = self._s.recv(2)
#         if data != 'U ':
#             print "Wrong handshake"
#         self._s.send("U ")
#         print "Connected"
#         thread.start_new_thread(self.run, ())

#     def run(self):
#         while True:
#             length = ord(self._s.recv(1))
#             data = self._s.recv(length)
#             data = [ord(c) for c in data][1:]
#             #print "Recv %d bytes" % (length), ActiveMessage(data)
#             if self._in_queue.full():
#                 print "Warning: Buffer overflow"
#                 self._in_queue.get()
#             p = RawPacket()
#             p.data = data
#             self._in_queue.put(p, block=False)

#     def read(self, timeout=0):
#         return self._in_queue.get()

#     def write(self, payload):
#         print "SFClient: write:", payload
#         if type(payload) != type([]):
#             # Assume this will be derived from Packet
#             payload = payload.payload()
#         payload = [0] + payload
#         self._s.send(chr(len(payload)))
#         self._s.send(''.join([chr(c) for c in payload]))
#         return True


################################################################################

class Packet:
    """
    The Packet class offers a handy way to build pack and unpack
    binary data based on a given pattern.
    """

    def _decode(self, v):
        r = long(0)
        for i in v:
            r = (r << 8) + i
        return r

    def _encode(self, val, dim):
        output = []
        for i in range(dim):
            output.append(int(val & 0xFF))
            val = val >> 8
        output.reverse()
        return output

    def _sign(self, val, dim):
        if val > (1 << (dim * 8 - 1)):
            return val - (1 << (dim * 8))
        return val

    def __init__(self, desc, packet = None):
        offset = 0
        boffset = 0
        sum = 0
        for i in range(len(desc)-1, -1, -1):
            (n, t, s) = desc[i]
            if s == None:
                if sum > 0:
                    desc[i] = (n, t, -sum)
                break
            sum += s
        self.__dict__['_schema'] = [(t, s) for (n, t, s) in desc]
        self.__dict__['_names'] = [n for (n, t, s) in desc]
        self.__dict__['_values'] = []
        if type(packet) == type([]):
            for (t, s) in self._schema:
                if t == 'int':
                    self._values.append(self._decode(packet[offset:offset + s]))
                    offset += s
                elif t == 'sint':
                    self._values.append(self._sign(self._decode(packet[offset:offset + s]), s))
                    offset += s
                elif t == 'bint':
                    doffset = 8 - (boffset + s)
                    self._values.append((packet[offset] >> doffset) & ((1<<s) - 1))
                    boffset += s
                    if boffset == 8:
                        offset += 1
                        boffset = 0
                elif t == 'string':
                    self._values.append(''.join([chr(i) for i in packet[offset:offset + s]]))
                    offset += s
                elif t == 'blob':
                    if s:
                        if s > 0:
                            self._values.append(packet[offset:offset + s])
                            offset += s
                        else:
                            self._values.append(packet[offset:s])
                            offset = len(packet) + s
                    else:
                        self._values.append(packet[offset:])
        elif type(packet) == type(()):
            for i in packet:
                self._values.append(i)
        else:
            for v in self._schema:
                self._values.append(None)

    def __repr__(self):
        return self._values.__repr__()

    def __str__(self):
        r = ""
        for i in range(len(self._names)):
            r += "%s: %s " % (self._names[i], self._values[i])
        for i in range(len(self._names), len(self._values)):
            r += "%s" % self._values[i]
        return r

    # Implement the struct behavior
    def __getattr__(self, name):
        if type(name) == type(0):
            return self._names[name]
        else:
            return self._values[self._names.index(name)]

    def __setattr__(self, name, value):
        if type(name) == type(0):
            self._values[name] = value
        else:
            self._values[self._names.index(name)] = value

    def __ne__(self, other):
        if other.__class__ == self.__class__:
            return self._values != other._values
        else:
            return True

    def __eq__(self, other):
        if other.__class__ == self.__class__:
            return self._values == other._values
        else:
            return False

    def __nonzero__(self):
        return True;

    # Implement the map behavior
    def __getitem__(self, key):
        return self.__getattr__(key)

    def __setitem__(self, key, value):
        self.__setattr__(key, value)

    def __len__(self):
        return len(self._values)

    def keys(self):
        return self._names

    def values(self):
        return self._values

    # Custom functions
    def names(self):
        return self._names

    def sizes(self):
        return self._schema

    def payload(self):
        r = []
        boffset = 0
        for i in range(len(self._schema)):
            (t, s) = self._schema[i]
            if t == 'int':
                r += self._encode(self._values[i], s)
                boffset = 0
            elif t == 'bint':
                doffset = 8 - (boffset + s)
                if boffset == 0:
                    r += [self._values[i] << doffset]
                else:
                    r[-1] |= self._values[i] << doffset
                boffset += s
                if boffset == 8:
                    boffset = 0
            elif self._values[i] != []:
                r += self._values[i]
        for i in self._values[len(self._schema):]:
            r += i
        return r


class RawPacket(Packet):
    def __init__(self, ts = None, data = None):
        Packet.__init__(self,
                        [('ts' ,  'int', 4),
                         ('data', 'blob', None)],
                        None)
        self.ts = ts;
        self.data = data

class AckFrame(Packet):
    def __init__(self, payload = None):
        if isinstance(payload, Packet):
            if isinstance(payload, RawPacket):
                payload = payload.data
            else:
                payload = payload.payload()
        Packet.__init__(self,
                        [('protocol', 'int', 1),
                         ('seqno',    'int', 1)],
                        payload)

class DataFrame(Packet):
    def __init__(self, payload = None):
        if isinstance(payload, Packet):
            if isinstance(payload, RawPacket):
                payload = payload.data
            else:
                payload = payload.payload()
        Packet.__init__(self,
                        [('protocol',  'int', 1),
                         ('seqno',     'int', 1),
                         ('data',      'blob', None)],
                        payload)

class NoAckDataFrame(Packet):
    def __init__(self, payload = None):
        if isinstance(payload, Packet):
            if isinstance(payload, RawPacket):
                payload = payload.data
            else:
                payload = payload.payload()
        Packet.__init__(self,
                        [('protocol',  'int', 1),
                         ('seqno',     'int', 1),
                         ('data',      'blob', None)],
                        payload)

class ActiveMessage(Packet):
    def __init__(self, packet = None, amId = 0x00, dest = 0xFFFF):
        payload = None
        if type(packet) == type([]):
            payload = packet
        elif isinstance(packet, NoAckDataFrame):
            payload = packet.data
            packet = None
        elif isinstance(packet, DataFrame):
            payload = packet.data
            packet = None
        Packet.__init__(self,
                        [('length',      'int', 1),
                         ('group',       'int', 1),
                         ('destination', 'int', 2),
                         ('type',        'int', 1),
                         ('source',      'int', 2),
                         ('data',        'blob', None)],
                        payload)
        if payload == None:
            self.destination = dest
            self.source = 0x0000
            self.group = 0x00
            self.type = amId
            self.data = []
            if isinstance(packet, Packet):
                self.data = packet.payload()
            self.length = len(self.data)
    
