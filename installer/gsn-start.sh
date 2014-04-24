#!/bin/bash

for jarFile in $( ls lib/*jar ); do
     cp=$cp:./$jarFile
done
$JAVA_HOME/bin/java -classpath $cp -splash:lib/GSN_green_medium.png -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Log4JLogger -Dorg.mortbay.log.LogFactory.noDiscovery=false gsn.Main 22232 &

        
