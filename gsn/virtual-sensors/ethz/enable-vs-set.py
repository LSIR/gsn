#!/usr/bin/env python

import os
import sys
import time

abort = False

if len(sys.argv) != 4 or (sys.argv[3].lower() != "create" and sys.argv[3].lower() != "check" and sys.argv[3].lower() != "delete"):
  print 'usage: enable-vs-set.py <target> <db> (create|check|delete)'
  print '  create    Creates missing symlinks'
  print '  check     Reports differences between existing links and configuration'
  print '  delete    Removes obsolete symlinks'
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

# read list file
linklist = []
fd = open(filepath, 'r')
for line in fd:
  if line.startswith('#'):
    continue
  if len(line) <= 1:
    continue
  link = line.rstrip()
  if h2:
    source = 'ethz/h2/' + link 
  else:
    source = 'ethz/mysql/' + link 
  linklist.append(source)
fd.close()

# delete symlinks in virtual-sensors?
if sys.argv[3].lower() == "delete":
  for path in os.listdir('.'):
    if (os.path.isfile(path) and path.endswith('.xml')):
      if (os.path.islink(path)):
        if not (os.readlink(path) in linklist):
          print 'delete ' + path + '...'
          os.unlink(path)
      else:
        print path + ' is not a symlink'
  exit(0)

# check symlinks in virtual-sensors
if sys.argv[3].lower() == "check":
  for source in linklist:
    link = os.path.basename(source)
    if os.path.exists(source):  
      if not os.path.exists(link):
        print "not linked: " + link + ' -> ' + source
  for path in os.listdir('.'):
    if (os.path.isfile(path) and path.endswith('.xml')):
      if (os.path.islink(path)):
        if not (os.readlink(path) in linklist):
          print 'not in list: ' + os.readlink(path)
      else:
        print path + ' is not a symlink'
  exit(0)

# create symlinks in virtual-sensors
if sys.argv[3].lower() == "create":
  for source in linklist:
    link = os.path.basename(source)
    if os.path.exists(source):
      if not os.path.exists(link):
        print link + ' -> ' + source
        os.symlink(source, link)
#      time.sleep(300)
    else:
      print 'cannot create symlink ' + link + '...'
  exit(0)