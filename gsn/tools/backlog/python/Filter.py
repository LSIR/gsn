# -*- coding: UTF-8 -*-
__author__      = "Ben Buchli <bbuchli@ethz.ch"
__copyright__   = "Copyright 2010, ETH Zurich, Switzerland, Ben Buchli"
__license__     = "GPL"
__version__     = "$Revision$"
__date__        = "$Date$"
__id__          = "$Id$"
__source__      = "$URL$"

class Filter():
    
    def __init__(self,taps):
        #history size (nbr of taps)
        self._taps = taps;
        #history table
        self._ht = [0 for i in range(0,self._taps)]
        self._divisor = 0
        
    '''
    public filter function
    '''
    def filter(self, val):
        if (val == None):
            return
        self._enq(val)
        return self._filter()
    
    '''
    Returns history table
    '''
    def getHistoryTable(self):
        return self._ht
    
    '''
    Returns filter size
    '''
    def setHistorySize(self,size):
        self._taps = size
        self._ht = [self._ht[i] for i in range(0,self._taps-1)]

    '''
    enqueues the given value
    '''
    def _enq(self,val):
        #print("Before " + str(self._ht))
        for j in range(0,self._taps):
            self._ht[self._taps-j-1] = self._ht[self._taps-j-2]
        self._ht[0] = val
        if (self._divisor < self._taps):
            self._divisor += 1
        #print("After " + str(self._ht))
        
    '''
    private filter
    '''
    def _filter(self):
        s = 0
        for i in range(0, self._taps):
            s += self._ht[i]
#        print("Filtered " + str(s/self._divisor))
        return s/self._divisor
        
        