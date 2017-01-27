# -*- coding: UTF-8 -*-
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010 ETH Zurich Switzerland Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"


'''
    DPP message structure
    Byte         |0   |1   |2                   |3          |4   |5   |6 |7 |8|9|10|11|12|13|14|15|16........?|last 2 bytes|
    Header       |device_id|ext_msg (MSB) & type|payload_len|target_id|seqnr|   generation_time   |
    Payload                                                                                       |max 32bytes|
    CRC16                                                                                                     |   crc16    |
'''

'''
 The DPP Msg Types
'''

MSG_TYPE_INVALID        = 0
MSG_TYPE_TIMESYNC       = 1
MSG_TYPE_LOG            = 2

MSG_TYPE_COMM_CMD       = 10
MSG_TYPE_COMM_HEALTH    = 11

MSG_TYPE_APP_FIRMWARE   = 50

'''
 LWB commands
'''
