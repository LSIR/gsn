package gsn.beans;

/**
* @author Roman Lim
*/

public class NetworkTopology {
	
	public NetworkTopology(boolean configurable) {
		this.configurable = configurable;
	}
	
	public NetworkTopology() {}

	public SensorNode[] sensornodes;
	
	@SuppressWarnings("unused")
	private boolean configurable = false;
}
