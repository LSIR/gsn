package gsn.wrappers.backlog.plugins;

import gsn.beans.DataField;
import gsn.wrappers.BackLogWrapper;

import java.io.IOException;
import java.io.Serializable;
import java.util.Hashtable;

import org.apache.log4j.Logger;


/**
 * This plugin listens for incoming 6712 sampler messages and offers the functionality to
 * upload 6712 sampler tasks.
 * 
 * @author Daniel Burgener
 */
public class Sampler6712Plugin extends AbstractPlugin {
	private static final String STATUS_DATA_TYPE = "status-data-type";

	private static final String SAMPLING_RESULT = "sampling_result";
	private static final String SAMPLER_STATUS = "sampler_status";
	
	private static DataField[] samplingResultDataField = {
		new DataField("TIMESTAMP", "BIGINT"),
		new DataField("GENERATION_TIME", "BIGINT"),
		new DataField("DEVICE_ID", "INTEGER"),
		
		new DataField("SAMPLING_TRIGGER_SOURCE", "INTEGER"),
		new DataField("BOTTLE_NUMBER", "INTEGER"),
		new DataField("VOLUME", "INTEGER"),
		new DataField("SAMPLER_OVERAL_STATUS", "INTEGER"),
		new DataField("SAMPLER_MODEL", "INTEGER"),
		new DataField("SAMPLER_ID", "INTEGER"),
		new DataField("SAMPLER_TIME", "DOUBLE"),
		new DataField("SAMPLER_STATUS", "INTEGER"),
		new DataField("MOST_RECENT_SAMPLE_TIME", "DOUBLE"),
		new DataField("MOST_RECENT_SAMPLE_BOTTLE", "INTEGER"),
		new DataField("MOST_RECENT_SAMPLE_VOLUME", "INTEGER"),
		new DataField("MOST_RECENT_SAMPLE_RESULT", "INTEGER"),
		new DataField("CHECKSUM", "INTEGER")};
	
	private static DataField[] samplerStatusDataField = {
		new DataField("TIMESTAMP", "BIGINT"),
		new DataField("GENERATION_TIME", "BIGINT"),
		new DataField("DEVICE_ID", "INTEGER"),
		
		new DataField("LM92_TEMP", "INTEGER"),
		new DataField("V_EXT2", "INTEGER"),
		new DataField("V_EXT1", "INTEGER"),
		new DataField("V_EXT3", "INTEGER"),
		new DataField("I_V12DC_EXT", "INTEGER"),
		new DataField("V12DC_IN", "INTEGER"),
		new DataField("I_V12DC_IN", "INTEGER"),
		new DataField("VCC_5_0", "INTEGER"),
		new DataField("VCC_NODE", "INTEGER"),
		new DataField("I_VCC_NODE", "INTEGER"),
		new DataField("VCC_4_2", "INTEGER")};	

	private static final Hashtable<String, NameDataFieldPair> statusNamingTable = new Hashtable<String, NameDataFieldPair>();
	static
	{
		statusNamingTable.put(SAMPLING_RESULT, new NameDataFieldPair(0, samplingResultDataField));
		statusNamingTable.put(SAMPLER_STATUS, new NameDataFieldPair(1, samplerStatusDataField));
	}	
	
	private enum Task{
		// list with tasks enumerations and according code and string
		SEND_COMMAND_TASK(0, "send_command"),
		TURN_ON_SAMPLER_TASK(1, "turn_on_sampler"),
		TAKE_SAMPLE_TASK(2, "take_sample"),
		REQ_SAMPLER_STATUS_TASK(3, "req_sampler_status"),
		REQ_DATA_STATUS_TASK(4, "req_data_status");
		
		private final short code;	 	// code representing task
		private final String string;	// string representing task
		
		private Task(int code, String string){
			this.code = (short)code;
			this.string = string;
		}
		
		public short getCode(){
			return this.code;
		}
		
