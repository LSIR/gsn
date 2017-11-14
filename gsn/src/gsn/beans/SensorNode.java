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
	
	// this has to be the same as in the Position Mapping VS!
	public static final short SIB_TINYNODE = 1;
	public static final short WGPS_TINYNODE = 2;
	public static final short POWERSWITCH_TN = 3;
	public static final short BASESTATION = 4;
	public static final short GPS_CORESTATION = 5;
	public static final short CAMZILLA_CORESTATION = 6;
	public static final short GPS_LOGGER = 7;
	public static final short WEBCAM = 8;
	public static final short AE_TINYNODE = 9;
	public static final short OUTPACK_MATE_3 = 10;
	public static final short IMIS_WEATHER_STATION = 11;
	public static final short GPS_L2 = 12;
	public static final short DPP = 13;
	public static final Short NODE_TYPE_UNKNOWN = -1;
	
	public static final short [] corestationTypes = {4, 5, 6};
	
	static final int VOLTAGE_HISTORY_SIZE = 60; // ~ 2h 
	
	public Short nodetype = SIB_TINYNODE;
		
	public Integer node_id;
	public Integer parent_id;
	public Long timestamp;		
	public Long generation_time;
	
	public Integer packet_count = 0;
	
	public Double vsys;
	public Double vsdi;
	public Double current;
	public Double temperature;
	public Double humidity;
	public Integer flash_count;
	public Integer uptime;
	public Boolean corestation_online;
	public Boolean iscorestation;
	public Boolean iswgpsv2;
	public Integer db_entries;
	
	public SensorNodeConfiguration configuration;
	public SensorNodeConfiguration pendingConfiguration;
			
	public ArrayList<Link> links;
	
	public Integer position;
	public String geoposition;
	
	private Integer batterylevel; 
	private LinkedList<Double> voltageHistory = new LinkedList<Double>();
	private DecimalFormat df = new DecimalFormat("0.00");
	
	public Coordinate coordinate = null;
	
	public SensorNode() {
		links = new ArrayList<Link>();
	}
	
	public SensorNode(Integer node_id) {
		this.node_id=node_id;
		links = new ArrayList<Link>();
	}
	
	public boolean isSibNode() {
		return nodetype == SIB_TINYNODE;
	}
	
	public boolean isAccessNode() {
		return nodetype == BASESTATION;
	}
	
	public boolean isPowerSwitch() {
		return nodetype == POWERSWITCH_TN;
	}
	
	public boolean isBBControl() {
		return nodetype == GPS_CORESTATION || nodetype == CAMZILLA_CORESTATION;
	}

	public boolean isAENode() {
		return nodetype == AE_TINYNODE;
	}

	public boolean isWGPSNode() {
		return nodetype == WGPS_TINYNODE;
	}

	public boolean isOutpackMate3Node() {
		return nodetype == OUTPACK_MATE_3;
	}

	public boolean isIMISWeatherStationNode() {
		return nodetype == IMIS_WEATHER_STATION;
	}

	public boolean isGPSL2Node() {
		return nodetype == GPS_L2;
	}

	public boolean isDPPNode() {
		return nodetype == DPP;
	}

	public boolean isDozerNode() {
		return isBBControl() || isAccessNode() || isAENode() || isWGPSNode() || isSibNode() || isPowerSwitch();
	}

	public Boolean isWGPSv2() {
		if (iswgpsv2 == null)
			return false;
		else
			return iswgpsv2;
	}
	
	public boolean hasSHT21() {
		return isBBControl() || isAccessNode() || isAENode() || (isWGPSNode() && !isWGPSv2());
	}
	
	public boolean hasSHT15() {
		return isSibNode() || isPowerSwitch();
	}

	public void setNodeType(Short nodetype) {
		this.nodetype = nodetype;
		if (nodetype == BASESTATION || nodetype == GPS_CORESTATION || nodetype == CAMZILLA_CORESTATION)
			iscorestation = true;
		else
			iscorestation = false;
	}
	
	public String getVsys() {
		if (vsys!=null)
			return df.format(vsys);
		return null;
	}
	
	public Double getVsysDbl() {
		return vsys;
	}
	
	public String getVsdi() {
		if (vsdi!=null)
			return df.format(vsdi);
		return null;
	}
	
	public Double getVsdiDbl() {
		return vsdi;
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
		if (!isSibNode() && !isBBControl() && !isAccessNode() && !isPowerSwitch() && !isWGPSNode()) {
			batterylevel = null;
			return batterylevel;
		}
		else if ((isAENode() || isBBControl() || isAccessNode() || isWGPSNode()) && voltageHistory.size()>0) {
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
		iscorestation = true;
	}

	public void setWGPSv2() {
		iswgpsv2 = true;
	}

	public boolean isDozerSink() {
		return node_id <= 1024;
	}
	
	public void updateNode(SensorNode node) {
		this.nodetype = node.nodetype;
		this.node_id = node.node_id;
		this.parent_id = node.parent_id;
		this.timestamp = node.timestamp;		
		this.generation_time = node.generation_time;
		
		this.packet_count = node.packet_count;
		
		this.setVsys(node.getVsysDbl());
		this.setVsdi(node.getVsdi());
		this.current = node.current;
		this.temperature = node.temperature;
		this.humidity = node.humidity;
		this.flash_count = node.flash_count;
		this.uptime = node.uptime;
		this.corestation_online = node.corestation_online;
		this.iscorestation = node.iscorestation;
		this.iswgpsv2 = node.iswgpsv2;
		this.db_entries = node.db_entries;
		
		this.configuration = node.configuration;
		this.pendingConfiguration = node.pendingConfiguration;
				
		this.links = node.links;
		
		this.position = node.position;
		this.geoposition = node.geoposition;
		
		this.coordinate = node.coordinate;
	}

	@Override
	public String toString() {
		String s = "SensorNode: ";
		
		s += "device_id=" + node_id + " / position=" + position + "\n";
		s += "DozerNode: " + (isDozerNode()? "yes":"no") + "\n";
		s += "DozerSink: " + (isDozerSink()? "yes":"no") + "\n";
		s += "AccessNode: " + (isAccessNode()? "yes":"no") + "\n";
		s += "SibNode: " + (isSibNode()? "yes":"no") + "\n";
		s += "WGPSNode: " + (isWGPSNode()? "yes":"no") + "\n";
		s += "WGPSv2: " + (isWGPSv2()? "yes":"no") + "\n";
		s += "PowerSwitch: " + (isPowerSwitch()? "yes":"no") + "\n";
		s += "AENode: " + (isAENode()? "yes":"no") + "\n";
		s += "BBControl: " + (isBBControl()? "yes":"no") + "\n";
		s += "DPPNode: " + (isDPPNode()? "yes":"no") + "\n";
		s += "GPSL2Node: " + (isGPSL2Node()? "yes":"no") + "\n";
		s += "IMISWeatherStationNode: " + (isIMISWeatherStationNode()? "yes":"no") + "\n";
		s += "OutpackMate3Node: " + (isOutpackMate3Node()? "yes":"no") + "\n";
		
		s += "temperature=" + temperature + ", humidity=" + humidity + "\n";
		s += "vsys=" + vsys + ", vsdi=" + vsdi + ", current=" + current;
		
		return s;
	}
}
