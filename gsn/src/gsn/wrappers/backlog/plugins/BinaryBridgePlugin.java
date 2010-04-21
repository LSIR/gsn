package gsn.wrappers.backlog.plugins;

import org.apache.log4j.Logger;
import java.io.Serializable;

import gsn.beans.DataField;


/**
 * This plugin forwards (bridges) the incoming binary data to the connected virtual sensor,
 * e.g. to forward an image (jpeg,...). The first four bytes in the received packet are used
 * to specify the origin of the message, e.g. image from webcam xy.
 * <p>
 * The SQL column name is DATA. Thus, queries in the virtual sensor have to be pointed to the DATA
 * column for further processing the binary data.
 * 
 * @author Tonio Gsell
 */
public class BinaryBridgePlugin extends AbstractPlugin {

	private final transient Logger logger = Logger.getLogger( BinaryBridgePlugin.class );
	
	private final DataField[] dataField = new DataField[] {new DataField("TIMESTAMP", "BIGINT"),
															new DataField("CORE_STATION_ID", "INTEGER"),
										  				   	new DataField("FILEPATH", "VARCHAR(255)"),
										  				   	new DataField("DATA", "binary")};

	@Override
	public byte getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.BINARY_MESSAGE_TYPE;
	}

	@Override
	public DataField[] getOutputFormat() {
		return dataField;
	}


	@Override
	public String getPluginName() {
		return "BinaryBridgePlugin";
	}

	@Override
	public boolean messageReceived(int coreStationId, long timestamp, byte[] packet) {
		StringBuffer sb = new StringBuffer();
		int i;
		for (i = 0; i < packet.length; i++) {
			if (packet[i] == 0) break;
			sb.append((char) packet[i]);
		}
		// find index of first null byte
		Serializable[] data = {timestamp, coreStationId, sb.toString(), java.util.Arrays.copyOfRange(packet, i+1, packet.length)};
		if(dataProcessed(System.currentTimeMillis(), data)) {
			ackMessage(timestamp);
			return true;
		} else {
			logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");
			return false;
		}
	}
}
