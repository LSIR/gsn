package gsn.wrappers.backlog.plugins;

import java.io.Serializable;

import org.apache.log4j.Logger;

import gsn.beans.DataField;


public class GPSNAVPlugin extends AbstractPlugin {

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
						new DataField("MEASUREMENT_ID", "BIGINT")};

	private final transient Logger logger = Logger.getLogger( GPSNAVPlugin.class );

	@Override
	public short getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.GPS_NAV_MESSAGE_TYPE;
	}


	@Override
	public String getPluginName() {
		return "GPSNAVPlugin";
	}

	@Override
	public DataField[] getOutputFormat() {
		return dataField;
	}

	@Override
	public boolean messageReceived(int deviceId, long timestamp, Serializable[] data) {

	  long measurement_id = -1;
	  
		if (data.length < 15 || data.length > 16) {
			logger.debug("The message with timestamp >" + timestamp + "< seems unparsable.");
			ackMessage(timestamp, super.priority);
		}
		else {
		  if (data.length == 15) {
		    if( dataProcessed(System.currentTimeMillis(), new Serializable[] {timestamp, timestamp, deviceId, data[1], data[2], data[3], data[4], data[5], data[6], data[7], data[8], data[9], data[10], data[11], data[12], measurement_id}) )
		      ackMessage(timestamp, super.priority);
		    else
		      logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");
		  }
		  else if (data.length == 16) {
        if( dataProcessed(System.currentTimeMillis(), new Serializable[] {timestamp, timestamp, deviceId, data[1], data[2], data[3], data[4], data[5], data[6], data[7], data[8], data[9], data[10], data[11], data[12], data[15]}) )
          ackMessage(timestamp, super.priority);
        else
          logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");
      }
		}
		
		return true;
	}
}
