echo .
echo UserManager
echo ***************
call %AXIS2_HOME%\bin\wsdl2java.bat -o generated -s -p gsn.msr.senseweb.usermanager -t -uri usermanager.service.xml
echo SensorManager
echo ***************
call %AXIS2_HOME%\bin\wsdl2java.bat -o generated -s -p gsn.msr.senseweb.sensormanager -t -uri sensormanager.service.xml
echo UserManagerDH
echo ***************
call %AXIS2_HOME%\bin\wsdl2java.bat -o generated -s -p gsn.msr.senseweb.usermanagerdh -t -uri usermanagerdh.service.xml
echo DataHub
echo ***************
call %AXIS2_HOME%\bin\wsdl2java.bat -o generated -s -ss -sd -p gsn.msr.senseweb.datahub -t -uri datahub.service.xml