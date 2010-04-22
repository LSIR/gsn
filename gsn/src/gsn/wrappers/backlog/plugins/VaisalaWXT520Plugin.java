package gsn.wrappers.backlog.plugins;

import java.io.Serializable;

import org.apache.log4j.Logger;

import gsn.beans.DataField;


public class VaisalaWXT520Plugin extends AbstractPlugin {

	private DataField[] dataField = {	new DataField("TIMESTAMP", "BIGINT"),
						new DataField("CORE_STATION_ID", "INTEGER"),
						new DataField("Wu", "VARCHAR(100)"),
						new DataField("Tu", "VARCHAR(100)"),
						new DataField("Ru", "VARCHAR(100)"),
						new DataField("Su", "VARCHAR(100)")};

	private final transient Logger logger = Logger.getLogger( VaisalaWXT520Plugin.class );

	@Override
	public byte getMessageType() {
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
	public boolean messageReceived(int coreStationId, long timestamp, byte[] packet) {
		Serializable[] data = new Serializable[dataField.length];

		data[0] = timestamp;
		data[1] = coreStationId;

		int len;
		int count = 1;
		int start_index = 0;
		for (int i = 0; i<packet.length; i++) {
			if (packet[i] == 0) {
				len = i - start_index;
				if ((len == 0) || (len > 100)) break;
				count++;
				if (count >= dataField.length)
					break;
				data[count] = new String(packet, start_index, len);
				start_index = i+1;
			}
		}

		if (count != (dataField.length - 1)) {
			logger.error("The message with timestamp >" + timestamp + "< seems unparsable.");
			return true;
		}
		
		if( dataProcessed(System.currentTimeMillis(), data) )
			ackMessage(timestamp);
		else
			logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");

		return true;
	}
}
