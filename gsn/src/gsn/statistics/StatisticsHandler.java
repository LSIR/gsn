package gsn.statistics;

import java.util.Iterator;
import java.util.Vector;

import org.apache.log4j.Logger;

public class StatisticsHandler {
	
	private final transient Logger logger = Logger.getLogger( StatisticsHandler.class );
	
	private static StatisticsHandler instance = new StatisticsHandler();

	private Vector<StatisticsListener> statisticsListeners = new Vector<StatisticsListener>();
	

	private StatisticsHandler() {}

	public static StatisticsHandler getInstance() {
		return instance;
	}
	
	public void registerListener(StatisticsListener listener) {
		statisticsListeners.add(listener);
		logger.info("statistics listener >" + listener.listenerName() + "< registered");
	}
	
	public void deregisterListener(StatisticsListener listener) {
		statisticsListeners.remove(listener);
		logger.info("statistics listener >" + listener.listenerName() + "< deregistered");
	}

	public boolean inputEvent(String producerVS, StatisticsElement statisticsElement) {
		if (statisticsListeners.isEmpty())
			return false;
		boolean ret = false;
		for(Iterator<StatisticsListener> iter = statisticsListeners.iterator(); iter.hasNext();)
			if(iter.next().inputEvent(producerVS, statisticsElement))
				ret = true;
		return ret;
	}

	public boolean outputEvent(String producerVS, StatisticsElement statisticsElement) {
		if (statisticsListeners.isEmpty())
			return false;
		boolean ret = false;
		for(Iterator<StatisticsListener> iter = statisticsListeners.iterator(); iter.hasNext();)
			if(iter.next().outputEvent(producerVS, statisticsElement))
				ret = true;
		return ret;
	}
}
