package gsn.beans;

import java.util.ArrayList;

public class DeviceMappings {

	public ArrayList<PositionMappings> positionMappings;
	public ArrayList<SensorMappings> sensorMappings;
	public ArrayList<GeoMapping> geoMappings;

	public Long positionTimeMin = null;
	public Long positionTimeMax = null;
	public ArrayList<TimeTick> positionTimeTicks;

	public Long sensorTimeMin = null;
	public Long sensorTimeMax = null;
	public ArrayList<TimeTick> sensorTimeTicks;

	public DeviceMappings() {}

	public DeviceMappings(ArrayList<PositionMappings> positionMappings, ArrayList<SensorMappings> sensorMappings, ArrayList<GeoMapping> geoMappings) {
		this.positionMappings = positionMappings;
		this.sensorMappings = sensorMappings;
		this.geoMappings = geoMappings;
		
		positionTimeTicks = new ArrayList<TimeTick>();
		sensorTimeTicks = new ArrayList<TimeTick>();
		
		calcPositionTimeAxis();
		calcSensorTimeAxis();
	}
	
	private void calcPositionTimeAxis() {
		for (PositionMappings mappings: positionMappings) {
			for (PositionMap map: mappings.mappings) {
				if (map.begin != null && (positionTimeMin == null || positionTimeMin.compareTo(map.begin) > 0))
					positionTimeMin = map.begin;
				if (positionTimeMax == null || map.end == null || positionTimeMax.compareTo(map.end) < 0)
					positionTimeMax = map.end;
				
				if (map.end != null) {
					boolean newTick = true;
					for (TimeTick t: positionTimeTicks) {
						if (map.end.compareTo(t.tick+172800000L) < 0 && map.end.compareTo(t.tick-172800000L) > 0) {
							newTick = false;
							break;
						}
					}
					if (newTick)
						positionTimeTicks.add(new TimeTick(map.end));
					
					newTick = true;
					for (TimeTick t: positionTimeTicks) {
						if (map.begin.compareTo(t.tick+172800000L) < 0 && map.begin.compareTo(t.tick-172800000L) > 0) {
							newTick = false;
							break;
						}
					}
					if (newTick)
						positionTimeTicks.add(new TimeTick(map.begin));
				}
			}
		}
	}
	
	private void calcSensorTimeAxis() {
		for (SensorMappings mappings: sensorMappings) {
			for (SensorMap map: mappings.mappings) {
				if (map.begin != null && (sensorTimeMin == null || sensorTimeMin.compareTo(map.begin) > 0))
					sensorTimeMin = map.begin;
				if (sensorTimeMax == null || map.end == null || sensorTimeMax.compareTo(map.end) > 0)
					sensorTimeMax = map.end;
				
				if (map.end != null) {
					boolean newTick = true;
					for (TimeTick t: sensorTimeTicks) {
						if (map.end.compareTo(t.tick+172800000L) < 0 && map.end.compareTo(t.tick-172800000L) > 0) {
							newTick = false;
							break;
						}
					}
					if (newTick)
						sensorTimeTicks.add(new TimeTick(map.end));

					newTick = true;
					for (TimeTick t: sensorTimeTicks) {
						if (map.begin.compareTo(t.tick+172800000L) < 0 && map.begin.compareTo(t.tick-172800000L) > 0) {
							newTick = false;
							break;
						}
					}
					if (newTick)
						sensorTimeTicks.add(new TimeTick(map.begin));
				}
			}
		}
	}
}
