package gsn.wrappers.backlog.plugins;

import java.io.Serializable;
import java.util.Hashtable;

import org.apache.log4j.Logger;

import gsn.beans.DataField;
import gsn.wrappers.BackLogWrapper;
import gsn.wrappers.backlog.BackLogMessage;


public class OZ47Plugin extends AbstractPlugin {
  private static final String STATUS_DATA_TYPE = "status-data-type";

	private static final String STATISTICS_NAMING = "statistics";
	private static final String READINGS_NAMING = "readings";

	private static DataField[] readingsDataField = {	
            new DataField("TIMESTAMP", "BIGINT"),
						new DataField("GENERATION_TIME", "BIGINT"),
						new DataField("DEVICE_ID", "INTEGER"),
						new DataField("OZONE_SENSOR_1", "INTEGER"),
						new DataField("OZONE_SENSOR_2", "INTEGER"),
						new DataField("RESISTANCE_1", "INTEGER"),
						new DataField("RESISTANCE_2", "INTEGER"),
						new DataField("TEMPERATURE", "DOUBLE"),
						new DataField("HUMIDITY", "INTEGER"),
						new DataField("SENSOR_ID", "INTEGER")
  };
  
  private static DataField[] statisticsDataField = {	
            new DataField("TIMESTAMP", "BIGINT"),
						new DataField("GENERATION_TIME", "BIGINT"),
						new DataField("DEVICE_ID", "INTEGER"),
						new DataField("LOT_NO", "INTEGER"),
						new DataField("CELL_NO", "INTEGER"),
						new DataField("CALIB_WEEK", "INTEGER"),
						new DataField("CALIB_YEAR", "INTEGER"),
						new DataField("TIMER_PRE_HEAT", "INTEGER"),
						new DataField("AUTO_CALIB", "INTEGER"),
						new DataField("TIMER_DELAY", "INTEGER"),
						new DataField("TIMER_CYCLE", "INTEGER"),
						new DataField("TIMER_PULSE", "INTEGER"),
						new DataField("OFFSET_MEM", "INTEGER"),
						new DataField("OFFSET_VERIF", "INTEGER"),
						new DataField("OFFSET", "INTEGER"),
						new DataField("CALIB_PARAM_X0_1", "DOUBLE"),
						new DataField("CALIB_PARAM_X1_1", "DOUBLE"),
						new DataField("CALIB_PARAM_X2_1", "DOUBLE"),
						new DataField("CALIB_PARAM_X3_1", "DOUBLE"),
						new DataField("CALIB_PARAM_KT_1", "DOUBLE"),
						new DataField("CALIB_PARAM_X0_2", "DOUBLE"),
						new DataField("CALIB_PARAM_X1_2", "DOUBLE"),
						new DataField("CALIB_PARAM_X2_2", "DOUBLE"),
						new DataField("CALIB_PARAM_X3_2", "DOUBLE"),
						new DataField("CALIB_PARAM_KT_2", "DOUBLE"),
						new DataField("SENSOR_ID", "INTEGER")
  };

  private static final Hashtable<String, NameDataFieldPair> statusNamingTable = new Hashtable<String, NameDataFieldPair>();
	static
	{
		statusNamingTable.put(STATISTICS_NAMING, new NameDataFieldPair(1, statisticsDataField));
		statusNamingTable.put(READINGS_NAMING, new NameDataFieldPair(2, readingsDataField));
	}

	private final transient Logger logger = Logger.getLogger( OZ47Plugin.class );

  private String statusDataType;
	
	
	@Override
	public boolean initialize ( BackLogWrapper backlogwrapper, String coreStationName, String deploymentName) {
		super.activeBackLogWrapper = backlogwrapper;
		try {
			statusDataType = getActiveAddressBean().getPredicateValueWithException(STATUS_DATA_TYPE).toLowerCase();
		} catch (Exception e) {
			logger.error(statusDataType);
			logger.error(e.getMessage());
			return false;
		}
		if (statusNamingTable.get(statusDataType) == null) {
			logger.error("wrong " + STATUS_DATA_TYPE + " predicate key specified in virtual sensor XML file! (" + STATUS_DATA_TYPE + "=" + statusDataType + ")");
			return false;
		}
		logger.info("using OZ47 data type: " + statusDataType);
        
    registerListener();

    if (statusDataType.equalsIgnoreCase(STATISTICS_NAMING))
      setName("OZ47Plugin-Statistics-" + coreStationName + "-Thread");
    else if (statusDataType.equalsIgnoreCase(READINGS_NAMING))
     	setName("OZ47Plugin-Readings-" + coreStationName + "-Thread");
		
		return true;
	}



