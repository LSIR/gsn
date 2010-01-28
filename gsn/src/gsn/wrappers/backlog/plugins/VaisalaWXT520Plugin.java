package gsn.wrappers.backlog.plugins;

import java.io.Serializable;

import org.apache.log4j.Logger;

import gsn.beans.DataField;
import gsn.wrappers.BackLogWrapper;


/**
 * This plugin offers the functionality to send commands to the Backlog Python program
 * on the deployment side. It also listens for incoming BackLogStatus messages.
 * <p>
 * Any new command pointed directly to the Backlog Python program should be implemented
 * in this class.
 * 
 * @author Tonio Gsell
 */
public class VaisalaWXT520Plugin extends AbstractPlugin {

	private DataField[] dataField = {	new DataField("TIMESTAMP", "BIGINT"), 
						new DataField("Wu", "VARCHAR(100)"),
						new DataField("Tu", "VARCHAR(100)"),
						new DataField("Ru", "VARCHAR(100)"),
						new DataField("Su", "VARCHAR(100)")};

	private final transient Logger logger = Logger.getLogger( VaisalaWXT520Plugin.class );
	
	@Override
	public boolean initialize(BackLogWrapper backLogWrapper) {
		super.initialize(backLogWrapper);
		return true;
	}

	@Override
	public byte getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.VAISALA_WXT520_MESSAGE_TYPE;
	}

	@Override
	public DataField[] getOutputFormat() {
		return dataField;
	}

	@Override
	public int packetReceived(long timestamp, byte[] packet) {
		Serializable[] data = new Serializable[dataField.length];

		data[0] = timestamp;

		int len;
		int count = 0;
		int start_index = 0;
		for (int i = 0; i<packet.length; i++) {
			if (count >= dataField.length) {
				break;
			}
			if (packet[i] == 0) {
				len = i - start_index;
				if ((len == 0) || (count == 0 && len > 30) || (count != 0 && len > 10)) break;
				count++;
				data[count] = new String(packet, start_index, len);
				start_index = i+1;
			}
		}

		if (count != (dataField.length - 1)) {
			logger.warn("The message with timestamp >" + timestamp + "< seems unparsable.");
			return PACKET_PROCESSED;
		}
		
		if( dataProcessed(System.currentTimeMillis(), data) )
			ackMessage(timestamp);
		else
			logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");

		return PACKET_PROCESSED;
	}
}
