package gsn.wrappers.backlog.plugins;

import java.io.Serializable;
import java.util.Hashtable;

import org.apache.log4j.Logger;

import gsn.beans.DataField;
import gsn.wrappers.BackLogWrapper;


public class STEVALPlugin extends AbstractPlugin {
	private static final String STATUS_DATA_TYPE = "status-data-type";

	private static final String STATIC_NAMING = "static";
	private static final String DYNAMIC_RAW_NAMING = "dynamic_raw";
	private static final String DYNAMIC_PROC_NAMING = "dynamic_proc";
	private static final String DYNAMIC_RAW_PROC_NAMING = "dynamic_raw_proc";
	
	private static DataField[] staticDataField = {
			new DataField("TIMESTAMP", "BIGINT"),
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("DEVICE_ID", "INTEGER"),
			
			new DataField("DEVICE_NAME", "VARCHAR(256)"),
	        new DataField("FIRMWARE_VERSION", "VARCHAR(256)")};
	
	private static DataField[] dynamicRawDataField = {
			new DataField("TIMESTAMP", "BIGINT"),
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("DEVICE_ID", "INTEGER"),
			
			new DataField("DURATION", "DOUBLE"),
	    new DataField("TIME_OF_DATA", "DOUBLE"),
	    new DataField("X_DATA", "VARCHAR(8192)"),
	    new DataField("Y_DATA", "VARCHAR(8192)"),
	    new DataField("Z_DATA", "VARCHAR(8192)"),
	    new DataField("MEASUREMENT_ID", "BIGINT")};
	
	private static DataField[] dynamicProcDataField = {
			new DataField("TIMESTAMP", "BIGINT"), 
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("DEVICE_ID", "INTEGER"),
			
			new DataField("DURATION", "DOUBLE"),
      new DataField("TIME_OF_DATA", "DOUBLE"),
      new DataField("Xp_DATA1", "DOUBLE"),
      new DataField("Xp_FREQ1", "DOUBLE"),
      new DataField("Xp_DATA2", "DOUBLE"),
      new DataField("Xp_FREQ2", "DOUBLE"),
      new DataField("Xp_DATA3", "DOUBLE"),
      new DataField("Xp_FREQ3", "DOUBLE"),
      new DataField("Xp_DATA4", "DOUBLE"),
      new DataField("Xp_FREQ4", "DOUBLE"),
      new DataField("Xp_DATA5", "DOUBLE"),
      new DataField("Xp_FREQ5", "DOUBLE"),
      new DataField("Xp_DATA6", "DOUBLE"),
      new DataField("Xp_FREQ6", "DOUBLE"),
      new DataField("Xp_DATA7", "DOUBLE"),
      new DataField("Xp_FREQ7", "DOUBLE"),
      new DataField("Xp_DATA8", "DOUBLE"),
      new DataField("Xp_FREQ8", "DOUBLE"),
      new DataField("Xp_DATA9", "DOUBLE"),
      new DataField("Xp_FREQ9", "DOUBLE"),
      new DataField("Xp_DATA10", "DOUBLE"),
      new DataField("Xp_FREQ10", "DOUBLE"),
      new DataField("Yp_DATA1", "DOUBLE"),
      new DataField("Yp_FREQ1", "DOUBLE"),
      new DataField("Yp_DATA2", "DOUBLE"),
      new DataField("Yp_FREQ2", "DOUBLE"),
      new DataField("Yp_DATA3", "DOUBLE"),
      new DataField("Yp_FREQ3", "DOUBLE"),
      new DataField("Yp_DATA4", "DOUBLE"),
      new DataField("Yp_FREQ4", "DOUBLE"),
      new DataField("Yp_DATA5", "DOUBLE"),
      new DataField("Yp_FREQ5", "DOUBLE"),
      new DataField("Yp_DATA6", "DOUBLE"),
      new DataField("Yp_FREQ6", "DOUBLE"),
      new DataField("Yp_DATA7", "DOUBLE"),
      new DataField("Yp_FREQ7", "DOUBLE"),
      new DataField("Yp_DATA8", "DOUBLE"),
      new DataField("Yp_FREQ8", "DOUBLE"),
      new DataField("Yp_DATA9", "DOUBLE"),
      new DataField("Yp_FREQ9", "DOUBLE"),
      new DataField("Yp_DATA10", "DOUBLE"),
      new DataField("Yp_FREQ10", "DOUBLE"),
      new DataField("Zp_DATA1", "DOUBLE"),
      new DataField("Zp_FREQ1", "DOUBLE"),
      new DataField("Zp_DATA2", "DOUBLE"),
      new DataField("Zp_FREQ2", "DOUBLE"),
      new DataField("Zp_DATA3", "DOUBLE"),
      new DataField("Zp_FREQ3", "DOUBLE"),
      new DataField("Zp_DATA4", "DOUBLE"),
      new DataField("Zp_FREQ4", "DOUBLE"),
      new DataField("Zp_DATA5", "DOUBLE"),
      new DataField("Zp_FREQ5", "DOUBLE"),
      new DataField("Zp_DATA6", "DOUBLE"),
      new DataField("Zp_FREQ6", "DOUBLE"),
      new DataField("Zp_DATA7", "DOUBLE"),
      new DataField("Zp_FREQ7", "DOUBLE"),
      new DataField("Zp_DATA8", "DOUBLE"),
      new DataField("Zp_FREQ8", "DOUBLE"),
      new DataField("Zp_DATA9", "DOUBLE"),
      new DataField("Zp_FREQ9", "DOUBLE"),
      new DataField("Zp_DATA10", "DOUBLE"),
      new DataField("Zp_FREQ10", "DOUBLE"),
      new DataField("MEASUREMENT_ID", "BIGINT")};
	
