package gsn.hydrosys.sensormanager;

import gsn.beans.DataField;

public interface SensorManager {
	
	public DataField registerQuery(String queryName, DataField[] outputStructure, String[] vsnames, String query);

	public DataField unregisterQuery(String queryName);
	
}
