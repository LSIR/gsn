package gsn.wrappers.backlog.plugins;

import java.io.Serializable;

import org.apache.log4j.Logger;

import gsn.beans.DataField;


public class VaisalaWXT520Plugin extends AbstractPlugin {

	private DataField[] dataField = {	new DataField("TIMESTAMP", "BIGINT"),
						new DataField("GENERATION_TIME", "BIGINT"),
						new DataField("DEVICE_ID", "INTEGER"),
						new DataField("Wu", "VARCHAR(100)"),
						new DataField("Tu", "VARCHAR(100)"),
						new DataField("Ru", "VARCHAR(100)"),
						new DataField("Su", "VARCHAR(100)")};

	private final transient Logger logger = Logger.getLogger( VaisalaWXT520Plugin.class );

	@Override
	public short getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.VAISALA_WXT520_MESSAGE_TYPE;
	}


	@Override
	public String getPluginName() {
		return "VaisalaWXT520Plugin";
	}

	@Override
	public DataField[] getOutputFormat() {
		return dataField;
	}

	@Override
	public boolean messageReceived(int deviceId, long timestamp, Serializable[] data) {

		if (data.length != (dataField.length - 3)) {
			logger.error("The message with timestamp >" + timestamp + "< seems unparsable.");
			return true;
		}
		
		if( dataProcessed(System.currentTimeMillis(), concat(new Serializable[] {timestamp, timestamp, deviceId}, data)) )
			ackMessage(timestamp, super.priority);
		else
			logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");

		return true;
	}
}
