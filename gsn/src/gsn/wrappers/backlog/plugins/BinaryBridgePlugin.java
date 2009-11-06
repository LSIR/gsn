package gsn.wrappers.backlog.plugins;

import org.apache.log4j.Logger;
import java.io.Serializable;

import gsn.beans.DataField;
import gsn.wrappers.BackLogWrapper;


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

	// mandatory
	private static final String BINARY_LISTENER_ID = "listener-id";
	

	private final transient Logger logger = Logger.getLogger( BinaryBridgePlugin.class );
	private final DataField[] dataField = {new DataField("TIMESTAMP", "BIGINT"), new DataField( "DATA" , "binary" )};
	
	@Override
	public boolean initialize ( BackLogWrapper backLogWrapper ) {
		super.initialize(backLogWrapper);
		if ( getActiveAddressBean().getPredicateValue( BINARY_LISTENER_ID ) == null ) {
			logger.error("Loading the PSBackLog wrapper failed due to missing *" + BINARY_LISTENER_ID + "* parameter.");
			return false;
	    }
		return true;
	}

	@Override
	public byte getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.BINARY_MESSAGE_TYPE;
	}

	@Override
	public DataField[] getOutputFormat() {
		return dataField;
	}

	@Override
	public int packetReceived(long timestamp, byte[] packet) {
		if( arr2int(packet, 0) == getActiveAddressBean().getPredicateValueAsInt( BINARY_LISTENER_ID , 0) ) {
			Serializable[] data = {timestamp, java.util.Arrays.copyOfRange(packet, 4, packet.length)};
			if(dataProcessed(System.currentTimeMillis(), data))
				ackMessage(timestamp);
			else
				logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");
		}
		return PACKET_PROCESSED;
	}
	
	
	private static int arr2int (byte[] arr, int start) {
		int i = 0;
		int len = 4;
		int cnt = 0;
		byte[] tmp = new byte[len];
		for (i = start; i < (start + len); i++) {
			tmp[cnt] = arr[i];
			cnt++;
		}
		int accum = 0;
		i = 0;
		for ( int shiftBy = 0; shiftBy < 32; shiftBy += 8 ) {
			accum |= ( (int)( tmp[i] & 0xff ) ) << shiftBy;
			i++;
		}
		return accum;
	}
}
