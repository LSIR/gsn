package gsn.vsensor;

import java.util.ArrayList;
import java.io.Serializable;

import org.apache.log4j.Logger;

import gsn.beans.DataField;
import gsn.beans.StreamElement;

public class GPSStreamJoiner extends BridgeVirtualSensorPermasense {
	
	private static final transient Logger logger = Logger.getLogger(GPSStreamJoiner.class);
	
	private static String JOIN_TYPE_STR = "join_type";

	private static String OZONE_WITH_GPS = "ozone";
	private static String CO_WITH_GPS = "co";
	private static String ACC_WITH_GPS = "acc";

	private static int OZONE_WITH_GPS_NAMING = 1;
	private static int CO_WITH_GPS_NAMING = 2;
	private static int ACC_WITH_GPS_NAMING = 3;
	
	private static int MAX_BUFFER_SIZE = 1000;
	private ArrayList<StreamElement> sensorBuffer = new ArrayList<StreamElement>();
	private ArrayList<StreamElement> gpsBuffer = new ArrayList<StreamElement>();

	private static DataField[] ozoneGpsDataField = {	
    new DataField("OZONE_PPB_1", "DOUBLE"),
    new DataField("OZONE_PPB_2", "DOUBLE"),
    new DataField("RESISTANCE_1", "INTEGER"),
    new DataField("RESISTANCE_2", "INTEGER"),
    new DataField("TEMPERATURE", "DOUBLE"),
    new DataField("HUMIDITY", "INTEGER"),
    new DataField("UTC_POS_TIME", "INTEGER"),
    new DataField("LATITUDE", "DOUBLE"),
    new DataField("LONGITUDE", "DOUBLE"),
    new DataField("QUALITY", "INTEGER"),
    new DataField("NR_SATELLITES", "INTEGER"),
    new DataField("HDOP", "DOUBLE"),
    new DataField("GEOID_HEIGHT", "DOUBLE")
  };
	
	private static DataField[] coGpsDataField = { 
    new DataField("SENSOR_CURRENT", "DOUBLE"),
    new DataField("SENSOR_PPM", "DOUBLE"),
    new DataField("AMBIENT_TEMP", "DOUBLE"),
    new DataField("SENSITIVITY_COMP", "DOUBLE"),
    new DataField("UTC_POS_TIME", "INTEGER"),
    new DataField("LATITUDE", "DOUBLE"),
    new DataField("LONGITUDE", "DOUBLE"),
    new DataField("QUALITY", "INTEGER"),
    new DataField("NR_SATELLITES", "INTEGER"),
    new DataField("HDOP", "DOUBLE"),
    new DataField("GEOID_HEIGHT", "DOUBLE")
	};
	
	private static DataField[] accGpsDataField = { 
    new DataField("DURATION", "DOUBLE"),
    new DataField("TIME_OF_DATA", "BIGINT"),
    new DataField("UTC_POS_TIME", "INTEGER"),
    new DataField("LATITUDE", "DOUBLE"),
    new DataField("LONGITUDE", "DOUBLE"),
    new DataField("QUALITY", "INTEGER"),
    new DataField("NR_SATELLITES", "INTEGER"),
    new DataField("HDOP", "DOUBLE"),
    new DataField("GEOID_HEIGHT", "DOUBLE")
  };
  
	private int joinType = -1;

