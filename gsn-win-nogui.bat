@echo off
setlocal ENABLEDELAYEDEXPANSION

SET CLASSPATH=/usr/lib/jvm/java-6-openjdk/jre\..\lib\tools.jar
FOR /R ./lib %%c in (*.jar) DO SET CLASSPATH=!CLASSPATH!;%%c
java -classpath "%CLASSPATH%" gsn.Main
