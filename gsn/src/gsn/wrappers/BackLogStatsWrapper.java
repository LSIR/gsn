package gsn.wrappers;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.wrappers.backlog.BackLogMessage;
import gsn.wrappers.backlog.statistics.DeploymentStatistics;
import gsn.wrappers.backlog.statistics.StatisticListener;
import gsn.wrappers.backlog.statistics.StatisticsMain;

public class BackLogStatsWrapper extends AbstractWrapper implements StatisticListener {
	private final transient Logger logger = Logger.getLogger( BackLogStatsWrapper.class );
	
	private final static int DEFAULT_SAMPLING_RATE_MS = 30000;
	
	private final static DataField[] outputStructure = new DataField[] {
			new DataField("generation_time", DataTypes.BIGINT),
			new DataField("device_id", DataTypes.INTEGER),
		
			new DataField("connected", DataTypes.TINYINT),

			new DataField("in_total_counter", DataTypes.BIGINT),
			new DataField("in_total_stuffed", DataTypes.BIGINT),
			new DataField("in_total_unstuffed", DataTypes.BIGINT),
			new DataField("out_total_counter", DataTypes.BIGINT),
			new DataField("out_total_stuffed", DataTypes.BIGINT),
			new DataField("out_total_unstuffed", DataTypes.BIGINT),
			
			new DataField("in_ack_counter", DataTypes.BIGINT),
			new DataField("in_ack_volume", DataTypes.BIGINT),
			new DataField("in_ping_counter", DataTypes.BIGINT),
			new DataField("in_ping_volume", DataTypes.BIGINT),
			new DataField("in_ping_ack_counter", DataTypes.BIGINT),
			new DataField("in_ping_ack_volume", DataTypes.BIGINT),
			
			new DataField("out_ack_counter", DataTypes.BIGINT),
			new DataField("out_ack_volume", DataTypes.BIGINT),
			new DataField("out_ping_counter", DataTypes.BIGINT),
			new DataField("out_ping_volume", DataTypes.BIGINT),
			new DataField("out_ping_ack_counter", DataTypes.BIGINT),
			new DataField("out_ping_ack_volume", DataTypes.BIGINT),
			
			new DataField("out_queue_limit_counter", DataTypes.BIGINT),
			new DataField("out_queue_limit_volume", DataTypes.BIGINT),
			new DataField("out_queue_ready_counter", DataTypes.BIGINT),
			new DataField("out_queue_ready_volume", DataTypes.BIGINT)
	};

	private int sampling_rate = DEFAULT_SAMPLING_RATE_MS;
	private boolean stopped = false;
	private Object event = new Object();
	private DeploymentStatistics stats;

	@Override
	public boolean initialize() {
		String deployment = getActiveAddressBean().getVirtualSensorName().split("_")[0].toLowerCase();
		
		stats = StatisticsMain.getDeploymentStatsInstance(deployment, this);
		
		String predicate = getActiveAddressBean().getPredicateValue("sampling-rate");
		if ( predicate != null ) {
			try {
				sampling_rate = Integer.parseInt(predicate)*1000;
			} catch (NumberFormatException e) {
				logger.warn("sampling-rate is not parsable, set to default ("+DEFAULT_SAMPLING_RATE_MS+"ms)");
			}
		}

		return true;
	}
	
	
	public void run() {
		long timestamp;
		
		while (!stopped) {
			timestamp = System.currentTimeMillis();

			Map<Integer, Boolean> connected = stats.isConnectedList();

			if (connected != null) {
				Iterator<Integer> iter = connected.keySet().iterator();
				
				if (iter != null) {
					for (; iter.hasNext();) {
						int deviceid = iter.next();
						
						generateStreamElement(timestamp, deviceid);
					}
				}
			}
			
			try {
				synchronized (event) {
					event.wait(sampling_rate);
				}
			} catch (InterruptedException e) {
				break;
			}
		}
	}
	
	
	private void generateStreamElement(long timestamp, int deviceid) {
		Serializable[] output = new Serializable[outputStructure.length];
		output[0] = timestamp;
		output[1] = deviceid;
		
		if (stats.isConnectedList().get(deviceid))
			output[2] = (byte)1;
		else
			output[2] = (byte)0;
		
		int counter = 3;
		for (Iterator<Map<Integer, Long>> it = getStatsList().iterator(); it.hasNext();)
			output[counter++] = it.next().get(deviceid);
		
		postStreamElement(new StreamElement(outputStructure, output, timestamp));
	}
	
	
	@SuppressWarnings("unchecked")
	private List<Map<Integer, Long>> getStatsList() {
		return Arrays.asList(
				stats.getTotalMsgRecvCounter(),
				stats.getTotalRecvByteCounter(),
				stats.getTotalMsgRecvByteCounter(),
				stats.getTotalMsgSendCounter(),
				stats.getTotalSendByteCounter(),
				stats.getTotalMsgSendByteCounter(),
				
				stats.getMsgRecvCounterList(BackLogMessage.ACK_MESSAGE_TYPE),
				stats.getMsgRecvByteCounterList(BackLogMessage.ACK_MESSAGE_TYPE),
				stats.getMsgRecvCounterList(BackLogMessage.PING_MESSAGE_TYPE),
				stats.getMsgRecvByteCounterList(BackLogMessage.PING_MESSAGE_TYPE),
				stats.getMsgRecvCounterList(BackLogMessage.PING_ACK_MESSAGE_TYPE),
				stats.getMsgRecvByteCounterList(BackLogMessage.PING_ACK_MESSAGE_TYPE),
				
				stats.getMsgSendCounterList(BackLogMessage.ACK_MESSAGE_TYPE),
				stats.getMsgSendByteCounterList(BackLogMessage.ACK_MESSAGE_TYPE),
				stats.getMsgSendCounterList(BackLogMessage.PING_MESSAGE_TYPE),
				stats.getMsgSendByteCounterList(BackLogMessage.PING_MESSAGE_TYPE),
				stats.getMsgSendCounterList(BackLogMessage.PING_ACK_MESSAGE_TYPE),
				stats.getMsgSendByteCounterList(BackLogMessage.PING_ACK_MESSAGE_TYPE),
				stats.getMsgSendCounterList(BackLogMessage.MESSAGE_QUEUE_LIMIT_MESSAGE_TYPE),
				stats.getMsgSendByteCounterList(BackLogMessage.MESSAGE_QUEUE_LIMIT_MESSAGE_TYPE),
				stats.getMsgSendCounterList(BackLogMessage.MESSAGE_QUEUE_READY_MESSAGE_TYPE),
				stats.getMsgSendByteCounterList(BackLogMessage.MESSAGE_QUEUE_READY_MESSAGE_TYPE));
	}


	@Override
	public void connectionStatusChanged(int deviceId) {
		generateStreamElement(System.currentTimeMillis(), deviceId);
	}

	
	@Override
	public DataField[] getOutputFormat() {
		return outputStructure;
	}

	
	@Override
	public String getWrapperName() {
		return "StatisticsWrapper";
	}

	
	@Override
	public void dispose() {
		stopped = true;
		synchronized (event) {
			event.notify();
		}
	}
}
