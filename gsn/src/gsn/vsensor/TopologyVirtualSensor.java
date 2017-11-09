
package gsn.vsensor;

import java.io.ByteArrayInputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;

import gsn.Main;
import gsn.Mappings;
import gsn.beans.InputInfo;
import gsn.beans.Link;
import gsn.beans.NetworkTopology;
import gsn.beans.SensorNode;
import gsn.beans.SensorNodeConfiguration;
import gsn.beans.StreamElement;
import gsn.storage.DataEnumerator;

/**
* @author Roman Lim
*/

public class TopologyVirtualSensor extends AbstractVirtualSensor {
	
	public static final long STANDBY_TIMEOUT_MS = 300000L; // time until a node is seen as unconnected
	public static final long NODE_TIMEOUT = 30L * 24L * 3600000L; // time until a node and its history is removed
	private static final long LINK_TIMEOUT = 3600000L;	   // time until a link is removed
	private static final long NODE_CONFIGURABLE_TIME = 5L * 60000L; // time until a node is not configurable anymore
	private static final long NODE_CONFIGURE_TIMEOUT = 6L * 60000L; // time to wait until configuration is resent
	private static final long NODE_CONFIGURE_NEXT_TRY_TIMEOUT = 30000L; // time to wait until next configuration entry is tried
	private static final long NODE_CONFIGURE_CHECK_TIMEOUT = 2L * 3600000L; // time to wait between config checks
	private static final long NODE_CONFIGURE_CHECK_TIMEOUT_STARTUP = 300000L; // time to wait between config checks on startup
	private static final short EVENT_DATACONFIG = 40;
	private static final short EVENT_PSB_POWER = 32;
	private static final short EVENT_BB_POWER_OFF = 31;
	private static final short EVENT_BB_POWER_ON = 30;
	public static final short DATA_CONTROL_CMD = 1;
	public static final short DATA_CONTROL_CMD1 = 3;
	public static final short DATA_CONTROL_CMD2 = 4;
	public static final short GUMSTIX_CTRL_CMD = 14;
	public static final short DATA_CONTROL_NETWORK_FLAG = 0x400;
	public static final short DATA_CONTROL_SENSOR_FLAG = 0x800;
	public static final short DATA_CONTROL_WRITE_FLAG = 0x800;
	public static final int BROADCAST_ADDR = 0xFFFF;
	private static short REPETITION_COUNT = 3;
	
	private final transient Logger logger = Logger.getLogger( this.getClass() );
	
	private static final String[] configurationParameters = {
		"node-id-field",						// 0
		"parent-id-field",						// 1
		"timestamp-field",						// 2
		"generation-time-field",				// 3
		"vsys-field",							// 4
		"current-field",						// 5
		"temperature-field",					// 6
		"humidity-field",						// 7
		"flash-count-field",					// 8
		"uptime-field",							// 9
		"access-node-stream-name",				// 10
		"powerswitch-stream-name",				// 11
		"rssi-stream-name",						// 12: this stream does not count to the packetcount
		"rssi-node-id-field",					// 13
		"rssi-field",							// 14
		"eventlogger-stream-name",				// 15: this stream does not count to the packetcount
		"eventlogger-id-field",					// 16
		"eventlogger-value-field",				// 17
		"valid-field",							// 18
		"powerswitch-p1-field",					// 19
		"powerswitch-p2-field",					// 20
		"sdivoltage-field",						// 21
		"ae-stream-name",						// 22
		"corestation-statistics-stream-name",	// 23: this stream does not count to the packetcount
		"wgps-stream-name",						// 24
		"nodehealth-stream-name",				// 25
		"corestation-connected-field",			// 26
        "backlogstatus-dynamic-stream-name",	// 27: this stream does not count to the packetcount
        "backlogstatus-db-entries-field",		// 28
        "dpp-stream-name",						// 29
        "wgpsv2-imu-stream-name",				// 30
	};
	
	private static final String commandConfigurationParameter = "dozer-command-vs";

	private String [] configuration = {};
	private String commandConfigurationValue;
	private Map<Integer, SensorNode> nodes;
	private CommandScheduler scheduler; 
	private boolean configurable;
	
