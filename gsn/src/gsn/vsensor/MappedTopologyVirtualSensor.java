package gsn.vsensor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Hashtable;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;

import gsn.beans.NetworkTopology;
import gsn.beans.SensorNode;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;

import com.vividsolutions.jts.geom.Coordinate;

public class MappedTopologyVirtualSensor extends AbstractVirtualSensor {
	
	private static final int MAPPING_UPDATE_TIMEOUT_MS = 5 * 60 * 1000;
	
	private static final transient Logger logger = Logger.getLogger( MappedTopologyVirtualSensor.class );
	private String deployment;
	private Hashtable<Integer, MappingEntry> cachedmappings = new Hashtable<Integer, MappingEntry>();
	
	@SuppressWarnings("unchecked")
	@Override
	public void dataAvailable(String inputStreamName,
			StreamElement streamElement) {	
		IBindingFactory bfact = null;
		NetworkTopology topology = null;
		long start = System.nanoTime();
		// streamElement should contain topology in xml representation
		if (streamElement.getData("data") != null) {
			try {
				Serializable s = streamElement.getData("data");
				if (s instanceof byte[]) {
					bfact = BindingDirectory.getFactory(NetworkTopology.class);
					IUnmarshallingContext uctx = bfact.createUnmarshallingContext();		
					topology = (NetworkTopology) uctx.unmarshalDocument(new ByteArrayInputStream(
						(byte[])s), "UTF-8");
					long now = System.currentTimeMillis(); 
					Hashtable<Integer, MappingEntry> node_ids = (Hashtable<Integer, MappingEntry>) cachedmappings.clone();
					
					for (SensorNode n: topology.sensornodes) {
						if (n.node_id !=null && n.generation_time != null) {
							node_ids.remove(n.node_id);
							if (now - n.timestamp < MAPPING_UPDATE_TIMEOUT_MS) {
								n.position = DataMapping.getPosition(n.node_id.intValue(), new Timestamp(n.generation_time), deployment, getVirtualSensorConfiguration().getName(), inputStreamName);
								if (n.position != null) {
									n.coordinate = DataMapping.getCoordinate(n.position.intValue(), deployment, getVirtualSensorConfiguration().getName(), inputStreamName);
									cachedmappings.put(n.node_id, new MappingEntry(n.position, n.coordinate));
								}
							}
							else {
								// use cached mapping
								if (cachedmappings.containsKey(n.node_id)) {
									n.position = cachedmappings.get(n.node_id).position;
									n.coordinate = cachedmappings.get(n.node_id).coordinate;
								}
							}
						}
					}
					for (Entry<Integer, MappingEntry> e: node_ids.entrySet()) {
						cachedmappings.remove(e.getKey());
					}
					topology.mapped = true;
					logger.debug("successfully imported network topology.");
				}
				else {
					logger.warn("wrong data type for topology: "+s.getClass().getCanonicalName());
					return;
				}
			} catch (JiBXException e) {
				logger.error("unmarshall did fail: "+e);
				return;
			}
	    }
		else {
			logger.warn("Stream did not contain topology information.");
			return;
		}
		
		// write topology
		try {
			IMarshallingContext mctx = bfact.createMarshallingContext();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			mctx.marshalDocument(topology, "UTF-8", null, baos);
			StreamElement se = new StreamElement(getVirtualSensorConfiguration().getOutputStructure(),  new Serializable[]{baos.toString().getBytes()});
			dataProduced( se );
		} catch (JiBXException e) {
			e.printStackTrace();
			logger.error(e);
		}
		if (logger.isDebugEnabled())
			logger.debug("position mapping for topology: " + Long.toString((System.nanoTime() - start) / 1000) + " us");
	}

	@Override
	public void dispose() {
		DataMapping.removeVS(this);
	}

	@Override
	public boolean initialize() {
		VSensorConfig vsensor = getVirtualSensorConfiguration();
		deployment = vsensor.getName().split("_")[0].toLowerCase();
		try {
			DataMapping.registerVS(this, deployment);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return false;
		}
		return true;
	}
	
	private class MappingEntry {
		public Coordinate coordinate;
		public Integer position;
		
		public MappingEntry(Integer position, Coordinate coordinate) {
			this.position = position;
			this.coordinate = coordinate;
		}
	}

}
