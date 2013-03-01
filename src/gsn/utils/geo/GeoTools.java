package gsn.utils.geo;

public class GeoTools {
	
	/**
	 * Get the shortest distance in meter on the surface of the earth between two WGS84 coordinates, assuming the earth is a perfect sphere
	 * @param lat1: latitude of the first point
	 * @param lon1: longitude of the first point
	 * @param lat2: latitude of the second point
	 * @param lon2: longitude of the second point
	 * @return the distance in meter
	 */
	public static double getDistance(double lat1,double lon1,double lat2,double lon2){
		double R = 6371000; // earth radius in meter
		double dLat = Math.toRadians(lat2-lat1);
		double dLon = Math.toRadians(lon2-lon1);
		double a = Math.sin(dLat/2) * Math.sin(dLat/2) + 
		        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
		        Math.sin(dLon/2) * Math.sin(dLon/2); 
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
		return R * c;
	}


}
