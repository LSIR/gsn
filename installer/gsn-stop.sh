#!/bin/bash

for jarFile in $( ls lib/*jar ); do
     cp=$cp:./$jarFile
done
java -classpath $cp \ 
  -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Log4JLogger \ 
  -Dorg.mortbay.log.LogFactory.noDiscovery=false \
   gsn.GSNStop 22232         
