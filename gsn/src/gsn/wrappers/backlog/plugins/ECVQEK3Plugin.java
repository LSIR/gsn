package gsn.wrappers.backlog.plugins;

import java.io.Serializable;

import org.apache.log4j.Logger;

import gsn.beans.DataField;


public class ECVQEK3Plugin extends AbstractPlugin {

	private DataField[] dataField = {	new DataField("TIMESTAMP", "BIGINT"),
						new DataField("GENERATION_TIME", "BIGINT"),
						new DataField("DEVICE_ID", "INTEGER"),
						new DataField("RAW_DATA", "VARCHAR(256)"),
						new DataField("MEASUREMENT_ID", "BIGINT")};

	private final transient Logger logger = Logger.getLogger( ECVQEK3Plugin.class );

	@Override
	public short getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.ECVQEK3_MESSAGE_TYPE;
	}


	@Override
	public String getPluginName() {
		return "ECVQEK3Plugin";
	}

	@Override
	public DataField[] getOutputFormat() {
		return dataField;
	}

	@Override
	public boolean messageReceived(int deviceId, long timestamp, Serializable[] data) {

	  long measurement_id = -1;
	  
		if (data.length < 1 || data.length > 2) {
			logger.error("The message with timestamp >" + timestamp + "< seems unparsable.(length: " + data.length + ")");
			ackMessage(timestamp, super.priority);
			return true;
		}

		if (data.length == 1) {
  		if( dataProcessed(System.currentTimeMillis(), new Serializable[] {timestamp, timestamp, deviceId, data[0], measurement_id}) )
  			ackMessage(timestamp, super.priority);
  		else
  			logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");
		}
		else if (data.length == 2) {
      if( dataProcessed(System.currentTimeMillis(), new Serializable[] {timestamp, timestamp, deviceId, data[0], data[1]}) )
        ackMessage(timestamp, super.priority);
      else
        logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");
    }

		return true;
	}
}
