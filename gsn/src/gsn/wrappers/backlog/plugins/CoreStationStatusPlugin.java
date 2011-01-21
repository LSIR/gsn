package gsn.wrappers.backlog.plugins;

import java.io.Serializable;

import org.apache.log4j.Logger;

import gsn.beans.DataField;


/**
 * This plugin reads incoming CoreStationStatus messages and interpretes them properly.
 * <p>
 * Any new status information coming directly from the CoreStation should be implemented
 * in this class.
 * 
 * @author Tonio Gsell
 */
public class CoreStationStatusPlugin extends AbstractPlugin {
	
	private DataField[] dataField = {	new DataField("TIMESTAMP", "BIGINT"),
						new DataField("GENERATION_TIME", "BIGINT"),
						new DataField("DEVICE_ID", "INTEGER"),
						new DataField("V_EXT2", "INTEGER"),
						new DataField("V_EXT1", "INTEGER"),
						new DataField("V_EXT3", "INTEGER"),
						new DataField("I_V12DC_EXT", "INTEGER"),
						new DataField("V12DC_IN", "INTEGER"),
						new DataField("I_V12DC_IN", "INTEGER"),
						new DataField("VCC_5_0", "INTEGER"),
						new DataField("VCC_NODE", "INTEGER"),
						new DataField("I_VCC_NODE", "INTEGER"),
						new DataField("VCC_4_2", "INTEGER")};

	private final transient Logger logger = Logger.getLogger( CoreStationStatusPlugin.class );

	@Override
	public byte getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.CORESTATION_STATUS_MESSAGE_TYPE;
	}

	@Override
	public DataField[] getOutputFormat() {
		return dataField;
	}


	@Override
	public String getPluginName() {
		return "CoreStationStatusPlugin";
	}

	@Override
	public boolean messageReceived(int deviceId, long timestamp, Serializable[] data) {
		if(data.length == dataField.length-3) {
			for (int index=0; index<data.length; index++) {
				try {
					data[index] = toInteger(data[index]);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					return true;
				}
			}
		}
		else
			logger.error("received message length does not match");

		Serializable[] header = {timestamp, timestamp, deviceId};
		
		if( dataProcessed(System.currentTimeMillis(), concat(header, data)) )
			ackMessage(timestamp, super.priority);
		else
			logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");
		return true;
	}
}