	@Override
	synchronized public void dataAvailable(String inputStreamName, StreamElement data) {
		Integer node_id=null;
		Long timestamp=null;
		Long generation_time=null;
		Serializable s;
		boolean notifyscheduler=false;
		Short event_value = null;
		Short event_id = null;
		Short node_type = null;
		Byte valid = null;
		Boolean p1 = null;
		Boolean p2 = null;
		
		if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( "data received under the name *" ).append( inputStreamName ).append( "* to the TopologyVS." ).toString( ) );
		s = data.getData(configuration[0]);
		if (s instanceof Integer) {
			node_id = (Integer)s;
		}
		s = data.getData(configuration[2]);
		if (s instanceof Long) {
			timestamp = (Long)s;
		}
		s = data.getData(configuration[3]);
		if (s instanceof Long) {
			generation_time = (Long)s;
		}
		if (node_id==null || timestamp==null || generation_time==null) {
			logger.error("No node id specified, skipping stream element (stream "+inputStreamName+")");
			return;
		}
		synchronized (nodes) {
			if (!nodes.containsKey(node_id)) {
				logger.debug("new node: "+node_id);	
				nodes.put(node_id, new SensorNode(node_id)); 
			}
			SensorNode node = nodes.get(node_id);
			
			// save always latest timestamp
			boolean isLatest = false;
			if (node.timestamp==null || node.timestamp.compareTo(timestamp) < 0) {
				isLatest = true;
			}
				
			// RSSI
			if (inputStreamName.startsWith(configuration[12])) {
				// save always latest rssi information
				if (isLatest) {
					logger.debug( "got rssi info:" + data.getData("HEADER_ORIGINATORID") + " " + data.getData("RSSI_NODEID") + " " + data.getData("RSSI"));
					Link newlink=null;
					Integer rssi_node_id;
					Short rssi;
					s = data.getData(configuration[13]);
					if (s instanceof Integer) {
						rssi_node_id = (Integer)s;
					}
					else {
						logger.debug("rssi node id wrong type");
						return;
					}
					s = data.getData(configuration[14]);
					if (s instanceof Short) {
						rssi = (Short)s;
					}
					else {
						logger.debug("rssi wrong type");
						return;
					}
					for (Iterator<Link> j = node.links.iterator(); j.hasNext();) { 
						Link l = j.next();
						if (l.node_id.equals(rssi_node_id)) {
							newlink = l;
							break;
						}
					}
					if (newlink == null) {
						newlink = new Link();
						node.links.add(newlink);
					}
					newlink.node_id=rssi_node_id;
					newlink.rssi=rssi;
					newlink.timestamp=timestamp;
					// do not count rssi info to packets
					node.timestamp = timestamp;
					node.generation_time = generation_time;
				}
			}
			// events
			else if (inputStreamName.startsWith(configuration[15])) {
				if (isLatest) {
					node.timestamp = timestamp;
					node.generation_time = generation_time;
				}
				// eventlogger
				s = data.getData(configuration[16]);
				if (s instanceof Short) {
					event_id = (Short)s;
				}
				s = data.getData(configuration[17]);
				if (s instanceof Integer) {
					event_value = ((Integer)s).shortValue();
				}
				logger.debug("got event "+event_id+" with value "+(event_value!=null?event_value:"null"));
				if (event_id == EVENT_DATACONFIG && event_value!=null) {
					logger.debug("added data configuration");
					if (node.configuration==null)
						node.configuration = new SensorNodeConfiguration(event_value, node.nodetype, timestamp);
					else
						node.configuration.update(event_value, node.nodetype);
					if (node.pendingConfiguration!=null) {
						if (node.pendingConfiguration.hasDataConfig1() && node.configuration.getConfiguration1().equals(node.pendingConfiguration.getConfiguration1()))
							node.pendingConfiguration.removeDataConfig1();
						if (node.pendingConfiguration.hasDataConfig2() && node.configuration.getConfiguration2().equals(node.pendingConfiguration.getConfiguration2()))
							node.pendingConfiguration.removeDataConfig2();
						if (!node.pendingConfiguration.hasDataConfig1() && !node.pendingConfiguration.hasDataConfig2() && !node.pendingConfiguration.hasPortConfig())
							node.pendingConfiguration=null;
					}
					notifyscheduler=true;
					node_type = new Short(node.nodetype);
				}
				else if (event_id == EVENT_PSB_POWER && event_value!=null) {
					p1 = (event_value & 1) > 0;
					p2 = (event_value & 2) > 0;
					logger.debug("received port info (event): "+p1+" "+p2);
					if (node.pendingConfiguration!=null) {
						logger.debug("have pending "+node.pendingConfiguration.powerswitch_p1+" "+node.pendingConfiguration.powerswitch_p2);
						if (node.pendingConfiguration.hasPortConfig() && node.pendingConfiguration.powerswitch_p1.equals(p1) && node.pendingConfiguration.powerswitch_p2.equals(p2)) {
							node.pendingConfiguration.removePortConfig();
							logger.debug("remove port config");
						}
						if (!node.pendingConfiguration.hasDataConfig1() && !node.pendingConfiguration.hasDataConfig2() && !node.pendingConfiguration.hasPortConfig())
							node.pendingConfiguration=null;
					}
					notifyscheduler=true;
					node_type = new Short(node.nodetype);
				}
				else if (event_id == EVENT_BB_POWER_ON || event_id == EVENT_BB_POWER_OFF) {
					if (event_id == EVENT_BB_POWER_ON)
						node.corestation_online = new Boolean(true);
					else
						node.corestation_online = new Boolean(false);
				}
				// do not count events to packets
			}
			// CoreStation Statistics
			else if (inputStreamName.equals(configuration[23])) {
				if (node.iscorestation == null || !node.iscorestation)
					node.setCorestation();
				// save always latest CoreStation information
				if (isLatest) {
					Byte connected = null;
					node.timestamp = timestamp;
					node.generation_time = generation_time;
					
					s = data.getData(configuration[26]);
					if (s instanceof Byte) {
						connected = (Byte)s;
					}
					
					if (connected == 1 && (node.corestation_online == null || !node.corestation_online))
						node.corestation_online = new Boolean(true);
					else if  (connected == 0 && (node.corestation_online == null || node.corestation_online))
						node.corestation_online = new Boolean(false);
					else
						return;
				}
				else
					return;
			}
			// BackLogStatus Dynamic
			else if (inputStreamName.equals(configuration[27])) {
				if (node.corestation_online == null || !node.corestation_online)
					node.corestation_online = new Boolean(true);
				// save always latest BackLog DB information
				if (isLatest) {
					Integer dbEntries = null;
					s = data.getData(configuration[28]);
					if (s instanceof Integer) {
						dbEntries = (Integer)s;
					}
					node.db_entries = dbEntries;
					node.timestamp = timestamp;
					node.generation_time = generation_time;
				}
				else
					return;
			}
			else {
				if (isLatest) {
					node.timestamp = timestamp;
					node.generation_time = generation_time;
				}
				node.packet_count++;
				if (inputStreamName.equals(configuration[10]) && !node.isAccessNode()) {
					node.setNodeType(SensorNode.BASESTATION);
					// adjust configuration
					node.pendingConfiguration=null;
					if (node.configuration!=null)
						node.configuration=new SensorNodeConfiguration(node.configuration, node.nodetype);
				}
				else if (inputStreamName.equals(configuration[11])) { // power switch packets
					if (!node.isPowerSwitch()) {
						node.setNodeType(SensorNode.POWERSWITCH_TN);
						// 	adjust configuration
						node.pendingConfiguration=null;
						if (node.configuration!=null)
							node.configuration=new SensorNodeConfiguration(node.configuration, node.nodetype);
					}
					s = data.getData(configuration[19]);
					if (s instanceof Byte)
						p1 = ((Byte)s == 1);
					s = data.getData(configuration[20]);
					if (s instanceof Byte)
						p2 = ((Byte)s == 1);
					if (p1!=null && p2!=null) {
						if (node.configuration==null)
							node.configuration=new SensorNodeConfiguration(p1, p2);
						else
							node.configuration.update(p1, p2);
						logger.debug("received port info: "+p1+" "+p2);
						if (node.pendingConfiguration!=null) {
							if (node.pendingConfiguration.hasPortConfig())
								logger.debug("pending config: "+node.pendingConfiguration.powerswitch_p1+" "+node.pendingConfiguration.powerswitch_p2);
							if (node.pendingConfiguration.hasPortConfig() && node.pendingConfiguration.powerswitch_p1.equals(p1) && node.pendingConfiguration.powerswitch_p2.equals(p2)) {
								node.pendingConfiguration.removePortConfig();
							}
							if (!node.pendingConfiguration.hasDataConfig1() && !node.pendingConfiguration.hasDataConfig2() && !node.pendingConfiguration.hasPortConfig())
								node.pendingConfiguration=null;
						}
						notifyscheduler=true;
						event_id = EVENT_PSB_POWER;
						node_type = new Short(node.nodetype);
					}
				}
				else if (inputStreamName.equals(configuration[22])) { // ae-board packets
					if (!node.isAENode())
						node.setNodeType(SensorNode.AE_TINYNODE);
					else
						return;
				}
				else if (inputStreamName.equals(configuration[24])) { // wgps-board space vehicle packets
					if(!node.isWGPSNode())
						node.setNodeType(SensorNode.WGPS_TINYNODE);
					else
						// we do not want all sv packets generating a topology stream
						return;
				}
				else if (inputStreamName.equals(configuration[29])) { // dpp packets
					if(!node.isAccessNode() && !node.isDPPNode())
						node.setNodeType(SensorNode.DPP);
				}
				else if (inputStreamName.equals(configuration[30])) { // wgps v2 imu packets
					node.setWGPSv2();
				}
				s = data.getData(configuration[1]);
				if (s instanceof Integer) {
					node.parent_id = (Integer)s;
				}
	
				// dozer health
				// save always latest health information
				if ((inputStreamName.equals(configuration[25]) || inputStreamName.equals(configuration[29])) && isLatest) {
					// Vsys
					s = data.getData(configuration[4]);
					if (s instanceof Integer)
						node.vsys = new Double((Integer)s);
					// Current
					s = data.getData(configuration[5]);
					if (s instanceof Integer)
						node.current = new Double((Integer)s);
					// Valid
					s = data.getData(configuration[18]);
					if (s instanceof Byte) {
						valid = (Byte)s;
					}
					// Temperature
					s = data.getData(configuration[6]);
					if (s instanceof Integer) {
						if (valid!=null && valid==0) {
							logger.debug("sample not valid");
							node.temperature = null;
							node.humidity = null;
						}
						else {
							node.temperature = new Double((Integer)s);
							// Humidity
							s = data.getData(configuration[7]);
							if (s instanceof Integer)
								node.humidity = new Double((Integer)s);
						}
					}
					// Flash count
					s = data.getData(configuration[8]);
					if (s instanceof Integer)
						node.flash_count = (Integer)s;
					// Uptime
					s = data.getData(configuration[9]);
					if (s instanceof Integer)
						node.uptime = (Integer)s;
					else if (s instanceof Long)
						node.uptime = ((Long)s).intValue();
					// VSdi
					s = data.getData(configuration[21]);
					if (s instanceof Integer)
						node.vsdi = new Double((Integer)s);
				}
			}
			// remove outdated information
			Long now = System.currentTimeMillis();
			for (Iterator<SensorNode> i = nodes.values().iterator(); i.hasNext();) {
				SensorNode n = i.next();
				if (now - n.timestamp > NODE_TIMEOUT) {
					logger.debug("remove node "+n.node_id+", last timestamp was "+n.timestamp);
					i.remove();	
				}
				else {
					for (Iterator<Link> j = n.links.iterator(); j.hasNext();) { 
						Link l = j.next();
						if (now - l.timestamp > LINK_TIMEOUT) {
							logger.debug("remove link from "+n.node_id+" to "+l.node_id+", last timestamp was "+l.timestamp);
							j.remove();
						}
					}
				}
			}
			generateData();
		}
		// notify scheduler
		if (notifyscheduler && scheduler!=null) {
			logger.debug("notify scheduler");
			if (event_id == EVENT_DATACONFIG)
				scheduler.configurationUpdate(node_id, event_value, node_type);
			else if (p1!=null && p2!=null)
				scheduler.portConfigurationUpdate(node_id, p1, p2, node_type);
		}

	}
	
	private void addToQueue(Integer node_id, Short nodetype, SensorNodeConfiguration c, List<SensorNode> queue) {
		// for each configuration packet add one item to queue
		if (c.hasDataConfig1()) {
			logger.debug("add data config 1 to queue for node "+node_id);
			SensorNode newnode = new SensorNode(node_id);
			newnode.pendingConfiguration = new SensorNodeConfiguration(c, nodetype);
			newnode.pendingConfiguration.removeDataConfig2();
			newnode.pendingConfiguration.removePortConfig();
			newnode.pendingConfiguration.querytype = null;
			newnode.pendingConfiguration.timestamp = System.currentTimeMillis();
			queue.add(newnode);
		}
		if (c.hasDataConfig2()) {
			logger.debug("add data config 2 to queue for node "+node_id);
			SensorNode newnode = new SensorNode(node_id);
			newnode.pendingConfiguration = new SensorNodeConfiguration(c, nodetype);
			newnode.pendingConfiguration.removeDataConfig1();
			newnode.pendingConfiguration.removePortConfig();
			newnode.pendingConfiguration.querytype = null;
			newnode.pendingConfiguration.timestamp = System.currentTimeMillis();
			queue.add(newnode);
		}
		if (c.hasPortConfig()) {
			logger.debug("add port config to queue for node "+node_id);
			SensorNode newnode = new SensorNode(node_id);
			newnode.pendingConfiguration = new SensorNodeConfiguration(c, nodetype);
			newnode.pendingConfiguration.removeDataConfig1();
			newnode.pendingConfiguration.removeDataConfig2();
			newnode.pendingConfiguration.querytype = null;
			newnode.pendingConfiguration.timestamp = System.currentTimeMillis();
			queue.add(newnode);
		}
		if (c.isQuery()) {
			logger.debug("add query to queue for node "+node_id);
			SensorNode newnode = new SensorNode(node_id);
			newnode.pendingConfiguration = new SensorNodeConfiguration(c, nodetype);
			newnode.pendingConfiguration.removeDataConfig1();
			newnode.pendingConfiguration.removeDataConfig2();
			newnode.pendingConfiguration.removePortConfig();
			newnode.pendingConfiguration.timestamp = System.currentTimeMillis();
			queue.add(newnode);			
		}
	}
	
	@Override
	synchronized public InputInfo dataFromWeb ( String action,String[] paramNames, Serializable[] paramValues ) {
		ArrayList<SensorNode> configurationQueue;
		// read new network configuration
		int index = Arrays.asList(paramNames).indexOf("configuration");
		if (index < 0) {
			logger.debug("field <configuration> not found.");
			return new InputInfo(getVirtualSensorConfiguration().getName(), "field <configuration> not found.", false);
		}
		logger.debug("trying to parse configuration.");
		IBindingFactory bfact;
		try {
			Serializable s = paramValues[index];
			if (s instanceof String) {
				bfact = BindingDirectory.getFactory(NetworkTopology.class);
				IUnmarshallingContext uctx = bfact.createUnmarshallingContext();		
				NetworkTopology parsedconfiguration = (NetworkTopology) uctx.unmarshalDocument(new ByteArrayInputStream(
						((String)s).getBytes()), "UTF-8");
				configurationQueue = new ArrayList<SensorNode>();
				synchronized (nodes) {
					// remove all pending configurations
					// are there any missing configurations ?
					for (SensorNode n: parsedconfiguration.sensornodes) {
						// compare with current configuration
						SensorNode cn = nodes.get(n.node_id);
						if (n.node_id !=null && n.configuration!=null 
								&& cn!=null
								&& (
										cn.configuration==null
										|| !n.configuration.equals(cn.configuration))
						){
							logger.debug("new configuration for node "+n.node_id);
							// always enable events
							n.configuration.events = true;
							// always enable health
							n.configuration.health = true;
							cn.pendingConfiguration = new SensorNodeConfiguration(
									n.configuration,
									cn.nodetype);
							logger.debug("old config "+ cn.configuration + " "+cn.configuration.getConfiguration1() + " "+cn.configuration.getConfiguration2());
							logger.debug("new config "+ cn.pendingConfiguration + " "+cn.pendingConfiguration.getConfiguration1() + " "+cn.pendingConfiguration.getConfiguration2());
							if (cn.pendingConfiguration.hasDataConfig1() && cn.pendingConfiguration.getConfiguration1().equals(cn.configuration.getConfiguration1())) {
								cn.pendingConfiguration.removeDataConfig1();
								logger.debug("same data config");
							}
							if (cn.pendingConfiguration.hasDataConfig2() && cn.pendingConfiguration.getConfiguration2().equals(cn.configuration.getConfiguration2())) {
								cn.pendingConfiguration.removeDataConfig2();
								logger.debug("same data config");
							}
							if (cn.pendingConfiguration.hasPortConfig() && cn.pendingConfiguration.getPortConfiguration().equals(cn.configuration.getPortConfiguration())) {
								cn.pendingConfiguration.removePortConfig();
								logger.debug("same port config");
							}
							if (!cn.pendingConfiguration.hasPortConfig() && !cn.pendingConfiguration.hasDataConfig1() && !cn.pendingConfiguration.hasDataConfig2())
								cn.pendingConfiguration=null;
							if (cn.pendingConfiguration!=null) {
								// add to queue
								addToQueue(cn.node_id, cn.nodetype, cn.pendingConfiguration, configurationQueue);
								cn.pendingConfiguration.timestamp = System.currentTimeMillis();
							}
						}
					}
					generateData();
				}
				logger.info("successfully read new configuration.");

			}
			else {
				logger.warn("data type was "+s.getClass().getCanonicalName());
				return new InputInfo(getVirtualSensorConfiguration().getName(), "data type was "+s.getClass().getCanonicalName(), false);
			}
		} catch (JiBXException e) {
			logger.error("unmarshall did fail: "+e);
			return new InputInfo(getVirtualSensorConfiguration().getName(), "unmarshall did fail: "+e.getMessage(), false);
		}
		// schedule reconfigure commands
		if(configurationQueue.size()>0 && scheduler!=null) {
			scheduler.reschedule(configurationQueue);
		}
		return new InputInfo(getVirtualSensorConfiguration().getName(), "", true);
	}
	
	synchronized void generateData() {
		NetworkTopology net = new NetworkTopology(configurable); 
		net.sensornodes = new ArrayList<SensorNode>(nodes.values());
		try {
			IBindingFactory bfact = BindingDirectory.getFactory(NetworkTopology.class);
			IMarshallingContext mctx = bfact.createMarshallingContext();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			mctx.marshalDocument(net, "UTF-8", null, baos);
			StreamElement se = new StreamElement(getVirtualSensorConfiguration().getOutputStructure(),  new Serializable[]{baos.toString().getBytes()});
			dataProduced( se );
		} catch (JiBXException e) {
			e.printStackTrace();
			logger.error(e);
		}
	}

	@Override
	public void dispose() {
		generateData();
		if (scheduler != null)
			scheduler.shutdown();
	}

	@Override
	public boolean initialize() {
		ArrayList<SensorNode> configurationQueue = new ArrayList<SensorNode>();
		TreeMap <  String , String > params = getVirtualSensorConfiguration( ).getMainClassInitialParams( );
		// check if all parameters are specified
		ArrayList<String> c = new ArrayList<String>();
		for (String s : configurationParameters) {
			String val = params.get(s);
			if (val == null) {
				logger.error("Could not initialize, missing parameter "+s);
				return false;
			}
			c.add(val);
		}
		configuration = c.toArray(configuration);
		commandConfigurationValue = params.get(commandConfigurationParameter);
		if (commandConfigurationValue!=null) {
			commandConfigurationValue = commandConfigurationValue.toLowerCase();
			logger.debug("start command scheduler");
			scheduler = new CommandScheduler(commandConfigurationValue, this);
			configurable = true;
		}
		nodes = new Hashtable<Integer, SensorNode>();
		// load last available topology information
		String virtual_sensor_name = getVirtualSensorConfiguration().getName();
		StringBuilder query=  new StringBuilder("select * from " ).append(virtual_sensor_name).append(" where timed = (select max(timed) from " ).append(virtual_sensor_name).append(") order by PK desc limit 1");
		ArrayList<StreamElement> latestvalues=new ArrayList<StreamElement>() ;
		try {
	      DataEnumerator result = Main.getStorage(getVirtualSensorConfiguration()).executeQuery( query , false );
	      while ( result.hasMoreElements( ) ) 
	    	  latestvalues.add(result.nextElement());
	    } catch (SQLException e) {
	      logger.error("ERROR IN EXECUTING, query: "+query);
	      logger.error(e.getMessage(),e);
	    }
	    if (latestvalues.size()>0) {
	    	IBindingFactory bfact;
			try {
				Serializable s = latestvalues.get(latestvalues.size()-1).getData()[0];
				if (s instanceof byte[]) {
					bfact = BindingDirectory.getFactory(NetworkTopology.class);
					IUnmarshallingContext uctx = bfact.createUnmarshallingContext();		
					NetworkTopology lastTopology = (NetworkTopology) uctx.unmarshalDocument(new ByteArrayInputStream(
						(byte[])s), "UTF-8");
					for (SensorNode n: lastTopology.sensornodes) {
						if (n.node_id !=null && n.timestamp != null && n.generation_time != null) {
							// remove pending configurations that are already done
							if (n.configuration!=null && n.pendingConfiguration!=null) {
								if (n.pendingConfiguration.hasDataConfig1() && n.pendingConfiguration.getConfiguration1().equals(n.configuration.getConfiguration1()))
									n.pendingConfiguration.removeDataConfig1();
								if (n.pendingConfiguration.hasDataConfig2() && n.pendingConfiguration.getConfiguration2().equals(n.configuration.getConfiguration2()))
									n.pendingConfiguration.removeDataConfig2();
								if (n.pendingConfiguration.hasPortConfig() && n.pendingConfiguration.getPortConfiguration().equals(n.configuration.getPortConfiguration()))
									n.pendingConfiguration.removePortConfig();
								if (!n.pendingConfiguration.hasPortConfig() && !n.pendingConfiguration.hasDataConfig1() && !n.pendingConfiguration.hasDataConfig2())
									n.pendingConfiguration=null;
								if (n.pendingConfiguration!=null) {
									// add to queue
									addToQueue(n.node_id, n.nodetype, n.pendingConfiguration, configurationQueue);
								}
							}
							nodes.put(n.node_id, n);
						}
					}
					logger.info("successfully imported last network topology.");
				}
				else {
					logger.warn("data type was "+s.getClass().getCanonicalName());
				}
			} catch (JiBXException e) {
				logger.error("unmarshall did fail: "+e);
			}
	    }
	    else {
	    	logger.info("no old network status found.");
	    }
	    if (scheduler!=null) {
	    	scheduler.start();
	    	if (configurationQueue.size()>0)
	    		scheduler.reschedule(configurationQueue);
		}
		return true;
	}
	
	protected boolean isOnline (Integer node_id) {
		synchronized (nodes) {
			return nodes.containsKey(node_id) && System.currentTimeMillis() - nodes.get(node_id).generation_time < NODE_CONFIGURABLE_TIME; 
		}
	}
	
	protected void queryUnconfiguredNodes() {
		boolean broadcastquery = false;
		boolean dosinglequery = false;
		ArrayList<SensorNode> configurationQueue = new ArrayList<SensorNode>();
		synchronized (nodes) {			
			// count online nodes with unknown configuration
			int toconfigure = 0;
			int online = 0;
			for (SensorNode n: nodes.values()) {
				if (n.node_id !=null && n.timestamp != null && n.generation_time != null && (System.currentTimeMillis() - n.generation_time < NODE_CONFIGURABLE_TIME)) {
					online++;
					if ((n.configuration == null || !n.configuration.hasDataConfig1()) && !n.isDozerSink())
						toconfigure++;
				}
			}
			dosinglequery = (double)toconfigure < 0.75 * (double)online; // do configuration query with broadcast if there are more than 75% unknown
			if (toconfigure > 0) {
				logger.info("There are "+toconfigure+" unknown configurations (number of online nodes: "+online+")");
				for (SensorNode n: nodes.values()) {
					if (n.node_id !=null && n.timestamp != null && n.generation_time != null) {
						if ((n.configuration == null || !n.configuration.hasDataConfig1()) && (System.currentTimeMillis() - n.generation_time < NODE_CONFIGURABLE_TIME)) {
							if (n.isDozerSink() || dosinglequery) {
								logger.info("query node "+n.node_id+" for configuration.");
								addToQueue(n.node_id, n.nodetype, new SensorNodeConfiguration(SensorNodeConfiguration.QUERY_TYPE_OLD), configurationQueue);
								addToQueue(n.node_id, n.nodetype, new SensorNodeConfiguration(SensorNodeConfiguration.QUERY_TYPE_1), configurationQueue);
								addToQueue(n.node_id, n.nodetype, new SensorNodeConfiguration(SensorNodeConfiguration.QUERY_TYPE_2), configurationQueue);
							}
							else if (!broadcastquery) {
								logger.info("broadcast query for configuration.");
								broadcastquery = true;
								addToQueue(BROADCAST_ADDR, n.nodetype, new SensorNodeConfiguration(SensorNodeConfiguration.QUERY_TYPE_OLD), configurationQueue);
								addToQueue(BROADCAST_ADDR, n.nodetype, new SensorNodeConfiguration(SensorNodeConfiguration.QUERY_TYPE_1), configurationQueue);
								addToQueue(BROADCAST_ADDR, n.nodetype, new SensorNodeConfiguration(SensorNodeConfiguration.QUERY_TYPE_2), configurationQueue);
							}
						}
					}
				}
			}
		}
		if (configurationQueue.size()>0 && scheduler!=null) {
			scheduler.reschedule(configurationQueue);
		}
	}
	
	class CommandScheduler extends Thread {

		private final static short CONFIG_NONE = 0;
		private final static short CONFIG_DATA1 = 1;
		private final static short CONFIG_DATA2 = 2;
		private final static short CONFIG_PORT = 3;

		private volatile boolean running=false;
		private List<SensorNode> queue;
		private List<SensorNode> newqueue;
		private String CommandVSName;
		private SensorNode currentNode;
		private boolean rescheduled;
		private boolean configurationdone;
		private TopologyVirtualSensor tvs;
		private short configType = CONFIG_NONE;
		
		
		public CommandScheduler(String CommandVSName, TopologyVirtualSensor tvs) {
			this.CommandVSName = CommandVSName;
			this.tvs = tvs;
			setName("CommandScheduler");
			queue = new ArrayList<SensorNode>();
			logger.debug("use vs "+CommandVSName+" for commands");
		}
		
		synchronized public void reschedule(Iterable<SensorNode> network) {
			newqueue = new ArrayList<SensorNode>();
			for (Iterator<SensorNode> i=network.iterator();i.hasNext();) {
				SensorNode n = i.next();
				if (n.pendingConfiguration != null) {
					newqueue.add(n);
				}
			}
			logger.debug("reschedule, "+newqueue.size()+" entries");
			synchronized (this) {
				rescheduled = true;
				this.notify();
			}
		}
		
		private void sendDataConfigQueryCommand(Integer destination, SensorNodeConfiguration config) {
			AbstractVirtualSensor vs;
			String[] fieldnames = {"destination","cmd","arg","repetitioncnt"};
			Serializable[] values =  {destination.toString(), Integer.toString(config.querytype), "0", Short.toString(REPETITION_COUNT)};
			logger.debug(CommandVSName+"< dest: "+destination +" data query config: "+config.querytype);
			try {
				vs = Mappings.getVSensorInstanceByVSName(CommandVSName).borrowVS();
				logger.debug("send command via "+vs.getVirtualSensorConfiguration().getName());
				vs.dataFromWeb("tosmsg", fieldnames, values);
				Mappings.getVSensorInstanceByVSName(CommandVSName).returnVS(vs);
			} catch (Exception e) {
				e.printStackTrace();
				logger.error(e);
			}
		}
		
		private void sendDataConfig1Command(Integer destination, SensorNodeConfiguration config) {
			AbstractVirtualSensor vs;
			String[] fieldnames = {"destination","cmd","arg","repetitioncnt"};
			Serializable[] values =  {destination.toString(), Short.toString(DATA_CONTROL_CMD), config==null?"0":(Short.toString((short)(config.getConfiguration1() + DATA_CONTROL_NETWORK_FLAG + DATA_CONTROL_SENSOR_FLAG))), Short.toString(REPETITION_COUNT)};
			// old or new config command ?
			if (config != null && config.vaisala_wxt520!=null && config.vaisala_wxt520==true) {
				values[1]=Short.toString(DATA_CONTROL_CMD1);
				values[2]= config==null?"0":(Short.toString((short)(config.getConfiguration1() + DATA_CONTROL_WRITE_FLAG)));
			}
			logger.debug(CommandVSName+"< dest: "+destination +" data config: "+(config==null?"null":config.getConfiguration1()));
			try {
				vs = Mappings.getVSensorInstanceByVSName(CommandVSName).borrowVS();
				logger.debug("send command via "+vs.getVirtualSensorConfiguration().getName());
				vs.dataFromWeb("tosmsg", fieldnames, values);
				Mappings.getVSensorInstanceByVSName(CommandVSName).returnVS(vs);
			} catch (Exception e) {
				e.printStackTrace();
				logger.error(e);
			}
		}
		
		private void sendDataConfig2Command(Integer destination, SensorNodeConfiguration config) {
			AbstractVirtualSensor vs;
			String[] fieldnames = {"destination","cmd","arg","repetitioncnt"};
			Serializable[] values =  {destination.toString(), Short.toString(DATA_CONTROL_CMD2), config==null?"0":(Short.toString((short)(config.getConfiguration2() + DATA_CONTROL_WRITE_FLAG))), Short.toString(REPETITION_COUNT)};
			logger.debug(CommandVSName+"< dest: "+destination +" data config: "+(config==null?"null":config.getConfiguration2()));
			try {
				vs = Mappings.getVSensorInstanceByVSName(CommandVSName).borrowVS();
				logger.debug("send command via "+vs.getVirtualSensorConfiguration().getName());
				vs.dataFromWeb("tosmsg", fieldnames, values);
				Mappings.getVSensorInstanceByVSName(CommandVSName).returnVS(vs);
			} catch (Exception e) {
				e.printStackTrace();
				logger.error(e);
			}
		}
		
		private void sendPortConfigCommand(Integer destination, SensorNodeConfiguration config) {
			AbstractVirtualSensor vs;
			String[] fieldnames = {"destination","cmd","arg","repetitioncnt"};
			Serializable[] values =  {destination.toString(), Short.toString(GUMSTIX_CTRL_CMD), Short.toString((short)(config.getPortConfiguration())), Short.toString(REPETITION_COUNT)};
			logger.debug(CommandVSName+"< dest: "+destination +" port config: "+config.getPortConfiguration());
			try {
				vs = Mappings.getVSensorInstanceByVSName(CommandVSName).borrowVS();
				logger.debug("send command via "+vs.getVirtualSensorConfiguration().getName());
				vs.dataFromWeb("tosmsg", fieldnames, values);
				Mappings.getVSensorInstanceByVSName(CommandVSName).returnVS(vs);
			} catch (Exception e) {
				e.printStackTrace();
				logger.error(e);
			}
		}
		
		public void configurationUpdate(Integer node_id, Short event_value, Short node_type) {
			synchronized (this) {
				logger.debug("configurationUpdate: "+node_id+" "+event_value+" "+node_type);
				if (currentNode!=null && node_id.equals(currentNode.node_id)) {
					logger.debug("received configuration update from current node ["+node_id+"]");
					SensorNodeConfiguration testconfig = new SensorNodeConfiguration(event_value, node_type);
					if (configType==CONFIG_DATA1 && currentNode.pendingConfiguration.getConfiguration1().equals(testconfig.getConfiguration1())) {
						logger.debug("data configuration1 done");
						configurationdone = true;
						this.notify();	
					}
					else if (configType==CONFIG_DATA2 && currentNode.pendingConfiguration.getConfiguration2().equals(testconfig.getConfiguration2())) {
						logger.debug("data configuration2 done");
						configurationdone = true;
						this.notify();	
					}
				}
				else {
					logger.debug("not current node "+(currentNode==null?"null":currentNode.node_id));
					for (Iterator<SensorNode> i=queue.iterator();i.hasNext();){
						SensorNode n = i.next();
						if (n.node_id.equals(node_id)) {
							SensorNodeConfiguration testconfig = new SensorNodeConfiguration(event_value, node_type);
							if (n.pendingConfiguration.hasDataConfig1()) {
								if (n.pendingConfiguration.getConfiguration1().equals(testconfig.getConfiguration1())) {
									n.pendingConfiguration.removeDataConfig1();
									if (!n.pendingConfiguration.hasPortConfig() && !n.pendingConfiguration.hasDataConfig2()) {
										logger.debug("remove pending config for node "+node_id);
										i.remove();
									}
								}
							}
							if (n.pendingConfiguration.hasDataConfig2()) {
								if (n.pendingConfiguration.getConfiguration2().equals(testconfig.getConfiguration2())) {
									n.pendingConfiguration.removeDataConfig2();
									if (!n.pendingConfiguration.hasPortConfig() && !n.pendingConfiguration.hasDataConfig1()) {
										logger.debug("remove pending config for node "+node_id);
										i.remove();
									}
								}
							}
						}
					}
				}
			}
		}
		
		public void portConfigurationUpdate(Integer node_id, Boolean p1, Boolean p2, Short node_type) {
			synchronized (this) {
				logger.debug("port configurationUpdate: "+node_id+" "+p1+" "+p2+" "+node_type);				
				if (currentNode!=null && node_id.equals(currentNode.node_id)) {
					logger.debug("received configuration update from current node ["+node_id+"]");
					SensorNodeConfiguration testconfig = new SensorNodeConfiguration(p1, p2);
					if (configType==CONFIG_PORT && currentNode.pendingConfiguration.getPortConfiguration().equals(testconfig.getPortConfiguration())) {
						logger.debug("port configuration done");
						configurationdone = true;
						this.notify();	
					}
				}
				else {
					logger.debug("not current node "+(currentNode==null?"null":currentNode.node_id));
					for (Iterator<SensorNode> i=queue.iterator();i.hasNext();){
						SensorNode n = i.next();
						if (n.node_id.equals(node_id) && n.pendingConfiguration.hasPortConfig()) {
							SensorNodeConfiguration testconfig = new SensorNodeConfiguration(p1, p2);
							if (n.pendingConfiguration.getPortConfiguration().equals(testconfig.getPortConfiguration())) {
								n.pendingConfiguration.removePortConfig();
								if (!n.pendingConfiguration.hasDataConfig1() && !n.pendingConfiguration.hasDataConfig2()) {
									logger.debug("remove pending config for node "+node_id);
									i.remove();
								}
							}
						}
					}
				}
			}
		}

		@Override
		public void run() {
			running=true;
			long timeout;
			try {
				synchronized (this) {
					while (running && queue.size()==0) {
						logger.debug("check for online nodes that returned no configuration information.");
						tvs.queryUnconfiguredNodes();
						if (rescheduled) {
							rescheduled = false;
							queue = newqueue;							
						}

						if (queue.size()==0) {
							logger.debug("suspend scheduler shortly on startup");
							this.wait(NODE_CONFIGURE_CHECK_TIMEOUT_STARTUP);
						}
					}
					while (running) {
						if (queue.size()==0) {
							logger.debug("suspend scheduler");
							this.wait(NODE_CONFIGURE_CHECK_TIMEOUT);
						}
						logger.debug("unsuspend scheduler");
						if (rescheduled) {
							rescheduled = false;
							queue = newqueue;
						}
						if (!running)
							break;
						// node online?
						logger.debug("queue has "+queue.size()+" entries");
						if (queue.size()==0) {
							logger.debug("check for online nodes that returned no configuration information.");
							tvs.queryUnconfiguredNodes();
							continue;
						}
						for (Iterator<SensorNode> i = queue.iterator();i.hasNext();) {
							SensorNode n=i.next();
							configurationdone = false;
							if (tvs.isOnline(n.node_id) || n.node_id.equals(BROADCAST_ADDR)) {
								currentNode = n;
								break;
							}
							else {
								logger.debug("node "+n.node_id+" is not online");
							}
						}
						if (currentNode != null) {
							queue.remove(currentNode);
							queue.add(currentNode);
							// send command
							if (currentNode.pendingConfiguration.isQuery()) {
								logger.debug("send query to "+currentNode.node_id);
								sendDataConfigQueryCommand(currentNode.node_id, currentNode.pendingConfiguration);
								configurationdone = true;
							}
							else if (currentNode.pendingConfiguration.hasDataConfig1()) {
								logger.debug("send data 1 configuration to "+currentNode.node_id);
								configType = CONFIG_DATA1;
								sendDataConfig1Command(currentNode.node_id, currentNode.pendingConfiguration);
							}
							else if (currentNode.pendingConfiguration.hasDataConfig2()) {
								logger.debug("send data 2 configuration to "+currentNode.node_id);
								configType = CONFIG_DATA2;
								sendDataConfig2Command(currentNode.node_id, currentNode.pendingConfiguration);
							}
							else {
								logger.debug("send port configuration to "+currentNode.node_id);
								configType = CONFIG_PORT;
								sendPortConfigCommand(currentNode.node_id, currentNode.pendingConfiguration);
							}
							timeout = NODE_CONFIGURE_TIMEOUT;
						}
						else
							timeout = NODE_CONFIGURE_NEXT_TRY_TIMEOUT;
						// wait
						logger.debug("wait");
						this.wait(timeout);
						logger.debug("timeout done");
						if (rescheduled)
							continue;
						if (currentNode!=null) {
							if (configurationdone) {
								// configuration was success-full
								logger.debug("configuration done for node "+currentNode.node_id);
								//remove from queue
								queue.remove(currentNode);
							}
							else {
								logger.debug("configuration failed for node "+currentNode.node_id);
							}
						}
						currentNode = null;
					}
				}
			} catch (Exception e) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				PrintStream ps = new PrintStream(baos);
				e.printStackTrace(ps);
				logger.debug(baos.toString());
				logger.error(e);
			}	
			logger.debug("command scheduler ended");
		}	
		
		public void shutdown() {
			running = false;
			synchronized (this) {
				this.notifyAll();
			}
		}
	}

}
