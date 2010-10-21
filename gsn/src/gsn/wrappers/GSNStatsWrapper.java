package gsn.wrappers;

import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;

public class GSNStatsWrapper extends AbstractWrapper
{
	private final transient Logger logger = Logger.getLogger( GSNStatsWrapper.class );
	
	private final static int DEFAULT_SAMPLING_RATE_MS = 30000;
	
	private final static DataField[] outputStructure = new DataField[] {
															new DataField("uptime", DataTypes.INTEGER),
															new DataField("memory_heap_used", DataTypes.INTEGER),
															new DataField("memory_nonheap_used", DataTypes.INTEGER),
															new DataField("thread_blocked_cnt", DataTypes.SMALLINT),
															new DataField("thread_new_cnt", DataTypes.SMALLINT),
															new DataField("thread_runnable_cnt", DataTypes.SMALLINT),
															new DataField("thread_terminated_cnt", DataTypes.SMALLINT),
															new DataField("thread_timed_waiting_cnt", DataTypes.SMALLINT),
															new DataField("thread_waiting_cnt", DataTypes.SMALLINT),
															new DataField("thread_total_cnt", DataTypes.SMALLINT),
															new DataField("thread_blocked_rate", DataTypes.SMALLINT),
															new DataField("thread_blocked_time", DataTypes.INTEGER),
															new DataField("thread_waited_rate", DataTypes.SMALLINT),
															new DataField("thread_waited_time", DataTypes.INTEGER),
															};
	
	private int sampling_rate = DEFAULT_SAMPLING_RATE_MS;
	private boolean stopped = false;
	private Object event = new Object();
	private HashMap<Long,ThreadEntry> thread_states = new HashMap<Long,ThreadEntry>();
	
	private class ThreadEntry {
		private boolean valid = true;
		private long blockedCount;
		private long blockedTime;
		private long waitedCount;
		private long waitedTime;
		private ThreadEntry(long blockedCount, long blockedTime, long waitedCount, long waitedTime) {
			this.blockedCount = blockedCount;
			this.blockedTime = blockedTime;
			this.waitedCount = waitedCount;
			this.waitedTime = waitedTime;
		}
	}
	
	public boolean initialize()	{
		String predicate = getActiveAddressBean().getPredicateValue("sampling-rate");
		if ( predicate != null ) {
			try {
				sampling_rate = Integer.parseInt(predicate);
			} catch (NumberFormatException e) {
				logger.warn("sampling-rate is not parsable, set to default ("+DEFAULT_SAMPLING_RATE_MS+"ms)");
			}
		}

		return true;
	}
	
