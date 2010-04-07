#!/usr/bin/env python

import os
import sys
import time
import string

if len(sys.argv) < 2:
  print 'usage: enable-vs-set.py <filter>'
  sys.exit(-1)

filter = sys.argv[1:]

# change to virtual-sensors
os.chdir(sys.path[0] + '/..')

# delete symlinks in virtual-sensors
for path in os.listdir('.'):
  if (os.path.isfile(path) and path.endswith('.xml')):
    if (os.path.islink(path)):
      accepted = True
      for f in filter:
        if (string.find(path, f) == -1):
          accepted = False
          break     
      if (accepted):
        #raw_input('press enter to recycle ' + path + '...')
        print path
        os.unlink(path)
        time.sleep(5)
        os.symlink(os.path.dirname(path) + 'ethz/' + os.path.basename(path), path)
        time.sleep(300)
