__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010 ETH Zurich Switzerland Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

# The AM Msg Types
AM_ALL = 'all'

AM_CONTROLCOMMAND = 0x21,
AM_BEACONCOMMAND = 0x22,

AM_DATANODEHEALTH = 0x80,
AM_DATANODEINFO = 0x82,
AM_DATAADCDIFF = 0x84,
AM_DATADIGITALDCX = 0x86,
AM_DATAADCMUX1 = 0x8A,
AM_DATAADCMUX2 = 0x8C,
AM_DATAWXT520WINDPTH = 0x92,
AM_DATAWXT520PREC = 0x94,
AM_DATAWXT520SUP = 0xA4,
AM_DATAWXT520IDENT = 0xA6,
AM_DATADECAGONMUX = 0xA0,
AM_DOZERBASESTATUS = 0x88,
AM_DOZERBASEDEBUG = 0x8E,
AM_COMMANDMSG = 0x50,
AM_DRIFT = 0x96, 
AM_RSSI = 0x9a,
AM_STATECOUNTER = 0x9e,
AM_DOZEROBJECTSTATUS = 0x90,