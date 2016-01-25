#!/bin/bash

#$JAVA_HOME/bin/java -classpath ./lib/* -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Log4JLogger -Dorg.mortbay.log.LogFactory.noDiscovery=false gsn.GSNStop 22232         
java -classpath $cp gsn.GSNStop 22232         
