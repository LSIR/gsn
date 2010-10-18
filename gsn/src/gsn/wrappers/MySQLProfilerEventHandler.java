package gsn.wrappers;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.profiler.ProfilerEvent;
import com.mysql.jdbc.profiler.ProfilerEventHandler;

public class MySQLProfilerEventHandler implements ProfilerEventHandler
{
	private final transient Logger logger = Logger.getLogger( MySQLProfilerEventHandler.class );

	protected static boolean initialized = false;
	protected static LinkedList<MySQLStat> queue = new LinkedList<MySQLStat>();
        protected static long old_timestamp = System.currentTimeMillis();	

	protected class MySQLStat {
		protected final int duration;
		protected final QueryType query_type;
		protected MySQLStat(int duration, QueryType query_type) {
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
		switch (evt.getEventType()) {
		case (ProfilerEvent.TYPE_QUERY):
			logger.info("[QUERY] "+evt.getEventDuration()+evt.getDurationUnits()+" "+replaceNonPrintableCharactersWithSpace(evt.getMessage()));
			if (evt.getMessage().startsWith("select ")) {
				synchronized (queue) {
					queue.add(new MySQLStat((int) evt.getEventDuration(), MySQLProfilerEventHandler.QueryType.SELECT));
				}
			} else if (evt.getMessage().startsWith("insert ")) {
				synchronized (queue) {
					queue.add(new MySQLStat((int) evt.getEventDuration(), MySQLProfilerEventHandler.QueryType.INSERT));
				}
			} else if (evt.getMessage().startsWith("delete ")) {
				synchronized (queue) {
					queue.add(new MySQLStat((int) evt.getEventDuration(), MySQLProfilerEventHandler.QueryType.DELETE));
				}
			} else {
				synchronized (queue) {
					queue.add(new MySQLStat((int) evt.getEventDuration(), MySQLProfilerEventHandler.QueryType.OTHERS));
				}
			}
			break;
		case (ProfilerEvent.TYPE_FETCH):
			logger.info("[FETCH] "+evt.getEventDuration()+evt.getDurationUnits());
			synchronized (queue) {
				queue.add(new MySQLStat((int) evt.getEventDuration(), MySQLProfilerEventHandler.QueryType.FETCH));
			}
			break;			
		case (ProfilerEvent.TYPE_SLOW_QUERY):
			logger.warn("[SLOWQ] "+evt.getEventDuration()+evt.getDurationUnits()+" "+replaceNonPrintableCharactersWithSpace(evt.getMessage().substring(evt.getMessage().indexOf(':') + 2)));
			break;
		default:
			logger.debug(evt.getMessage());
			break;
		}
	}

	public void init(Connection conn, Properties props) throws SQLException {
		initialized = true;
	}

	public void destroy() { }

	private static String replaceNonPrintableCharactersWithSpace(String s) {
		StringBuffer sb = new StringBuffer(s.trim());
		char c;
		boolean lastCharIsSpace = false;
		// if non-printable character, replace it with space
		for (int i=0; i<sb.length(); i++) {
			c = sb.charAt(i);
			if (c < ' ' || c > '~') {
				sb.setCharAt(i, ' ');
			}
		}
		// remove sequential spaces
		for (int i=0; i<sb.length(); ) {
			if (sb.charAt(i) == ' ') {
				if (lastCharIsSpace) {
					sb.deleteCharAt(i);
				} else {
					lastCharIsSpace = true;
					i++;
				}
			} else {
				lastCharIsSpace = false;
				i++;
			}
		}
		return sb.toString();
	}
}
