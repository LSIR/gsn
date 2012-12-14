package gsn.utils.models;

import gsn.beans.DataField;
import gsn.beans.StreamElement;

public class NNModel extends AbstractModel {
	
	private int BUFFER_SIZE = 200;
	
	private double DIST_THRESHOLD = 200;
	
	private StreamElement[] buffer = new StreamElement[BUFFER_SIZE];
	private int ptr = 0;
	
	private DataField[] outputfield = null;

	@Override
	public StreamElement[] query(StreamElement params) {
		StreamElement[] ret = new StreamElement[1];
		ret [0] = null;
		if (outputfield == null) return null;
		   double dist = Double.MAX_VALUE;
		   for (StreamElement se : buffer){
			if (se == null) continue;
			double d = getDistance(se,params);
			if (d < dist){
				dist = d;
				ret[0] = se;
			}
		  }
		  if(dist > DIST_THRESHOLD && nextModel != null){
			  ret = nextModel.query(params);
		  }
		return ret;
	}
	
	private double getDistance(StreamElement s1,StreamElement s2){
           Double lat1 = (Double) s1.getData("latitude");
           Double lon1 = (Double) s1.getData("longitude");
           Double lat2 = (Double) s2.getData("latitude");
           Double lon2 = (Double) s2.getData("longitude");
           if (lat1 != null && lat2 != null && lon1 != null && lon2 != null){ // get real distance in meter
        	   return getDistance(lat1,lon1,lat2,lon2);
           }
           else{ // get L2 distance using all parameters
        	   double diff = 0;
        	   double sum = 0;
        	   boolean compared = false;
        	  for (String s : s2.getFieldNames()) {
        		  if (s1.getData(s) != null){
        			  diff = (Double) s2.getData(s) - (Double) s1.getData(s); // !!!! _HARDCODED
        			  compared = true;
        		  }
        		  sum += diff *diff;
        	  }
        	  if (!compared){
        		  return Double.MAX_VALUE;
        	  }
        	  return Math.sqrt(sum);
           }
	}
	
	
	private double getDistance(double lat1,double lon1,double lat2,double lon2){
		double R = 6371; // km
		double dLat = (lat2-lat1) * Math.PI / 180.0;
		double dLon = (lon2-lon1)* Math.PI / 180.0;
		double a = Math.sin(dLat/2) * Math.sin(dLat/2) + 
		        Math.cos(lat1* Math.PI / 180.0) * Math.cos(lat2* Math.PI / 180.0) *
		        Math.sin(dLon/2) * Math.sin(dLon/2); 
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
		return R * c;
	}

	@Override
	public StreamElement pushData(StreamElement se) {
		buffer[ptr] = se;
		ptr = (ptr + 1) % BUFFER_SIZE;
		return se;
	}

	@Override
	public void setParam(String k, String string) {
		if (k.equals("distance")){
			try{
			DIST_THRESHOLD = Double.parseDouble(string);
			}catch(Exception e){
				
			}
		}
		if (k.equals("buffer")){
			try{
				BUFFER_SIZE = Integer.parseInt(string);
			}catch(Exception e){
				
			}
		}
		
	}


}