	@Override
	public boolean initialize() {
		String type = getVirtualSensorConfiguration().getMainClassInitialParams().get(JOIN_TYPE_STR);
		if (type == null) {
			logger.error(JOIN_TYPE_STR + " has to be specified");
			return false;
		}
		if (type.equalsIgnoreCase(OZONE_WITH_GPS))
			joinType = OZONE_WITH_GPS_NAMING;
		else if (type.equalsIgnoreCase(CO_WITH_GPS))
			joinType = CO_WITH_GPS_NAMING;
		else if (type.equalsIgnoreCase(ACC_WITH_GPS))
      joinType = ACC_WITH_GPS_NAMING;
		else {
			logger.error(JOIN_TYPE_STR + " string has to be " + OZONE_WITH_GPS + ", " + CO_WITH_GPS + " or " + ACC_WITH_GPS);
			return false;
		}
		
		return super.initialize();
	}

	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
		try {
		  
		  int dev_id = ((Integer)data.getData("DEVICE_ID")).intValue();
		  long m_id = ((Long)data.getData("MEASUREMENT_ID")).longValue();
		  
		  ArrayList<StreamElement> checkBuffer;
		  ArrayList<StreamElement> streamBuffer;
      if (inputStreamName.equalsIgnoreCase("GPS")) {
        checkBuffer = sensorBuffer;
        streamBuffer = gpsBuffer;
      }
      else if (inputStreamName.equalsIgnoreCase("SENSOR")) {
        checkBuffer = gpsBuffer;
        streamBuffer = sensorBuffer;
      }
      else {
        logger.error("streamName has to be gps or sensor");
        return;
      }
      
      // Check buffer for the appropriate measurement and device id
      for (int i = 0; i < checkBuffer.size(); i++) {
        if (((Integer)checkBuffer.get(i).getData("DEVICE_ID")).intValue() == dev_id && ((Long)checkBuffer.get(i).getData("MEASUREMENT_ID")).longValue() == m_id) {
          // Remove entry from the buffer and push data to db
          StreamElement bufData = checkBuffer.get(i);
          checkBuffer.remove(i);
          
          if (inputStreamName.equalsIgnoreCase("GPS") && joinType == OZONE_WITH_GPS_NAMING)
            data = new StreamElement(data, ozoneGpsDataField, new Serializable[] {bufData.getData("OZONE_SENSOR_1"), bufData.getData("OZONE_SENSOR_2"), bufData.getData("RESISTANCE_1"), bufData.getData("RESISTANCE_2"), bufData.getData("TEMPERATURE"), bufData.getData("HUMIDITY"), data.getData("UTC_POS_TIME"), data.getData("LATITUDE"), data.getData("LONGITUDE"), data.getData("QUALITY"), data.getData("NR_SATELLITES"), data.getData("HDOP"), data.getData("GEOID_HEIGHT")});
          else if (inputStreamName.equalsIgnoreCase("SENSOR") && joinType == OZONE_WITH_GPS_NAMING)
            data = new StreamElement(data, ozoneGpsDataField, new Serializable[] {data.getData("OZONE_SENSOR_1"), data.getData("OZONE_SENSOR_2"), data.getData("RESISTANCE_1"), data.getData("RESISTANCE_2"), data.getData("TEMPERATURE"), data.getData("HUMIDITY"), bufData.getData("UTC_POS_TIME"), bufData.getData("LATITUDE"), bufData.getData("LONGITUDE"), bufData.getData("QUALITY"), bufData.getData("NR_SATELLITES"), bufData.getData("HDOP"), bufData.getData("GEOID_HEIGHT")});
          else if (inputStreamName.equalsIgnoreCase("GPS") && joinType == ACC_WITH_GPS_NAMING)
            data = new StreamElement(data, accGpsDataField, new Serializable[] {bufData.getData("DURATION"), bufData.getData("TIME_OF_DATA"), data.getData("UTC_POS_TIME"), data.getData("LATITUDE"), data.getData("LONGITUDE"), data.getData("QUALITY"), data.getData("NR_SATELLITES"), data.getData("HDOP"), data.getData("GEOID_HEIGHT")});
          else if (inputStreamName.equalsIgnoreCase("SENSOR") && joinType == ACC_WITH_GPS_NAMING)
            data = new StreamElement(data, accGpsDataField, new Serializable[] {data.getData("DURATION"), data.getData("TIME_OF_DATA"), bufData.getData("UTC_POS_TIME"), bufData.getData("LATITUDE"), bufData.getData("LONGITUDE"), bufData.getData("QUALITY"), bufData.getData("NR_SATELLITES"), bufData.getData("HDOP"), bufData.getData("GEOID_HEIGHT")});
          else if (inputStreamName.equalsIgnoreCase("GPS") && joinType == CO_WITH_GPS_NAMING)
            data = new StreamElement(data, coGpsDataField, new Serializable[] {bufData.getData("SENSOR_CURRENT"), bufData.getData("SENSOR_PPM_1"), bufData.getData("AMBIENT_TEMP"), bufData.getData("SENSITIVITY_COMP"), data.getData("UTC_POS_TIME"), data.getData("LATITUDE"), data.getData("LONGITUDE"), data.getData("QUALITY"), data.getData("NR_SATELLITES"), data.getData("HDOP"), data.getData("GEOID_HEIGHT")});
          else if (inputStreamName.equalsIgnoreCase("SENSOR") && joinType == CO_WITH_GPS_NAMING)
            data = new StreamElement(data, coGpsDataField, new Serializable[] {data.getData("SENSOR_CURRENT"), data.getData("SENSOR_PPM_1"), data.getData("AMBIENT_TEMP"), data.getData("SENSITIVITY_COMP"), bufData.getData("UTC_POS_TIME"), bufData.getData("LATITUDE"), bufData.getData("LONGITUDE"), bufData.getData("QUALITY"), bufData.getData("NR_SATELLITES"), bufData.getData("HDOP"), bufData.getData("GEOID_HEIGHT")});
          else {
            logger.error("No match for inputStreamName " + inputStreamName + " and joinType " + joinType);
          }
          
          super.dataAvailable(inputStreamName, data);
          return;
        }
      }
      // No match found, add stream element to the streamBuffer and check buffer size
      if (streamBuffer.size() == MAX_BUFFER_SIZE) {
        logger.info("Remove buffered element with measurement_id " + (Long)streamBuffer.get(0).getData("MEASUREMENT_ID") + " from " + inputStreamName + " buffer of joinType " + joinType);
        streamBuffer.remove(0);
      }
      streamBuffer.add(data);
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

}
