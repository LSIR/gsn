@echo off
setlocal ENABLEDELAYEDEXPANSION

"%JAVA_HOME%\bin\java" -classpath "%JAVA_HOME%\lib\rt.jar;./conf/;./lib/*" gsn.GSNStop 22232
