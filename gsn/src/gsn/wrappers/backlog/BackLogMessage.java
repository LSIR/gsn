package gsn.wrappers.backlog;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;


/**
 * Defines a backlog message, used by the protocol communicated 
 * between the Python backlog program running on a CoreStation and
 * the {@link AsyncCoreStationClient} on side of GSN. It offers the
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

	/** The CoreStation status message type. This message type is used to
     *  receive CoreStation status messages by the
	 *  {@link gsn.wrappers.backlog.plugins.CoreStationStatusPlugin CoreStationStatusPlugin}. */
	public static final byte CORESTATION_STATUS_MESSAGE_TYPE = 11;

	/** The TOS message type. This message type is used to
	 *  send/receive TOS messages by the
	 *  {@link gsn.wrappers.backlog.plugins.MigMessagePlugin MigMessagePlugin}. */
	public static final byte TOS_MESSAGE_TYPE = 20;
	public static final byte TOS1x_MESSAGE_TYPE = 21;

	/** The binary message type. This message type is used to
     *  receive any binary data by the
	 *  {@link gsn.wrappers.backlog.plugins.BinaryPlugin BinaryPlugin}. */
	public static final byte BINARY_MESSAGE_TYPE = 30;

	/** The Vaisala WXT520 message type. This message type is used to
     *  receive Vaisala WXT520 weather station data.
	 *  {@link gsn.wrappers.backlog.plugins.BinaryBridgePlugin BinaryBridgePlugin}. */
	public static final byte VAISALA_WXT520_MESSAGE_TYPE = 40;

	/** The Schedule message type. This message type is used to
     *  send/receive Schedule data.
	 *  {@link gsn.wrappers.backlog.plugins.SchedulePlugin SchedulePlugin}. */
	public static final byte SCHEDULE_MESSAGE_TYPE = 50;

	/** The GPS message type. This message type is used to
     *  send/receive GPS data.
	 *  {@link gsn.wrappers.backlog.plugins.GPSPlugin GPSPlugin}. */
	public static final byte GPS_MESSAGE_TYPE = 60;
	
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
	
	private Serializable[] payload = {};
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
	 * @param payload of the message.
	 * 
	 * @throws IOException if the payload is too big
	 */
	public BackLogMessage(byte type, long timestamp, Serializable[] payload) throws Exception {
		if( payload == null )
			throw new NullPointerException("The payload should not be null");
		
		checkPayload(payload);
		
		this.type = type;
		this.timestamp = timestamp;
		this.payload = payload;
	}
	
	
	/** 
	 * Class constructor specifying the message.
	 * 
	 * @param binary message as byte array.
	 * @throws IOException if the message length exceeds MAX_PAYLOAD_SIZE+9
	 */
	public BackLogMessage(byte[] message) throws IOException {
		ByteArrayInputStream bin = new ByteArrayInputStream(message);
		DataInputStream din = new DataInputStream(bin);
		
		type = din.readByte();
		timestamp = din.readLong();
		
		String format = null;
		
		try {
			format = din.readUTF();
		}
		catch (IOException e) {
			return;
		}

		payload = new Serializable [format.length()];
		int payloadIndex = 0;
		CharacterIterator it = new StringCharacterIterator(format);
		for (char ch=it.first(); ch != CharacterIterator.DONE; ch=it.next()) {
			switch (ch) {
			case '0':
				payload[payloadIndex] = null;
				break;
			case 'b':
				payload[payloadIndex] = din.readByte();
				break;
			case '?':
				payload[payloadIndex] = din.readBoolean();
				break;
			case 'h':
				payload[payloadIndex] = din.readShort();
				break;
			case 'i':
				payload[payloadIndex] = din.readInt();
				break;
			case 'q':
				payload[payloadIndex] = din.readLong();
				break;
			case 'd':
				payload[payloadIndex] = din.readDouble();
				break;
			case 's':
				payload[payloadIndex] = din.readUTF();
				break;
			case 'X':
				byte [] data = new byte [din.readUnsignedShort()];
				din.readFully(data);
				payload[payloadIndex] = data;
				break;
			default:
				throw new IOException("unrecognized format character received");
			}
			payloadIndex++;
		}
	}
	
	
	/** 
	 * Get the message as byte array.
	 * 
	 * @return the message as byte array
	 */
	public byte[] getBinaryMessage() {
		ByteArrayOutputStream bout = new ByteArrayOutputStream(9);
		DataOutputStream dout = new DataOutputStream(bout);
		ByteArrayOutputStream databout = new ByteArrayOutputStream(1);
		DataOutputStream datadout = new DataOutputStream(databout);
		try {
			String format = "";

			for (int i=0; i<payload.length; i++) {
				if (payload[i] == null) {
					format += "0";
				}
				else {
					if (payload[i] instanceof Byte) {
						datadout.writeByte((Byte)payload[i]);
						format += "b";
					}
					else if (payload[i] instanceof Boolean) {
						datadout.writeBoolean((Boolean)payload[i]);
						format += "?";
					}
					else if (payload[i] instanceof Short) {
						datadout.writeShort((Short)payload[i]);
						format += "h";
					}
					else if (payload[i] instanceof Integer) {
						datadout.writeInt((Integer)payload[i]);
						format += "i";
					}
					else if (payload[i] instanceof Long) {
						datadout.writeLong((Long)payload[i]);
						format += "q";
					}
					else if (payload[i] instanceof Double) {
						datadout.writeDouble((Double)payload[i]);
						format += "d";
					}
					else if (payload[i] instanceof String) {
						datadout.writeUTF((String)payload[i]);
						format += "s";
					}
					else if (payload[i] instanceof byte[]) {
						datadout.writeShort(((byte[])payload[i]).length);
						datadout.write((byte[])payload[i]);
						format += "X";
					}
					else if (payload[i] instanceof Byte[]) {
						datadout.writeShort(((Byte[])payload[i]).length);
						byte [] tmp = new byte [((Byte[])payload[i]).length];
						for (int j=0; j<((Byte[])payload[i]).length; j++)
							tmp[j] = ((Byte[])payload[i])[j];
						datadout.write(tmp);
						format += "X";
					}
					else
						System.err.print("unsupported type");
				}
			}

			dout.writeByte(type);
			dout.writeLong(timestamp);
			if (format != "") {
				dout.writeUTF(format);
				dout.write(databout.toByteArray());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bout.toByteArray();
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
	 * @return the payload as Serializable array
	 */
	public Serializable[] getPayload() {
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
	public void setPayload(Serializable[] payload) throws IOException {
		checkPayload(payload);
		
		this.payload = payload;
	}
	
	
	private void checkPayload(Serializable[] payload) throws IOException {
		int length = 0;
		for (int i=0; i<payload.length; i++) {
			if (payload[i] == null)
				continue;
			else if (payload[i] instanceof Byte)
				length += 1;
			else if (payload[i] instanceof Boolean)
				length += 1;
			else if (payload[i] instanceof Short)
				length += 2;
			else if (payload[i] instanceof Integer)
				length += 4;
			else if (payload[i] instanceof Long)
				length += 8;
			else if (payload[i] instanceof Double)
				length += 8;
			else if (payload[i] instanceof String)
				length += ((String)payload[i]).length()*2+2;
			else if (payload[i] instanceof byte[])
				length += ((byte[])payload[i]).length;
			else if (payload[i] instanceof Byte[])
				length += ((Byte[])payload[i]).length;
			else
				throw new IOException("unsupported type in payload.");
		}

		if( length > MAX_PAYLOAD_SIZE )
			throw new IOException("The payload exceeds the maximum size of " + MAX_PAYLOAD_SIZE + "bytes.");
	}
}
