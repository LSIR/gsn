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
		  new DataField("RAW_DATA", "VARCHAR(512)")};

	private final transient Logger logger = Logger.getLogger( AlphasensePlugin.class );

	@Override
	public byte getMessageType() {
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
		
		if (data.length != 2) {
			logger.error("The message with timestamp >" + timestamp + "< seems unparsable.(length: " + data.length + ")");
			ackMessage(timestamp, super.priority);
			return true;
		}

		try {
			if( dataProcessed(System.currentTimeMillis(), new Serializable[] {timestamp, timestamp, deviceId, toShort(data[0]), data[1]}) )
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
