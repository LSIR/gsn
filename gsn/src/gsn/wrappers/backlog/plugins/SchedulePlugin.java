package gsn.wrappers.backlog.plugins;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;

import gsn.Main;
import gsn.beans.DataField;



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
	
	private static final byte TYPE_NO_SCHEDULE_AVAILABLE = 0;
	private static final byte TYPE_SCHEDULE_SAME = 1;
	private static final byte TYPE_NEW_SCHEDULE = 2;
	private static final byte GSN_TYPE_GET_SCHEDULE = 3;

	private final transient Logger logger = Logger.getLogger( SchedulePlugin.class );
	
	private DataField[] dataField = {new DataField("DEVICE_ID", "INTEGER"),
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("TRANSMISSION_TIME", "BIGINT"),
			new DataField("SCHEDULE", "binary")};

	@Override
	public byte getMessageType() {
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
				// get the newest schedule from the SQL database
				conn = Main.getStorage(getActiveAddressBean().getVirtualSensorName()).getConnection();
				StringBuilder query = new StringBuilder();
				query.append("select * from ").append(activeBackLogWrapper.getActiveAddressBean().getVirtualSensorName()).append(" where device_id = ").append(deviceId).append(" order by timed desc limit 1");
				rs = Main.getStorage(getActiveAddressBean().getVirtualSensorName()).executeQueryWithResultSet(query, conn);
				
				if (rs.next()) {
					// get the creation time of the newest schedule
					long creationtime = rs.getLong("generation_time");
					long transmissiontime = rs.getLong("transmission_time");
					Integer id = rs.getInt("device_id");
					byte[] schedule = rs.getBytes("schedule");
					Main.getStorage(getActiveAddressBean().getVirtualSensorName()).close(conn);

					if (logger.isDebugEnabled())
						logger.debug("creation time: " + creationtime);
					if (timestamp ==  creationtime) {
						// if the schedule on the deployment has the same creation
						// time as the newest one in the database, we do not have
						// to resend it
						if (logger.isDebugEnabled())
							logger.debug("no new schedule available");
						Serializable [] pkt = {TYPE_SCHEDULE_SAME};
						try {
							sendRemote(System.currentTimeMillis(), pkt, super.priority);
						} catch (IOException e) {
							logger.warn(e.getMessage());
						}
					}
					else {
						// send the new schedule to the deployment
						if (logger.isDebugEnabled())
							logger.debug("send new schedule (" + new String(schedule) + ")");
	
						Serializable [] pkt = {TYPE_NEW_SCHEDULE, creationtime, schedule};
						try {
							sendRemote(System.currentTimeMillis(), pkt, super.priority);
							
							if (transmissiontime == 0) {
								long time = System.currentTimeMillis();
								Serializable[] out = {id, creationtime, time, schedule};
								dataProcessed(time, out);
							}
						} catch (IOException e) {
							logger.warn(e.getMessage());
						}
					}
				} else {
					try {
						// we do not have any schedule available in the database
						Serializable [] pkt = {TYPE_NO_SCHEDULE_AVAILABLE};
						sendRemote(System.currentTimeMillis(), pkt, super.priority);
						logger.warn("schedule request received but no schedule available in database");
					} catch (IOException e) {
						logger.warn(e.getMessage());
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			} finally {
				Main.getStorage(getActiveAddressBean().getVirtualSensorName()).close(rs);
				Main.getStorage(getActiveAddressBean().getVirtualSensorName()).close(conn);
			}
			return true;
		}
		else {
			logger.error("unknown message type received");
			return false;
		}
	}

	@Override
	public boolean sendToPlugin(String action, String[] paramNames, Object[] paramValues) {
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
			
			Serializable [] pkt = {TYPE_NEW_SCHEDULE, time, schedule};
			boolean sent = false;
			// and try to send it to the deployment
			try {
				sent = sendRemote(System.currentTimeMillis(), pkt, super.priority);
			} catch (IOException e) {
				logger.warn(e.getMessage());
			}
			if (sent) {
				Serializable[] data = {id, time, time, schedule};
				dataProcessed(time, data);

				logger.info("Received schedule which has been directly transmitted");
			}
			else {
				Serializable[] data = {id, time, null, schedule};
				dataProcessed(time, data);

				logger.info("Received schedule and will transmit it the next time it is requested.");
			}

			return true;
		}
		else
			return false;
	}
}
