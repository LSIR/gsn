package gsn.wrappers.backlog.plugins;

import java.io.IOException;
import java.io.Serializable;
import java.util.Hashtable;

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
public class BackLogStatusPlugin extends AbstractPlugin {
	private static final String STATUS_DATA_TYPE = "status-data-type";

	private static final String STATIC_NAMING = "static";
	private static final String REVISION_NAMING = "revision";
	private static final String DYNAMIC_NAMING = "dynamic";
	
	private static DataField[] staticDataField = {
			new DataField("TIMESTAMP", "BIGINT"),
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("DEVICE_ID", "INTEGER"),

			new DataField("PYTHON_IMPLEMENTATION", "VARCHAR(255)"),
			new DataField("PYTHON_VERSION", "VARCHAR(255)"),
			new DataField("PYTHON_COMPILER", "VARCHAR(255)"),
			new DataField("PYTHON_BUILD", "VARCHAR(255)"),
			new DataField("PYTHON_BUILD_DATE", "VARCHAR(255)"),
			new DataField("LAST_CLEAN_SHUTDOWN", "BIGINT")};
	
	private static DataField[] revisionDataField = {
			new DataField("TIMESTAMP", "BIGINT"),
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("DEVICE_ID", "INTEGER"),

			new DataField("FILE_REVISION", "VARCHAR(255)"),
			new DataField("FILE_MD5", "VARCHAR(255)")};
	
	private static DataField[] dynamicDataField = {
			new DataField("TIMESTAMP", "BIGINT"), 
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("DEVICE_ID", "INTEGER"),
			
			new DataField("UPTIME", "INTEGER"),
			new DataField("ERROR_COUNTER", "INTEGER"),
			new DataField("EXCEPTION_COUNTER", "INTEGER"),
			
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
			new DataField("DB_STORE_COUNTER", "INTEGER"),
			new DataField("DB_REMOVE_COUNTER", "INTEGER"),
			new DataField("DB_STORE_TIME_MIN", "INTEGER"),
			new DataField("DB_STORE_TIME_MEAN", "INTEGER"),
			new DataField("DB_STORE_TIME_MAX", "INTEGER"),
			new DataField("DB_REMOVE_TIME_MIN", "INTEGER"),
			new DataField("DB_REMOVE_TIME_MEAN", "INTEGER"),
			new DataField("DB_REMOVE_TIME_MAX", "INTEGER"),
            
			new DataField("SCHEDULE_CREATED", "BIGINT"),
			new DataField("SCHEDULE_PLUG_ACTION_COUNTER", "INTEGER"),
			new DataField("SCHEDULE_SCRIPT_EXEC_COUNTER", "INTEGER"),
            
			new DataField("PLUG_FIN_IN_TIME_COUNTER", "INTEGER"),
			new DataField("PLUG_NOT_FIN_IN_TIME_COUNTER", "INTEGER"),
			new DataField("SCRIPT_FIN_SUC_IN_TIME_COUNTER", "INTEGER"),
			new DataField("SCRIPT_FIN_UNSUC_IN_TIME_COUNTER", "INTEGER"),
			new DataField("SCRIPT_NOT_FIN_IN_TIME_COUNTER", "INTEGER"),

			new DataField("TOS_MSG_RECV_COUNTER", "INTEGER"),
			new DataField("TOS_ACK_SEND_COUNTER", "INTEGER"),
			new DataField("TOS_MSG_SEND_COUNTER", "INTEGER"),

			new DataField("VM_PEAK", "INTEGER"),
			new DataField("VM_SIZE", "INTEGER"),
			new DataField("VM_LCK", "INTEGER"),
			new DataField("VM_HWM", "INTEGER"),
			new DataField("VM_RSS", "INTEGER"),
			new DataField("VM_DATA", "INTEGER"),
			new DataField("VM_STK", "INTEGER"),
			new DataField("VM_EXE", "INTEGER"),
			new DataField("VM_LIB", "INTEGER"),
			new DataField("VM_PTE", "INTEGER"),
			new DataField("THREADS", "INTEGER"),
			new DataField("VOLUNTARY_CTXT_SWITCHES", "INTEGER"),
			new DataField("NONVOLUNTARY_CTXT_SWITCHES", "INTEGER"),
			
			new DataField("RUSAGE_UTIME", "DOUBLE"),
			new DataField("RUSAGE_STIME", "DOUBLE"),
			new DataField("RUSAGE_MINFLT", "INTEGER"),
			new DataField("RUSAGE_MAJFLT", "INTEGER"),
			new DataField("RUSAGE_NVCSW", "INTEGER"),
			new DataField("RUSAGE_NIVCSW", "INTEGER")};
	
