package gsn.wrappers.backlog.plugins;

import java.io.Serializable;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.log4j.Logger;

import gsn.beans.DataField;


/**
 * This plugin offers the functionality to send commands to the Backlog Python program
 * on the deployment side. It also listens for incoming BackLogStatus messages.
 * <p>
 * Any new command pointed directly to the Backlog Python program should be implemented
 * in this class.
 * 
 * @author Tonio Gsell
 */
public class BackLogStatusPlugin extends AbstractPlugin {
	
	private DataField[] dataField = {	new DataField("TIMESTAMP", "BIGINT"), 
						new DataField("GENERATION_TIME", "BIGINT"),
						new DataField("DEVICE_ID", "INTEGER"),
						new DataField("ERROR_COUNTER", "INTEGER"),
						new DataField("EXCEPTION_COUNTER", "INTEGER"),
						new DataField("BACKLOG_DB_ENTRIES", "INTEGER"),
						new DataField("BACKLOG_DB_SIZE_KB", "INTEGER"),
						new DataField("IN_COUNTER", "INTEGER"),
						new DataField("OUT_COUNTER", "INTEGER"),
						new DataField("BACKLOG_COUNTER", "INTEGER"),
						new DataField("CONNECTION_LOSSES", "INTEGER"),
						new DataField("UPTIME", "INTEGER"),
						new DataField("DB_STORE_TIME_MIN", "INTEGER"),
						new DataField("DB_STORE_TIME_MEAN", "INTEGER"),
						new DataField("DB_STORE_TIME_MAX", "INTEGER"),
						new DataField("DB_REMOVE_TIME_MIN", "INTEGER"),
						new DataField("DB_REMOVE_TIME_MEAN", "INTEGER"),
						new DataField("DB_REMOVE_TIME_MAX", "INTEGER")};

	private final transient Logger logger = Logger.getLogger( BackLogStatusPlugin.class );

	@Override
	public byte getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.BACKLOG_STATUS_MESSAGE_TYPE;
	}

	@Override
	public DataField[] getOutputFormat() {
		return dataField;
	}


	@Override
	public String getPluginName() {
		return "BackLogStatusPlugin";
	}

	@Override
	public boolean messageReceived(int deviceId, long timestamp, byte[] packet) {
		logger.debug("message received from CoreStation with DeviceId: " + deviceId);

		// Parse the Message
		ByteBuffer buffer = ByteBuffer.allocate(packet.length);
		buffer.put(packet);
		
		buffer.position(0);
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		Integer error_counter = null;
		Integer exception_counter = null;
		Integer backlog_db_entries = null;
		Integer backlog_db_size = null;
		Integer in_counter = null;
		Integer out_counter = null;
		Integer backlog_counter = null;
		Integer connection_losses = null;
		Integer backlog_uptime = null;
		Integer minstoretime = null;
		Integer maxstoretime = null;
		Integer meanstoretime = null;
		Integer minremovetime = null;
		Integer maxremovetime = null;
		Integer meanremovetime = null;
		
		try {
			error_counter = buffer.getInt();
			exception_counter = buffer.getInt();
			backlog_db_entries = buffer.getInt();
			backlog_db_size = buffer.getInt();
			in_counter = buffer.getInt();
			out_counter = buffer.getInt();
			backlog_counter = buffer.getInt();
			connection_losses = buffer.getInt();
			backlog_uptime = buffer.getInt();
			minstoretime = buffer.getInt();
			maxstoretime = buffer.getInt();
			meanstoretime = buffer.getInt();
			minremovetime = buffer.getInt();
			maxremovetime = buffer.getInt();
			meanremovetime = buffer.getInt();
		} catch (BufferUnderflowException e) {
			logger.error(e.getMessage());
		}

		if (minstoretime != null && minstoretime == -1)
			minstoretime = null;
		if (maxstoretime != null && maxstoretime == -1)
			maxstoretime = null;
		if (meanstoretime != null && meanstoretime == -1)
			meanstoretime = null;
		if (minremovetime != null && minremovetime == -1)
			minremovetime = null;
		if (maxremovetime != null && maxremovetime == -1)
			maxremovetime = null;
		if (meanremovetime != null && meanremovetime == -1)
			meanremovetime = null;
		
		Serializable[] data = {timestamp, timestamp, deviceId, error_counter, exception_counter, backlog_db_entries, backlog_db_size, in_counter, out_counter, backlog_counter, connection_losses, backlog_uptime, minstoretime, meanstoretime, maxstoretime, minremovetime, meanremovetime, maxremovetime};
		
		if (dataProcessed(System.currentTimeMillis(), data))
			ackMessage(timestamp, super.priority);
		else
			logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");
		return true;
	}

	
	
	/** 
	 * Implements the interpretation and packaging of the status
	 * commands.
	 * <p>
	 * The incoming commands, e.g. by the web input, arrive here and have
	 * to be packed into a byte array in such a way, that the Python Backlog
	 * program can unpack it.
	 */
	@Override
	public boolean sendToPlugin(String action, String[] paramNames, Object[] paramValues) {
		if( action.compareToIgnoreCase("resend_backlogged_data") == 0 ) {
			byte[] command = {1};
			try {
				if( sendRemote(System.currentTimeMillis(), command, super.priority) ) {
					logger.debug("Upload command sent (resend backlogged data)");
				}
				else {
					logger.warn("Upload command (resend backlogged data)");
					return false;
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		
		return true;
	}
}
