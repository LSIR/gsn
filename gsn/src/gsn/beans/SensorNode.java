package gsn.beans;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import org.apache.log4j.Logger;
import com.vividsolutions.jts.geom.Coordinate;

/**
* @author Roman Lim
*/

public class SensorNode {
	
	private final transient Logger logger = Logger.getLogger( this.getClass() );
	
	static final int NODE_TYPE_SIB = 0;
	static final int NODE_TYPE_ACCESS_NODE = 1;
	static final int NODE_TYPE_POWERSWITCH = 2;
	static final int NODE_TYPE_BBCONTROL = 3;
	static final int NODE_TYPE_AE = 4;
	
	static final int VOLTAGE_HISTORY_SIZE = 60; // ~ 2h 
	
	public Integer nodetype = NODE_TYPE_SIB; // default type
		
	public Integer node_id;
	public Integer parent_id;
	public Long timestamp;		
	public Long generation_time;
	
	public Integer packet_count = 0;
	
	private Double vsys;
	private Double vsdi;
	public Double current;
	public Double temperature;
	public Double humidity;
	public Integer flash_count;
	public Integer uptime;
	public Boolean corestation_running;
	public Boolean iscorestation;
	
	public SensorNodeConfiguration configuration;
	public SensorNodeConfiguration pendingConfiguration;
			
	public ArrayList<Link> links;
	
	public Integer position;
	public String geoposition;
	
	private Integer batterylevel; 
	private LinkedList<Double> voltageHistory = new LinkedList<Double>();
	private DecimalFormat df = new DecimalFormat("0.00");
	
	public Coordinate coordinate = new Coordinate();
	
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

	public boolean isAENode() {
		return nodetype == NODE_TYPE_AE;
	}
	
	public boolean hasSHT21() {
		return isBBControl() || isAccessNode() || isAENode();
	}
	
	public boolean hasSHT15() {
		return isSibNode() || isPowerSwitch();
	}

	public void setSibNode() {
		nodetype = NODE_TYPE_SIB;
		iscorestation = false;
	}
	
	public void setAccessNode() {
		nodetype = NODE_TYPE_ACCESS_NODE;
		iscorestation = true;
	}
	
	public void setPowerSwitch() {
		nodetype = NODE_TYPE_POWERSWITCH;
		iscorestation = false;
	}
	
	public void setBBControl() {
		nodetype = NODE_TYPE_BBCONTROL;
		iscorestation = true;
	}
	
	public void setAENode() {
		nodetype = NODE_TYPE_AE;
		iscorestation = false;
	}	
	
	public String getVsys() {
		if (vsys!=null)
			return df.format(vsys);
		return null;
	}
	
	public String getVsdi() {
		if (vsdi!=null)
			return df.format(vsdi);
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
		if (vsys != null) {
			this.vsys = new Double(vsys);
			updateVoltageHistory();
		}
	}
	
	public void setVsys(Double vsys) {
		if (vsys != null) {
			this.vsys = vsys;
			updateVoltageHistory();
		}
	}

	public void setVsdi(Double vsdi) {
		if (vsdi != null) {
			this.vsdi = vsdi;
		}
	}

	public void setVsdi(String vsdi) {
		if (vsdi != null) {
			this.vsdi = new Double(vsdi);
		}
	}

	public Integer getBatteryLevel() {
		Double mean=0d;
		Double std=0d;
		Double noiseupper=0d;Double uppercount = 0d;
		Double noiselower=0d;Double lowercount = 0d;
		if (!isSibNode() && !isBBControl() && !isAccessNode()) {
			batterylevel = null;
			return batterylevel;
		}
		else if ((isBBControl() || isAccessNode()) && voltageHistory.size()>0) {
			if (voltageHistory.getLast()<11.5)
				batterylevel=50;
			else if (voltageHistory.getLast()<12)
				batterylevel=75;
			else
				batterylevel=100;
			return batterylevel;
		}
		if (voltageHistory.size()<2)
			return batterylevel;
		for (Double d:voltageHistory) {
			mean+=d;
		}
		mean = mean / voltageHistory.size();
		if (mean < 0.5) {
			batterylevel = null;
			return batterylevel;
		}
		else if (mean < 3.25) {
			batterylevel = 0;
			return batterylevel;
		}
		for (Double d:voltageHistory) {
			std+=Math.pow(d-mean,2);
		}
		std = 9*std/voltageHistory.size();
		for (Double d:voltageHistory) {
			if (Math.pow(d-mean,2) < std) {
				if (d<mean) {
					noiselower+=d-mean;
					lowercount++;
				} else {
					noiseupper+=d-mean;
					uppercount++;
				}
			}
		}
		if (uppercount==0 || lowercount==0)
			return batterylevel;
		Double level = noiseupper/uppercount - noiselower/lowercount;
		logger.debug("voltage noise level for node "+node_id+" :"+level);
		if (level<0.02)
			batterylevel = 100;
		else if (level<0.04)
			batterylevel = 50;
		else if (level<0.08)
			batterylevel = 25;
		else if (level<0.1)
			batterylevel = 5;
		else
			batterylevel = 0;
		return batterylevel;
	}
	
	public void setBatteryLevel(Integer level) {
		batterylevel = level;
	}
	
	private void updateVoltageHistory () {
		voltageHistory.add(this.vsys);
		if (voltageHistory.size()>VOLTAGE_HISTORY_SIZE)
			voltageHistory.removeFirst();
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
	
	// coordinate
	public void setCoordinateLongitude(String l) {
		if (l != null) {
			if (coordinate == null) {
				coordinate = new Coordinate();
			}
			coordinate.x = new Double(l);
		}
	}
	
	public void setCoordinateLatitude(String l) {
		if (l != null) {
			if (coordinate == null) {
				coordinate = new Coordinate();
			}
			coordinate.y = new Double(l);
		}
	}

	public void setCoordinateAltitude(String a) {
		if (a != null) {
			if (coordinate == null) {
				coordinate = new Coordinate();
			}
			coordinate.z = new Double(a);
		}
	}
	
	public String getCoordinateLongitude() {
		if (coordinate == null)
			return null;
		return new Double(coordinate.x).toString();
	}
	
	public String getCoordinateLatitude() {
		if (coordinate == null)
			return null;
		return new Double(coordinate.y).toString();
	}

	public String getCoordinateAltitude() {
		if (coordinate == null)
			return null;
		return new Double(coordinate.z).toString();
	}

	public void setCorestation() {
		iscorestation = new Boolean(true);
		corestation_running = true;
	}

}
