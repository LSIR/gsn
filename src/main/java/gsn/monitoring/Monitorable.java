package gsn.monitoring;

import java.util.Hashtable;

public interface Monitorable {
	public Hashtable<String, Object> getStatistics();
}
