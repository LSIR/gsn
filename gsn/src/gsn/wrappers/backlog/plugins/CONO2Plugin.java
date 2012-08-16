package gsn.wrappers.backlog.plugins;

import java.io.Serializable;
import org.apache.log4j.Logger;
import gsn.beans.DataField;


public class CONO2Plugin extends AbstractPlugin {

	private static DataField[] dataField = {
		  new DataField("TIMESTAMP", "BIGINT"),
		  new DataField("GENERATION_TIME", "BIGINT"),
		  new DataField("DEVICE_ID", "INTEGER"),
		  new DataField("RAW_DATA", "VARCHAR(256)"),
		  new DataField("MEASUREMENT_ID", "BIGINT")};

	private final transient Logger logger = Logger.getLogger( CONO2Plugin.class );

	@Override
	public short getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.CONO2_MESSAGE_TYPE;
	}

	@Override
	public String getPluginName() {
        return "CONO2Plugin";
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

			if( dataProcessed(System.currentTimeMillis(), new Serializable[] {timestamp, timestamp, deviceId, data[0], toLong(data[1])}) )
				ackMessage(timestamp, super.priority);
			else
				logger.warn("The CONO2 readings message with timestamp >" + timestamp + "< could not be stored in the database."); 
		} catch (Exception e) {
			logger.warn("CONO2 data could not be parsed: " + e.getMessage(), e);
			ackMessage(timestamp, super.priority);
			return true;
		}
		return true;
	}
}