	private static DataField[] dynamicRawProcDataField = {
		new DataField("TIMESTAMP", "BIGINT"), 
		new DataField("GENERATION_TIME", "BIGINT"),
		new DataField("DEVICE_ID", "INTEGER"),
		
		new DataField("DURATION", "DOUBLE"),
    new DataField("TIME_OF_DATA", "DOUBLE"),
    new DataField("X_DATA", "VARCHAR(8192)"),
    new DataField("Y_DATA", "VARCHAR(8192)"),
    new DataField("Z_DATA", "VARCHAR(8192)"),
    new DataField("Xp_DATA1", "DOUBLE"),
    new DataField("Xp_FREQ1", "DOUBLE"),
    new DataField("Xp_DATA2", "DOUBLE"),
    new DataField("Xp_FREQ2", "DOUBLE"),
    new DataField("Xp_DATA3", "DOUBLE"),
    new DataField("Xp_FREQ3", "DOUBLE"),
    new DataField("Xp_DATA4", "DOUBLE"),
    new DataField("Xp_FREQ4", "DOUBLE"),
    new DataField("Xp_DATA5", "DOUBLE"),
    new DataField("Xp_FREQ5", "DOUBLE"),
    new DataField("Xp_DATA6", "DOUBLE"),
    new DataField("Xp_FREQ6", "DOUBLE"),
    new DataField("Xp_DATA7", "DOUBLE"),
    new DataField("Xp_FREQ7", "DOUBLE"),
    new DataField("Xp_DATA8", "DOUBLE"),
    new DataField("Xp_FREQ8", "DOUBLE"),
    new DataField("Xp_DATA9", "DOUBLE"),
    new DataField("Xp_FREQ9", "DOUBLE"),
    new DataField("Xp_DATA10", "DOUBLE"),
    new DataField("Xp_FREQ10", "DOUBLE"),
    new DataField("Yp_DATA1", "DOUBLE"),
    new DataField("Yp_FREQ1", "DOUBLE"),
    new DataField("Yp_DATA2", "DOUBLE"),
    new DataField("Yp_FREQ2", "DOUBLE"),
    new DataField("Yp_DATA3", "DOUBLE"),
    new DataField("Yp_FREQ3", "DOUBLE"),
    new DataField("Yp_DATA4", "DOUBLE"),
    new DataField("Yp_FREQ4", "DOUBLE"),
    new DataField("Yp_DATA5", "DOUBLE"),
    new DataField("Yp_FREQ5", "DOUBLE"),
    new DataField("Yp_DATA6", "DOUBLE"),
    new DataField("Yp_FREQ6", "DOUBLE"),
    new DataField("Yp_DATA7", "DOUBLE"),
    new DataField("Yp_FREQ7", "DOUBLE"),
    new DataField("Yp_DATA8", "DOUBLE"),
    new DataField("Yp_FREQ8", "DOUBLE"),
    new DataField("Yp_DATA9", "DOUBLE"),
    new DataField("Yp_FREQ9", "DOUBLE"),
    new DataField("Yp_DATA10", "DOUBLE"),
    new DataField("Yp_FREQ10", "DOUBLE"),
    new DataField("Zp_DATA1", "DOUBLE"),
    new DataField("Zp_FREQ1", "DOUBLE"),
    new DataField("Zp_DATA2", "DOUBLE"),
    new DataField("Zp_FREQ2", "DOUBLE"),
    new DataField("Zp_DATA3", "DOUBLE"),
    new DataField("Zp_FREQ3", "DOUBLE"),
    new DataField("Zp_DATA4", "DOUBLE"),
    new DataField("Zp_FREQ4", "DOUBLE"),
    new DataField("Zp_DATA5", "DOUBLE"),
    new DataField("Zp_FREQ5", "DOUBLE"),
    new DataField("Zp_DATA6", "DOUBLE"),
    new DataField("Zp_FREQ6", "DOUBLE"),
    new DataField("Zp_DATA7", "DOUBLE"),
    new DataField("Zp_FREQ7", "DOUBLE"),
    new DataField("Zp_DATA8", "DOUBLE"),
    new DataField("Zp_FREQ8", "DOUBLE"),
    new DataField("Zp_DATA9", "DOUBLE"),
    new DataField("Zp_FREQ9", "DOUBLE"),
    new DataField("Zp_DATA10", "DOUBLE"),
    new DataField("Zp_FREQ10", "DOUBLE"),
	  new DataField("MEASUREMENT_ID", "BIGINT")};
	
