@echo off
setlocal ENABLEDELAYEDEXPANSION

"%JAVA_HOME%\bin\java" -classpath "%JAVA_HOME%\lib\rt.jar;./lib/*" ^
  -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Log4JLogger^
  -Dorg.mortbay.log.LogFactory.noDiscovery=false gsn.GSNStop 22232
