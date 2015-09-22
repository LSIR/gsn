package gsn.utils.models;

import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.Serializable;
import java.util.Properties;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;


public class OpenSenseVSZPM extends AbstractModel {
	
	private String MODEL_FOLDER;

        private static final transient Logger logger = LoggerFactory.getLogger( OpenSenseVSZPM.class );
	
	private static final String[] OUTPUT_FIELDS = new String [] {"TEMPERATURE","LATITUDE","LONGITUDE","PM_REL","PM_ABS","NUMBER"};
	private static final double[] PM_THRESHOLDS = new double [] {0, 15, 30, 50, 100, Integer.MAX_VALUE};
	private static final int[] PM_MAP = new int [] {4,3,5,2,1};
	
	private Classifier cls_pm;

	    public boolean initialize() {
	    	
			Properties modelconf = new Properties( );
			try {
				modelconf.load( new FileInputStream( "conf/model.properties" ) );
			} catch (Exception e) {
				e.printStackTrace();
			}
			MODEL_FOLDER=modelconf.getProperty("folder");
	    	try {
				cls_pm = (Classifier) weka.core.SerializationHelper.read(MODEL_FOLDER + "ZURICH_PM_IBK.model");
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
	        return true;
	    }

		@Override
		public StreamElement[] query(StreamElement data) {
			// TODO Auto-generated method stub
			
			double t = 0;
			int h = 0;
			double lat = 0;
			double lon = 0;
			double pm = 0;
			double pm_ug = 0;
			long time = 0;
			int abs_ind = -1;
			int result = -1;
			//logger.warn(data);
			
			// read essential data from streamelement
			for (String fieldName : data.getFieldNames()) {

	            if (fieldName.equalsIgnoreCase("number")){
	                try{
	                	pm = (Double) data.getData(fieldName);
	                	pm_ug = 0.0005*pm; //convert to ug/m3 to calculate absolute index
		    			for (int i=0; i<5; i++)
		    			{
		    				if(pm_ug >= PM_THRESHOLDS[i] && pm_ug < PM_THRESHOLDS[i+1])
		    					abs_ind = i+1; 
		    			}
	                }
	            catch (NullPointerException e){pm = -1.0;}}
	            if (fieldName.equalsIgnoreCase("temp"))
	                t = (Double) data.getData(fieldName);
	            if (fieldName.equalsIgnoreCase("humidity"))
	                h = (Integer) data.getData(fieldName);
	            if (fieldName.equalsIgnoreCase("latitude"))
	                lat = ((Double) data.getData(fieldName))/100;
	            if (fieldName.equalsIgnoreCase("longitude"))
	                lon = ((Double) data.getData(fieldName))/100;
	            if (fieldName.equalsIgnoreCase("timed"))
	                time = (Long) data.getData(fieldName);
	        }	

			Instances data_pm;
			int classify = 0;

			FastVector classVal = new FastVector();
	        classVal.addElement("(10681.5-17887.5]");
	        classVal.addElement("(7628.5-10681.5]");
	        classVal.addElement("(17887.5-inf)");
	        classVal.addElement("(5439.5-7628.5]");
	        classVal.addElement("(-inf-5439.5]");
			
			// Create attributes to be used with classifiers
			// Create instances for each pollutant with attribute values pollutant
			// Set instance's values for "pollutant concentration"
	        //Check for null values by checking if pollutant values are >= 0.0, they are -1.0 if null returned from database
	        
			if(pm >= 0.0)
			{
				classify = 1;
				
		        FastVector attributes_pm = new FastVector();
				
		        Attribute latitude = new Attribute("latitude");
				attributes_pm.addElement((Attribute)latitude);
				Attribute longitude = new Attribute("longitude");
				attributes_pm.addElement((Attribute)longitude);
				Attribute temperature = new Attribute("temperature");
				attributes_pm.addElement((Attribute)temperature);
				Attribute timed = new Attribute("timed");
				attributes_pm.addElement(timed);
				
				attributes_pm.addElement(new Attribute("pm",classVal));
				
				data_pm = new Instances("TestInstances",attributes_pm,0);
				Instance inst_pm = new Instance(data_pm.numAttributes());
				
				inst_pm.setValue(temperature, (double) t);
				inst_pm.setValue(latitude, (double) lat);
				inst_pm.setValue(longitude, (double) lon);
				inst_pm.setValue(timed, (double) time%31556926);
		        data_pm.add(inst_pm);
		        data_pm.setClassIndex(data_pm.numAttributes() - 1);

				// Test the model, returns 0 if null value of pollutant received
				try {
					if(classify == 1){
						result = PM_MAP[(int) cls_pm.classifyInstance(data_pm.instance(0))];
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			
			// create output streamelement to be sent to application, neglect pollutant if index = -1
			
			StreamElement[] se = new StreamElement[1];
			
			se[0] = new StreamElement( OUTPUT_FIELDS , 
					new Byte[] {DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.INTEGER,  DataTypes.INTEGER,DataTypes.DOUBLE} , 
					new Serializable [] {t, lat, lon, result, abs_ind,pm} , data.getTimeStamp());
			
			return se;
			
		}

		@Override
		public StreamElement[] pushData(StreamElement streamElement,String origin) {
			// TODO Auto-generated method stub
			return null;
		}


		@Override
		public void setParam(String k, String string) {
			// TODO Auto-generated method stub
			
		}
}
