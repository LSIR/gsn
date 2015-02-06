#!/usr/bin/python

import urllib, sys
from collections import defaultdict


def main():
    f = urllib.urlopen("http://127.0.0.1:22001/stat?header=true")
    r = f.read().splitlines()
    if len(sys.argv) < 2:
        data(r)


    if sys.argv[1] == "config":
        vs = defaultdict(list)
        print "multigraph gsn_core"
        print "graph_title GSN Server status"
        print "graph_period minute"
        for k in r[0][:-1].split(","):
            kk = k.split(":")
            if kk[0] == "":
                print "%s.label %s"%(kk[2],kk[2])
                if kk[3] == "count":
                    print "%s.min 0"%(kk[2])
                    print "%s.type DERIVE"%(kk[2])
            else:
                vs[kk[0]].append(kk[1:])
        for k,v in vs.iteritems():
            print "multigraph gsn_vsensors_"+k
            print "graph_title GSN Virtual Sensor '"+k+"' status"
            print "graph_period minute"
            for vv in v:
                if vv[0] == "":
                    print "%s.label %s"%(vv[1],vv[1])
                    if vv[2] == "count":
                        print "%s.min 0"%(vv[1])
                        print "%s.type DERIVE"%(vv[1])
                else:
                    print "%s_%s.label (%s) %s"%(vv[0],vv[1],vv[0],vv[1])
                    if vv[2] == "count":
                        print "%s_%s.min 0"%(vv[0],vv[1])
                        print "%s_%s.type DERIVE"%(vv[0],vv[1])

    elif sys.argv[1] == "help":
        usage()
    else:
        data(r)

def data(r):
    keys = r[0][:-1].split(",")
    vals = r[1][:-1].split(",")
    vs = defaultdict(list)
    print "multigraph gsn_core"
    for i in range(len(keys)):
        kk = keys[i].split(":")
        if kk[0] == "":
            print "%s.value %s"%(kk[2],vals[i])
        else:
            vs[kk[0]].append((kk[1],kk[2],vals[i]))
    for k,v in vs.iteritems():
        print "multigraph gsn_vsensors_"+k
        for vv in v:
            if vv[0] == "":
                print "%s.value %s"%(vv[1],vv[2])
            else:
                print "%s_%s.value %s"%(vv[0],vv[1],vv[2])
    sys.exit(0)



def usage():

    print ' -----------------------------------------------------------------'
    print ' Julien Eberle (julien.eberle@a3.epfl.ch) EPFL, LSIR Feb 2015'
    print ' '
    print ' Munin plugin for gathering statistics about GSN'
    print ' '
    print ' config  get the graph configuration'
    print ' help print this text'
    print ' get data from the local GSN server'
    print ' -----------------------------------------------------------------'
    sys.exit(1)

#-------------------------------

if __name__ == "__main__":
    main()
