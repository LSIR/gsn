package gsn.beans;

public class PositionMap {
	public Integer device_id;
	public Long begin;
	public Long end;
	public String comment;
	
	public PositionMap() {}
			
	public PositionMap(Integer id, Long begin, Long end, String comment) {
		this.device_id = id;
		this.begin = begin;
		this.end = end;
		this.comment = comment;
	}
}
