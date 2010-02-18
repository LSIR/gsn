package gsn.wrappers.backlog;

import java.io.IOException;


/**
 * Defines a backlog message, used by the protocol communicated 
 * between the Python backlog program running at the deployment and
 * the {@link DeploymentClient} on side of GSN. It offers the
 * functionality to access the message format.
 * <p>
 * The binary message has the following format:
 * <ul><table border="1">
 * 	<tr>
 *   <th colspan="2">header</th>
 *   <th rowspan="2">payload</th>
 *  </tr>
 * 	<tr>
 *   <th>type</th>
 *   <th>timestamp</th>
 *  </tr>
 *  <tr>
 *   <td>1 byte</td>
 *   <td>8 bytes</td>
 *   <td>maximum MAX_PAYLOAD_SIZE bytes</td>
 *  </tr>
 * </table></ul>
 * 
 * There are three predefined message types:
 * <ul>
 * <li>ACK_MESSAGE_TYPE = 1: The acknowledge message type.
 *  This message type is used to acknowledge data messages.</li>
 * <li>PING_MESSAGE_TYPE = 2: The ping message type. This message
 *  type is used to ping a deployment, thus, requesting a
 *  PING_ACK_MESSAGE_TYPE.</li>
 * <li>PING_ACK_MESSAGE_TYPE = 3: The ping acknowledge message type.
 *  This message type is used to acknowledge a ping request.</li>
 * </ul>
 * 
 * Each plugin type used by {@link gsn.wrappers.BackLogWrapper BackLogWrapper} must
 * specify a unique message type, which has to be named here:
 * <ul>
 * <li>BACKLOG_COMMAND_MESSAGE_TYPE = 10: The backlog command message type.
 *  This message type is used to send/receive backlog command messages by the
 *  {@link gsn.wrappers.backlog.plugins.BackLogCommandPlugin BackLogCommandPlugin}.</li>
 * <li>TOS_MESSAGE_TYPE = 11: The TOS message type. This message
 *  type is used to send/receive TOS messages by the
 *  {@link gsn.wrappers.backlog.plugins.MigMessagePlugin MigMessagePlugin}.</li>
 * </ul>
 * 
 * @author	Tonio Gsell
 * 
 * @see gsn.wrappers.BackLogWrapper BackLogWrapper
 */
public class BackLogMessage {
	
	
	
	/* ##############################################################
	 * #                                                            #
	 * #        Add new message types for additional plugins.       #
	 */
	
	/** The backlog command message type. This message type is used to
	 *  send/receive backlog command messages by the
	 *  {@link gsn.wrappers.backlog.plugins.BackLogStatusPlugin BackLogStatusPlugin}. */
	public static final byte BACKLOG_STATUS_MESSAGE_TYPE = 10;

	/** The basestation status message type. This message type is used to
     *  receive basestation status messages by the
	 *  {@link gsn.wrappers.backlog.plugins.BasestationStatusPlugin BasestationStatusPlugin}. */
	public static final byte BASESTATION_STATUS_MESSAGE_TYPE = 11;

	/** The TOS message type. This message type is used to
	 *  send/receive TOS messages by the
	 *  {@link gsn.wrappers.backlog.plugins.MigMessagePlugin MigMessagePlugin}. */
	public static final byte TOS_MESSAGE_TYPE = 20;
	public static final byte TOS1x_MESSAGE_TYPE = 21;

	/** The binary message type. This message type is used to
     *  receive any binary data by the
	 *  {@link gsn.wrappers.backlog.plugins.BinaryBridgePlugin BinaryBridgePlugin}. */
	public static final byte BINARY_MESSAGE_TYPE = 30;
	public static final byte BIG_BINARY_MESSAGE_TYPE = 31;

	/** The Vaisala WXT520 message type. This message type is used to
     *  receive Vaisala WXT520 weather station data.
	 *  {@link gsn.wrappers.backlog.plugins.BinaryBridgePlugin BinaryBridgePlugin}. */
	public static final byte VAISALA_WXT520_MESSAGE_TYPE = 40;
	
	/* #                                                            #
	 * #                                                            #
	 * ############################################################## */

	
	
	
	
	/** The acknowledge message type. This message type is used to
	 *  acknowledge data messages. */
	public static final byte ACK_MESSAGE_TYPE = 1;
	/** The ping message type. This message type is used to
	 *  ping a deployment, thus, requesting a PING_ACK_MESSAGE_TYPE. */
	public static final byte PING_MESSAGE_TYPE = 2;
	/** The ping acknowledge message type. This message type is used to
	 *  acknowledge a ping request. */
	public static final byte PING_ACK_MESSAGE_TYPE = 3;
	

	/** 
	 * The maximum supported payload size (2^32-9bytes). This is due to
	 * the sending mechanism. A sent message is defined by preceding
	 * four bytes containing the message size. A message consists of
	 * the type field (1byte), the timestamp (8byte) and the payload
	 * (2^32-9bytes).
	 **/
	public static double MAX_PAYLOAD_SIZE = Math.pow(2, 32) - 9;
	
