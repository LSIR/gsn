package gsn.beans;

public class GeoMapping {
	public Integer position;
	public Double longitude;
	public Double latitude;
	public Double altitude;
	public String comment;
	
	public GeoMapping() {}
	
	public GeoMapping(Integer position, Double longitude, Double latitude, Double altitude, String comment) {
		this.position = position;
		this.longitude = longitude;
		this.latitude = latitude;
		this.altitude = altitude;
		this.comment = comment;
	}
}
