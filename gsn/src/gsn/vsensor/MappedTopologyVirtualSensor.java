package gsn.vsensor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;

import gsn.beans.DataTypes;
import gsn.beans.NetworkTopology;
import gsn.beans.SensorNode;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.wrappers.DataMappingWrapper;
import gsn.wrappers.DataMappingWrapper.MappedEntry;

public class MappedTopologyVirtualSensor extends AbstractVirtualSensor {
	
	private static final transient Logger logger = Logger.getLogger( MappedTopologyVirtualSensor.class );
	private String deployment;

	private Map<Integer, SensorNode> nodes;
	
	@Override
	public void dataAvailable(String inputStreamName, StreamElement streamElement) {
		try {
			IBindingFactory bfact = null;
			NetworkTopology topology = null;
			ArrayList<Integer> nodesToBeRemoved = new ArrayList<Integer>();
			long start = System.nanoTime();
			Long now = System.currentTimeMillis();
			// streamElement should contain topology in xml representation
			if (streamElement.getData("data") != null) {
				try {
					Serializable s = streamElement.getData("data");
					if (s instanceof byte[]) {
						HashMap<Integer, MappedEntry> allPositions = DataMappingWrapper.getAllPositions(deployment, getVirtualSensorConfiguration().getName(), inputStreamName);
						bfact = BindingDirectory.getFactory(NetworkTopology.class);
						IUnmarshallingContext uctx = bfact.createUnmarshallingContext();		
						topology = (NetworkTopology) uctx.unmarshalDocument(new ByteArrayInputStream(
							(byte[])s), "UTF-8");
						
						for (int i=0; i<topology.sensornodes.size(); i++) {
							SensorNode n = topology.sensornodes.get(i);
							if (n.node_id !=null && n.generation_time != null) {
								try {
									n.position = DataMappingWrapper.getPosition(n.node_id.intValue(), n.generation_time, deployment, getVirtualSensorConfiguration().getName(), inputStreamName, false);
									if (n.position != null) {
										MappedEntry mappedEntry = allPositions.get(n.position);
										if (mappedEntry != null) {
											mappedEntry.spotted = true;
											allPositions.put(n.position, mappedEntry);
										}
										n.nodetype = DataMappingWrapper.getDeviceType(n.node_id.intValue(), n.generation_time, deployment, getVirtualSensorConfiguration().getName(), inputStreamName);
										n.coordinate = DataMappingWrapper.getCoordinate(n.position.intValue(), deployment, getVirtualSensorConfiguration().getName(), inputStreamName);
										n.iscorestation = false;
										for (int j=0; j<SensorNode.corestationTypes.length; j++)
											if (n.nodetype.shortValue() == SensorNode.corestationTypes[j])
												n.iscorestation = true;
										
										Integer vsys = null;
										if (n.getVsysDbl() != null)
											vsys = n.getVsysDbl().intValue();
										Integer vsdi = null;
										if (n.getVsdiDbl() != null)
											vsdi = n.getVsdiDbl().intValue();
										Integer current = null;
										if (n.getCurrent() != null && !n.getCurrent().trim().isEmpty())
											current = ((Double)Double.parseDouble(n.getCurrent())).intValue();
										Integer temp = null;
										if (n.getTemperature() != null && !n.getTemperature().trim().isEmpty())
											temp = ((Double)Double.parseDouble(n.getTemperature())).intValue();
										StreamElement se = new StreamElement(
												new String[]{"position", "generation_time", "payload_sysvoltage", "payload_sdivoltage", "payload_current", "payload_temperature"}, 
												new Byte[]{DataTypes.INTEGER, DataTypes.BIGINT, DataTypes.INTEGER, DataTypes.INTEGER, DataTypes.INTEGER, DataTypes.INTEGER},
												new Serializable[] {n.position, n.generation_time, vsys, vsdi, current, temp});
										se = DataMappingWrapper.getConvertedValues(se, deployment, getVirtualSensorConfiguration().getName(), inputStreamName);
										
										n.temperature = n.current = n.vsys = n.vsdi = null;
										
										String str = (String)se.getData("sdivoltage");
										if (str != null)
											n.setVsdi(Double.parseDouble(str));
										
										str = (String)se.getData("sysvoltage");
										if (str != null) {
											if (n.iscorestation || n.isWGPSNode() || n.isAENode() || n.isAccessNode() || n.isBBControl())
												n.setVsys(n.getVsdi());
											else
												n.setVsys(Double.parseDouble(str));
										}
	
										str = (String)se.getData("current");
										if (str != null)
											n.current = Double.parseDouble(str);
										else
											n.current = null;
										
	
										str = (String)se.getData("temperature");
										if (str != null)
											n.temperature = Double.parseDouble(str);
										else
											n.temperature = null;
										
										if (n.temperature != null && n.humidity != null) {
											if (n.isWGPSv2() != null && n.isWGPSv2()) { // WGPSv2
												n.humidity = n.humidity / 100.0;
											}
											else if (n.hasSHT15()) { // SHT15
												Double hum_rel = new Double(-4 + (0.0405d * n.humidity) - 0.0000028d * Math.pow(n.humidity, 2));			
												n.humidity = new Double((n.temperature - 25) * (0.01d + (0.00008d * n.humidity)) + hum_rel);
											}
											else if (n.hasSHT21()) { // SHT21
												n.humidity = -6d + 125d * n.humidity / Math.pow(2, 12);
											}
										}
										else
											n.humidity = null;
									}
									else {
										n.humidity = n.temperature = n.current = n.vsys = n.vsdi = null;
										
										if (now - n.timestamp > TopologyVirtualSensor.STANDBY_TIMEOUT_MS && now - n.timestamp < TopologyVirtualSensor.NODE_TIMEOUT)
											nodesToBeRemoved.add(n.node_id);
									}
									
									if (nodes.containsKey(n.node_id)) {
										SensorNode node = nodes.get(n.node_id);
										node.updateNode(n);
										topology.sensornodes.set(i, node);
									}
									nodes.put(n.node_id, topology.sensornodes.get(i));
								} catch (Exception e) {
									logger.error("device_id=" + n.node_id + ": " + e.getMessage(), e);
								}
									
							}
						}
						if (!nodesToBeRemoved.isEmpty()) {
							Iterator<SensorNode> iter = topology.sensornodes.iterator();
							while (iter.hasNext()) {
								SensorNode n = iter.next();
								if (nodesToBeRemoved.contains(n.node_id)) {
									logger.debug("remove node with id " + n.node_id);
									nodes.remove(n.node_id);
									iter.remove();
								}
									
							}
						}
						
						Iterator<Entry<Integer, MappedEntry>> iter = allPositions.entrySet().iterator();
						while(iter.hasNext()) {
							Entry<Integer, MappedEntry> entry = iter.next();
							if (!entry.getValue().spotted) {
								SensorNode node = new SensorNode(entry.getValue().deviceId);
								node.setNodeType(entry.getValue().deviceType);
								node.position = entry.getKey();
								if (node.isDozerNode())
									topology.sensornodes.add(node);
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
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e);
		}
	}

	@Override
	public boolean initialize() {
		VSensorConfig vsensor = getVirtualSensorConfiguration();
		deployment = vsensor.getName().split("_")[0].toLowerCase();

		nodes = new Hashtable<Integer, SensorNode>();
		
		return true;
	}

	@Override
	public void dispose() {}

}
