package gsn.wrappers.backlog.plugins;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.ResultSet;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;

import gsn.Main;
import gsn.beans.DataField;
import gsn.beans.InputInfo;



/**
 * The SchedulePlugin offers the functionality to schedule different
 * jobs (bash scripts, programs, etc.) on the deployment system in a
 * well defined interval. The schedule is formated in a crontab-like
 * manner and can be defined and altered on side of GSN as needed using
 * the virtual sensors web input. A new schedule will be directly
 * transmitted to the deployment if a connection exists or will be
 * requested as soon as a connection opens.
 * 
 * This plugin accepts a schedule file from the web input and stores it
 * in the SQL database. It tries to send it directly to the deployment.
 * It answers on a 'get schedule request' from the deployment with a
 * new schedule if one exists, with a 'no schedule available' message
 * if no schedule is available or with a 'same schedule' message if
 * the newest message available has already been transmitted to the
 * deployment.
 * 
 * @author Tonio Gsell
 */
public class SchedulePlugin extends AbstractPlugin {
	
	private static final byte TYPE_NO_SCHEDULE = 0;
	private static final byte TYPE_NO_NEW_SCHEDULE = 1;
	private static final byte TYPE_SCHEDULE = 2;
	private static final byte GSN_TYPE_GET_SCHEDULE = 3;

	private final transient Logger logger = Logger.getLogger( SchedulePlugin.class );
	
	private DataField[] dataField = {new DataField("DEVICE_ID", "INTEGER"),
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("TRANSMISSION_TIME", "BIGINT"),
			new DataField("GENERATED_BY", "VARCHAR(256)"),
			new DataField("SCHEDULE", "binary")};

	@Override
	public short getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.SCHEDULE_MESSAGE_TYPE;
	}

	@Override
	public DataField[] getOutputFormat() {
		return dataField;
	}

	@Override
	public String getPluginName() {
		return "SchedulePlugin";
	}

	@Override
	public boolean messageReceived(int deviceId, long timestamp, Serializable[] data) {
		if (((Byte)data[0]) == GSN_TYPE_GET_SCHEDULE) {
			Connection conn = null;
			ResultSet rs = null;
			try {
				Serializable [] reply;
				// get the newest schedule from the SQL database
				conn = Main.getStorage(getActiveAddressBean().getVirtualSensorConfig()).getConnection();
				StringBuilder query = new StringBuilder();
				query.append("select * from ").append(activeBackLogWrapper.getActiveAddressBean().getVirtualSensorName()).append(" where device_id = ").append(deviceId).append(" and transmission_time is null order by timed desc limit 1");
				rs = Main.getStorage(getActiveAddressBean().getVirtualSensorConfig()).executeQueryWithResultSet(query, conn);
				
				byte[] schedule = null;
				long timestamp_gsn = 0;
				if (rs.next()) {
					query = new StringBuilder();
					schedule = rs.getBytes("schedule");
					timestamp_gsn = rs.getLong("generation_time");
					query.append("select * from ").append(activeBackLogWrapper.getActiveAddressBean().getVirtualSensorName()).append(" where device_id = ").append(deviceId).append(" and generation_time = ").append(rs.getLong("generation_time")).append(" and transmission_time is not null order by timed desc limit 1");
					ResultSet result = Main.getStorage(getActiveAddressBean().getVirtualSensorConfig()).executeQueryWithResultSet(query, conn);
					if (result.next())
						schedule = null;
				}
				Main.getStorage(getActiveAddressBean().getVirtualSensorConfig()).close(rs);
				
				if (schedule == null) {
					query = new StringBuilder();
					query.append("select * from ").append(activeBackLogWrapper.getActiveAddressBean().getVirtualSensorName()).append(" where device_id = ").append(deviceId).append(" order by timed desc limit 1");
					rs = Main.getStorage(getActiveAddressBean().getVirtualSensorConfig()).executeQueryWithResultSet(query, conn);
					
					if (rs.next()) {
						// get the creation time of the newest schedule
						timestamp_gsn = rs.getLong("generation_time");
						schedule = rs.getBytes("schedule");
	
						if (timestamp_gsn <= timestamp) {
							// if the schedule on the deployment has the same or a newer
							// creation time as the newest one in the database, we do not have
							// to resend anything
							if (logger.isDebugEnabled())
								logger.debug("no newer schedule available");
							reply = new Serializable [] {TYPE_NO_NEW_SCHEDULE};
						}
						else {
							// send the new schedule to the deployment
							if (logger.isDebugEnabled())
								logger.debug("send new schedule (" + new String(schedule) + ")");
	
							reply = new Serializable [] {TYPE_SCHEDULE, timestamp_gsn, schedule};
						}
					} else {
						// we do not have any schedule available in the database
						reply = new Serializable []  {TYPE_NO_SCHEDULE};
						logger.warn("schedule request received but no schedule available in database");
					}
				}
				else {
					// a manually uploaded schedule has not yet been transmitted -> transmit it to the CoreStation
					if (logger.isDebugEnabled())
						logger.debug("send manually uploaded schedule (" + new String(schedule) + ")");
					reply = new Serializable [] {TYPE_SCHEDULE, timestamp_gsn, schedule};
				}
				
				try {
					sendRemote(System.currentTimeMillis(), reply, super.priority);
				} catch (IOException e) {
					logger.warn(e.getMessage());
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			} finally {
				Main.getStorage(getActiveAddressBean().getVirtualSensorConfig()).close(rs);
				Main.getStorage(getActiveAddressBean().getVirtualSensorConfig()).close(conn);
			}
			return true;
		} else if (((Byte)data[0]) == TYPE_SCHEDULE) {
			long time = System.currentTimeMillis();
			try {
				if(dataProcessed(time, new Serializable[] {deviceId, timestamp, time, (String)data[1], ((String)data[2]).getBytes("UTF-8")}))
					ackMessage(timestamp, super.priority);
				else
					return false;
			} catch (UnsupportedEncodingException e) {
				logger.error(e.getMessage(), e);
				return false;
			}
			return true;
		}
		else {
			logger.error("unknown message type received");
			return false;
		}
	}

	@Override
	public InputInfo sendToPlugin(String action, String[] paramNames, Object[] paramValues) {
		if( action.compareToIgnoreCase("schedule_command") == 0 ) {
			byte [] schedule = null;
			int id = -1;
			long time = System.currentTimeMillis();
			for (int i = 0 ; i < paramNames.length ; i++) {
				if( paramNames[i].compareToIgnoreCase("schedule") == 0 ) {
					// store the schedule received from the web input in the database
					schedule = ((FileItem)paramValues[i]).get();
				}
				else if( paramNames[i].compareToIgnoreCase("core_station") == 0 ) {
					id = Integer.parseInt((String)paramValues[i]);
				}
			}

			dataProcessed(time, new Serializable[] {id, time, null, "GSN", schedule});
			logger.info("Received manually uploaded schedule.");
			
			// and try to send it to the deployment
			try {
				if (sendRemote(System.currentTimeMillis(), new Serializable [] {TYPE_SCHEDULE, time, schedule}, super.priority))
					return new InputInfo(getActiveAddressBean().toString(), "schedule successfully sent to CoreStation", true);
				else
					return new InputInfo(getActiveAddressBean().toString(), "schedule could not be sent to CoreStation", false);
			} catch (IOException e) {
				logger.warn(e.getMessage());
				return new InputInfo(getActiveAddressBean().toString(), e.getMessage(), false);
			}
		}
		else
			return new InputInfo(getActiveAddressBean().toString(), "action >" + action + "< not supported", false);
	}
}
