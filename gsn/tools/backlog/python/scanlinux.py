#! /usr/bin/env python
# -*- coding: UTF-8 -*-#
__author__      = "Ben Buchli <bbuchli@ethz.ch"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Ben Buchli"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"
"""\
Scan for serial ports. Linux specific variant that also includes USB/Serial
adapters.

Part of pySerial (http://pyserial.sf.net)
(C) 2009 <cliechti@gmx.net>
"""

import serial
import glob

def scan():
    """scan for available ports. return a list of device names."""
    return glob.glob('/dev/acm/ublox*')

if __name__=='__main__':
    print "Found ports:"
    for name in scan():
        print name
