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
						new DataField("Sn", "VARCHAR(30)"),
						new DataField("Wu_Dn", "VARCHAR(10)"),
						new DataField("Wu_Dm", "VARCHAR(10)"),
						new DataField("Wu_Dx", "VARCHAR(10)"),
						new DataField("Wu_Sn", "VARCHAR(10)"),
						new DataField("Wu_Sm", "VARCHAR(10)"),
						new DataField("Wu_Sx", "VARCHAR(10)"),
						new DataField("Tu_Ta", "VARCHAR(10)"),
						new DataField("Tu_Tp", "VARCHAR(10)"),
						new DataField("Tu_Ua", "VARCHAR(10)"),
						new DataField("Tu_Pa", "VARCHAR(10)"),
						new DataField("Ru_Rc", "VARCHAR(10)"),
						new DataField("Ru_Rd", "VARCHAR(10)"),
						new DataField("Ru_Ri", "VARCHAR(10)"),
						new DataField("Ru_Hc", "VARCHAR(10)"),
						new DataField("Ru_Hd", "VARCHAR(10)"),
						new DataField("Ru_Hi", "VARCHAR(10)"),
						new DataField("Ru_Rp", "VARCHAR(10)"),
						new DataField("Ru_Hp", "VARCHAR(10)"),
						new DataField("Su_Th", "VARCHAR(10)"),
						new DataField("Su_Vh", "VARCHAR(10)"),
						new DataField("Su_Vs", "VARCHAR(10)"),
						new DataField("Su_Vr", "VARCHAR(10)")};

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
