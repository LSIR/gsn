package gsn.monitoring;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.Hashtable;

public class MemoryMonitor implements Monitorable {
	
	private MemoryMXBean mbean = ManagementFactory.getMemoryMXBean( );
	
	public Hashtable<String, Object> getStatistics(){
		Hashtable<String, Object> stat = new Hashtable<String, Object>();
		stat.put("core.memory.gauge.heap", mbean.getHeapMemoryUsage().getUsed());
		stat.put("core.memory.gauge.nonHeap", mbean.getNonHeapMemoryUsage().getUsed());
		stat.put("core.memory.gauge.pendingFinalizationCount", mbean.getObjectPendingFinalizationCount());
		return stat;
	}

}
