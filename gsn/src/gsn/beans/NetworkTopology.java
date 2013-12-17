package gsn.beans;

import java.util.ArrayList;

/**
* @author Roman Lim
*/

public class NetworkTopology {
	
	public NetworkTopology(boolean configurable) {
		this.configurable = configurable;
	}
	
	public NetworkTopology() {}

	public ArrayList<SensorNode> sensornodes;
	
	@SuppressWarnings("unused")
	private boolean configurable = false;

	public boolean mapped = false;
}
