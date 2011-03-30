package gsn.wrappers.backlog.statistics;

import gsn.wrappers.BackLogStatsWrapper;

import java.io.IOException;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

public class DeploymentStatistics {
	
	protected final transient Logger logger = Logger.getLogger( DeploymentStatistics.class );
	
	private Map<String,CoreStationStatistics> coreStationToCoreStationStatsList = Collections.synchronizedMap(new Hashtable<String,CoreStationStatistics>());
	private BackLogStatsWrapper blstatswrapper = null;
	
	public DeploymentStatistics(BackLogStatsWrapper statswrapper) {
		blstatswrapper = statswrapper;
	}

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
	
	
	public BackLogStatsWrapper getStatsWrapper() {
		return blstatswrapper;
	}
	
	
	public void setStatsWrapper(BackLogStatsWrapper statswrapper) {
		blstatswrapper = statswrapper;
	}
	
	
	public Map<Integer, Boolean> isConnectedList() {
		if (coreStationToCoreStationStatsList.isEmpty())
			return null;
		Map<Integer, Boolean> map = new Hashtable<Integer, Boolean>();
		for (Iterator<CoreStationStatistics> iter = coreStationToCoreStationStatsList.values().iterator(); iter.hasNext();) {
			CoreStationStatistics csstat = iter.next();
			Integer id = csstat.getDeviceId();
			Boolean val = csstat.isConnected();
			if (id != null && val != null)
				map.put(id, val);
		}
		return map;
	}

	
	public Map<Integer, Long> getTotalMsgRecvCounter() {
		if (coreStationToCoreStationStatsList.isEmpty())
			return null;
		Map<Integer, Long> map = new Hashtable<Integer, Long>();
		for (Iterator<CoreStationStatistics> iter = coreStationToCoreStationStatsList.values().iterator(); iter.hasNext();) {
			CoreStationStatistics csstat = iter.next();
			Integer id = csstat.getDeviceId();
			Long val = csstat.getTotalMsgRecvCounter();
			if (id != null && val != null)
				map.put(id, val);
		}
		return map;
	}
	
	public Map<Integer, Long> getMsgRecvCounterList(int type) {
		if (coreStationToCoreStationStatsList.isEmpty())
			return null;
		Map<Integer, Long> map = new Hashtable<Integer, Long>();
		for (Iterator<CoreStationStatistics> iter = coreStationToCoreStationStatsList.values().iterator(); iter.hasNext();) {
			CoreStationStatistics csstat = iter.next();
			Integer id = csstat.getDeviceId();
			Long val = csstat.getMsgRecvCounter(type);
			if (id != null && val != null)
				map.put(id, val);
		}
		return map;
	}
	
	public Map<Integer, Long> getTotalRecvByteCounter() {
		if (coreStationToCoreStationStatsList.isEmpty())
			return null;
		Map<Integer, Long> map = new Hashtable<Integer, Long>();
		for (Iterator<CoreStationStatistics> iter = coreStationToCoreStationStatsList.values().iterator(); iter.hasNext();) {
			CoreStationStatistics csstat = iter.next();
			Integer id = csstat.getDeviceId();
			Long val = csstat.getTotalRecvByteCounter();
			if (id != null && val != null)
				map.put(id, val);
		}
		return map;
	}
	
	public Map<Integer, Long> getTotalMsgRecvByteCounter() {
		if (coreStationToCoreStationStatsList.isEmpty())
			return null;
		Map<Integer, Long> map = new Hashtable<Integer, Long>();
		for (Iterator<CoreStationStatistics> iter = coreStationToCoreStationStatsList.values().iterator(); iter.hasNext();) {
			CoreStationStatistics csstat = iter.next();
			Integer id = csstat.getDeviceId();
			Long val = csstat.getTotalMsgRecvByteCounter();
			if (id != null && val != null)
				map.put(id, val);
		}
		return map;
	}
	
	public Map<Integer, Long> getMsgRecvByteCounterList(int type) {
		if (coreStationToCoreStationStatsList.isEmpty())
			return null;
		Map<Integer, Long> map = new Hashtable<Integer, Long>();
		for (Iterator<CoreStationStatistics> iter = coreStationToCoreStationStatsList.values().iterator(); iter.hasNext();) {
			CoreStationStatistics csstat = iter.next();
			Integer id = csstat.getDeviceId();
			Long val = csstat.getMsgRecvByteCounter(type);
			if (id != null && val != null)
				map.put(id, val);
		}
		return map;
	}

	
	public Map<Integer, Long> getTotalMsgSendCounter() {
		if (coreStationToCoreStationStatsList.isEmpty())
			return null;
		Map<Integer, Long> map = new Hashtable<Integer, Long>();
		for (Iterator<CoreStationStatistics> iter = coreStationToCoreStationStatsList.values().iterator(); iter.hasNext();) {
			CoreStationStatistics csstat = iter.next();
			Integer id = csstat.getDeviceId();
			Long val = csstat.getTotalMsgSendCounter();
			if (id != null && val != null)
				map.put(id, val);
		}
		return map;
	}
	
	public Map<Integer, Long> getMsgSendCounterList(int type) {
		if (coreStationToCoreStationStatsList.isEmpty())
			return null;
		Map<Integer, Long> map = new Hashtable<Integer, Long>();
		for (Iterator<CoreStationStatistics> iter = coreStationToCoreStationStatsList.values().iterator(); iter.hasNext();) {
			CoreStationStatistics csstat = iter.next();
			Integer id = csstat.getDeviceId();
			Long val = csstat.getMsgSendCounter(type);
			if (id != null && val != null)
				map.put(id, val);
		}
		return map;
	}
	
	public Map<Integer, Long> getTotalSendByteCounter() {
		if (coreStationToCoreStationStatsList.isEmpty())
			return null;
		Map<Integer, Long> map = new Hashtable<Integer, Long>();
		for (Iterator<CoreStationStatistics> iter = coreStationToCoreStationStatsList.values().iterator(); iter.hasNext();) {
			CoreStationStatistics csstat = iter.next();
			Integer id = csstat.getDeviceId();
			Long val = csstat.getTotalSendByteCounter();
			if (id != null && val != null)
				map.put(id, val);
		}
		return map;
	}
	
	public Map<Integer, Long> getTotalMsgSendByteCounter() {
		if (coreStationToCoreStationStatsList.isEmpty())
			return null;
		Map<Integer, Long> map = new Hashtable<Integer, Long>();
		for (Iterator<CoreStationStatistics> iter = coreStationToCoreStationStatsList.values().iterator(); iter.hasNext();) {
			CoreStationStatistics csstat = iter.next();
			Integer id = csstat.getDeviceId();
			Long val = csstat.getTotalMsgSendByteCounter();
			if (id != null && val != null)
				map.put(id, val);
		}
		return map;
	}
	
	public Map<Integer, Long> getMsgSendByteCounterList(int type) {
		if (coreStationToCoreStationStatsList.isEmpty())
			return null;
		Map<Integer, Long> map = new Hashtable<Integer, Long>();
		for (Iterator<CoreStationStatistics> iter = coreStationToCoreStationStatsList.values().iterator(); iter.hasNext();) {
			CoreStationStatistics csstat = iter.next();
			Integer id = csstat.getDeviceId();
			Long val = csstat.getMsgSendByteCounter(type);
			if (id != null && val != null)
				map.put(id, val);
		}
		return map;
	}
}
