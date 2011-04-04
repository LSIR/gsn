package gsn.wrappers;

import java.io.Serializable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.statistics.StatisticsElement;
import gsn.statistics.StatisticsHandler;
import gsn.statistics.StatisticsListener;

public class DataFlowStatsWrapper extends AbstractWrapper implements StatisticsListener {
	
	private final static int MAX_STATS_QUEUE_SIZE = 1000;
	
	private final static DataField[] outputStructure = new DataField[] {
				new DataField("TIMESTAMP", "BIGINT"),
				new DataField("EVENT_TYPE", "TINYINT"),
				new DataField("VS_NAME", "VARCHAR(255)"),
				new DataField("SOURCE", "VARCHAR(255)"),
				new DataField("STREAM", "VARCHAR(255)"),
				new DataField("VOLUME", "BIGINT")};

	protected final transient Logger logger = Logger.getLogger( DataFlowStatsWrapper.class );

	private BlockingQueue<StreamElement> seQueue;
	private boolean dispose = false;

	@Override
	public DataField[] getOutputFormat() {
		return outputStructure;
	}

	@Override
	public boolean initialize() {
		seQueue = new LinkedBlockingQueue<StreamElement>(MAX_STATS_QUEUE_SIZE);
		StatisticsHandler.getInstance().registerListener(this);
		return true;
	}

	@Override
	public void dispose() {
		StatisticsHandler.getInstance().deregisterListener(this);
		dispose = true;
		seQueue.offer(new StreamElement(outputStructure, new Serializable[]{(long)0, (byte)2, "end", "end", "end", (long)0}));
	}

	@Override
	public String getWrapperName() {
		return "DataFlowStatsWrapper";
	}

	@Override
	public String listenerName() {
		return getWrapperName();
	}
	
	public void run() {
		logger.info("thread started");
		StreamElement se = null;
		while (!dispose) {
			try {
				se = seQueue.take();
				if (dispose)
					break;
				postStreamElement(se);
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
			}
		}
		logger.info("thread died");
	}

	@Override
	public boolean inputEvent(String producerVS, StatisticsElement se) {
		Serializable [] data = new Serializable[]{se.getProcessTime(), (byte)0, producerVS, se.getSource(), se.getStream(), se.getVolume()};
		StreamElement streamElement = new StreamElement(outputStructure, data);
		streamElement.doNotProduceStatistics();
		if (seQueue.offer(streamElement))
			return true;
		else {
			logger.warn("statistics event queue has reached its limit => droping the statistics input message");
			return false;
		}
	}

	@Override
	public boolean outputEvent(String producerVS, StatisticsElement se) {
		Serializable [] data = new Serializable[]{se.getProcessTime(), (byte)1, producerVS, se.getSource(), se.getStream(), se.getVolume()};
		StreamElement streamElement = new StreamElement(outputStructure, data);
		streamElement.doNotProduceStatistics();
		if (seQueue.offer(streamElement))
			return true;
		else {
			logger.warn("statistics event queue has reached its limit => droping the statistics output message");
			return false;
		}
	}

}
