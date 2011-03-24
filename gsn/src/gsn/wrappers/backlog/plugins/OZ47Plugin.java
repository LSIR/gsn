package gsn.wrappers.backlog.plugins;

import java.io.Serializable;

import org.apache.log4j.Logger;

import gsn.beans.DataField;


public class OZ47Plugin extends AbstractPlugin {

	private DataField[] dataField = {	new DataField("TIMESTAMP", "BIGINT"),
						new DataField("GENERATION_TIME", "BIGINT"),
						new DataField("DEVICE_ID", "INTEGER"),
						new DataField("OZONE_SENSOR_1", "INTEGER"),
						new DataField("OZONE_SENSOR_2", "INTEGER"),
						new DataField("RESISTANCE_1", "INTEGER"),
						new DataField("RESISTANCE_2", "INTEGER"),
						new DataField("TEMPERATURE", "DOUBLE"),
						new DataField("HUMIDITY", "INTEGER")
  };

	private final transient Logger logger = Logger.getLogger( OZ47Plugin.class );

	@Override
	public byte getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.OZ47_MESSAGE_TYPE;
	}


	@Override
	public String getPluginName() {
		return "OZ47Plugin";
	}

	@Override
	public DataField[] getOutputFormat() {
		return dataField;
	}

  public static String replaceCharAt(String s, int pos, char c) {
    return s.substring(0,pos) + c + s.substring(pos+1);
  }

	@Override
	public boolean messageReceived(int deviceId, long timestamp, Serializable[] data) {

		logger.debug("message received from CoreStation with DeviceId: " + deviceId);
		
    if (data.length != 1) {
			logger.error("The message with timestamp >" + timestamp + "< seems unparsable.(length: " + data.length + ")");
      return true;
		}

    try {
    
      // Convert the coded values in the string to hex values
      String str = new String((String)data[0]);
      for (int i = 0; i < str.length(); i++) {
        char c = str.charAt(i);
        if (c > 57 && c < 64) {
          char r = (char)((int)c + 7);	
          str = replaceCharAt(str,i,r);
        }
      }
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
      
      if( dataProcessed(System.currentTimeMillis(), new Serializable[] {timestamp, timestamp, deviceId, s1, s2, r1, r2, t, h}) )
        ackMessage(timestamp, super.priority);
      //if( dataProcessed(System.currentTimeMillis(), concat(new Serializable[] {timestamp, timestamp, deviceId}, data)) )
      //	ackMessage(timestamp, super.priority);
      else
        logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");

    } catch (Exception e) {
      logger.warn("OZ47 data could not be parsed.");
			//logger.warn(e.getMessage(), e);


		    if( dataProcessed(System.currentTimeMillis(), new Serializable[] {timestamp, timestamp, deviceId, null, null, null, null, null, null}) )
			    ackMessage(timestamp, super.priority);
		    else
			    logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");
			return true;
		}




		return true;
	}
}
