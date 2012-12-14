package gsn.utils.models;

import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import org.apache.log4j.Logger;
import java.io.Serializable;


public class OpenSenseVSZCO extends AbstractModel {

        private static final transient Logger logger = Logger.getLogger( OpenSenseVSZCO.class );
	
	private static final String[] OUTPUT_FIELDS = new String [] {"CO_REL","CO_ABS"};
	private static final double[] CO_THRESHOLDS = new double [] {0, 5000, 7500, 10000, 20000, Integer.MAX_VALUE};
	public boolean initialize() {
	        return true;
	    }

		@Override
		public StreamElement[] query(StreamElement data) {
			// TODO Auto-generated method stub
			
			double co = 0;
			double co_ug = 0;
			double lat = 0;
			double lon = 0;
			double t = 0;
			long time = 0;
			int abs_ind = -1;
			//logger.warn(data);
			
			// read essential data from streamelement
			for (String fieldName : data.getFieldNames()) {
	            if (fieldName.equalsIgnoreCase("sensor_ppm")){
	                try{
	                	co = (Double) data.getData(fieldName);
		                co_ug = 1145*co;  //convert to ug/m3 to calculate absolute index
		    			for (int i=0; i<5; i++)
		    			{
		    				if(co_ug >= CO_THRESHOLDS[i] && co_ug < CO_THRESHOLDS[i+1])
		    					abs_ind = i+1; 
		    			}}
	            catch (NullPointerException e){co = -1.0; abs_ind = -1;}}
	            
	            if (fieldName.equalsIgnoreCase("ambient_temp"))
	                t = (Double) data.getData(fieldName);
	            if (fieldName.equalsIgnoreCase("latitude"))
	                lat = (Integer) data.getData(fieldName);
	            if (fieldName.equalsIgnoreCase("longitude"))
	                lon = (Integer) data.getData(fieldName);
	            if (fieldName.equalsIgnoreCase("timed"))
	                time = (Long) data.getData(fieldName);
	        }	
			
			OpenSense_Zurich_CO co_model = new OpenSense_Zurich_CO(co, lat, lon, t, time);
			int result = co_model.applyModel();
			
			StreamElement[] se = new StreamElement[1];
			// create output streamelement to be sent to application, neglect pollutant if index = -1
			se[0] = new StreamElement(OUTPUT_FIELDS , 
					new Byte[] {DataTypes.INTEGER,  DataTypes.INTEGER,  DataTypes.DOUBLE} , 
					new Serializable [] {result, abs_ind, t} , time);
			
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