	public void run() {
		short thread_blocked_cnt, thread_new_cnt, thread_runnable_cnt, thread_terminated_cnt, thread_timed_waiting_cnt, thread_waiting_cnt;
		long timestamp, diff, blockedCount, blockedTime, waitedCount, waitedTime;
		long old_timestamp = 0;
		double thread_blocked_acc, thread_blocked_acc_time, thread_waited_acc, thread_waited_acc_time;
		ThreadInfo[] threads;
		ThreadEntry te;
		Iterator<ThreadEntry> i;
		Serializable[] output = new Serializable[outputStructure.length];
		ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
		threadBean.setThreadContentionMonitoringEnabled(true);
		RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
		MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
		while (!stopped) {
			try {
				synchronized (event) {
					event.wait(sampling_rate);
				}
			} catch (InterruptedException e) {
				break;
			}
		
			thread_blocked_cnt = 0;
			thread_new_cnt = 0;
			thread_runnable_cnt = 0;
			thread_terminated_cnt = 0;
			thread_timed_waiting_cnt = 0;
			thread_waiting_cnt = 0;
			thread_blocked_acc = 0.0;
			thread_blocked_acc_time = 0.0;
			thread_waited_acc = 0.0;
			thread_waited_acc_time = 0.0;
			
			i = thread_states.values().iterator();
			while (i.hasNext()) {
				i.next().valid = false;
			}
			
			timestamp = System.currentTimeMillis();
			
			output[0] = (int) (runtimeBean.getUptime() / 1000);
			output[1] = (int) (memoryBean.getHeapMemoryUsage().getUsed() / 1024);
			output[2] = (int) (memoryBean.getNonHeapMemoryUsage().getUsed() / 1024);
			
			threads = threadBean.getThreadInfo(threadBean.getAllThreadIds());
			for (int t=0; t<threads.length; t++) {
				switch (threads[t].getThreadState()) {
				case BLOCKED:
					thread_blocked_cnt++;
					break;
				case NEW:
					thread_new_cnt++;
					break;					
				case RUNNABLE:
					thread_runnable_cnt++;
					break;
				case TERMINATED:
					thread_terminated_cnt++;
					break;
				case TIMED_WAITING:
					thread_timed_waiting_cnt++;
					break;
				case WAITING:
					thread_waiting_cnt++;
					break;
				}
				blockedCount = threads[t].getBlockedCount();
				blockedTime = threads[t].getBlockedTime();
				waitedCount = threads[t].getWaitedCount();
				waitedTime = threads[t].getWaitedTime();
				if (thread_states.containsKey(threads[t].getThreadId())) {
					te = thread_states.get(threads[t].getThreadId());
					diff = blockedCount - te.blockedCount;
					te.blockedCount = blockedCount;
					blockedCount = diff;
					diff = blockedTime - te.blockedTime;
					te.blockedTime = blockedTime;
					blockedTime = diff;
					diff = waitedCount - te.waitedCount;
					te.waitedCount = waitedCount;
					waitedCount = diff;
					diff = waitedTime - te.waitedTime;
					te.waitedTime = waitedTime;
					waitedTime = diff;
					te.valid = true;
				} else {
					thread_states.put(threads[t].getThreadId(), new ThreadEntry(blockedCount, blockedTime, waitedCount, waitedTime));
				}
				if (logger.isInfoEnabled()) {
					logger.info(threads[t].getThreadName()+" blocked:"+blockedCount+":"+blockedTime+"ms waited:"+waitedCount+":"+waitedTime+"ms");
				}					
				thread_blocked_acc += blockedCount;
				thread_blocked_acc_time += blockedTime;
				thread_waited_acc += waitedCount;
				thread_waited_acc_time += waitedTime;
			}

			i = thread_states.values().iterator();
			while (i.hasNext()) {
				if (!i.next().valid) {
					i.remove();
				}
			}

			// recheck if nonvalid entries are removed
			i = thread_states.values().iterator();
			while (i.hasNext()) {
				assert(i.next().valid);
			}	
			
			output[3] = thread_blocked_cnt;
			output[4] = thread_new_cnt;
			output[5] = thread_runnable_cnt;
			output[6] = thread_terminated_cnt;
			output[7] = thread_timed_waiting_cnt;
			output[8] = thread_waiting_cnt;
			output[9] = (short) threads.length;
			thread_blocked_acc /= threads.length;
			thread_blocked_acc_time /= threads.length;
			thread_waited_acc /= threads.length;
			thread_waited_acc_time /= threads.length;
			if (old_timestamp != 0) {
				diff = timestamp - old_timestamp;
				output[10] = (short) (thread_blocked_acc * 60000 / diff);
				output[11] = (int) (thread_blocked_acc_time * 1000000 / diff);
				output[12] = (short) (thread_waited_acc * 60000 / diff);
				output[13] = (int) (thread_waited_acc_time * 1000000 / diff);
			} else {
				output[10] = null;
				output[11] = null;
				output[12] = null;
				output[13] = null;
			}
			
			postStreamElement(new StreamElement(outputStructure, output, timestamp));
			
			old_timestamp = timestamp;
		}
	}

	public void dispose() {
		stopped = true;
		synchronized (event) {
			event.notify();
		}
	}

	public DataField[] getOutputFormat() {
		return outputStructure;
	}

	public String getWrapperName() {
		return "GSNStatsWrapper";
	}
}
