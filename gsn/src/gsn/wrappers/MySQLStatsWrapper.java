package gsn.wrappers;

import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.log.Log;
import com.mysql.jdbc.profiler.ProfilerEvent;
import com.mysql.jdbc.profiler.ProfilerEventHandler;

public class MySQLStatsWrapper extends AbstractWrapper implements ProfilerEventHandler
{
	private final transient Logger logger = Logger.getLogger( MySQLStatsWrapper.class );
	
	private final static int DEFAULT_SAMPLING_RATE_MS = 30000;
	
	private final static DataField[] outputStructure = new DataField[] {
															new DataField("select_cnt", DataTypes.INTEGER),
															new DataField("select_max", DataTypes.INTEGER),
															new DataField("select_avg", DataTypes.DOUBLE),
															new DataField("insert_cnt", DataTypes.INTEGER),
															new DataField("insert_max", DataTypes.INTEGER),
															new DataField("insert_avg", DataTypes.DOUBLE),
															new DataField("delete_cnt", DataTypes.INTEGER),
															new DataField("delete_max", DataTypes.INTEGER),
															new DataField("delete_avg", DataTypes.DOUBLE),
															new DataField("others_cnt", DataTypes.INTEGER),															
															new DataField("others_max", DataTypes.INTEGER),
															new DataField("others_avg", DataTypes.DOUBLE),
															new DataField("fetch_cnt", DataTypes.INTEGER),															
															new DataField("fetch_max", DataTypes.INTEGER),
															new DataField("fetch_avg", DataTypes.DOUBLE),
															new DataField("query_usage", DataTypes.SMALLINT),
															new DataField("fetch_usage", DataTypes.SMALLINT),
															};	
	
	private int sampling_rate = DEFAULT_SAMPLING_RATE_MS;
	private Log mysqlLogger;
	private boolean stopped = false;
	private LinkedList<MySQLStat> queue = new LinkedList<MySQLStat>();
	private Object event = new Object();
	private long old_timestamp = System.currentTimeMillis();
	
	private class MySQLStat {
		protected final long duration;
		protected final type query_type;
		protected MySQLStat(long duration, type query_type) {
			this.duration = duration;
			this.query_type = query_type;
		}
		public String toString() {
			return "MySQLStat:duration="+duration+",query_type="+query_type;
		}
	}
	
	private enum type {
	    SELECT, INSERT, DELETE, OTHERS, FETCH 
	}
	
	public boolean initialize()	{
		if (mysqlLogger == null) {
			logger.warn("make sure that profilerEventHandler=" + MySQLStatsWrapper.class.getName() + "and profileSQL=true properties are set in MySQL JDBC URL");
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
		int size, select_cnt, insert_cnt, delete_cnt, others_cnt, fetch_cnt;
		long timestamp, select_max, select_sum, insert_max, insert_sum, delete_max, delete_sum, others_max, others_sum, fetch_max, fetch_sum;
		Serializable[] output = new Serializable[outputStructure.length];
		MySQLStat[] elem = null;
		while (!stopped) {
			try {
				synchronized (event) {
					event.wait(sampling_rate);
				}
			} catch (InterruptedException e) {
				break;
			}
		
			timestamp = System.currentTimeMillis();
			
			synchronized (queue) {
				size = queue.size();
				if (size > 0) {
					elem = new MySQLStat[size]; 
					for (int i=0; i<size; i++) {
						elem[i] = queue.poll();
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
				output[2] = (double) select_sum / select_cnt;
			} else {
				output[1] = null;
				output[2] = null;
			}
			output[3] = insert_cnt;
			if (insert_cnt > 0) {
				output[4] = insert_max;
				output[5] = (double) insert_sum / insert_cnt;
			} else {
				output[4] = null;
				output[5] = null;
			}
			output[6] = delete_cnt;
			if (delete_cnt > 0) {
				output[7] = delete_max;
				output[8] = (double) delete_sum / delete_cnt;
			} else {
				output[7] = null;
				output[8] = null;
			}
			output[9] = others_cnt;
			if (others_cnt > 0) {
				output[10] = others_max;
				output[11] = (double) others_sum / others_cnt;
			} else {
				output[10] = null;
				output[11] = null;
			}
			output[12] = fetch_cnt;
			if (fetch_cnt > 0) {
				output[13] = fetch_max;
				output[14] = (double) fetch_sum / fetch_cnt;
			} else {
				output[13] = null;
				output[14] = null;
			}
			output[15] = (select_sum + insert_sum + delete_sum + others_sum) * 100 / (timestamp - old_timestamp);
			output[16] = fetch_sum * 100 / (timestamp - old_timestamp);
			
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
		return "MySQLStatsWrapper";
	}

	public void consumeEvent(ProfilerEvent evt) {
		if (mysqlLogger == null || stopped) return;
		switch (evt.getEventType()) {
		case (ProfilerEvent.TYPE_QUERY):
			mysqlLogger.logInfo("[QUERY] "+evt.getEventDuration()+evt.getDurationUnits()+" "+evt.getMessage(), evt.getEventCreationPoint());
			if (evt.getMessage().startsWith("select ")) {
				synchronized (queue) {
					queue.add(new MySQLStat(evt.getEventDuration(), MySQLStatsWrapper.type.SELECT));
				}
			} else if (evt.getMessage().startsWith("insert ")) {
				synchronized (queue) {
					queue.add(new MySQLStat(evt.getEventDuration(), MySQLStatsWrapper.type.INSERT));
				}
			} else if (evt.getMessage().startsWith("delete ")) {
				synchronized (queue) {
					queue.add(new MySQLStat(evt.getEventDuration(), MySQLStatsWrapper.type.DELETE));
				}
			} else {
				synchronized (queue) {
					queue.add(new MySQLStat(evt.getEventDuration(), MySQLStatsWrapper.type.OTHERS));
				}
			}
			break;
		case (ProfilerEvent.TYPE_FETCH):
			mysqlLogger.logInfo("[FETCH] "+evt.getEventDuration()+evt.getDurationUnits(), evt.getEventCreationPoint());
			if (!stopped) {
				synchronized (queue) {
					queue.add(new MySQLStat(evt.getEventDuration(), MySQLStatsWrapper.type.FETCH));
				}
			}			
			break;			
		case (ProfilerEvent.TYPE_SLOW_QUERY):
			mysqlLogger.logWarn("[SLOWQ] "+evt.getEventDuration()+evt.getDurationUnits()+" "+evt.getMessage(), evt.getEventCreationPoint());
			break;
		default:
			mysqlLogger.logDebug(evt);
			break;
		}
	}

	public void init(Connection conn, Properties props) throws SQLException {
		mysqlLogger = conn.getLog();
	}
}
