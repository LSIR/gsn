package gsn.msr.sensormap;
import gsn.beans.DataField;
import gsn.beans.VSensorConfig;
import gsn.msr.sensormap.sensorman.ServiceStub.CreateVectorSensorType;
import gsn.utils.KeyValueImp;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import org.apache.commons.collections.KeyValue;


public class LoginToMSRSense {
  
  public static void main(String[] args) throws RemoteException {
    String host= "http://micssrv22.epfl.ch/";
    VSensorConfig conf = new VSensorConfig();
    conf.setName("Test99");
    conf.setDescription("Desc1");
    conf.setAddressing(new KeyValue[] {new KeyValueImp("latitude","1"),new KeyValueImp("Longitude","1"),new KeyValueImp("Altitude","1")});
    conf.setOutputStructure(new DataField[] {new DataField("temperature","integer"),new DataField("light","double") });
    String username = "GSN@gsn.com";
    String password = "GSN@gsn.com";

    register_sensor(username, password, conf, host);
    delete_sensor(username, password, conf);
  }

    /**
     * Tries to register a virtual sensor. Returns an integer for status.
     *
     * @param  username username for sensorweb user account
     * @param  password password for sensorweb user account
     * @param  conf     configuration of the virtual sensor
     * @param  gsnURI   URI of the GSN instance
     * @return status of operation is integer. 0 for not created, 1 for created new, 2 for already exists.
     * @throws java.rmi.RemoteException java.rmi.RemoteException
     */

  public static int register_sensor(String username,String password,VSensorConfig conf,String gsnURI ) throws RemoteException {
    
    gsn.msr.sensormap.userman.ServiceStub login = new gsn.msr.sensormap.userman.ServiceStub();
    gsn.msr.sensormap.userman.ServiceStub.GetPassCode getPassCodeParams = new gsn.msr.sensormap.userman.ServiceStub.GetPassCode();

    getPassCodeParams.setUserName(username);
    getPassCodeParams.setPassword(password);
    String passcodeStr = login.GetPassCode(getPassCodeParams).getGetPassCodeResult().getGuid();
    System.out.println(passcodeStr);
    
    gsn.msr.sensormap.sensorman.ServiceStub stub = new gsn.msr.sensormap.sensorman.ServiceStub(); //the default implementation should point to the right endpoint
    gsn.msr.sensormap.sensorman.ServiceStub.Guid passGUID = new gsn.msr.sensormap.sensorman.ServiceStub.Guid();
    passGUID.setGuid(passcodeStr);
    CreateVectorSensorType createVSensorTypeParams = new gsn.msr.sensormap.sensorman.ServiceStub.CreateVectorSensorType();
    createVSensorTypeParams.setPassCode(passGUID);
    createVSensorTypeParams.setPublisherName(username);
    createVSensorTypeParams.setName("GSNStreamElement-"+conf.getName());
    createVSensorTypeParams.setUri(gsnURI+"#"+conf.getName());
    
    
    gsn.msr.sensormap.sensorman.ServiceStub.ArrayOfString arrayOfString = new gsn.msr.sensormap.sensorman.ServiceStub.ArrayOfString ();
    ArrayList<String> fields=  new ArrayList<String>();
    for (DataField df : conf.getOutputStructure())
      fields.add("Generic");
    arrayOfString.setString(fields.toArray(new String[] {}));
    createVSensorTypeParams.setComponentTypes(arrayOfString);
    
    String call_output = stub.CreateVectorSensorType(createVSensorTypeParams).getCreateVectorSensorTypeResult();
    System.out.println(">>OUTPUT OF CREATION OF SENSOR TYPE: "+call_output);
  
    gsn.msr.sensormap.sensorman.ServiceStub.RegisterVectorSensor registerVectorSensorParams = new gsn.msr.sensormap.sensorman.ServiceStub.RegisterVectorSensor();
    
    gsn.msr.sensormap.sensorman.ServiceStub.SensorInfo sensor = new gsn.msr.sensormap.sensorman.ServiceStub.SensorInfo();
    sensor.setDataType("Vector");
    sensor.setPublisherName(username);
    sensor.setOriginalPublisherName(username);  ///////////////
    sensor.setUrl(gsnURI);
    sensor.setAltitude(conf.getAltitude());
    sensor.setLatitude(conf.getLatitude());
    sensor.setLongitude(conf.getLongitude());
    sensor.setDescription(conf.getDescription());
    sensor.setSensorName(conf.getName());
    sensor.setSensorType(gsnURI+"#"+conf.getName());
    sensor.setEntryTime(new GregorianCalendar());
    sensor.setWebServiceUrl(gsnURI);
    registerVectorSensorParams.setPassCode(passGUID);
    registerVectorSensorParams.setPublisherName(username);
    registerVectorSensorParams.setSensor(sensor);
    
    
    call_output = stub.RegisterVectorSensor(registerVectorSensorParams).getRegisterVectorSensorResult();
    System.out.println(">>OUTPUT OF CREATION OF VECTOR SENSOR: "+call_output);

        if (call_output.indexOf("OK")>0) return 1;


        if  (call_output.indexOf("Error: Sensor with the same publisher name and sensor name already exists")>0)
        return 2;

        return 0;


//    
//    out = sensors.insertVectorSensor(username, passcode, sensor );
//    System.out.println(">>OUTPUT OF INSERTION: "+out);
//    System.out.println("Registered Sensors ...");
//    List<SensorInfo> info = sensors.getSensorsByPublisher(username, passcode).getSensorInfo();
//    for (SensorInfo si : info) {
//      System.out.println(si.getSensorName());
//      sensors.deleteSensor(username, passcode, si.getSensorName());
//    }
  }

