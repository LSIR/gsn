package gsn.wrappers.backlog.plugins;

import java.io.Serializable;

import org.apache.log4j.Logger;

import gsn.beans.DataField;


/**
 * This plugin reads incoming BasestationStatus messages and interpretes them properly.
 * <p>
 * Any new status information coming directly from the basestation should be implemented
 * in this class.
 * 
 * @author Tonio Gsell
 */
public class BasestationStatusPlugin extends AbstractPlugin {
	
	private DataField[] dataField = {	new DataField("TIMESTAMP", "BIGINT"),
						new DataField("V_PV_MV", "INTEGER"),
						new DataField("I_PV_UA", "INTEGER"),
						new DataField("V_12BAT_MV", "INTEGER"),
						new DataField("I_V12DC_EXT_UA", "INTEGER"),
						new DataField("V12DC_IN_MV", "INTEGER"),
						new DataField("I_V12DC_IN_UA", "INTEGER"),
						new DataField("VCC_5_0_MV", "INTEGER"),
						new DataField("VCC_NODE_MV", "INTEGER"),
						new DataField("I_VCC_NODE_UA", "INTEGER"),
						new DataField("VCC_4_2_MV", "INTEGER")};

	private final transient Logger logger = Logger.getLogger( BasestationStatusPlugin.class );

	@Override
	public byte getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.BASESTATION_STATUS_MESSAGE_TYPE;
	}

	@Override
	public DataField[] getOutputFormat() {
		return dataField;
	}


	@Override
	public String getPluginName() {
		return "BasestationStatusPlugin";
	}

	@Override
	public int packetReceived(long timestamp, byte[] packet) {
		Serializable[] data = new Serializable[dataField.length];
		data[0] = timestamp;
		if(packet.length == 40) {
			for( int i=1; i<dataField.length; i++) {
				Integer tmp = arr2int(packet, (i-1)*4);
				if( tmp == 0xFFFFFFFF )
					tmp = null;
				data[i] = tmp;
			}
		}
		else
			logger.error("received message length does not match");
		
		if( dataProcessed(System.currentTimeMillis(), data) )
			ackMessage(timestamp);
		else
			logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");
		return PACKET_PROCESSED;
	}
}
