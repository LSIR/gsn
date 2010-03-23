'''
Created on Mar 23, 2010

@author: Tonio Gsell
'''

import BackLogMessage
from AbstractPlugin import AbstractPluginClass


class SchedulePluginClass(AbstractPluginClass):
    '''
    This plugin offers the functionality to 
    '''

    '''
    data/instance attributes:
    '''
    
    
    def getMsgType(self):
        return BackLogMessage.SCHEDULE_MESSAGE_TYPE