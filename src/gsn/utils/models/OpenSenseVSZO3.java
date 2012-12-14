package gsn.utils.models;

import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;

import org.apache.log4j.Logger;
import java.io.Serializable;


public class OpenSenseVSZO3 extends AbstractModel {

        private static final transient Logger logger = Logger.getLogger( OpenSenseVSZO3.class );
	
	private static final String[] OUTPUT_FIELDS = new String [] {"TEMPERATURE","HUMIDITY","O3_REL","O3_ABS"};
	private static final double[] O3_THRESHOLDS = new double [] {0, 60, 120, 180, 240, Integer.MAX_VALUE};

	    public boolean initialize() {
	        return true;
	    }

		@Override
		public StreamElement[] query(StreamElement data) {
			// TODO Auto-generated method stub
			
			double t = 0;
			int h = 0;
			double lat = 0;
			double lon = 0;
			double o3 = 0;
			double o3_ug = 0;
			long time = 0;
			int abs_ind = -1;
			//logger.warn(data);
			
			// read essential data from streamelement
			for (String fieldName : data.getFieldNames()) {

	            if (fieldName.equalsIgnoreCase("ozone_ppb")){
	                try{
	                	o3 = (Double) data.getData(fieldName);
	                	o3_ug = 0.5*o3; //convert to ug/m3 to calculate absolute index
		    			for (int i=0; i<5; i++)
		    			{
		    				if(o3_ug >= O3_THRESHOLDS[i] && o3_ug < O3_THRESHOLDS[i+1])
		    					abs_ind = i+1; 
		    			}
	                }
	            catch (NullPointerException e){o3 = -1.0;}}
	            if (fieldName.equalsIgnoreCase("temperature"))
	                t = (Double) data.getData(fieldName);
	            if (fieldName.equalsIgnoreCase("humidity"))
	                h = (Integer) data.getData(fieldName);
	            if (fieldName.equalsIgnoreCase("latitude"))
	                lat = (Integer) data.getData(fieldName);
	            if (fieldName.equalsIgnoreCase("longitude"))
	                lon = (Integer) data.getData(fieldName);
	            if (fieldName.equalsIgnoreCase("timed"))
	                time = (Long) data.getData(fieldName);
	        }	
				
			OpenSense_Zurich_O3 o3_model = new OpenSense_Zurich_O3(o3, t, h, lat, lon, time);
			int result = o3_model.applyModel();
					
			// create output streamelement to be sent to application, neglect pollutant if index = -1
			
			StreamElement[] se = new StreamElement[1];
			
			se[0] = new StreamElement( OUTPUT_FIELDS , 
					new Byte[] {DataTypes.DOUBLE,  DataTypes.INTEGER, DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.INTEGER,  DataTypes.INTEGER} , 
					new Serializable [] {t, h, lat, lon, result, abs_ind} , time);
			
			return se;
			
		}

		@Override
		public DataField[] getOutputFields() {
			// TODO Auto-generated method stub
			return null;
		}


		@Override
		public StreamElement pushData(StreamElement streamElement) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setOutputFields(DataField[] outputStructure) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setParam(String k, String string) {
			// TODO Auto-generated method stub
			
		}
}
