package gsn.wrappers.backlog.plugins;

import java.io.Serializable;

import org.apache.log4j.Logger;

import gsn.beans.DataField;
import gsn.wrappers.BackLogWrapper;


public class B4SensorPlugin extends AbstractPlugin {

	private DataField[] dataField = {	new DataField("TIMESTAMP", "BIGINT"),
						new DataField("GENERATION_TIME", "BIGINT"),
						new DataField("DEVICE_ID", "INTEGER"),
						new DataField("UTC_RAW_DATA", "VARCHAR(32)"),
						new DataField("LAT_RAW_DATA", "VARCHAR(32)"),
						new DataField("LAT_N_RAW_DATA", "VARCHAR(32)"),
						new DataField("LONG_RAW_DATA", "VARCHAR(32)"),
						new DataField("LONG_N_RAW_DATA", "VARCHAR(32)"),
						new DataField("QUAL_RAW_DATA", "VARCHAR(32)"),
						new DataField("SAT_RAW_DATA", "VARCHAR(32)"),
						new DataField("HDOP_RAW_DATA", "VARCHAR(32)"),
						new DataField("GEO_HEIGHT_RAW_DATA", "VARCHAR(32)"),
						new DataField("GEO_HEIGHT_U_RAW_DATA", "VARCHAR(32)"),
						new DataField("GEO_SEP_RAW_DATA", "VARCHAR(32)"),
						new DataField("GEO_SEP_U_RAW_DATA", "VARCHAR(32)"),
						new DataField("ADC_CHANNEL_01_MV", "DOUBLE"),
						new DataField("ADC_CHANNEL_01_PPM", "DOUBLE"),
						new DataField("ADC_CHANNEL_02_MV", "DOUBLE")};

	private final transient Logger logger = Logger.getLogger( B4SensorPlugin.class );
	
	private int consecutiveErrors;

	@Override
	public short getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.B4SENSOR_MESSAGE_TYPE;
	}


	@Override
	public String getPluginName() {
		return "B4SensorPlugin";
	}

	@Override
	public DataField[] getOutputFormat() {
		return dataField;
	}
	
	@Override
  public boolean initialize ( BackLogWrapper backlogwrapper, String coreStationName, String deploymentName) {
    super.initialize(backlogwrapper, coreStationName, deploymentName);        
    return true;
  }

	@Override
	public boolean messageReceived(int deviceId, long timestamp, Serializable[] data) {

	
	if (data.length < 3 || data.length > 17)
	{
		logger.error("message (last timestamp >" + timestamp + "<) could not be parsed. (length: " + data.length + ")");
        ackMessage(timestamp, super.priority);
	} else 
	{		 
		 if (data.length == 3) 
		{
		    if( dataProcessed(System.currentTimeMillis(), new Serializable[] {timestamp, timestamp, deviceId, null, null, null, null, null, null, null, null, null, null, null, null, data[0], data[1], data[2]}) )
		      ackMessage(timestamp, super.priority);
		    else
		      logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");
		} else if (data.length == 17) 
		{		
        if( dataProcessed(System.currentTimeMillis(), new Serializable[] {timestamp, timestamp, deviceId, data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7], data[8], data[9], data[10], data[11], data[14], data[15], data[16]}) )
          ackMessage(timestamp, super.priority);
        else
          logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");
      	}
	}
		
		return true;
	}
}
