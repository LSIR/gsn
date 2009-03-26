package gsn.msr.sensormap;
import gsn.Main;
import gsn.beans.DataField;
import gsn.beans.VSensorConfig;
import gsn.msr.sensormap.sensorman.ServiceStub.CreateVectorSensorType;
import gsn.utils.KeyValueImp;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import org.apache.commons.collections.KeyValue;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


public class LoginToMSRSense {

    public static int REGISTER_SENSOR_ERROR_NOT_CREATED = 0;
    public static int REGISTER_SENSOR_OK_CREATED_NEW =1;
    public static int REGISTER_SENSOR_ERROR_ALREADY_EXISTS = 2;
    public static int DELETE_SENSOR_OK = 1;
    public static int DELETE_SENSOR_ERROR_DOESNT_EXIST = 0;

    private static transient Logger logger = Logger.getLogger ( LoginToMSRSense.class );
    public static final String     DEFAULT_GSN_LOG4J_PROPERTIES     = "conf/log4j.properties";

    public static void main(String[] args) throws RemoteException {

        PropertyConfigurator.configure ( DEFAULT_GSN_LOG4J_PROPERTIES );
        String host= "http://micssrv22.epfl.ch/";
        VSensorConfig conf = new VSensorConfig();
        conf.setName("Test99");
        conf.setDescription("Desc1");
        conf.setAddressing(new KeyValue[] {new KeyValueImp("latitude","1"),new KeyValueImp("Longitude","1"),new KeyValueImp("Altitude","1")});
        conf.setOutputStructure(new DataField[] {new DataField("temperature","integer"),new DataField("light","double") });
        String username = "GSNTEST@gsn.com";
        String password = "GSNTEST@gsn.com";

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

    logger.warn("Registering sensor "+conf.getName()+" on Sensormap...");
    gsn.msr.sensormap.userman.ServiceStub login = new gsn.msr.sensormap.userman.ServiceStub();
    gsn.msr.sensormap.userman.ServiceStub.GetPassCode getPassCodeParams = new gsn.msr.sensormap.userman.ServiceStub.GetPassCode();

    logger.warn("Using username:"+username+" password:*****"); // mask password
    getPassCodeParams.setUserName(username);
    getPassCodeParams.setPassword(password);
    String passcodeStr = login.GetPassCode(getPassCodeParams).getGetPassCodeResult().getGuid();

    logger.warn("Got GUID passcode:"+passcodeStr);

    gsn.msr.sensormap.sensorman.ServiceStub stub = new gsn.msr.sensormap.sensorman.ServiceStub(); //the default implementation should point to the right endpoint
    gsn.msr.sensormap.sensorman.ServiceStub.Guid passGUID = new gsn.msr.sensormap.sensorman.ServiceStub.Guid();
    passGUID.setGuid(passcodeStr);
    CreateVectorSensorType createVSensorTypeParams = new gsn.msr.sensormap.sensorman.ServiceStub.CreateVectorSensorType();
    createVSensorTypeParams.setPassCode(passGUID);
    createVSensorTypeParams.setPublisherName(username);
    createVSensorTypeParams.setName("GSNStreamElement-"+conf.getName());
    String uri = gsnURI+"#"+conf.getName();
	createVSensorTypeParams.setUri(uri);
        

    gsn.msr.sensormap.sensorman.ServiceStub.ArrayOfString arrayOfString = new gsn.msr.sensormap.sensorman.ServiceStub.ArrayOfString ();
    ArrayList<String> fields=  new ArrayList<String>();
    for (DataField df : conf.getOutputStructure())
        fields.add("Generic");
    arrayOfString.setString(fields.toArray(new String[] {}));
    createVSensorTypeParams.setComponentTypes(arrayOfString);

    logger.warn("Creating new vector sensor type "+createVSensorTypeParams.getName()+" With URI: "+uri);
    String call_output = stub.CreateVectorSensorType(createVSensorTypeParams).getCreateVectorSensorTypeResult();
        
    if (call_output.indexOf("OK")>=0)
        logger.warn("Type "+createVSensorTypeParams.getName()+" created correctly. SensorMap says: "+call_output);
    else
        logger.warn("Type "+createVSensorTypeParams.getName()+" was not created. SensorMap says: "+call_output);

    gsn.msr.sensormap.sensorman.ServiceStub.RegisterVectorSensor registerVectorSensorParams = new gsn.msr.sensormap.sensorman.ServiceStub.RegisterVectorSensor();

    gsn.msr.sensormap.sensorman.ServiceStub.SensorInfo sensor = new gsn.msr.sensormap.sensorman.ServiceStub.SensorInfo();
    sensor.setDataType("Vector");
    sensor.setPublisherName(username);
    sensor.setOriginalPublisherName(username);
    sensor.setUrl(gsnURI);
    //logger.warn("ALTITUDE:"+conf.getAltitude());
    sensor.setAltitude(conf.getAltitude());
    sensor.setLatitude(conf.getLatitude());
    sensor.setLongitude(conf.getLongitude());
    sensor.setDescription(conf.getDescription());
    sensor.setSensorName(conf.getName());
    sensor.setSensorType(uri);
    sensor.setEntryTime(new GregorianCalendar());
    sensor.setAccessControl("protected");
    String groupName= Main.getContainerConfig().getMsrMap().get("group-name");
    if (groupName != null && groupName.trim().length()>0)
    	sensor.setGroupName(groupName);
    
    sensor.setWebServiceUrl(gsnURI+"services/Service?wsdl");
    registerVectorSensorParams.setPassCode(passGUID);
    registerVectorSensorParams.setPublisherName(username);
    registerVectorSensorParams.setSensor(sensor);
        

    call_output = stub.RegisterVectorSensor(registerVectorSensorParams).getRegisterVectorSensorResult();

    if (call_output.indexOf("OK")>=0){
        logger.warn("Sensor "+conf.getName()+" registered correctly. SensorMap says: "+call_output);
        return REGISTER_SENSOR_OK_CREATED_NEW;
    }

    if (call_output.indexOf("Error: Sensor with the same publisher name and sensor name already exists")>=0){
        logger.warn("Sensor "+conf.getName()+" not registered (already exists). SensorMap says: "+call_output);
        return REGISTER_SENSOR_ERROR_ALREADY_EXISTS;
    }

    logger.warn("Sensor "+conf.getName()+" not registered. SensorMap says: "+call_output);
    return REGISTER_SENSOR_ERROR_NOT_CREATED;

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

      logger.warn("Deleting sensor "+conf.getName()+" from Sensormap...");
      gsn.msr.sensormap.sensorman.ServiceStub stub = new gsn.msr.sensormap.sensorman.ServiceStub();

      gsn.msr.sensormap.userman.ServiceStub login = new gsn.msr.sensormap.userman.ServiceStub();
      gsn.msr.sensormap.userman.ServiceStub.GetPassCode getPassCodeParams = new gsn.msr.sensormap.userman.ServiceStub.GetPassCode();

      logger.warn("Using username:"+username+" password:*****"); // mask password
      getPassCodeParams.setUserName(username);
      getPassCodeParams.setPassword(password);
      String passcodeStr = login.GetPassCode(getPassCodeParams).getGetPassCodeResult().getGuid();
      logger.warn("Got GUID passcode:"+passcodeStr);

      gsn.msr.sensormap.sensorman.ServiceStub.Guid passGUID = new gsn.msr.sensormap.sensorman.ServiceStub.Guid();
      passGUID.setGuid(passcodeStr);

      // create DeleteVectorSensor object
      gsn.msr.sensormap.sensorman.ServiceStub.DeleteVectorSensor deleteVectorSensorParams = new gsn.msr.sensormap.sensorman.ServiceStub.DeleteVectorSensor();
      deleteVectorSensorParams.setPassCode(passGUID);
      deleteVectorSensorParams.setPublisherName(username);
      deleteVectorSensorParams.setOriginalPublisherName(username);
      deleteVectorSensorParams.setSensorName(conf.getName());

      logger.warn("calling DeleteVectorSensor with parameters:");
      logger.warn("...passCode:"+deleteVectorSensorParams.getPassCode());
      logger.warn("...PublisherName:"+deleteVectorSensorParams.getPublisherName());
      logger.warn("...OriginalPublisherName:"+deleteVectorSensorParams.getOriginalPublisherName());
      logger.warn("...SensorName:"+deleteVectorSensorParams.getSensorName());

      gsn.msr.sensormap.sensorman.ServiceStub.DeleteVectorSensorResponse output = stub.DeleteVectorSensor(deleteVectorSensorParams);
      String call_output = output.getDeleteVectorSensorResult();

      if (call_output.indexOf("OK")>=0) {
          logger.warn("Sensor "+conf.getName()+" deleted correctly. SensorMap says: "+call_output);
          return DELETE_SENSOR_OK;
      }
      else {
          logger.warn("Sensor "+conf.getName()+" not deleted. SensorMap says: "+call_output);
          return DELETE_SENSOR_ERROR_DOESNT_EXIST;
      }
    }

}
