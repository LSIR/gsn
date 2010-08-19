
package gsn.vsensor;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
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
import gsn.beans.Link;
import gsn.beans.NetworkTopology;
import gsn.beans.SensorNode;
import gsn.beans.StreamElement;
import gsn.storage.DataEnumerator;

/**
* @author Roman Lim
*/

public class TopologyVirtualSensor extends AbstractVirtualSensor {
	
	private static final long LINK_TIMEOUT = 3600000;	   // time until a link is removed
	private static final long NODE_TIMEOUT = 24 * 3600000; // time until a node and its history is removed
	
	private final transient Logger logger = Logger.getLogger( this.getClass() );
	
	private static final String[] configurationParameters = {
		"node-id-field",
		"parent-id-field",
		"timestamp-field",
		"generation-time-field",
		"vsys-field",
		"current-field",
		"temperature-field",
		"humidity-field",
		"flash-count-field",
		"uptime-field",
		"access-node-stream-name",
		"powerswitch-stream-name",
		"rssi-stream-name", // this stream does not count to the packetcount
		"rssi-node-id-field",
		"rssi-field"
	};
	
	private String [] configuration = {};
	private Map<Integer, SensorNode> nodes;
	
	@Override
	public void dataAvailable(String inputStreamName,
			StreamElement data) {
		Integer node_id=null;
		Long timestamp=null;
		Long generation_time=null;
		Serializable s;
		
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
			logger.error("No node id specified, skipping stream element.");
			return;
		}
		if (!nodes.containsKey(node_id)) {
			logger.debug("new node: "+node_id);	
			nodes.put(node_id, new SensorNode(node_id)); 
		}
		else
			logger.debug("already seen:"+node_id);
		SensorNode node = nodes.get(node_id);
		
		if (inputStreamName.startsWith(configuration[12])) {
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
			return; // do not count rssi info to packets
		}
		node.packet_count++;
		if (inputStreamName.equals(configuration[10]))
			node.setAccessNode();
		else if (inputStreamName.equals(configuration[11]))
			node.setPowerSwitch();
		s = data.getData(configuration[1]);
		if (s instanceof Integer) {
			node.parent_id = (Integer)s;
		}

		// health
		s = data.getData(configuration[4]);
		if (s instanceof Integer) {
			if (node.isSibNode()) {
				node.vsys =  new Double((Integer)s)  * (2.56d/65536d) * (39d/24d);
			}
			else {
				logger.debug("vsys: use access node conversion");
				node.vsys =  new Double((Integer)s)  * (3d / 4095d);
			}
		}
		s = data.getData(configuration[5]);
		if (s instanceof Integer) {
			if ((Integer)s==0xffff)
				node.current=null;
			else
				node.current = new Double((Integer)s) * 2.56 / Math.pow(2, 16) / 0.15 * 10;
		}
		s = data.getData(configuration[6]);
		if (s instanceof Integer) {
			if ((Integer)s==0xffff) {
				node.temperature = null;
				node.humidity = null;
			}
			else {
				node.temperature = new Double(-39.63d + (0.01d * (new Double((Integer)s))));
				s = data.getData(configuration[7]);
				if (s instanceof Integer) {
					if ((Integer)s==0xffff)
						node.humidity = null;
					else {
						Double d = new Double((Integer)s);
						Double hum_rel = new Double(-4 + (0.0405d * d) - 0.0000028d * Math.pow(d, 2));				
						node.humidity = new Double((node.temperature - 25) * (0.01d + (0.00008d * d)) + hum_rel);
					}
				}
			}
		}
		s = data.getData(configuration[8]);
		if (s instanceof Integer)
			node.flash_count = (Integer)s;
		s = data.getData(configuration[9]);
		if (s instanceof Integer)
			node.uptime = (Integer)s;

		node.timestamp = timestamp;
		node.generation_time = generation_time;
		
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
		
		NetworkTopology net = new NetworkTopology();
		net.sensornodes = (SensorNode[])nodes.values().toArray(new SensorNode[nodes.size()]);
		
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
	}

	@Override
	public boolean initialize() {
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
		nodes = new Hashtable<Integer, SensorNode>();
		// load last available topology information
		String virtual_sensor_name = getVirtualSensorConfiguration().getName();
		StringBuilder query=  new StringBuilder("select * from " ).append(virtual_sensor_name).append(" where timed = (select max(timed) from " ).append(virtual_sensor_name).append(") order by PK desc limit 1");
		ArrayList<StreamElement> latestvalues=new ArrayList<StreamElement>() ;
		try {
	      DataEnumerator result = Main.getStorage(virtual_sensor_name).executeQuery( query , false );
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
						nodes.put(n.node_id, n);
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
		return true;
	}

}
