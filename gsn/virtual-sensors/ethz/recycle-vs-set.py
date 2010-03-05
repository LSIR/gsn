#!/usr/bin/env python

import os
import sys
import time
import string

abort = False

if len(sys.argv) != 2:
  print 'usage: enable-vs-set.py <filter>'
  sys.exit(-1)

filter = sys.argv[1]

# change to virtual-sensors
os.chdir(sys.path[0] + '/..')

# delete symlinks in virtual-sensors
for path in os.listdir('.'):
  if (os.path.isfile(path) and path.endswith('.xml')):
    if (os.path.islink(path)):
      if (string.find(path, filter) != -1):     
        raw_input('press enter to recycle ' + path + '...')
        os.unlink(path)
        time.sleep(5)
        os.symlink(os.path.dirname(path) + 'ethz/' + os.path.basename(path), path)
