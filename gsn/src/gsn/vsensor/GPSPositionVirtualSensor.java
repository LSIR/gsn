
package gsn.vsensor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.JiBXException;

import gsn.beans.NetworkTopology;
import gsn.beans.SensorNode;
import gsn.beans.StreamElement;

public class GPSPositionVirtualSensor extends AbstractVirtualSensor {
	
	private final transient Logger logger = Logger.getLogger( this.getClass() );
	
	private static final String[] configurationParameters = {
		"device-id-field",
		"timestamp-field",
		"generation-time-field",
		"latitude-field",
		"longitude-field",
	};
	
	private String [] configuration = {};
	private Map<Integer, SensorNode> nodes;
	private boolean configurable;
	
	@Override
	synchronized public void dataAvailable(String inputStreamName, StreamElement data) {
		Integer node_id=null;
		Long timestamp=null;
		Long generation_time=null;
		Serializable s;
		
		if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( "data received under the name *" ).append( inputStreamName ).append( "* to the GPSPositionVS." ).toString( ) );
		s = data.getData(configuration[0]);
		if (s instanceof Integer) {
			node_id = (Integer)s;
		}
		s = data.getData(configuration[1]);
		if (s instanceof Long) {
			timestamp = (Long)s;
		}
		s = data.getData(configuration[2]);
		if (s instanceof Long) {
			generation_time = (Long)s;
		}
		if (node_id==null || timestamp==null || generation_time==null) {
			logger.error("No node id, timestamp, or generation time specified, skipping stream element.");
			return;
		}
		synchronized (nodes) {
		if (!nodes.containsKey(node_id)) {
			logger.debug("new node: "+node_id);	
			nodes.put(node_id, new SensorNode(node_id)); 
		}
		SensorNode node = nodes.get(node_id);
		// save always latest timestamp
		if (node.timestamp==null || node.timestamp < timestamp) {
			node.timestamp = timestamp;
			node.generation_time = generation_time;
		}
		
		// Latitude
		s = data.getData(configuration[3]);
			if (s instanceof Double) {
		   node.coordinate.x = (Double)s;
	  	}
		// Longitude
		s = data.getData(configuration[4]);
			if (s instanceof Double) {
		   node.coordinate.y = (Double)s;
	  	}
		
		generateData();
		}
	}
	
	synchronized void generateData() {
		NetworkTopology net = new NetworkTopology(configurable); 
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
	public synchronized void dispose() {
		DataMapping.removeVS(this);
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
    
		return true;
	}

}
