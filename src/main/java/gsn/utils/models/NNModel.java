package gsn.utils.models;

import java.io.Serializable;

import org.apache.log4j.Logger;

import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.utils.geo.GeoTools;

public class NNModel extends AbstractModel {
	
    private static final transient Logger logger = Logger.getLogger( NNModel.class );
	
	private int BUFFER_SIZE = 200;
	
	private double DIST_THRESHOLD = 200;
	
	private StreamElement[] buffer;
	private int ptr = 0;
	
	
	@Override
	public boolean initialize(){
		buffer = new StreamElement[BUFFER_SIZE];
		return true;
	}

	@Override
	public StreamElement[] query(StreamElement params) {
		StreamElement[] ret = new StreamElement[1];
		StreamElement[] ret1 = new StreamElement[1];
		ret [0] = null;
		 double dist = Double.MAX_VALUE;
		 for (StreamElement se : buffer){
			if (se == null) continue;
			double d = getDistance(se,params);
			if (d < dist){
				dist = d;
				ret[0] = se;
			}
		  }
		  if(dist > DIST_THRESHOLD && getVirtualSensor().getModel(1) != null){//query the second model by passing NN for unknown parameters
			  if (ret[0] != null){
				   Serializable[] se = ret[0].getData().clone();
				   int i = 0;
				  for(String s :ret[0].getFieldNames())
				  {
					  if (params.getData(s) != null)
					      se[i] = params.getData(s);
					  i ++;
				  }
				  StreamElement pa = new StreamElement(ret[0].getFieldNames(),ret[0].getFieldTypes(),se,ret[0].getTimeStamp());
				  ret1 = getVirtualSensor().getModel(1).query(pa);
			  }else{
				  ret1 = getVirtualSensor().getModel(1).query(params);
			  }
		  }else{
			return ret;	
		  }
		  if (ret1[0] == null){
			  return ret; 
		  }
		return ret1;
	}
	
	/**
	 * returns the L2 distance between two StreamElements, only considering the double and int fields.
	 * If location is defined with latitude and longitude field it is used instead.
	 * @param s1: one streamElement
	 * @param s2: the other streamElement
	 * @return the distance between the StreamElements
	 */
	private double getDistance(StreamElement s1,StreamElement s2){
           Double lat1 = (Double) s1.getData("latitude");
           Double lon1 = (Double) s1.getData("longitude");
           Double lat2 = (Double) s2.getData("latitude");
           Double lon2 = (Double) s2.getData("longitude");
           if (lat1 != null && lat2 != null && lon1 != null && lon2 != null){ // get real distance in meter
        	   // !!!! _HARDCODED conversion of gps coordinates
        	   //return GeoTools.getDistance((lat1-1880)/60.0,(lon1-320)/60.0,(lat2-1880)/60.0,(lon2-320)/60.0);
        	   return GeoTools.getDistance(lat1, lon1, lat2, lon2);
           }
           else{ // get L2 distance using all parameters (considering only double and int)
        	   double diff = 0;
        	   double sum = 0;
        	   boolean compared = false;
        	  for (String s : s2.getFieldNames()) {
        		  if (s1.getData(s) != null && s1.getType(s)==s2.getType(s)){
        			  switch(s1.getType(s)){
	        			  case DataTypes.INTEGER:
	        			  case DataTypes.BIGINT:
	        			  case DataTypes.SMALLINT:
	        			  case DataTypes.TINYINT:
	        				  diff = (Integer) s2.getData(s) - (Integer) s1.getData(s);
	            			  compared = true;
	            			  break;
	        			  case DataTypes.DOUBLE:
	        			  case DataTypes.FLOAT:
	        				  diff = (Double) s2.getData(s) - (Double) s1.getData(s);
	            			  compared = true;
	            			  break;
        			  }  			  
        		  }
        		  sum += diff *diff;
        	  }
        	  if (!compared){
        		  return Double.MAX_VALUE;
        	  }
        	  return Math.sqrt(sum);
           }
	}

	
	@Override
	public StreamElement[] pushData(StreamElement se,String origin) {
		buffer[ptr] = se;
		ptr = (ptr + 1) % BUFFER_SIZE;
		return new StreamElement[]{se};
	}

	@Override
	public void setParam(String k, String string) {
		if (k.equals("distance")){
			try{
				DIST_THRESHOLD = Double.parseDouble(string);
			}catch(Exception e){}
		}
		if (k.equals("buffer")){
			try{
				BUFFER_SIZE = Integer.parseInt(string);
			}catch(Exception e){}
		}
		
	}


}