	private byte[] payload = null;
	private long timestamp = 0;
	private byte type = 0;
	
	
	/** 
	 * Class constructor specifying the message type.
	 * 
	 * @param type of the message
	 */
	public BackLogMessage(byte type) {
		this.type = type;
	}
	
	
	/** 
	 * Class constructor specifying the message type and
	 * the timestamp.
	 * 
	 * @param type of the message
	 * @param timestamp in milliseconds
	 */
	public BackLogMessage(byte type, long timestamp) {
		this.type = type;
		this.timestamp = timestamp;
	}
	
	
	/** 
	 * Class constructor specifying the message type, the timestamp
	 * and the payload.
	 * 
	 * @param type of the message
	 * @param timestamp in milliseconds
	 * @param payload of the message. Should not be
	 * 			bigger than MAX_PAYLOAD_SIZE.
	 * 
	 * @throws IOException if the payload length exceeds MAX_PAYLOAD_SIZE
	 */
	public BackLogMessage(byte type, long timestamp, byte[] payload) throws IOException {
		if( payload.length > MAX_PAYLOAD_SIZE )
			throw new IOException("The payload exceeds the maximum size of " + MAX_PAYLOAD_SIZE + "bits.");
		this.type = type;
		this.timestamp = timestamp;
		this.payload = payload;
	}
	
	
	/** 
	 * Class constructor specifying the message.
	 * 
	 * @param message as byte array.
	 * @throws IOException if the message length exceeds MAX_PAYLOAD_SIZE+9
	 */
	public BackLogMessage(byte[] message) throws IOException {
		if( message.length > MAX_PAYLOAD_SIZE+9 )
			throw new IOException("The message exceeds the maximum size of " + MAX_PAYLOAD_SIZE+9 + "bits.");
		type = message[0];
		timestamp = arr2long(message, 1);
		payload = java.util.Arrays.copyOfRange(message, 9, message.length);
	}
	
	
	/** 
	 * Get the message as byte array.
	 * 
	 * @return the message as byte array
	 */
	public byte[] getMessage() {
		byte[] message;
		if( payload == null )
			message = new byte [8 + 1];
		else
			message = new byte [payload.length + 8 + 1];
		message[0] = type;
		System.arraycopy(long2arr(timestamp), 0, message, 1, 8);
		if( payload != null )
			System.arraycopy(payload, 0, message, 9, payload.length);
		return message;
	}
	
	
	/** 
	 * Get the message type.
	 * 
	 * @return the type of the message
	 */
	public byte getType() {
		return type;
	}
	
	
	/** 
	 * Set the message type.
	 * 
	 * @param type of the message
	 */
	public void setType(byte type) {
		this.type = type;
	}
	
	
	/** 
	 * Get the timestamp of the message.
	 * 
	 * @return the timestamp of the message
	 */
	public long getTimestamp() {
		return timestamp;
	}
	
	
	/** 
	 * Set the timestamp of the message.
	 * 
	 * @param timestamp of the message
	 */
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	
	/** 
	 * Get the payload of the message as byte array.
	 * 
	 * @return the payload as byte array
	 */
	public byte[] getPayload() {
		return payload;
	}
	
	
	/** 
	 * Set the payload of the message.
	 * 
	 * @param payload of the message. Should not be
	 * 			bigger than MAX_PAYLOAD_SIZE.
	 * 
	 * @throws IOException if the payload length exceeds MAX_PAYLOAD_SIZE
	 */
	public void setPayload(byte[] payload) throws IOException {
		if( payload.length > MAX_PAYLOAD_SIZE )
			throw new IOException("The payload exceeds the maximum size of " + MAX_PAYLOAD_SIZE + "bits.");
		this.payload = payload;
	}
	
	
	/** 
	 * Clones this message.
	 * 
	 * @return the cloned message
	 */
	public BackLogMessage clone() {
		try {
			return new BackLogMessage(this.getMessage());
		} catch (IOException e) { return null; }
	}

	
	private static long arr2long (byte[] arr, int start) {
		int i = 0;
		int len = 8;
		int cnt = 0;
		byte[] tmp = new byte[len];
		for (i = start; i < (start + len); i++) {
			tmp[cnt] = arr[i];
			cnt++;
		}
		long accum = 0;
		i = 0;
		for ( int shiftBy = 0; shiftBy < 64; shiftBy += 8 ) {
			accum |= ( (long)( tmp[i] & 0xff ) ) << shiftBy;
			i++;
		}
		return accum;
	}

	
	private static byte[] long2arr (long l) {
		int len = 8;
		byte[] arr = new byte[len];

		int i = 0;
		for ( int shiftBy = 0; shiftBy < 64; shiftBy += 8 ) {
			arr[i] = (byte)( l >> shiftBy);
			i++;
		}
		return arr;
	}
}
