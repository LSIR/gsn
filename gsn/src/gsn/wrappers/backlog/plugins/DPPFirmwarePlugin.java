package gsn.wrappers.backlog.plugins;

import java.io.IOException;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;

import gsn.Main;
import gsn.beans.DataField;
import gsn.beans.InputInfo;
import gsn.wrappers.BackLogWrapper;

public class DPPFirmwarePlugin extends AbstractPlugin {

	public static final short FW_PKT_TYPE_DATA    = 0;     // firmware binary data with offset
	public static final short FW_PKT_TYPE_CHECK   = 1;     // request FW verification
	public static final short FW_PKT_TYPE_READY   = 2;     // response to a FW validation request
	public static final short FW_PKT_TYPE_DATAREQ = 3;     // request missing FW data packets
	public static final short FW_PKT_TYPE_UPDATE  = 4;     // initiate the FW update

	private static DataField[] dataField = {
			new DataField("TIMESTAMP", "BIGINT"),
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("GENERATION_TIME_MICROSEC", "BIGINT"),
			new DataField("DEVICE_ID", "INTEGER"),

			new DataField("TARGET_ID", "INTEGER"),
			new DataField("SEQNR", "INTEGER"),
			new DataField("VERSION", "INTEGER"),
			new DataField("MESSAGE_TYPE", "SMALLINT"),
			new DataField("MESSAGE", "VARCHAR(256)"),
			new DataField("FIRMWARE", "BINARY")};

	private final transient Logger logger = Logger.getLogger( DPPFirmwarePlugin.class );
	
	private int version;
	
	@Override
	public boolean initialize( BackLogWrapper backLogWrapper, String coreStationName, String deploymentName) {
		activeBackLogWrapper = backLogWrapper;
		String p = getActiveAddressBean().getPredicateValue("priority");
		if (p == null)
			priority = null;
		else
			priority = Integer.valueOf(p);
		
		version = 0;
		ResultSet rs = null;
		try {
			StringBuilder query = new StringBuilder();
			query.append("select version from ").append(getActiveAddressBean().getVirtualSensorName()).append(" where device_id = ").append(getDeviceID()).append(" order by timed desc limit 1");
			rs = Main.getStorage(getActiveAddressBean().getVirtualSensorConfig()).executeQueryWithResultSet(query, Main.getStorage(getActiveAddressBean().getVirtualSensorConfig()).getConnection());
		} catch (SQLException e) {
			logger.debug(e.getMessage());
		}

		try {
			if (rs != null) {
				while (rs.next()) {
					version = rs.getInt(1);
				}
			}
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
			return false;
		}
		logger.info("initial version: " + version);
		
		registerListener();
		return true;
	}
	
