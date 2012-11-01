package gsn.wrappers.backlog.plugins;

import gsn.beans.DataField;
import gsn.beans.InputInfo;
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
		new DataField("SAMPLING_RESULT", "INTEGER"),
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
		
		new DataField("NB_OF_BOTTLES", "INTEGER"),
		new DataField("BOTTLE_VOLUME_IN_LIT", "DOUBLE"),
		new DataField("SUCTION_LINE_LENGTH_IN_M", "DOUBLE"),
		new DataField("SUCTION_LINE_HEAD", "DOUBLE"),
		new DataField("NB_OF_RINSE_CYCLES", "INTEGER"),
		new DataField("NB_OF_RETRIES", "INTEGER"),
		
		new DataField("MC_SAMPLER_STATUS", "INTEGER"),
		new DataField("MC_SAMPLER_STATUS_EXTENSION", "INTEGER"),
		new DataField("MC_PROGRAM_STATUS", "INTEGER"),
				
		new DataField("EPC_SAMPLER_MODEL", "INTEGER"),
		new DataField("EPC_SAMPLER_ID", "INTEGER"),
		new DataField("EPC_SAMPLER_TIME", "DOUBLE"),
		new DataField("EPC_SAMPLER_STATUS", "INTEGER"),
		new DataField("EPC_MOST_RECENT_SAMPLE_TIME", "DOUBLE"),
		new DataField("EPC_MOST_RECENT_SAMPLE_BOTTLE", "INTEGER"),
		new DataField("EPC_MOST_RECENT_SAMPLE_VOLUME", "INTEGER"),
		new DataField("EPC_MOST_RECENT_SAMPLE_RESULT", "INTEGER"),
		new DataField("EPC_CHECKSUM", "INTEGER")};

	private static final Hashtable<String, NameDataFieldPair> statusNamingTable = new Hashtable<String, NameDataFieldPair>();
	static
	{
		statusNamingTable.put(SAMPLING_RESULT, new NameDataFieldPair(0, samplingResultDataField));
		statusNamingTable.put(SAMPLER_STATUS, new NameDataFieldPair(1, samplerStatusDataField));
	}	
	
	private enum Task{
		// list with tasks enumerations and according code and string
		TAKE_SAMPLE_TASK(2, "take_sample"),
		REINIT_SAMPLER_TASK(6, "reinit_sampler"),
		REPORT_STATUS_TASK(3, "report_status");
		
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
	public InputInfo sendToPlugin(String action, String[] paramNames, Object[] paramValues) {
		Serializable[] command = null;
		Task task;
				
		// determine task depending on action string
		task = Task.fromString(action);
		
		switch( task )
		{
			case REINIT_SAMPLER_TASK:
				// create command
				command = new Serializable[] {task.getCode()};
				break;
				
			case REPORT_STATUS_TASK:
				// create command
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
				logger.warn(">" + action + "< not supported");
				return new InputInfo(getActiveAddressBean().toString(), ">" + action + "< not supported", false);
		}

		
		try {
			if( sendRemote(System.currentTimeMillis(), command, super.priority) ) {
				if (logger.isDebugEnabled())
					logger.debug("6712 sampler task sent to CoreStation");
				return new InputInfo(getActiveAddressBean().toString(), "6712 sampler task successfully sent to CoreStation", true);
			}
			else {
				logger.warn("6712 sampler task could not be sent to CoreStation");
				return new InputInfo(getActiveAddressBean().toString(), "6712 sampler task could not be sent to CoreStation", false);
			}
		} catch (IOException e) {
			logger.warn(e.getMessage());
			return new InputInfo(getActiveAddressBean().toString(), e.getMessage(), false);
		}
	}
}
