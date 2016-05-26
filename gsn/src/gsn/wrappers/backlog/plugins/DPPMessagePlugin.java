package gsn.wrappers.backlog.plugins;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.log4j.Logger;

import gsn.beans.DataField;


/**
 * This plugin listens for incoming LWB DPP messages.
 * 
 * @author Tonio Gsell
 */
public class DPPMessagePlugin extends AbstractPlugin {
	
	private static DataField[] dataField = {
			new DataField("TIMESTAMP", "BIGINT"),
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("GENERATION_TIME_MICROSEC", "BIGINT"),
			new DataField("DEVICE_ID", "INTEGER"),
			new DataField("MESSAGE_TYPE", "INTEGER"),
			new DataField("SEQNR", "INTEGER"),
			new DataField("PAYLOAD_LENGTH", "INTEGER"),
            
			new DataField("TEMP", "INTEGER"),
			new DataField("VCC", "INTEGER"),
			new DataField("CPU_DC", "INTEGER"),
			new DataField("RF_DC", "INTEGER"),
			new DataField("RX_CNT", "INTEGER"),
			new DataField("N_RX_HOPS", "INTEGER"),
			new DataField("PER", "SMALLINT"),
			new DataField("SNR", "SMALLINT"),
			new DataField("RSSI1", "SMALLINT"),
			new DataField("RSSI2", "SMALLINT"),
			new DataField("RSSI3", "SMALLINT")};

	private final transient Logger logger = Logger.getLogger( DPPMessagePlugin.class );

	@Override
	public String getPluginName() {
		return "DPPMessagPlugin";
	}

	@Override
	public boolean messageReceived(int deviceId, long timestamp, Serializable[] data) {
		try {
			int device_id = toInteger(data[0]);
            int type = toInteger(data[1]);
            int payload_len = toInteger(data[2]);
            int seq_no = toInteger(data[3]);
            long generation_time = toLong(data[4]);
    		ByteBuffer payload = ByteBuffer.wrap((byte[]) data[5]);
    		payload.order(ByteOrder.LITTLE_ENDIAN);
            
    		int temp = payload.getShort() & 0xffff; // uint16_t
    		int vcc = payload.getShort() & 0xffff; // uint16_t
    		int cpu_dc = payload.getShort() & 0xffff; // uint16_t
    		int rf_dc = payload.getShort() & 0xffff; // uint16_t
    		int rx_cnt = payload.getShort() & 0xffff; // uint16_t
    		int n_rx_hops = payload.getShort() & 0xffff; // uint16_t
    		short per = payload.get();
    		short snr = payload.get();
    		short rssi1 = payload.get();
    		short rssi2 = payload.get();
    		short rssi3 = payload.get();
            
			if( dataProcessed(System.currentTimeMillis(), new Serializable[]{timestamp, (long)(generation_time/1000.0), generation_time, device_id, type, seq_no, payload_len, temp, vcc, cpu_dc, rf_dc, rx_cnt, n_rx_hops, per, snr, rssi1, rssi2, rssi3}) ) {
				ackMessage(timestamp, super.priority);
				return true;
			} else {
				logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return false;
	}

	@Override
	public short getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.DPP_MESSAGE_TYPE;
	}

	@Override
	public DataField[] getOutputFormat() {
		return dataField;
	}

}
