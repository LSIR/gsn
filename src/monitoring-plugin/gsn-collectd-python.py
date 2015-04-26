#!/usr/bin/python
#
#  to enable add the following to your collectd config
#  and copy this file to /opt/collectd/lib/collectd/plugins/python
#
#  <LoadPlugin python>
#    Globals true
#  </LoadPlugin>
#  <Plugin python>
#    ModulePath "/opt/collectd/lib/collectd/plugins/python"
#    Import "gsn-collectd-python"
#    <Module redis_info>
#       Host "localhost"
#    </Module>
#  </Plugin>
#
#########################################################



import collectd
import urllib, sys
from collections import defaultdict


GSN_URL = "http://127.0.0.1:22001/stat"

def configure_callback(conf):
    """Receive configuration block"""
    global GSN_URL
    for node in conf.children:
        if node.key == 'Url':
            GSN_URL = node.values[0]
        else:
            collectd.warning('gsn plugin: Unknown config key: %s.'% node.key)

def read_callback():
    """Read data"""
    global GSN_URL
    f = urllib.urlopen(GSN_URL)
    r = f.read().splitlines()
    if not r:
        collectd.error('gsn plugin: Nothing received from '+GSN_URL)
        return
    for k in r:
        p = k.partition(" ")
        kk = p[0].split(".")
        val = collectd.Values(plugin='gsn')
        if k[-1] == "count":
            val.type = 'counter'
        else:
            val.type = 'gauge'
        val.type_instance = "_".join(kk[:-1])
        val.values = [int(p[2])]
        val.dispatch()


# register callbacks
collectd.register_config(configure_callback)
collectd.register_read(read_callback)
