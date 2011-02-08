package gsn.vsensor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.sql.Timestamp;

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

public class MappedTopologyVirtualSensor extends AbstractVirtualSensor {
	
	private static final transient Logger logger = Logger.getLogger( MappedTopologyVirtualSensor.class );
	private String deployment;
	
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
					for (SensorNode n: topology.sensornodes) {
						if (n.node_id !=null && n.generation_time != null) {
							n.position = DataMapping.getPosition(deployment, n.node_id.intValue(), new Timestamp(n.generation_time));
						}
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

}
