package gsn.vsensor;

import gsn.beans.DataTypes;
import gsn.beans.StreamElement;

import java.io.Serializable;


public class OpenSense_Zurich_O3_Indexer extends AbstractVirtualSensor {

    private static final String[] OUTPUT_FIELDS = new String [] {"OZONE_PPB","TEMPERATURE","HUMIDITY","LATITUDE","LONGITUDE","O3_REL","O3_ABS"};
	private static final double[] O3_ABS_THRESHOLDS = new double [] {0, 60, 120, 180, 240, Integer.MAX_VALUE};
	private static final double[] O3_REL_THRESHOLDS = new double [] {0, 54.26528, 108.52996, 162.79464, 217.05932, Integer.MAX_VALUE};	

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
			int h = 0;
			double lat = 0;
			double lon = 0;
			double o3 = 0;
			double o3_ug = 0;
			long time = 0;
			int abs_ind = -1;
			int rel_ind = -1;
			//logger.warn(data);
			
			// read essential data from streamelement
			for (String fieldName : data.getFieldNames()) {

	            if (fieldName.equalsIgnoreCase("ozone_ppb") || fieldName.equalsIgnoreCase("ozoneCalibrated")){
	                try{
	                	o3 = (Double) data.getData(fieldName);
	                	o3_ug = 0.5*o3; //convert to ug/m3 to calculate absolute index
		    			for (int i=0; i<5; i++)
		    			{
		    				if(o3_ug >= O3_ABS_THRESHOLDS[i] && o3_ug < O3_ABS_THRESHOLDS[i+1])
		    					abs_ind = i+1; 
		    				if(o3 >= O3_REL_THRESHOLDS[i] && o3 < O3_REL_THRESHOLDS[i+1])
		    					rel_ind = i+1; 
		    			}
	                }
	            catch (NullPointerException e){o3 = -1.0;}}
	            if (fieldName.equalsIgnoreCase("ambient_temp") || fieldName.equalsIgnoreCase("temperature")){
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
	            if (fieldName.equalsIgnoreCase("humidity")){
	            	try{
		                h = (Integer) data.getData(fieldName);}
		            catch (NullPointerException e){h = 0;}}
	        }	
				
					
			// create output streamelement to be sent to application, neglect pollutant if index = -1
			StreamElement out = new StreamElement( OUTPUT_FIELDS , 
					new Byte[] {DataTypes.DOUBLE, DataTypes.DOUBLE,  DataTypes.INTEGER, DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.INTEGER,  DataTypes.INTEGER} , 
					new Serializable [] {o3, t, h, lat, lon, rel_ind, abs_ind} ,data.getTimeStamp());
			dataProduced(out);	
			
		}
}
