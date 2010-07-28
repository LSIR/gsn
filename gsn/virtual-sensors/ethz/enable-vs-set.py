#!/usr/bin/env python

import os
import sys
import time

abort = False

if len(sys.argv) != 3:
  print 'usage: enable-vs-set.py <target> <db>'
  sys.exit(-1)

filepath = sys.path[0] + '/conf/' + sys.argv[1] + '.list'

h2 = False
if sys.argv[2].lower() == "h2":
    h2 = True
elif sys.argv[2].lower() != "mysql":
    print 'usage: <db> can only be h2 or mysql'
    sys.exit(-1)

# check if first arguments points to valid file
if not os.path.exists(filepath):
  print sys.argv[1] + ' is no valid target (' + filepath + ' doesnt exists)'
  sys.exit(-2)

# change to virtual-sensors
os.chdir(sys.path[0] + '/..')

# delete symlinks in virtual-sensors
#for path in os.listdir('.'):
#  if (os.path.isfile(path) and path.endswith('.xml')):
#    if (os.path.islink(path)):
#      print 'delete ' + path + '...'
#      os.unlink(path)
#    else:
#      print path + ' is not a symlink, aborting...'
#      abort = True

if abort:
  sys.exit(-1)

# read list (given by argument)
fd = open(filepath, 'r')
for line in fd:
  if line.startswith('#'):
    continue
  link = line.rstrip()
  if h2:
      source = 'ethz/h2/' + link 
  else:
      source = 'ethz/mysql/' + link 
  if os.path.exists(source):
    if not os.path.exists(link):
      print link + ' -> ' + source
      os.symlink(source, link)
#      time.sleep(300)
  else:
    print 'cannot create symlink ' + link + '...'
fd.close()