	@Override
	public boolean messageReceived(int deviceId, long timestamp, Serializable[] data) {
		try {
            Long generation_time = toLong(data[0]);
			Integer device_id = toInteger(data[1]);
			Integer target_id = toInteger(data[2]);
			Integer seqnr = toInteger(data[3]);
			Short type = toShort(data[4]);
			Integer ver = toInteger(data[5]);
            String message = (String)data[6];
            //TODO: to binary
            Serializable ihex = data[7];
			if( dataProcessed(System.currentTimeMillis(), new Serializable[] {timestamp, (long)(generation_time/1000.0), generation_time, device_id, target_id, seqnr, ver, type, message, ihex}) ) {
				ackMessage(timestamp, super.priority);
				return true;
			} else {
				logger.warn("The DPP firmware message with timestamp >" + timestamp + "< could not be stored in the database.");
			}
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
		return false;
	}

	@Override
	public InputInfo sendToPlugin(String action, String[] paramNames, Object[] paramValues) {
		long time = System.currentTimeMillis();
		if( action.compareToIgnoreCase("firmware_data") == 0 ) {
			byte [] ihex_file = null;
			version = version++ % 65535;
			logger.info("firmware upload with version: " + version);
			for (int i = 0 ; i < paramNames.length ; i++) {
				if( paramNames[i].compareToIgnoreCase("intel_hex_file") == 0 )
					ihex_file = ((FileItem)paramValues[i]).get();
			}
			if (ihex_file == null) {
				logger.warn("ihex_file is missing: could not upload DPP firmware data");
				return new InputInfo(getActiveAddressBean().toString(), "ihex_file is missing: could not upload DPP firmware data", false);
			}
			
			try {
				if (!sendRemote(System.currentTimeMillis(), new Serializable [] {FW_PKT_TYPE_DATA, version, ihex_file}, super.priority)) {
					dataProcessed(time, new Serializable[] {time, time, time*1000L, getDeviceID(), null, null, version, FW_PKT_TYPE_DATA, "no connection to the CoreStation: could not upload DPP firmware data -> try again later", ihex_file});
					logger.warn("no connection to the CoreStation: could not upload DPP firmware data -> try again later");
					return new InputInfo(getActiveAddressBean().toString(), "no connection to the CoreStation: could not upload DPP firmware data -> try again later", false);
				}
				else
					return new InputInfo(getActiveAddressBean().toString(), "DPP firmware uploaded", true);
			} catch (IOException e) {
				dataProcessed(time, new Serializable[] {time, time, time*1000L, getDeviceID(), null, null, version, FW_PKT_TYPE_DATA, e.getMessage() + ": could not upload DPP firmware data -> try again later", ihex_file});
				logger.warn(e.getMessage() + ": could not upload DPP firmware data -> try again later");
				return new InputInfo(getActiveAddressBean().toString(), e.getMessage() + ": could not upload DPP firmware data -> try again later", false);
			}
		}
		else if(action.compareToIgnoreCase("firmware_update") == 0) {
			Integer target_id = null;
			for (int i = 0 ; i < paramNames.length ; i++) {
				if( paramNames[i].compareToIgnoreCase("target_id") == 0 )
					target_id = new Integer((String)paramValues[i]);
			}
			if (target_id == null) {
				logger.warn("target_id is missing: could not upload DPP firmware update command");
				return new InputInfo(getActiveAddressBean().toString(), "target_id is missing: could not upload DPP firmware update command", false);
			}
			
			try {
				if (!sendRemote(System.currentTimeMillis(), new Serializable [] {FW_PKT_TYPE_UPDATE, version, target_id}, super.priority)) {
					dataProcessed(time, new Serializable[] {time, time, time*1000L, getDeviceID(), null, null, version, FW_PKT_TYPE_UPDATE, "no connection to the CoreStation: could not upload DPP firmware update command -> try again later", null});
					logger.warn("no connection to the CoreStation: could not upload DPP firmware update command -> try again later");
					return new InputInfo(getActiveAddressBean().toString(), "no connection to the CoreStation: could not upload DPP firmware update command -> try again later", false);
				}
				else
					return new InputInfo(getActiveAddressBean().toString(), "DPP firmware uploaded", true);
			} catch (IOException e) {
				dataProcessed(time, new Serializable[] {time, time, time*1000L, getDeviceID(), null, null, version, FW_PKT_TYPE_UPDATE, e.getMessage() + ": could not upload DPP firmware update command -> try again later", null});
				logger.warn(e.getMessage() + ": could not upload DPP firmware update command -> try again later");
				return new InputInfo(getActiveAddressBean().toString(), e.getMessage() + ": could not upload DPP firmware update command -> try again later", false);
			}
		}
		else {
			logger.warn("action >" + action + "< not supported");
			return new InputInfo(getActiveAddressBean().toString(), "action >" + action + "< not supported", false);
		}
	}

	@Override
	public short getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.DPP_FIRMWARE_TYPE;
	}

	@Override
	public DataField[] getOutputFormat() {
		return dataField;
	}

	@Override
	public String getPluginName() {
        return "DPPFirmwarePlugin";
	}

}
