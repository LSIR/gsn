package gsn.wrappers.backlog.plugins;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;

import org.apache.log4j.Logger;

import gsn.beans.DataField;
import gsn.storage.StorageManager;

public class SchedulePlugin extends AbstractPlugin {
	

	private static final byte TYPE_NO_SCHEDULE_AVAILABLE = 0;
	private static final byte TYPE_SCHEDULE_SAME = 1;
	private static final byte TYPE_NEW_SCHEDULE = 2;
	private static final byte GSN_TYPE_GET_SCHEDULE = 3;

	private final transient Logger logger = Logger.getLogger( SchedulePlugin.class );
	
	private DataField[] dataField = {new DataField("CREATION_TIME", "BIGINT"),
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
	public boolean messageReceived(long timestamp, byte[] payload) {
		if (payload[0] == GSN_TYPE_GET_SCHEDULE) {
			Connection conn = null;
			try {
				conn = StorageManager.getInstance().getConnection();
				StringBuilder query = new StringBuilder();
				query.append("select * from ").append(activeBackLogWrapper.getActiveAddressBean().getVirtualSensorName()).append(" order by timed desc limit 1");
				ResultSet rs = StorageManager.executeQueryWithResultSet(query, conn);
				
				if (rs.next()) {
					long creationtime = rs.getLong("creation_time");
					logger.debug("creation time: " + creationtime);
					if (timestamp ==  creationtime) {
						logger.debug("no new schedule available");
						byte [] pkt = {TYPE_SCHEDULE_SAME};
						sendRemote(System.currentTimeMillis(), pkt);
					}
					else {
						byte[] schedule = rs.getBytes("schedule");
						logger.debug("send new schedule (" + new String(schedule) + ")");
	
						byte [] pkt = new byte [schedule.length + 9];
						pkt[0] = TYPE_NEW_SCHEDULE;
						System.arraycopy(long2arr(creationtime), 0, pkt, 1, 8);
						System.arraycopy(schedule, 0, pkt, 9, schedule.length);
						sendRemote(System.currentTimeMillis(), pkt);
						
						if (rs.getLong("transmission_time") == 0) {
							long time = System.currentTimeMillis();
							Serializable[] data = {creationtime, time, schedule};
							dataProcessed(time, data);
						}
					}
				} else {
					byte [] pkt = {TYPE_NO_SCHEDULE_AVAILABLE};
					sendRemote(System.currentTimeMillis(), pkt);
					logger.warn("schedule request received but no schedule available in database");
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			} finally {
				StorageManager.close(conn);
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
			for (int i = 0 ; i < paramNames.length ; i++) {
				if( paramNames[i].compareToIgnoreCase("schedule") == 0) {
					long time = System.currentTimeMillis();
					byte [] schedule = decode(((String)paramValues[i]).toCharArray());
					
					byte [] pkt = new byte [schedule.length + 9];
					pkt[0] = TYPE_NEW_SCHEDULE;
					System.arraycopy(long2arr(time), 0, pkt, 1, 8);
					System.arraycopy(schedule, 0, pkt, 9, schedule.length);
					boolean sent = false;
					try {
						sent = sendRemote(System.currentTimeMillis(), pkt);
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
					if (sent) {
						Serializable[] data = {time, time, schedule};
						dataProcessed(time, data);
					}
					else {
						Serializable[] data = {time, null, schedule};
						dataProcessed(time, data);
					}
				}
			}
		}
		else
			return false;

		logger.info("Received schedule and will transmit it the next time it is requested.");

		return true;
	}
	
	
	private static char[]    map1 = new char[64];
	   static {
	      int i=0;
	      for (char c='A'; c<='Z'; c++) map1[i++] = c;
	      for (char c='a'; c<='z'; c++) map1[i++] = c;
	      for (char c='0'; c<='9'; c++) map1[i++] = c;
	      map1[i++] = '+'; map1[i++] = '/'; }

	// Mapping table from Base64 characters to 6-bit nibbles.
	private static byte[]    map2 = new byte[128];
	   static {
	      for (int i=0; i<map2.length; i++) map2[i] = -1;
	      for (int i=0; i<64; i++) map2[map1[i]] = (byte)i; }
	   
   /**
   * Decodes a byte array from Base64 format.
   * No blanks or line breaks are allowed within the Base64 encoded input data.
   * @param in  A character array containing the Base64 encoded data.
   * @return    An array containing the decoded data bytes.
   * @throws    IllegalArgumentException If the input is not valid Base64 encoded data.
   */
   public static byte[] decode (char[] in) {
      return decode(in, 0, in.length); }

	
	/**
	* Decodes a byte array from Base64 format.
	* No blanks or line breaks are allowed within the Base64 encoded input data.
	* @param in    A character array containing the Base64 encoded data.
	* @param iOff  Offset of the first character in <code>in</code> to be processed.
	* @param iLen  Number of characters to process in <code>in</code>, starting at <code>iOff</code>.
	* @return      An array containing the decoded data bytes.
	* @throws      IllegalArgumentException If the input is not valid Base64 encoded data.
	*/
	public static byte[] decode (char[] in, int iOff, int iLen) {
	   if (iLen%4 != 0) throw new IllegalArgumentException ("Length of Base64 encoded input string is not a multiple of 4.");
	   while (iLen > 0 && in[iOff+iLen-1] == '=') iLen--;
	   int oLen = (iLen*3) / 4;
	   byte[] out = new byte[oLen];
	   int ip = iOff;
	   int iEnd = iOff + iLen;
	   int op = 0;
	   while (ip < iEnd) {
	      int i0 = in[ip++];
	      int i1 = in[ip++];
	      int i2 = ip < iEnd ? in[ip++] : 'A';
	      int i3 = ip < iEnd ? in[ip++] : 'A';
	      if (i0 > 127 || i1 > 127 || i2 > 127 || i3 > 127)
	         throw new IllegalArgumentException ("Illegal character in Base64 encoded data.");
	      int b0 = map2[i0];
	      int b1 = map2[i1];
	      int b2 = map2[i2];
	      int b3 = map2[i3];
	      if (b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0)
	         throw new IllegalArgumentException ("Illegal character in Base64 encoded data.");
	      int o0 = ( b0       <<2) | (b1>>>4);
	      int o1 = ((b1 & 0xf)<<4) | (b2>>>2);
	      int o2 = ((b2 &   3)<<6) |  b3;
	      out[op++] = (byte)o0;
	      if (op<oLen) out[op++] = (byte)o1;
	      if (op<oLen) out[op++] = (byte)o2; }
	   return out; }

}
