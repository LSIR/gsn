package gsn.wrappers.backlog.plugins;

import java.io.Serializable;
import java.util.Hashtable;
import java.lang.Integer;

import org.apache.log4j.Logger;

import gsn.beans.DataField;
import gsn.wrappers.BackLogWrapper;
import gsn.wrappers.backlog.BackLogMessage;

public class GPSPlugin extends AbstractPlugin {
	private static final String GPS_DATA_TYPE = "gps-data-type";

	private static final String RAW_NAMING = "raw";
	private static final String NAV_NAMING = "nav";

	private static final DataField[] navDataField = {
		    new DataField("TIMESTAMP", "BIGINT"),
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("DEVICE_ID", "INTEGER"),
			new DataField("UTC_POS_TIME", "DOUBLE"),
			new DataField("LATITUDE", "DOUBLE"),
			new DataField("LAT_HEMISPHERE", "DOUBLE"),
			new DataField("LONGITUDE", "DOUBLE"),
			new DataField("LONG_HEMISPHERE", "DOUBLE"),
			new DataField("QUALITY", "SMALLINT"),
			new DataField("NR_SATELLITES", "SMALLINT"),
			new DataField("HDOP", "SMALLINT"),
			new DataField("GEOID_HEIGHT", "SMALLINT"),
			new DataField("HEIGHT_DIF", "SMALLINT"),
			new DataField("AGE_DGPS", "SMALLINT"),
			new DataField("CHECKSUM", "SMALLINT")
			};

	private static final DataField[] rawDataField = {
		    new DataField("TIMESTAMP", "BIGINT"),
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("DEVICE_ID", "INTEGER"),
			new DataField("GPS_RAW_DATA_VERSION", "SMALLINT"),
			new DataField("GPS_SAMPLE_COUNT", "INTEGER"),
			new DataField("GPS_SATS", "INTEGER"),
			new DataField("GPS_RAW_DATA", "BINARY")
			};
	
	private static final Hashtable<String, NameDataFieldPair> gpsNamingTable = new Hashtable<String, NameDataFieldPair>();
	static
	{
		gpsNamingTable.put(NAV_NAMING, new NameDataFieldPair(1, navDataField));
		gpsNamingTable.put(RAW_NAMING, new NameDataFieldPair(2, rawDataField));
	}
	
	private final transient Logger logger = Logger.getLogger( GPSPlugin.class );

	private String gpsDataType;
	
	
	@Override
	public boolean initialize ( BackLogWrapper backlogwrapper, String coreStationName, String deploymentName) {
		super.activeBackLogWrapper = backlogwrapper;
		try {
			gpsDataType = getActiveAddressBean().getPredicateValueWithException(GPS_DATA_TYPE).toLowerCase();
		} catch (Exception e) {
			logger.error(gpsDataType);
			logger.error(e.getMessage());
			return false;
		}
		if (gpsNamingTable.get(gpsDataType) == null) {
			logger.error("wrong " + GPS_DATA_TYPE + " predicate key specified in virtual sensor XML file! (" + GPS_DATA_TYPE + "=" + gpsDataType + ")");
			return false;
		}
		logger.info("using GPS data type: " + gpsDataType);
        
        registerListener();

        if (gpsDataType.equalsIgnoreCase(RAW_NAMING))
        	setName("GPSPlugin-RAW-" + coreStationName + "-Thread");
        else if (gpsDataType.equalsIgnoreCase(NAV_NAMING))
    		setName("GPSPlugin-NAV-" + coreStationName + "-Thread");
		
		return true;
	}
	
	@Override
	public byte getMessageType() {
		return BackLogMessage.GPS_MESSAGE_TYPE;
	}

	@Override
	public DataField[] getOutputFormat() {
		return gpsNamingTable.get(gpsDataType).dataField;
	}

	@Override
	public String getPluginName() {
		return "GPSPlugin";
	}

	@Override
	public boolean messageReceived(int deviceId, long timestamp, Serializable[] data) {
		if (logger.isDebugEnabled())
			logger.debug("message received from CoreStation with DeviceId: " + deviceId);
		
		Serializable[] out = null;

		try {
			short msgType = toShort(data[0]);
			
			if (msgType == gpsNamingTable.get(gpsDataType).typeNumber) {
				logger.debug("msgType: " + msgType);
				if (gpsDataType.equalsIgnoreCase(NAV_NAMING)) {
					
				}
				else if (gpsDataType.equalsIgnoreCase(RAW_NAMING)) {
					out = new Serializable[]{timestamp, timestamp, deviceId, toShort(data[1]), toInteger(data[2]), toInteger(data[3]), data[4]};
				}
				else {
					logger.warn("Wrong GPS data type spedified.");
					return true;
				}
				
				if( dataProcessed(System.currentTimeMillis(), out) ) {
					ackMessage(timestamp, super.priority);
				} else {
					logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");
				}
			}
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			return true;
		}
		
		return true;
	}
}
