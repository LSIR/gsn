package gsn.wrappers.backlog.plugins;

import java.io.Serializable;

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
						new DataField("UPTIME", "INTEGER"),
						new DataField("ERROR_COUNTER", "INTEGER"),
						new DataField("EXCEPTION_COUNTER", "INTEGER"),
						new DataField("ACTIVE_THREAD_COUNTER", "INTEGER"),
						new DataField("GSN_PLUGIN_MSG_IN", "DOUBLE"),
						new DataField("GSN_PLUGIN_MSG_OUT", "DOUBLE"),
						new DataField("GSN_PLUGIN_MSG_IN_COUNTER", "INTEGER"),
						new DataField("GSN_PLUGIN_MSG_OUT_COUNTER", "INTEGER"),
						new DataField("GSN_PLUGIN_MSG_ACK_IN_COUNTER", "INTEGER"),
						new DataField("GSN_PING_OUT_COUNTER", "INTEGER"),
						new DataField("GSN_PING_ACK_IN_COUNTER", "INTEGER"),
						new DataField("GSN_PING_IN_COUNTER", "INTEGER"),
						new DataField("GSN_PING_ACK_OUT_COUNTER", "INTEGER"),
						new DataField("GSN_CONNECTION_LOSSES", "INTEGER"),
						new DataField("DB_ENTRIES", "INTEGER"),
						new DataField("DB_SIZE", "INTEGER"),
						new DataField("DB_STORE", "DOUBLE"),
						new DataField("DB_REMOVE", "DOUBLE"),
						new DataField("DB_STORE_COUNTER", "INTEGER"),
						new DataField("DB_REMOVE_COUNTER", "INTEGER"),
						new DataField("DB_STORE_TIME_MIN", "INTEGER"),
						new DataField("DB_STORE_TIME_MEAN", "INTEGER"),
						new DataField("DB_STORE_TIME_MAX", "INTEGER"),
						new DataField("DB_REMOVE_TIME_MIN", "INTEGER"),
						new DataField("DB_REMOVE_TIME_MEAN", "INTEGER"),
						new DataField("DB_REMOVE_TIME_MAX", "INTEGER"),
						new DataField("RUSAGE_UTIME", "DOUBLE"),
						new DataField("RUSAGE_STIME", "DOUBLE"),
						new DataField("RUSAGE_MAXRSS", "INTEGER"),
						new DataField("RUSAGE_IXRSS", "INTEGER"),
						new DataField("RUSAGE_IDRSS", "INTEGER"),
						new DataField("RUSAGE_ISRSS", "INTEGER"),
						new DataField("RUSAGE_MINFLT", "INTEGER"),
						new DataField("RUSAGE_MAJFLT", "INTEGER"),
						new DataField("RUSAGE_NSWAP", "INTEGER"),
						new DataField("RUSAGE_INBLOCK", "INTEGER"),
						new DataField("RUSAGE_OUBLOCK", "INTEGER"),
						new DataField("RUSAGE_MSGSND", "INTEGER"),
						new DataField("RUSAGE_MSGRCV", "INTEGER"),
						new DataField("RUSAGE_NSIGNALS", "INTEGER"),
						new DataField("RUSAGE_NVCSW", "INTEGER"),
						new DataField("RUSAGE_NIVCSW", "INTEGER")};

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
	public boolean messageReceived(int deviceId, long timestamp, Serializable[] data) {
		if (logger.isDebugEnabled())
			logger.debug("message received from CoreStation with DeviceId: " + deviceId);
		
		for (int index=0; index<data.length; index++) {
			try {
				if (!(data[index] instanceof Double))
					data[index] = toInteger(data[index]);
			} catch (Exception e) {
				logger.error(e.getMessage() + " (index=" + index + ", value=" + data[index] + ", type=" + data[index].getClass().getName() + ")", e);
				return true;
			}
		}

		Serializable[] header = {timestamp, timestamp, deviceId};
		
		if (dataProcessed(System.currentTimeMillis(), concat(header, data)))
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
			Serializable[] command = {1};
			try {
				if( sendRemote(System.currentTimeMillis(), command, super.priority) ) {
					if (logger.isDebugEnabled())
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
