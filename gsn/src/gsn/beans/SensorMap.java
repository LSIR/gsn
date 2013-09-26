package gsn.beans;

public class SensorMap {
	public Long begin;
	public Long end;
	public String sensortype;
	public Long sensortype_args;
	public String comment;
	
	public SensorMap() {}
	
	public SensorMap(Long begin, Long end, String sensortype, Long sensortype_args, String comment) {
		this.begin = begin;
		this.end = end;
		this.sensortype = sensortype;
		this.sensortype_args = sensortype_args;
		this.comment = comment;
	}
}