	private static final Hashtable<String, NameDataFieldPair> statusNamingTable = new Hashtable<String, NameDataFieldPair>();
	static
	{
		statusNamingTable.put(STATIC_NAMING, new NameDataFieldPair(1, staticDataField));
		statusNamingTable.put(REVISION_NAMING, new NameDataFieldPair(2, revisionDataField));
		statusNamingTable.put(DYNAMIC_NAMING, new NameDataFieldPair(3, dynamicDataField));
	}

	private final transient Logger logger = Logger.getLogger( BackLogStatusPlugin.class );

	private String statusDataType;
	
	
	@Override
	public boolean initialize ( BackLogWrapper backlogwrapper, String coreStationName, String deploymentName) {
		super.activeBackLogWrapper = backlogwrapper;
		try {
			statusDataType = getActiveAddressBean().getPredicateValueWithException(STATUS_DATA_TYPE).toLowerCase();
		} catch (Exception e) {
			logger.error(statusDataType);
			logger.error(e.getMessage());
			return false;
		}
		if (statusNamingTable.get(statusDataType) == null) {
			logger.error("wrong " + STATUS_DATA_TYPE + " predicate key specified in virtual sensor XML file! (" + STATUS_DATA_TYPE + "=" + statusDataType + ")");
			return false;
		}
		logger.info("using BackLogStatus data type: " + statusDataType);
        
        registerListener();

        if (statusDataType.equalsIgnoreCase(STATIC_NAMING))
        	setName("BackLogStatusPlugin-Static-" + coreStationName + "-Thread");
        else if (statusDataType.equalsIgnoreCase(REVISION_NAMING))
        	setName("BackLogStatusPlugin-HW-" + coreStationName + "-Thread");
        else if (statusDataType.equalsIgnoreCase(DYNAMIC_NAMING))
        	setName("BackLogStatusPlugin-SW-" + coreStationName + "-Thread");
		
		return true;
	}

	@Override
	public byte getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.BACKLOG_STATUS_MESSAGE_TYPE;
	}

	@Override
	public DataField[] getOutputFormat() {
		return statusNamingTable.get(statusDataType).dataField;
	}


	@Override
	public String getPluginName() {
        if (statusDataType.equalsIgnoreCase(STATIC_NAMING))
        	return "BackLogStatusPlugin-Static";
        else if (statusDataType.equalsIgnoreCase(REVISION_NAMING))
        	return "BackLogStatusPlugin-HW";
        else if (statusDataType.equalsIgnoreCase(DYNAMIC_NAMING))
        	return "BackLogStatusPlugin-SW";
        else
        	return "BackLogStatusPlugin";
	}

	@Override
	public boolean messageReceived(int deviceId, long timestamp, Serializable[] data) {
		Serializable[] header = {timestamp, timestamp, deviceId};
		
		try {
			short msgType = toShort(data[0]);
			
			if (msgType == statusNamingTable.get(statusDataType).typeNumber) {
				if (statusDataType.equalsIgnoreCase(STATIC_NAMING)) {
					data = checkAndCastData(data, 1, staticDataField, 3);
				}
				else if (statusDataType.equalsIgnoreCase(REVISION_NAMING)) {
					data = checkAndCastData(data, 1, revisionDataField, 3);
				}
				else if (statusDataType.equalsIgnoreCase(DYNAMIC_NAMING)) {
					data = checkAndCastData(data, 1, dynamicDataField, 3);
				}
				else {
					logger.warn("Wrong BackLogStatus data type spedified.");
					return false;
				}
				
				if( dataProcessed(System.currentTimeMillis(), concat(header, data)) ) {
					ackMessage(timestamp, super.priority);
					return true;
				} else {
					logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return false;
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
					logger.warn("Upload command (resend backlogged data) could not be sent");
					return false;
				}
			} catch (IOException e) {
				logger.warn(e.getMessage());
			}
		}
		
		return true;
	}
}
