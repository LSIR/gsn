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
			new DataField("LWB_T_TO_RX", "INTEGER"),
			new DataField("LWB_T_FLOOD", "INTEGER"),
			new DataField("LWB_N_RX_STARTED", "SMALLINT")};

	@Override
	public Serializable[] receivePayload(ByteBuffer payload) throws Exception {
		Long uptime = null;
		Integer temp = null;
		Integer vcc = null;
		Integer cpu_dc = null;
		Integer rf_dc = null;
		Integer lwb_rx_cnt = null;
		Integer lwb_n_rx_hops = null;
		Integer rf_per = null;
		Short rf_snr = null;
		Short lwb_rssi1 = null;
		Short lwb_rssi2 = null;
		Short lwb_rssi3 = null;
		Integer lwb_fsr= null;
		Short lwb_tx_buf = null;
		Short lwb_rx_buf = null;
		Short lwb_tx_drop = null;
		Short lwb_rx_drop = null;
		Short lwb_bootstrap_cnt = null;
		Short lwb_sleep_cnt = null;
		Integer lwb_t_to_rx = null;
		Integer lwb_t_flood = null;
		Short lwb_n_rx_started = null;
		
		try {
			uptime = payload.getInt() & 0xffffffffL; 			// uint32_t: uptime in seconds
			temp = (int) payload.getShort(); 					// int16_t: temperature value in 100x °C
			vcc = payload.getShort() & 0xffff; 					// uint16_t: supply voltage (raw ADC value)
			cpu_dc = payload.getShort() & 0xffff; 				// uint16_t: cpu duty cycle in per thousands
			rf_dc = payload.getShort() & 0xffff; 				// uint16_t: radio duty cycle in per thousands
			lwb_rx_cnt = payload.getShort() & 0xffff; 			// uint16_t: reception counter (total # successfully rcvd pkts)
			lwb_n_rx_hops = payload.getShort() & 0xffff;		// uint16_t: RX count (CRC ok) + hop cnts of last Glossy flood
			rf_per = payload.getShort() & 0xffff; 				// uint16_t: total packet error rate in per 10'000
			rf_snr = (short) (payload.get() & 0xff); 			// uint8_t: signal-to-noise ratio of the last reception
			lwb_rssi1 = (short) payload.get(); 					// int8_t: RSSI value of the first Glossy flood
			lwb_rssi2 = (short) payload.get(); 					// int8_t: RSSI value of the second Glossy flood
			lwb_rssi3 = (short) payload.get(); 					// int8_t: RSSI value of the third Glossy flood
			lwb_fsr = payload.getShort() & 0xffff; 				// uint16_t: LWB flood success rate in per 10'000
			lwb_tx_buf = (short) (payload.get() & 0xff); 		// uint8_t: number of packets in the transmit buffer
			lwb_rx_buf = (short) (payload.get() & 0xff); 		// uint8_t: number of packets in the receive buffer
			lwb_tx_drop = (short) (payload.get() & 0xff); 		// uint8_t: dropped tx packets since last health message
			lwb_rx_drop = (short) (payload.get() & 0xff); 		// uint8_t: dropped rx packets since last health message
			lwb_bootstrap_cnt = (short) (payload.get() & 0xff);	// uint8_t: 
			lwb_sleep_cnt = (short) (payload.get() & 0xff);		// uint8_t: 
			lwb_t_to_rx = payload.getShort() & 0xffff;			// uint16_t: time[us] to first rx (offset to glossy_start)
			lwb_t_flood = payload.getShort() & 0xffff;			// uint16_t: flood duration [us]
			lwb_n_rx_started = (short) (payload.get() & 0xff);	// uint8_t: preambles+sync det. in last flood
		} catch (Exception e) {
		}
        
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
