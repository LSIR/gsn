package gsn.vsensor;

import gsn.beans.DataTypes;
import gsn.beans.StreamElement;

import java.io.Serializable;


public class OpenSense_Zurich_CO_Indexer extends AbstractVirtualSensor {

        private static final String[] OUTPUT_FIELDS = new String [] {"SENSOR_PPM","TEMPERATURE","LATITUDE","LONGITUDE","CO_REL","CO_ABS"};
	private static final double[] CO_ABS_THRESHOLDS = new double [] {0, 5000, 7500, 10000, 20000, Integer.MAX_VALUE};
	private static final double[] CO_REL_THRESHOLDS = new double [] {0, 1.116, 2.232, 3.348, 4.464, Integer.MAX_VALUE};	

	    public boolean initialize() {
	        return true;
	    }

		@Override
		public void dispose() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void dataAvailable(String inputStreamName, StreamElement data) {
			// TODO Auto-generated method stub
			
			double t = 0;
			double lat = 0;
			double lon = 0;
			double co = 0;
			double co_ug = 0;
			long time = 0;
			int abs_ind = -1;
			int rel_ind = -1;
			//logger.warn(data);
			
			// read essential data from streamelement
			for (String fieldName : data.getFieldNames()) {

	            if (fieldName.equalsIgnoreCase("sensor_ppm")){
	                try{
	                	co = (Double) data.getData(fieldName);
	                	co_ug = 1145*co;  //convert to ug/m3 to calculate absolute index
		    			for (int i=0; i<5; i++)
		    			{
		    				if(co_ug >= CO_ABS_THRESHOLDS[i] && co_ug < CO_ABS_THRESHOLDS[i+1])
		    					abs_ind = i+1; 
		    				if(co >= CO_REL_THRESHOLDS[i] && co < CO_REL_THRESHOLDS[i+1])
		    					rel_ind = i+1; 
		    			}
	                }
	            catch (NullPointerException e){co = -1.0;}}
	            if (fieldName.equalsIgnoreCase("ambient_temp")){
	            	try{
	                t = (Double) data.getData(fieldName);}
	            catch (NullPointerException e){t = 0;}}
	            if (fieldName.equalsIgnoreCase("latitude")){
	            	try{
		                lat = (Double) data.getData(fieldName);}
		            catch (NullPointerException e){lat = 0;}}
	            if (fieldName.equalsIgnoreCase("longitude")){
	            	try{
		                lon = (Double) data.getData(fieldName);}
		            catch (NullPointerException e){lon = 0;}}
	            if (fieldName.equalsIgnoreCase("timed")){
	            	try{
		                time = (Long) data.getData(fieldName);}
		            catch (NullPointerException e){time = 0;}}
	        }	
				
					
			// create output streamelement to be sent to application, neglect pollutant if index = -1
			StreamElement out = new StreamElement( OUTPUT_FIELDS , 
					new Byte[] {DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.INTEGER,  DataTypes.INTEGER} , 
					new Serializable [] {co, t, lat, lon, rel_ind, abs_ind} , data.getTimeStamp());
			dataProduced(out);		
		}
}
