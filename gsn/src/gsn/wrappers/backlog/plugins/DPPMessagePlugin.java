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

			new DataField("UPTIME", "BIGINT"),
			new DataField("TEMP", "INTEGER"),
			new DataField("VCC", "INTEGER"),
			new DataField("CPU_DC", "INTEGER"),
			new DataField("RF_DC", "INTEGER"),
			new DataField("LWB_RX_CNT", "INTEGER"),
			new DataField("LWB_N_RX_HOPS", "INTEGER"),
			new DataField("RF_PER", "INTEGER"),
			new DataField("RF_SNR", "SMALLINT"),
			new DataField("LWB_RSSI1", "SMALLINT"),
			new DataField("LWB_RSSI2", "SMALLINT"),
			new DataField("LWB_RSSI3", "SMALLINT"),
			new DataField("LWB_FSR", "INTEGER"),
			new DataField("LWB_TX_BUF", "SMALLINT"),
			new DataField("LWB_RX_BUF", "SMALLINT"),
			new DataField("LWB_TX_DROP", "SMALLINT"),
			new DataField("LWB_RX_DROP", "SMALLINT"),
			new DataField("LWB_BOOTSTRAP_CNT", "SMALLINT"),
			new DataField("LWB_SLEEP_CNT", "SMALLINT"),
			new DataField("LFXT_TICKS", "BIGINT")};

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

    		long uptime = payload.getInt() & 0xffffffffL; 				// uint32_t: uptime in seconds
    		int temp = payload.getShort() & 0xffff; 					// int16_t: temperature value in 100x °C
    		int vcc = payload.getShort() & 0xffff; 						// uint16_t: supply voltage (raw ADC value)
    		int cpu_dc = payload.getShort() & 0xffff; 					// uint16_t: cpu duty cycle in per thousands
    		int rf_dc = payload.getShort() & 0xffff; 					// uint16_t: radio duty cycle in per thousands
    		int lwb_rx_cnt = payload.getShort() & 0xffff; 				// uint16_t: reception counter (total # successfully rcvd pkts)
    		int lwb_n_rx_hops = payload.getShort() & 0xffff;			// uint16_t: RX count (CRC ok) + hop cnts of last Glossy flood
    		int rf_per = payload.getShort() & 0xffff; 					// uint16_t: total packet error rate in per 10'000
    		short rf_snr = (short) (payload.get() & 0xff); 				// uint8_t: signal-to-noise ratio of the last reception
    		short lwb_rssi1 = payload.get(); 							// int8_t: RSSI value of the first Glossy flood
    		short lwb_rssi2 = payload.get(); 							// int8_t: RSSI value of the second Glossy flood
    		short lwb_rssi3 = payload.get(); 							// int8_t: RSSI value of the third Glossy flood
    		int lwb_fsr = payload.getShort() & 0xffff; 					// uint16_t: LWB flood success rate in per 10'000
    		short lwb_tx_buf = (short) (payload.get() & 0xff); 			// uint8_t: number of packets in the transmit buffer
    		short lwb_rx_buf = (short) (payload.get() & 0xff); 			// uint8_t: number of packets in the receive buffer
    		short lwb_tx_drop = (short) (payload.get() & 0xff); 		// uint8_t: dropped tx packets since last health message
    		short lwb_rx_drop = (short) (payload.get() & 0xff); 		// uint8_t: dropped rx packets since last health message
    		short lwb_bootstrap_cnt = (short) (payload.get() & 0xff);	// uint8_t: 
    		short lwb_sleep_cnt = (short) (payload.get() & 0xff);		// uint8_t: 
    		long lfxt_ticks = payload.getInt() & 0xffffffffL;			// uint32_t: in 32kHz ticks, rollover of ~36h
            
			if( dataProcessed(System.currentTimeMillis(), new Serializable[]{timestamp, (long)(generation_time/1000.0), generation_time, device_id, 
					type, seq_no, payload_len, uptime, temp, vcc, cpu_dc, rf_dc, lwb_rx_cnt, lwb_n_rx_hops, rf_per, rf_snr, lwb_rssi1, lwb_rssi2, 
					lwb_rssi3, lwb_fsr, lwb_tx_buf, lwb_rx_buf, lwb_tx_drop, lwb_rx_drop, lwb_bootstrap_cnt, lwb_sleep_cnt, lfxt_ticks}) ) {
				ackMessage(timestamp, super.priority);
				return true;
			} else {
				logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
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
