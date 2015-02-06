 #!/usr/bin/python

import urllib, sys


def main():
    f = urllib.urlopen("http://127.0.0.1:22001/stat?header=true")
    r = f.read().splitlines()
    if len(sys.argv) < 2:
        data(r)

    if sys.argv[1] == "config":
        print "graph_title GSN status"
        for k in r[0][:-1].split(","):
            print "%s.label %s"%(k,k)
            if k.endswith("count"):
                print "%s.min 0"%(k)
                print "%s.type DERIVE"%(k)
    elif sys.argv[1] == "help":
        usage()
    else:
        data(r)

def data(r):
     keys = r[0][:-1].split(",")
     vals = r[1][:-1].split(",")
     for i in range(len(keys)):
         print "%s.value %s"%(keys[i],vals[i])


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
