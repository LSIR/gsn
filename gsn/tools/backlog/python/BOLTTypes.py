# -*- coding: UTF-8 -*-
__author__      = "Tonio Gsell <tgsell@tik.ee.ethz.ch>"
__copyright__   = "Copyright 2010 ETH Zurich Switzerland Tonio Gsell"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"


'''
    BOLT message structure
    Byte         |0   |1   |2   |3   |4          |5 |6 |7|8|9|10|11|12|13|14|15...31|
    Msg queue    |size|
    Header            |device_id|type|payload_len|seqnr|   generation_time  |
    Payload                                                                 |
'''

'''
 The BOLT Msg Types
'''

MSG_TYPE_INVALID = 0
MSG_TYPE_TIMESYNC = 1
MSG_TYPE_DATA = 2


'''
 LWB commands
'''
# Dozer Beacon Structure
DOZER_BEACON_STRUCTURE = [('destination', 'int', 2), ('cmd', 'int', 2), ('repetitionCnt','int',1)]

# The Commands
