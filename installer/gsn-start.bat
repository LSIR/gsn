@echo off
setlocal ENABLEDELAYEDEXPANSION

SET safe-storage-port=25700
SET safe-storage-controller-port=25712
SET gsn-controller-port=22232
SET max-servlets=20
SET max-db-connections=8
SET max-sliding-db-connections=8
SET remote-keep-alive-period=15000
SET aes-enc-salt="this is a simple clear salt"
SET max-memory=128M

"%JAVA_HOME%\bin\java" -classpath "%JAVA_HOME%\lib\rt.jar;./conf/;./lib/*" -Xmx%max-memory% -splash:lib/GSN_green_medium.png -Djavax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl -DmaxServlets=%max-servlets% -DmaxDBConnections=%max-db-connections% -DmaxSlidingDBConnections=%max-sliding-db-connections% -Dsalt=%aes-enc-salt% -DremoteKeepAlivePeriod=%remote-keep-alive-period% gsn.Main %gsn-controller-port%

