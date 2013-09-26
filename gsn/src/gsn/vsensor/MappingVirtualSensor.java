package gsn.vsensor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.JiBXException;

import gsn.beans.DeviceMappings;
import gsn.beans.SensorMappings;
import gsn.beans.GeoMapping;
import gsn.beans.PositionMap;
import gsn.beans.PositionMappings;
import gsn.beans.SensorMap;
import gsn.beans.StreamElement;

public class MappingVirtualSensor extends BridgeVirtualSensorPermasense {

	private static final transient Logger logger = Logger.getLogger(MappingVirtualSensor.class);
	
	private Map<Integer, PositionMappings> positionMappings;
	private Map<Integer, SensorMappings> sensorMappings;
	private ArrayList<GeoMapping> geoMappings;

	@Override
	public boolean initialize() {
		positionMappings = new Hashtable<Integer, PositionMappings>();
		sensorMappings = new Hashtable<Integer, SensorMappings>();
		geoMappings = new ArrayList<GeoMapping>();
		
		return true;
	}
	
	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
		int position = (Integer)data.getData("position");
		
		if (inputStreamName.equalsIgnoreCase("position_mapping")) {
			if (!positionMappings.containsKey(position)) {
				positionMappings.put(position, new PositionMappings(position, new ArrayList<PositionMap>()));
			}
			positionMappings.get(position).add(new PositionMap((Integer)data.getData("device_id"), (Long)data.getData("begin"), (Long)data.getData("end"), (String)data.getData("comment")));
		}

		if (inputStreamName.equalsIgnoreCase("sensor_mapping")) {
			if (!sensorMappings.containsKey(position)) {
				sensorMappings.put(position, new SensorMappings(position, new ArrayList<SensorMap>()));
			}
			sensorMappings.get(position).add(new SensorMap((Long)data.getData("begin"), (Long)data.getData("end"), (String)data.getData("sensortype"), (Long)data.getData("sensortype_args"), (String)data.getData("comment")));
		}
		
		if (inputStreamName.equalsIgnoreCase("geo_mapping")) {
			GeoMapping geoMap = new GeoMapping((Integer)data.getData("position"), (Double)data.getData("longitude"), (Double)data.getData("latitude"), (Double)data.getData("altitude"), (String)data.getData("comment"));
			Iterator<GeoMapping> iter = geoMappings.iterator();
			while (iter.hasNext()) {
				GeoMapping map = iter.next();
				if (map.position == geoMap.position)
					iter.remove();
			}
			
			geoMappings.add(geoMap);
		}
		
		try {
			generateData();
		} catch (Exception e) {
			logger.error(data.toString(), e);
		}
	}

	synchronized void generateData() {
		DeviceMappings map = new DeviceMappings(new ArrayList<PositionMappings>(positionMappings.values()), new ArrayList<SensorMappings>(sensorMappings.values()), geoMappings);
		
		try {
			IBindingFactory bfact = BindingDirectory.getFactory(DeviceMappings.class);
			IMarshallingContext mctx = bfact.createMarshallingContext();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			mctx.marshalDocument(map, "UTF-8", null, baos);
			StreamElement se = new StreamElement(getVirtualSensorConfiguration().getOutputStructure(),  new Serializable[]{baos.toString().getBytes()});
			dataProduced( se );
		} catch (JiBXException e) {
			e.printStackTrace();
			logger.error(e);
		}
	}
}
