package gsn.beans;

public class PositionMap {
	public Integer device_id;
	public Short device_type;
	public Long begin;
	public Long end;
	public String comment;
	
	public PositionMap() {}
			
	public PositionMap(Integer id, Short type, Long begin, Long end, String comment) {
		this.device_id = id;
		this.device_type = type;
		this.begin = begin;
		this.end = end;
		this.comment = comment;
	}
}
