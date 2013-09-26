package gsn.beans;

import java.util.ArrayList;
import java.util.Iterator;

public class SensorMappings {
	public Integer position;
	public ArrayList<SensorMap> mappings;
	
	public SensorMappings() {}
	
	public SensorMappings(Integer position, ArrayList<SensorMap> mappings) {
		this.position = position;
		this.mappings = mappings;
	}

	public void add(SensorMap sensorMap) {
		Iterator<SensorMap> iter = mappings.iterator();
		while (iter.hasNext()) {
			SensorMap map = iter.next();
			if (map.end == null && map.begin.compareTo(sensorMap.begin) == 0)
				iter.remove();
		}
		mappings.add(sensorMap);
	}
}
