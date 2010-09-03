package gsn.beans;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
* @author Roman Lim
*/

public class SensorNode {
	
	static final int NODE_TYPE_SIB = 0;
	static final int NODE_TYPE_ACCESS_NODE = 1;
	static final int NODE_TYPE_POWERSWITCH = 2;
	static final int NODE_TYPE_BBCONTROL = 3;
	
	public Integer nodetype = NODE_TYPE_SIB; // default type
		
	public Integer node_id;
	public Integer parent_id;
	public Long timestamp;		
	public Long generation_time;
	
	public Integer packet_count = 0;
	
	public Double vsys;
	public Double current;
	public Double temperature;
	public Double humidity;
	public Integer flash_count;
	public Integer uptime;
	
	public SensorNodeConfiguration configuration;
	public SensorNodeConfiguration pendingConfiguration;
			
	public ArrayList<Link> links;
	
	private DecimalFormat df = new DecimalFormat("0.00");
	
	public SensorNode() {
		links = new ArrayList<Link>();
	}
	
	public SensorNode(Integer node_id) {
		this.node_id=node_id;
		links = new ArrayList<Link>();
	}
	
	public boolean isSibNode() {
		return nodetype == NODE_TYPE_SIB;
	}
	
	public boolean isAccessNode() {
		return nodetype == NODE_TYPE_ACCESS_NODE;
	}
	
	public boolean isPowerSwitch() {
		return nodetype == NODE_TYPE_POWERSWITCH;
	}
	
	public boolean isBBControl() {
		return nodetype == NODE_TYPE_BBCONTROL;
	}
	
	public void setSibNode() {
		nodetype = NODE_TYPE_SIB;
	}
	
	public void setAccessNode() {
		nodetype = NODE_TYPE_ACCESS_NODE;
	}
	
	public void setPowerSwitch() {
		nodetype = NODE_TYPE_POWERSWITCH;
	}
	
	public void setBBControl() {
		nodetype = NODE_TYPE_BBCONTROL;
	}
	
	public String getVsys() {
		if (vsys!=null)
			return df.format(vsys);
		return null;
	}

	public String getCurrent() {
		if (current!=null)
			return df.format(current);
		return null;
	}

	public String getTemperature() {
		if (temperature!=null)
			return df.format(temperature);
		return null;
	}

	public String getHumidity() {
		if (humidity!=null)
			return df.format(humidity);
		return null;
	}

	public void setVsys(String vsys) {
		if (vsys != null)
			this.vsys = new Double(vsys);
	}

	public void setCurrent(String current) {
		if (current != null)
			this.current = new Double(current);
	}

	public void setTemperature(String temperature) {
		if (temperature != null)
			this.temperature = new Double(temperature);
	}

	public void setHumidity(String humidity) {
		if (humidity != null)
			this.humidity = new Double(humidity);
	}
	
	
}
