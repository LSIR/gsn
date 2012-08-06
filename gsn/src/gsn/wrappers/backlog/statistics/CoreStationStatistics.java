package gsn.wrappers.backlog.statistics;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

public class CoreStationStatistics {
	
	private final transient Logger logger = Logger.getLogger( CoreStationStatistics.class );
	
	private String coreStationAddress = null;
	private Boolean isConnected = null;
	private Integer deviceId = null;
	Long recvTotal = null;
	Long sendTotal = null;
	private Map<Integer,Long> msgRecvCounterMap = Collections.synchronizedMap(new Hashtable<Integer,Long>());
	private Map<Integer,Long> msgRecvByteCounterMap = Collections.synchronizedMap(new Hashtable<Integer,Long>());
	private Map<Integer,Long> msgSendCounterMap = Collections.synchronizedMap(new Hashtable<Integer,Long>());
	private Map<Integer,Long> msgSendByteCounterMap = Collections.synchronizedMap(new Hashtable<Integer,Long>());
	
	
	public CoreStationStatistics(String corestation) {
		coreStationAddress = corestation;
	}
	
	
	public void setConnected(boolean conn) { isConnected = conn; }
	
	public Boolean isConnected() { return isConnected; }
	
	
	public void setDeviceId(int id) {
		if (deviceId != null && id != deviceId) {
			logger.warn("device id for CoreStation " + coreStationAddress + " has changed => reseting all statistics");
			recvTotal = null;
			sendTotal = null;
			msgRecvCounterMap.clear();
			msgRecvByteCounterMap.clear();
			msgSendCounterMap.clear();
			msgSendByteCounterMap.clear();
		}
		deviceId = id;
	}
	
	public Integer getDeviceId() { return deviceId; }
	
	
	public void msgReceived(int type, long size) {
		Long val = msgRecvCounterMap.get(type);
		if (val == null)
			msgRecvCounterMap.put(type, new Long(1));
		else
			msgRecvCounterMap.put(type, val + 1);
		
		val = msgRecvByteCounterMap.get(type);
		if (val == null)
			msgRecvByteCounterMap.put(type, size);
		else
			msgRecvByteCounterMap.put(type, val + size);
	}
	
	public void bytesReceived(long size) {
		if (recvTotal == null)
			recvTotal = size;
		else
			recvTotal += size;
	}
	
	public Long getTotalRecvByteCounter() {
		return recvTotal;
	}
	
	public Long getTotalMsgRecvCounter() {
		long total = 0;
		synchronized (msgRecvCounterMap) {
			for (Iterator<Long> iter = msgRecvCounterMap.values().iterator(); iter.hasNext();)
				total += iter.next();
		}
		return total;
	}
	
	public Long getMsgRecvCounter(int type) { return msgRecvCounterMap.get(type); }
	
	public Long getTotalMsgRecvByteCounter() {
		long total = 0;
		synchronized (msgRecvByteCounterMap) {
			for (Iterator<Long> iter = msgRecvByteCounterMap.values().iterator(); iter.hasNext();)
				total += iter.next();
		}
		return total;
	}
	
	public Long getMsgRecvByteCounter(int type) { return msgRecvByteCounterMap.get(type); }
	
	
	public void msgSent(int type, long size) {
		Long val = msgSendCounterMap.get(type);
		if (val == null)
			msgSendCounterMap.put(type, new Long(1));
		else
			msgSendCounterMap.put(type, val + 1);

		val = msgSendByteCounterMap.get(type);
		if (val == null)
			msgSendByteCounterMap.put(type, new Long(size));
		else
			msgSendByteCounterMap.put(type, val + size);
	}
	
	public void bytesSent(long size) {
		if (sendTotal == null)
			sendTotal = size;
		else
			sendTotal += size;
	}
	
	public Long getTotalSendByteCounter() {
		return sendTotal;
	}
	
	public Long getTotalMsgSendCounter() {
		long total = 0;
		synchronized (msgSendCounterMap) {
			for (Iterator<Long> iter = msgSendCounterMap.values().iterator(); iter.hasNext();)
				total += iter.next();
		}
		return total;
	}
	
	public Long getMsgSendCounter(int type) { return msgSendCounterMap.get(type); }
	
	public Long getTotalMsgSendByteCounter() {
		long total = 0;
		synchronized (msgSendByteCounterMap) {
			for (Iterator<Long> iter = msgSendByteCounterMap.values().iterator(); iter.hasNext();)
				total += iter.next();
		}
		return total;
	}
	
	public Long getMsgSendByteCounter(int type) { return msgSendByteCounterMap.get(type); }
}
