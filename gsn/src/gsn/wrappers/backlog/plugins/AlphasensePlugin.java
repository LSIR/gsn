package gsn.wrappers.backlog.plugins;

import java.io.Serializable;

import org.apache.log4j.Logger;

import gsn.beans.DataField;


public class AlphasensePlugin extends AbstractPlugin {

	private static DataField[] dataField = {
		  new DataField("TIMESTAMP", "BIGINT"),
		  new DataField("GENERATION_TIME", "BIGINT"),
		  new DataField("DEVICE_ID", "INTEGER"),
		  new DataField("MESSAGE_TYPE", "SMALLINT"),
		  new DataField("RAW_DATA", "VARCHAR(512)"),
		  new DataField("MEASUREMENT_ID", "BIGINT")};

	private final transient Logger logger = Logger.getLogger( AlphasensePlugin.class );

	@Override
	public short getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.ALPHASENSE_MESSAGE_TYPE;
	}

	@Override
	public String getPluginName() {
        return "AlphasensePlugin";
	}

	@Override
	public DataField[] getOutputFormat() {
    return dataField;
	}

	@Override
	public boolean messageReceived(int deviceId, long timestamp, Serializable[] data) {
		logger.debug("message received from CoreStation with DeviceId: " + deviceId);
		
		if (data.length < 2 || data.length > 3) {
			logger.error("The message with timestamp >" + timestamp + "< seems unparsable.(length: " + data.length + ")");
			ackMessage(timestamp, super.priority);
			return true;
		}

		try {
		  long measurement_id = -1;
		  if (data.length == 3) measurement_id = toLong(data[2]);
		  
			if( dataProcessed(System.currentTimeMillis(), new Serializable[] {timestamp, timestamp, deviceId, toShort(data[0]), data[1], measurement_id}) )
				ackMessage(timestamp, super.priority);
			else
				logger.warn("Alphasense message with timestamp >" + timestamp + "< could not be stored in the database."); 
		} catch (Exception e) {
			logger.warn("Alphasense message could not be parsed: " + e.getMessage(), e);
			ackMessage(timestamp, super.priority);
			return true;
		}
		return true;
	}
}
