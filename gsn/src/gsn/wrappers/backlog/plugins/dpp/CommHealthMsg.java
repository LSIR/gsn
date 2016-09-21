package gsn.wrappers.backlog.plugins.dpp;

import java.io.Serializable;
import java.nio.ByteBuffer;

import gsn.beans.DataField;

public class CommHealthMsg implements Message {
	
	private static DataField[] dataField = {
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
			new DataField("LWB_T_TO_RX", "BIGINT"),
			new DataField("LWB_T_FLOOD", "BIGINT"),
			new DataField("LWB_N_RX_STARTED", "BIGINT")};

	@Override
	public Serializable[] receivePayload(ByteBuffer payload) throws Exception {
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
		int lwb_t_to_rx = payload.getShort() & 0xffff;				// uint16_t: time[us] to first rx (offset to glossy_start)
		int lwb_t_flood = payload.getShort() & 0xffff;				// uint16_t: flood duration [us]
		short lwb_n_rx_started = (short) (payload.get() & 0xff);	// uint8_t: preambles+sync det. in last flood
        
		return new Serializable[]{uptime, temp, vcc, cpu_dc, rf_dc, lwb_rx_cnt, lwb_n_rx_hops, rf_per, rf_snr, lwb_rssi1, lwb_rssi2, 
				lwb_rssi3, lwb_fsr, lwb_tx_buf, lwb_rx_buf, lwb_tx_drop, lwb_rx_drop, lwb_bootstrap_cnt, lwb_sleep_cnt, lwb_t_to_rx, 
				lwb_t_flood, lwb_n_rx_started};
	}

	@Override
	public DataField[] getOutputFormat() {
		return dataField;
	}

	@Override
	public ByteBuffer sendPayload(String action, String[] paramNames, Object[] paramValues) throws Exception {
		throw new Exception("sendPayload not implemented");
	}

	@Override
	public int getType() {
		return gsn.wrappers.backlog.plugins.dpp.MessageTypes.MSG_TYPE_COMM_HEALTH;
	}

	@Override
	public boolean isExtended() {
		return false;
	}

	@Override
	public Serializable[] sendPayloadSuccess(boolean success) {
		return null;
	}
}