	@Override
	public byte getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.OZ47_MESSAGE_TYPE;
	}

  @Override
	public String getPluginName() {
        if (statusDataType.equalsIgnoreCase(STATISTICS_NAMING))
        	return "OZ47Plugin-Statistics";
        else if (statusDataType.equalsIgnoreCase(READINGS_NAMING))
        	return "OZ47Plugin-Readings";
        else
        	return "OZ47Plugin";
	}

	@Override
	public DataField[] getOutputFormat() {
    return statusNamingTable.get(statusDataType).dataField;
	}

  public static String replaceCharAt(String s, int pos, char c) {
    return s.substring(0,pos) + c + s.substring(pos+1);
  }
  
  public static double  hex2double(long valueHex) {
	
	  double exp = (double)((valueHex >>23 & 255)-127);
	  double sign = (double)(valueHex>>31);

	  if (sign == 0)
		  sign = 1;

  	double man = ((double)1.0f + (double)((valueHex & 8388607)/(double)8388608));
  	double expVal = Math.pow(2.0,exp);
  	double ans = sign * expVal * man;
  	return ans;

  }

	@Override
	public boolean messageReceived(int deviceId, long timestamp, Serializable[] data) {

		logger.debug("message received from CoreStation with DeviceId: " + deviceId);
		
    if (data.length != 2 && data.length != 3) {
			logger.error("The message with timestamp >" + timestamp + "< seems unparsable.(length: " + data.length + ")");
      return true;
		}

    try {
    
      // Convert the coded values in the string to hex values
      String str = new String((String)data[1]);
      for (int i = 0; i < str.length(); i++) {
        char c = str.charAt(i);
        if (c > 57 && c < 64) {
          char r = (char)((int)c + 7);	
          str = replaceCharAt(str,i,r);
        }
      }

      short msgType = toShort(data[0]);
      
      int sensor_id = -1;
      if (data.length == 3)
        sensor_id = toInteger(data[2]);
      
			if (msgType == statusNamingTable.get(statusDataType).typeNumber) {
				if (statusDataType.equalsIgnoreCase(STATISTICS_NAMING)) {
				
          int lotNo = Integer.parseInt(str.substring(4,6),16);
          int cellNo = Integer.parseInt(str.substring(6,8),16);
          int calibWeek = Integer.parseInt(str.substring(8,10),16);
          int calibYear = Integer.parseInt(str.substring(10,12),16);
          int gTimerPreHeat = Integer.parseInt(str.substring(17,19),16);
          int gAutoCalib = Integer.parseInt(str.substring(19,21),16);
          int gTimerDelay = Integer.parseInt(str.substring(30,32),16);
          int gTimerCycle = Integer.parseInt(str.substring(32,36),16);
          int gTimerPulse = Integer.parseInt(str.substring(36,38),16);
          int offsetMem = Integer.parseInt(str.substring(43,45),16) - 127;
          int offsetVerif = Integer.parseInt(str.substring(45,47),16) - 127;
          int gOffset = Integer.parseInt(str.substring(56,58),16) - 127;
          
          long v = Long.parseLong(str.substring(69,77),16);
          double x0_1 = hex2double(v);
          v = Long.parseLong(str.substring(82,90),16);
          double x1_1 = hex2double(v);
          v = Long.parseLong(str.substring(95,103),16);
          double x2_1 = hex2double(v);
          v = Long.parseLong(str.substring(108,116),16);
          double x3_1 = hex2double(v);
          v = Long.parseLong(str.substring(121,129),16);
          double kt_1 = hex2double(v);
          v = Long.parseLong(str.substring(134,142),16);
          double x0_2 = hex2double(v);
          v = Long.parseLong(str.substring(147,155),16);
          double x1_2 = hex2double(v);
          v = Long.parseLong(str.substring(160,168),16);
          double x2_2 = hex2double(v);
          v = Long.parseLong(str.substring(173,181),16);
          double x3_2 = hex2double(v);
          v = Long.parseLong(str.substring(186,194),16);
          double kt_2 = hex2double(v);
          
          if( dataProcessed(System.currentTimeMillis(), new Serializable[] {timestamp, timestamp, deviceId, lotNo, cellNo, calibWeek, calibYear, gTimerPreHeat, gAutoCalib, gTimerDelay, gTimerCycle, gTimerPulse, offsetMem, offsetVerif, gOffset, x0_1, x1_1, x2_1, x3_1, kt_1, x0_2, x1_2, x2_2, x3_2, kt_2, sensor_id}) )
            ackMessage(timestamp, super.priority);
          else
            logger.warn("The OZ47 statistics message with timestamp >" + timestamp + "< could not be stored in the database.");
         
        }
				else if (statusDataType.equalsIgnoreCase(READINGS_NAMING)) {
				
          // split string:
          // pos 2-3: sensor 1
          // pos 4-5: sensor 2
          // pos 6-8: resistance 1
          // pos 9-11: resistance 2
          // pos 12-14: temp
          // pos 15-16: humidity
          int s1= Integer.parseInt(str.substring(2,4),16);
          int s2= Integer.parseInt(str.substring(4,6),16);
          int r1= Integer.parseInt(str.substring(6,9),16);
          int r2= Integer.parseInt(str.substring(9,12),16);
          int _t= Integer.parseInt(str.substring(12,15),16);
          double t = _t/10-40;
          int h= Integer.parseInt(str.substring(15,17),16);
         
          //logger.info("OZ47 readings: " + s1 + " " + s2 + " " + r1 + " " + r2 + " " + t + " " + h );

          if( dataProcessed(System.currentTimeMillis(), new Serializable[] {timestamp, timestamp, deviceId, s1, s2, r1, r2, t, h, sensor_id}) )
            ackMessage(timestamp, super.priority);
          else
            logger.warn("The OZ47 readings message with timestamp >" + timestamp + "< could not be stored in the database.");       
            
        }
        else {
          logger.warn("Wrong OZ47 data type spedified, drop message.");
          ackMessage(timestamp, super.priority);
          return true;
        }


      }
    }
    catch (Exception e) {
      logger.warn("OZ47 data could not be parsed.");
			logger.warn(e.getMessage(), e);

		  //if( dataProcessed(System.currentTimeMillis(), new Serializable[] {timestamp, timestamp, deviceId, null, null, null, null, null, null}) )
			//  ackMessage(timestamp, super.priority);
		  //else
			logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database, drop message.");
      ackMessage(timestamp, super.priority);
			return true;
		}

		return true;
	}
}