        /**
     * Tries to delete a virtual sensor. Returns an integer for status.
     *
     * @param  username username for sensorweb user account
     * @param  password password for sensorweb user account
     * @param  conf     configuration of the virtual sensor
     * @return status of operation is integer. 0 for not deleted (already exists), 1 for deleted.
     * @throws java.rmi.RemoteException java.rmi.RemoteException
     */
  public static int delete_sensor(String username,String password,VSensorConfig conf) throws RemoteException{

      gsn.msr.sensormap.sensorman.ServiceStub stub = new gsn.msr.sensormap.sensorman.ServiceStub();

      gsn.msr.sensormap.userman.ServiceStub login = new gsn.msr.sensormap.userman.ServiceStub();
      gsn.msr.sensormap.userman.ServiceStub.GetPassCode getPassCodeParams = new gsn.msr.sensormap.userman.ServiceStub.GetPassCode();

      getPassCodeParams.setUserName(username);
      getPassCodeParams.setPassword(password);
      String passcodeStr = login.GetPassCode(getPassCodeParams).getGetPassCodeResult().getGuid();
      System.out.println(passcodeStr);

      gsn.msr.sensormap.sensorman.ServiceStub.Guid passGUID = new gsn.msr.sensormap.sensorman.ServiceStub.Guid();
      passGUID.setGuid(passcodeStr);

      // create DeleteSensor object
      gsn.msr.sensormap.sensorman.ServiceStub.DeleteSensor deleteSensorParams = new gsn.msr.sensormap.sensorman.ServiceStub.DeleteSensor();
      deleteSensorParams.setPassCode(passGUID);
      deleteSensorParams.setPublisherName(username);
      deleteSensorParams.setOriginalPublisherName(username);
      deleteSensorParams.setSensorName(conf.getName());

      System.out.println("calling DeleteSensor with parameters: \n...passCode:"+passGUID.toString());
      System.out.println("...PublisherName:"+username);
      System.out.println("...OriginalPublisherName:"+username);
      System.out.println("...SensorName:"+conf.getName());

      stub.DeleteSensor(deleteSensorParams);

      gsn.msr.sensormap.sensorman.ServiceStub.DeleteSensorResponse output = stub.DeleteSensor(deleteSensorParams);

      System.out.println("OUTPUT OF CREATION OF DELETE SENSOR: "+output.getDeleteSensorResult());

      if (output.getDeleteSensorResult().indexOf("OK")>0)
          return 1;
      else
          return 0;
    }

}
