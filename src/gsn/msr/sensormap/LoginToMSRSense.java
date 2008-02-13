package gsn.msr.sensormap;
import gsn.beans.DataField;
import gsn.beans.VSensorConfig;
import gsn.msr.sensormap.sensorman.ArrayOfString;
import gsn.msr.sensormap.sensorman.SensorInfo;
import gsn.msr.sensormap.sensorman.SensorService;
import gsn.msr.sensormap.sensorman.ServiceSoap;
import gsn.msr.sensormap.userman.LoginService;
import gsn.utils.KeyValueImp;
import java.util.List;
import org.apache.commons.collections.KeyValue;

public class LoginToMSRSense {
  
  public static void main(String[] args) {
    String host= "http://micssrv22.epfl.ch/";
    VSensorConfig conf = new VSensorConfig();
    conf.setName("Test1");
    conf.setDescription("Desc1");
    conf.setAddressing(new KeyValue[] {new KeyValueImp("latitude","1"),new KeyValueImp("Longitude","1"),new KeyValueImp("Altitude","1")});
    conf.setOutputStructure(new DataField[] {new DataField("tempetature","integer"),new DataField("light","double") });
    String username = "bla";
    String password = "bla@bla.com";
    register_sensor(username, password, conf, host);
  }
  public static void register_sensor(String username,String password,VSensorConfig conf,String host ) {
    
    LoginService login = new LoginService();
    String passcode = login.getServiceSoap12().getPassCode(username, password);
    
    System.out.println(passcode);
    
    ServiceSoap sensors = new SensorService().getServiceSoap12();
    // create an output type for each 
    ArrayOfString arrayOfString = new ArrayOfString();
    for (DataField df : conf.getOutputStructure())
      arrayOfString.getString().add(df.getName());
    String out = sensors.createVectorSesnsorType(username, passcode, "GSNStreamElement",arrayOfString );
    System.out.println(">>OUTPUT OF CREATION OF SENSOR TYPE: "+out);
  
    SensorInfo sensor = new SensorInfo();
    sensor.setDataType("vector");
    sensor.setPublisherName(username);
    sensor.setUrl(host);
    sensor.setAltitude(conf.getAltitude());
    sensor.setLatitude(conf.getLatitude());
    sensor.setLongitude(conf.getLongitude());
    sensor.setDescription(conf.getDescription());
    sensor.setSensorName(conf.getName());
    sensor.setSensorType("GSNStreamElement");
    
    out = sensors.insertVectorSensor(username, passcode, sensor );
    System.out.println(">>OUTPUT OF INSERTION: "+out);
    System.out.println("Registered Sensors ...");
    List<SensorInfo> info = sensors.getSensorsByPublisher(username, passcode).getSensorInfo();
    for (SensorInfo si : info) {
      System.out.println(si.getSensorName());
      sensors.deleteSensor(username, passcode, si.getSensorName());
    }
  }
  
}
