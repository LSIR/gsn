package gsn.wrappers;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Properties;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.log.Log;
import com.mysql.jdbc.profiler.ProfilerEvent;
import com.mysql.jdbc.profiler.ProfilerEventHandler;

public class MySQLProfilerEventHandler implements ProfilerEventHandler
{
	protected static Log mysqlLogger = null;
	protected static LinkedList<MySQLStat> queue = new LinkedList<MySQLStat>();
	
	protected class MySQLStat {
		protected final long duration;
		protected final QueryType query_type;
		protected MySQLStat(long duration, QueryType query_type) {
			this.duration = duration;
			this.query_type = query_type;
		}
		public String toString() {
			return "MySQLStat:duration="+duration+",query_type="+query_type;
		}
	}
	
	protected static enum QueryType {
	    SELECT, INSERT, DELETE, OTHERS, FETCH 
	}

	public MySQLProfilerEventHandler () { }

	public void consumeEvent(ProfilerEvent evt) {
		if (mysqlLogger == null) return;
		switch (evt.getEventType()) {
		case (ProfilerEvent.TYPE_QUERY):
			mysqlLogger.logInfo("[QUERY] "+evt.getEventDuration()+evt.getDurationUnits()+" "+evt.getMessage(), evt.getEventCreationPoint());
			if (evt.getMessage().startsWith("select ")) {
				synchronized (queue) {
					queue.add(new MySQLStat(evt.getEventDuration(), MySQLProfilerEventHandler.QueryType.SELECT));
				}
			} else if (evt.getMessage().startsWith("insert ")) {
				synchronized (queue) {
					queue.add(new MySQLStat(evt.getEventDuration(), MySQLProfilerEventHandler.QueryType.INSERT));
				}
			} else if (evt.getMessage().startsWith("delete ")) {
				synchronized (queue) {
					queue.add(new MySQLStat(evt.getEventDuration(), MySQLProfilerEventHandler.QueryType.DELETE));
				}
			} else {
				synchronized (queue) {
					queue.add(new MySQLStat(evt.getEventDuration(), MySQLProfilerEventHandler.QueryType.OTHERS));
				}
			}
			break;
		case (ProfilerEvent.TYPE_FETCH):
			mysqlLogger.logInfo("[FETCH] "+evt.getEventDuration()+evt.getDurationUnits(), evt.getEventCreationPoint());
			synchronized (queue) {
				queue.add(new MySQLStat(evt.getEventDuration(), MySQLProfilerEventHandler.QueryType.FETCH));
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
	
	public void destroy() {
		mysqlLogger = null;
	}
}