	private static final Hashtable<String, NameDataFieldPair> statusNamingTable = new Hashtable<String, NameDataFieldPair>();
	static
	{
		statusNamingTable.put(STATIC_NAMING, new NameDataFieldPair(1, staticDataField));
		statusNamingTable.put(DYNAMIC_RAW_NAMING, new NameDataFieldPair(2, dynamicRawDataField));
		statusNamingTable.put(DYNAMIC_PROC_NAMING, new NameDataFieldPair(3, dynamicProcDataField));
		statusNamingTable.put(DYNAMIC_RAW_PROC_NAMING, new NameDataFieldPair(4, dynamicRawProcDataField));
	}

	private final transient Logger logger = Logger.getLogger( STEVALPlugin.class );

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
		logger.info("using STEVAL data type: " + statusDataType);
        
        registerListener();

        if (statusDataType.equalsIgnoreCase(STATIC_NAMING))
        	setName("STEVALPlugin-Static-" + coreStationName + "-Thread");
        else if (statusDataType.equalsIgnoreCase(DYNAMIC_RAW_NAMING))
        	setName("STEVALPlugin-Dyn-Raw-" + coreStationName + "-Thread");
        else if (statusDataType.equalsIgnoreCase(DYNAMIC_PROC_NAMING))
        	setName("STEVALPlugin-Dyn-Proc-" + coreStationName + "-Thread");
        else if (statusDataType.equalsIgnoreCase(DYNAMIC_RAW_PROC_NAMING))
        	setName("STEVALPlugin-Dyn-Raw-Proc-" + coreStationName + "-Thread");
		
		return true;
	}

	@Override
	public short getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.STEVAL_MESSAGE_TYPE;
	}

	@Override
	public DataField[] getOutputFormat() {
		return statusNamingTable.get(statusDataType).dataField;
	}


	@Override
	public String getPluginName() {
        if (statusDataType.equalsIgnoreCase(STATIC_NAMING))
        	return "STEVALPlugin-Static";
        else if (statusDataType.equalsIgnoreCase(DYNAMIC_RAW_NAMING))
        	return "STEVALPlugin-Dyn-Raw";
        else if (statusDataType.equalsIgnoreCase(DYNAMIC_PROC_NAMING))
        	return "STEVALPlugin-Dyn-Proc";
        else if (statusDataType.equalsIgnoreCase(DYNAMIC_RAW_PROC_NAMING))
        	return "STEVALPlugin-Dyn-Raw-Proc";
        else
        	return "STEVALPlugin";
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
				else if (statusDataType.equalsIgnoreCase(DYNAMIC_RAW_NAMING)) {
					data = checkAndCastData(data, 1, dynamicRawDataField, 3);
				}
				else if (statusDataType.equalsIgnoreCase(DYNAMIC_PROC_NAMING)) {
					data = checkAndCastData(data, 1, dynamicProcDataField, 3);
				}
				else if (statusDataType.equalsIgnoreCase(DYNAMIC_RAW_PROC_NAMING)) {
					data = checkAndCastData(data, 1, dynamicRawProcDataField, 3);
				}
				else {
					logger.warn("Wrong STEVAL data type spedified.");
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
}
