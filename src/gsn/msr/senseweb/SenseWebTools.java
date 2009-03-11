package gsn.msr.senseweb;

import gsn.beans.DataField;
import gsn.beans.VSensorConfig;
import gsn.utils.KeyValueImp;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import org.apache.commons.collections.KeyValue;
import org.apache.axis2.AxisFault;
import gsn.msr.senseweb.MSRSensorDescriptor;
import gsn.msr.senseweb.sensormanager.ServiceStub;

public class SenseWebTools {
    public static void main(String[] args) throws RemoteException {
        String host= "http://lsirpc.epfl.ch/";
        VSensorConfig myVS = new VSensorConfig();
        myVS.setName("gsnTest1");
        myVS.setDescription("Desc1");
        myVS.setAddressing(new KeyValue[] {new KeyValueImp("Latitude","1"),new KeyValueImp("Longitude","1"),new KeyValueImp("Altitude","1")});
        myVS.setOutputStructure(new DataField[] {new DataField("temperature","integer"),new DataField("light","double") });
        String username = "GSN@gsn.com";
        String password = "GSN@gsn.com";
        logIt("Authenticating...");
        String guid = authenticate(username, password);
        logIt("Registered with GUID="+guid);
        registerSensor(guid, password, myVS, host);
        deleteSensor(guid, password, myVS, host);
  }

    public static String authenticate(String username,String password) throws RemoteException {
        gsn.msr.senseweb.usermanager.ServiceStub userManager = new gsn.msr.senseweb.usermanager.ServiceStub();
        gsn.msr.senseweb.usermanager.ServiceStub.GetPassCode getPassCodeParams = new gsn.msr.senseweb.usermanager.ServiceStub.GetPassCode();

        getPassCodeParams.setUserName(username);
        getPassCodeParams.setPassword(password);
        String passCodeStr = userManager.GetPassCode(getPassCodeParams).getGetPassCodeResult().getGuid();
        
        return passCodeStr;
    }
    public static boolean registerSensor(String guid, String username,VSensorConfig conf,String gsnURI) throws RemoteException{
        gsn.msr.senseweb.sensormanager.ServiceStub stub = new gsn.msr.senseweb.sensormanager.ServiceStub();
        //gsn.msr.senseweb.sensormanager.ServiceStub.Guid passGUID = new gsn.msr.senseweb.sensormanager.ServiceStub.Guid();
        //passGUID.setGuid(guid);

        // create SensorInfo object
        gsn.msr.senseweb.sensormanager.ServiceStub.SensorInfo sensor = new gsn.msr.senseweb.sensormanager.ServiceStub.SensorInfo();
        sensor.setDataType("scalar");
        sensor.setSensorType(MSRSensorDescriptor.SENSOR_TYPE_THERMOMETER); 
        sensor.setPublisherName(username);
        sensor.setOriginalPublisherName(username);
        sensor.setSensorName(conf.getName());
        sensor.setUrl(gsnURI);
        sensor.setDescription(conf.getDescription());
        sensor.setLatitude(conf.getLatitude());
        sensor.setLongitude(conf.getLongitude());
        sensor.setAltitude(conf.getAltitude());
        GregorianCalendar aDate = new GregorianCalendar();
                          aDate.set(2008,1,1);
        sensor.setEntryTime(aDate); //TODO: set appropriate date
        logIt("Date is "+aDate.toString());
        sensor.setReportPeriod(1);
        sensor.setSamplingPeriod(1);
        
        // create Guid object
        gsn.msr.senseweb.sensormanager.ServiceStub.Guid myGuid = new gsn.msr.senseweb.sensormanager.ServiceStub.Guid();
        myGuid.setGuid(guid);

        // create RegisterSensor object
        gsn.msr.senseweb.sensormanager.ServiceStub.RegisterSensor myRegisterSensor = new gsn.msr.senseweb.sensormanager.ServiceStub.RegisterSensor();
        myRegisterSensor.setPublisherName(sensor.getPublisherName());
        myRegisterSensor.setPassCode(myGuid);
        myRegisterSensor.setSensor(sensor);

        //
        ServiceStub.RegisterSensorResponse output = stub.RegisterSensor(myRegisterSensor);
        logIt("Result: "+output.getRegisterSensorResult());

        return (output.getRegisterSensorResult().indexOf("OK")>0);
    }

    public static boolean deleteSensor(String guid, String username,VSensorConfig conf,String gsnURI) throws RemoteException{
        gsn.msr.senseweb.sensormanager.ServiceStub stub = new gsn.msr.senseweb.sensormanager.ServiceStub();
        //gsn.msr.senseweb.sensormanager.ServiceStub.Guid passGUID = new gsn.msr.senseweb.sensormanager.ServiceStub.Guid();
        //passGUID.setGuid(guid);

        // create Guid object
        gsn.msr.senseweb.sensormanager.ServiceStub.Guid myGuid = new gsn.msr.senseweb.sensormanager.ServiceStub.Guid();
        myGuid.setGuid(guid);

        // create DeleteSensor object
        gsn.msr.senseweb.sensormanager.ServiceStub.DeleteSensor myDeleteSensor = new gsn.msr.senseweb.sensormanager.ServiceStub.DeleteSensor();
        myDeleteSensor.setPassCode(myGuid);
        myDeleteSensor.setPublisherName(username);
        myDeleteSensor.setOriginalPublisherName(username);
        myDeleteSensor.setSensorName(conf.getName());

        stub.DeleteSensor(myDeleteSensor);

        ServiceStub.DeleteSensorResponse output = stub.DeleteSensor(myDeleteSensor);
        logIt("Result: "+output.getDeleteSensorResult());

        return (output.getDeleteSensorResult().indexOf("OK")>0);
    }

        // simple logging
    public static void logIt(String s) {
           System.out.println(s);
    }

}
