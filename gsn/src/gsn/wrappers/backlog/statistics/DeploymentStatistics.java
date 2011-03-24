package gsn.wrappers.backlog.statistics;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

public class DeploymentStatistics {
	
	protected final transient Logger logger = Logger.getLogger( DeploymentStatistics.class );
	
	private Map<String,CoreStationStatistics> coreStationToCoreStationStatsList = Collections.synchronizedMap(new HashMap<String,CoreStationStatistics>());
	
	protected CoreStationStatistics newStatisticsClass(String coreStationAddress) throws IOException {
		if (!coreStationToCoreStationStatsList.containsKey(coreStationAddress))
			coreStationToCoreStationStatsList.put(coreStationAddress, new CoreStationStatistics(coreStationAddress));
		
		return coreStationToCoreStationStatsList.get(coreStationAddress);
	}

	protected void removeCoreStationStatsInstance(String coreStationAddress) throws IOException {
		if (!coreStationToCoreStationStatsList.containsKey(coreStationAddress))
			throw new IOException("CoreStation " + coreStationAddress + " does not exist in the statistics");
		coreStationToCoreStationStatsList.remove(coreStationAddress);
	}
	
	
	public Map<Integer, Boolean> isConnectedList() {
		Map<Integer, Boolean> map = new HashMap<Integer, Boolean>();
		for (Iterator<CoreStationStatistics> iter = coreStationToCoreStationStatsList.values().iterator(); iter.hasNext();) {
			CoreStationStatistics csstat = iter.next();
			map.put(csstat.getDeviceId(), csstat.isConnected());
		}
		return map;
	}

	
	public Map<Integer, Long> getTotalMsgRecvCounter() {
		Map<Integer, Long> map = new HashMap<Integer, Long>();
		for (Iterator<CoreStationStatistics> iter = coreStationToCoreStationStatsList.values().iterator(); iter.hasNext();) {
			CoreStationStatistics csstat = iter.next();
			map.put(csstat.getDeviceId(), csstat.getTotalMsgRecvCounter());
		}
		return map;
	}
	
	public Map<Integer, Long> getMsgRecvCounterList(int type) {
		Map<Integer, Long> map = new HashMap<Integer, Long>();
		for (Iterator<CoreStationStatistics> iter = coreStationToCoreStationStatsList.values().iterator(); iter.hasNext();) {
			CoreStationStatistics csstat = iter.next();
			map.put(csstat.getDeviceId(), csstat.getMsgRecvCounter(type));
		}
		return map;
	}
	
	public Map<Integer, Long> getTotalMsgRecvByteCounter() {
		Map<Integer, Long> map = new HashMap<Integer, Long>();
		for (Iterator<CoreStationStatistics> iter = coreStationToCoreStationStatsList.values().iterator(); iter.hasNext();) {
			CoreStationStatistics csstat = iter.next();
			map.put(csstat.getDeviceId(), csstat.getTotalMsgRecvByteCounter());
		}
		return map;
	}
	
	public Map<Integer, Long> getMsgRecvByteCounterList(int type) {
		Map<Integer, Long> map = new HashMap<Integer, Long>();
		for (Iterator<CoreStationStatistics> iter = coreStationToCoreStationStatsList.values().iterator(); iter.hasNext();) {
			CoreStationStatistics csstat = iter.next();
			map.put(csstat.getDeviceId(), csstat.getMsgRecvByteCounter(type));
		}
		return map;
	}

	
	public Map<Integer, Long> getTotalMsgSendCounter() {
		Map<Integer, Long> map = new HashMap<Integer, Long>();
		for (Iterator<CoreStationStatistics> iter = coreStationToCoreStationStatsList.values().iterator(); iter.hasNext();) {
			CoreStationStatistics csstat = iter.next();
			map.put(csstat.getDeviceId(), csstat.getTotalMsgSendCounter());
		}
		return map;
	}
	
	public Map<Integer, Long> getMsgSendCounterList(int type) {
		Map<Integer, Long> map = new HashMap<Integer, Long>();
		for (Iterator<CoreStationStatistics> iter = coreStationToCoreStationStatsList.values().iterator(); iter.hasNext();) {
			CoreStationStatistics csstat = iter.next();
			map.put(csstat.getDeviceId(), csstat.getMsgSendCounter(type));
		}
		return map;
	}
	
	public Map<Integer, Long> getTotalMsgSendByteCounter() {
		Map<Integer, Long> map = new HashMap<Integer, Long>();
		for (Iterator<CoreStationStatistics> iter = coreStationToCoreStationStatsList.values().iterator(); iter.hasNext();) {
			CoreStationStatistics csstat = iter.next();
			map.put(csstat.getDeviceId(), csstat.getTotalMsgSendByteCounter());
		}
		return map;
	}
	
	public Map<Integer, Long> getMsgSendByteCounterList(int type) {
		Map<Integer, Long> map = new HashMap<Integer, Long>();
		for (Iterator<CoreStationStatistics> iter = coreStationToCoreStationStatsList.values().iterator(); iter.hasNext();) {
			CoreStationStatistics csstat = iter.next();
			map.put(csstat.getDeviceId(), csstat.getMsgSendByteCounter(type));
		}
		return map;
	}
}
