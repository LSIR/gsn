package gsn.vsensor;

import gsn.beans.DataTypes;
import gsn.beans.StreamElement;

import java.io.Serializable;


public class OpenSense_Zurich_PM_Indexer extends AbstractVirtualSensor {

    private static final String[] OUTPUT_FIELDS = new String [] {"NUMBER","TEMPERATURE","LATITUDE","LONGITUDE","PM_REL","PM_ABS"};
	private static final double[] PM_ABS_THRESHOLDS = new double [] {0, 15, 30, 50, 100, Integer.MAX_VALUE};
	private static final double[] PM_REL_THRESHOLDS = new double [] {0, 5439.5, 7628.5, 10681.5, 17887.5, Integer.MAX_VALUE};	

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
			double pm = 0;
			double pm_ug = 0;
			long time = 0;
			int abs_ind = -1;
			int rel_ind = -1;
			//logger.warn(data);
			
			// read essential data from streamelement
			for (String fieldName : data.getFieldNames()) {

	            if (fieldName.equalsIgnoreCase("number")){
	                try{
	                	pm = (Double) data.getData(fieldName);
	                	pm_ug = 0.0005*pm;  //convert to ug/m3 to calculate absolute index
		    			for (int i=0; i<5; i++)
		    			{
		    				if(pm_ug >= PM_ABS_THRESHOLDS[i] && pm_ug < PM_ABS_THRESHOLDS[i+1])
		    					abs_ind = i+1; 
		    				if(pm >= PM_REL_THRESHOLDS[i] && pm < PM_REL_THRESHOLDS[i+1])
		    					rel_ind = i+1; 
		    			}
	                }
	            catch (NullPointerException e){pm = -1.0;}}
	            if (fieldName.equalsIgnoreCase("temp")){
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
					new Serializable [] {pm, t, lat, lon, rel_ind, abs_ind} , data.getTimeStamp());
			dataProduced(out);	
			
		}
}
