package gsn.monitoring;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.Hashtable;

public class MemoryMonitor implements Monitorable {
	
	private MemoryMXBean mbean = ManagementFactory.getMemoryMXBean( );
	
	public Hashtable<String, Object> getStatistics(){
		Hashtable<String, Object> stat = new Hashtable<String, Object>();
		stat.put("::heapMemoryUsed:value", mbean.getHeapMemoryUsage().getUsed());
		stat.put("::nonHeapMemoryUsed:value", mbean.getNonHeapMemoryUsage().getUsed());
		stat.put("::pendingFinalizationCount:value", mbean.getObjectPendingFinalizationCount());
		return stat;
	}

}
