package gsn.wrappers;

import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;

import java.io.Serializable;

import org.apache.log4j.Logger;

public class MySQLStatsWrapper extends AbstractWrapper
{
	private final transient Logger logger = Logger.getLogger( MySQLStatsWrapper.class );
	
	private final static int DEFAULT_SAMPLING_RATE_MS = 30000;
	
	private final static DataField[] outputStructure = new DataField[] {
															new DataField("select_cnt", DataTypes.INTEGER),
															new DataField("select_max", DataTypes.INTEGER),
															new DataField("select_avg", DataTypes.INTEGER),
															new DataField("insert_cnt", DataTypes.INTEGER),
															new DataField("insert_max", DataTypes.INTEGER),
															new DataField("insert_avg", DataTypes.INTEGER),
															new DataField("delete_cnt", DataTypes.INTEGER),
															new DataField("delete_max", DataTypes.INTEGER),
															new DataField("delete_avg", DataTypes.INTEGER),
															new DataField("others_cnt", DataTypes.INTEGER),															
															new DataField("others_max", DataTypes.INTEGER),
															new DataField("others_avg", DataTypes.INTEGER),
															new DataField("fetch_cnt", DataTypes.INTEGER),															
															new DataField("fetch_max", DataTypes.INTEGER),
															new DataField("fetch_avg", DataTypes.INTEGER),
															new DataField("query_usage", DataTypes.SMALLINT),
															new DataField("fetch_usage", DataTypes.SMALLINT),
															};	
	
	private int sampling_rate = DEFAULT_SAMPLING_RATE_MS;
	private boolean stopped = false;
	private Object event = new Object();
	
	public boolean initialize()	{
		if (!MySQLProfilerEventHandler.initialized) {
			logger.warn("make sure that profilerEventHandler=" + MySQLProfilerEventHandler.class.getName() + " and profileSQL=true properties are set in MySQL JDBC URL");
			return false;
		}
		
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
		int size, select_cnt, insert_cnt, delete_cnt, others_cnt, fetch_cnt, select_max, insert_max, delete_max, others_max, fetch_max;
		long timestamp, select_sum, insert_sum, delete_sum, others_sum, fetch_sum;
		Serializable[] output = new Serializable[outputStructure.length];
		MySQLProfilerEventHandler.MySQLStat[] elem = null;
		while (!stopped) {
			try {
				synchronized (event) {
					event.wait(sampling_rate);
				}
			} catch (InterruptedException e) {
				break;
			}
		
			timestamp = System.currentTimeMillis();
			
			synchronized (MySQLProfilerEventHandler.queue) {
				size = MySQLProfilerEventHandler.queue.size();
				if (size > 0) {
					elem = new MySQLProfilerEventHandler.MySQLStat[size]; 
					for (int i=0; i<size; i++) {
						elem[i] = MySQLProfilerEventHandler.queue.poll();
					}
				}
			}
			
			logger.debug("size="+size);
			
			select_cnt = 0;
			select_max = 0;
			select_sum = 0;
			insert_cnt = 0;
			insert_max = 0;
			insert_sum = 0;
			delete_cnt = 0;
			delete_max = 0;
			delete_sum = 0;
			others_cnt = 0;
			others_max = 0;
			others_sum = 0;
			fetch_cnt = 0;
			fetch_max = 0;
			fetch_sum = 0;
			
			for (int i=0; i<size; i++) {
				switch (elem[i].query_type) {
				case SELECT:
					if (elem[i].duration > select_max) {
						select_max = elem[i].duration; 
					}
					select_sum += elem[i].duration;
					select_cnt++;
					break;
				case INSERT:
					if (elem[i].duration > insert_max) {
						insert_max = elem[i].duration; 
					}
					insert_sum += elem[i].duration;
					insert_cnt++;
					break;
				case DELETE:
					if (elem[i].duration > delete_max) {
						delete_max = elem[i].duration; 
					}
					delete_sum += elem[i].duration;
					delete_cnt++;
					break;
				case OTHERS:
					if (elem[i].duration > others_max) {
						others_max = elem[i].duration; 
					}
					others_sum += elem[i].duration;
					others_cnt++;
					break;
				case FETCH:
					if (elem[i].duration > fetch_max) {
						fetch_max = elem[i].duration; 
					}
					fetch_sum += elem[i].duration;
					fetch_cnt++;
					break;
				}
			}
			
			output[0] = select_cnt;
			if (select_cnt > 0) {
				output[1] = select_max;
				output[2] = (int) (select_sum / select_cnt);
			} else {
				output[1] = null;
				output[2] = null;
			}
			output[3] = insert_cnt;
			if (insert_cnt > 0) {
				output[4] = insert_max;
				output[5] = (int) (insert_sum / insert_cnt);
			} else {
				output[4] = null;
				output[5] = null;
			}
			output[6] = delete_cnt;
			if (delete_cnt > 0) {
				output[7] = delete_max;
				output[8] = (int) (delete_sum / delete_cnt);
			} else {
				output[7] = null;
				output[8] = null;
			}
			output[9] = others_cnt;
			if (others_cnt > 0) {
				output[10] = others_max;
				output[11] = (int) (others_sum / others_cnt);
			} else {
				output[10] = null;
				output[11] = null;
			}
			output[12] = fetch_cnt;
			if (fetch_cnt > 0) {
				output[13] = fetch_max;
				output[14] = (int) (fetch_sum / fetch_cnt);
			} else {
				output[13] = null;
				output[14] = null;
			}
			output[15] = (short) ((select_sum + insert_sum + delete_sum + others_sum) * 100 / (timestamp - MySQLProfilerEventHandler.old_timestamp));
			output[16] = (short) (fetch_sum * 100 / (timestamp - MySQLProfilerEventHandler.old_timestamp));
			
			postStreamElement(new StreamElement(outputStructure, output, timestamp));
			
			MySQLProfilerEventHandler.old_timestamp = timestamp;
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
		return "MySQLStatsWrapper";
	}
}