		// translate the custom string representation back to the corresponding enum
		public static Task fromString(String text) {
		    if (text != null) {
		    	for (Task b : Task.values()) {
		    		if (text.equalsIgnoreCase(b.string)) {
		    			return b;
		    		}
		    	}
		    }
		    return null;
		}
	}
	
	
	private final transient Logger logger = Logger.getLogger( Sampler6712Plugin.class );

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
		logger.info("using CoreStationStatus data type: " + statusDataType);
        
        registerListener();

        if (statusDataType.equalsIgnoreCase(SAMPLING_RESULT))
        	setName("CoreStationStatusPlugin-SamplingResult-" + coreStationName + "-Thread");
        else if (statusDataType.equalsIgnoreCase(SAMPLER_STATUS))
        	setName("CoreStationStatusPlugin-SamplerStatus-" + coreStationName + "-Thread");
		
		return true;
	}	
	
	@Override
	public short getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.SAMPLER_6712_MESSAGE_TYPE;
	}

	@Override
	public DataField[] getOutputFormat() {
		return statusNamingTable.get(statusDataType).dataField;
	}

	@Override
	public String getPluginName() {
        if (statusDataType.equalsIgnoreCase(SAMPLING_RESULT))
        	return "Sampler6712Plugin-SamplingResult";
        else if (statusDataType.equalsIgnoreCase(SAMPLER_STATUS))
        	return "Sampler6712Plugin-SamplerStatus";
        else
        	return "Sampler6712Plugin";
	}

	@Override
	public boolean messageReceived(int deviceId, long timestamp, Serializable[] data) {
		Serializable[] header = {timestamp, timestamp, deviceId};
		
		try {
			short msgType = toShort(data[0]);
			
			if (msgType == statusNamingTable.get(statusDataType).typeNumber) {
				if (statusDataType.equalsIgnoreCase(SAMPLING_RESULT)) {
					data = checkAndCastData(data, 1, samplingResultDataField, 3);
				}
				else if (statusDataType.equalsIgnoreCase(SAMPLER_STATUS)) {
					data = checkAndCastData(data, 1, samplerStatusDataField, 3);
				}
				else {
					logger.warn("Wrong CoreStationStatus data type spedified.");
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
	
	
	@Override
	public boolean sendToPlugin(String action, String[] paramNames, Object[] paramValues) {
		Serializable[] command = null;
		Task task;
		String sCmd = "";
				
		// determine task depending on action string
		task = Task.fromString(action);
		
		switch( task )
		{
			case SEND_COMMAND_TASK:
				for (int i = 0 ; i < paramNames.length ; i++) {
					if( paramNames[i].compareToIgnoreCase("command") == 0 )
						sCmd = (String) paramValues[i];					
				}
				command = new Serializable[] {task.getCode(), sCmd};
				break;
				
			case TURN_ON_SAMPLER_TASK:			
			case REQ_SAMPLER_STATUS_TASK:				
			case REQ_DATA_STATUS_TASK:
				command = new Serializable[] {task.getCode()};
				break;
				
			case TAKE_SAMPLE_TASK:
				short shVolume = 0;
				byte bBottle = 0;
				for (int i = 0 ; i < paramNames.length ; i++) {
					if( paramNames[i].compareToIgnoreCase("bottle_number") == 0 )
						bBottle = Byte.valueOf((String) paramValues[i]);
						
					else if( paramNames[i].compareToIgnoreCase("volume") == 0 )
						shVolume = Short.valueOf((String) paramValues[i]);
				}
				
				// create command
				command = new Serializable[] {task.getCode(), bBottle, shVolume};
				break;
				
			default:
				logger.warn("unrecognized action >" + action + "<");
		}

		
		try {
			if( sendRemote(System.currentTimeMillis(), command, super.priority) ) {
				if (logger.isDebugEnabled())
					logger.debug("6712 sampler task sent to CoreStation");
			}
			else {
				logger.warn("6712 sampler task could not be sent to CoreStation");
				return false;
			}
		} catch (IOException e) {
			logger.warn(e.getMessage());
		}
		
		return true;
	}
}
