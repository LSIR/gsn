#!/usr/bin/python

import urllib
import sys
from collections import defaultdict


def main():
    f = urllib.urlopen("http://127.0.0.1:22001/stat")
    r = f.read().splitlines()
    if len(sys.argv) < 2:
        data(r)

    if sys.argv[1] == "config":
        groups = defaultdict(list)
        for k in r:
            kk = k.partition(" ")[0].split(".")
            groups[(kk[0], kk[1])].append(kk)

        for k, v in groups.iteritems():
            print "multigraph gsn_%s_%s" % (k[0], k[1])
            print "graph_title GSN Server %s %s" % (k[0], k[1])
            print "graph_period minute"
            for vv in v:
                n = "_".join(vv[2:-1])
                print "%s.label %s" % (n, " ".join(vv[2:-1]))
                if vv[-1] == "counter":
                    print "%s.min 0" % (n)
                    print "%s.type DERIVE" % (n)

    elif sys.argv[1] == "help":
        usage()
    else:
        data(r)


def data(r):
    groups = defaultdict(list)
    for k in r:
        p = k.partition(" ")
        kk = p[0].split(".")
        groups[(kk[0], kk[1])].append((kk, p[2]))

    for k, v in groups.iteritems():
        print "multigraph gsn_%s_%s" % (k[0], k[1])
        for vv in v:
            print "%s.value %s" % ("_".join(vv[0][2:-1]), vv[1])
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
