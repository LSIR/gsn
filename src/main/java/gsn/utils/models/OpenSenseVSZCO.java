package gsn.utils.models;

import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.Serializable;
import java.util.Properties;

import weka.classifiers.Classifier;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;



public class OpenSenseVSZCO extends AbstractModel {
	
	private String MODEL_FOLDER;

    private static final transient Logger logger = LoggerFactory.getLogger( OpenSenseVSZCO.class );
	
	private static final String[] OUTPUT_FIELDS = new String [] {"SENSOR_PPM","CO_REL","CO_ABS","temperature"};
	private static final double[] CO_THRESHOLDS = new double [] {0, 5000, 7500, 10000, 20000, Integer.MAX_VALUE};
	private static final int[] CO_MAP = new int [] {1,3,2,5,4};
	
	private SimpleKMeans kmeans_temp;
	private SimpleKMeans kmeans_loc;
	private Classifier cls_co;
	
	
	
	public boolean initialize() {
		
		Properties modelconf = new Properties( );
		try {
			modelconf.load( new FileInputStream( "conf/model.properties" ) );
		} catch (Exception e) {
			e.printStackTrace();
		}
		MODEL_FOLDER=modelconf.getProperty("folder");
		
		try {
		kmeans_temp = (SimpleKMeans) weka.core.SerializationHelper.read(MODEL_FOLDER + "ZURICH_CO_TEMP.model");
		kmeans_loc = (SimpleKMeans) weka.core.SerializationHelper.read(MODEL_FOLDER + "ZURICH_CO_LOC.model");
		
		cls_co = (Classifier) weka.core.SerializationHelper.read(MODEL_FOLDER + "ZURICH_CO_J48.model");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
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
			int result = -1;
			
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
	                lat = ((Double) data.getData(fieldName))/100;
	            if (fieldName.equalsIgnoreCase("longitude"))
	                lon = ((Double) data.getData(fieldName))/100;
	            if (fieldName.equalsIgnoreCase("timed"))
	                time = (Long) data.getData(fieldName);
	        }	


			Instances data_co;
			
			int classify = 0;
		
			FastVector classVal = new FastVector();
	        classVal.addElement("(-inf-1.116]");
	        classVal.addElement("(1.116-2.232]");
	        classVal.addElement("(3.348-4.464]");
	        classVal.addElement("(2.232-3.348]");
	        classVal.addElement("(4.464-inf)");
			
			// Create attributes to be used with classifiers
			// Create instances for each pollutant with attribute values pollutant
			// Set instance's values for "pollutant concentration"
	        //Check for null values by checking if pollutant values are >= 0.0, they are -1.0 if null returned from database
	        
			if(co >= 0.0)
			{
				classify = 1;
				
		        FastVector attributes_co = new FastVector();
				
		        FastVector temp_values = new FastVector(4);
		        temp_values.addElement("cluster2");
		        temp_values.addElement("cluster1");
		        temp_values.addElement("cluster0");
		        temp_values.addElement("cluster3");
		        Attribute temperature = new Attribute("temperature", temp_values);
		        attributes_co.addElement(temperature);
		        
		        FastVector loc_values = new FastVector(3);
		        loc_values.addElement("cluster1");
		        loc_values.addElement("cluster0");
		        loc_values.addElement("cluster2");
		        Attribute location = new Attribute("location", loc_values);
		        attributes_co.addElement(location);
				
				Attribute timed = new Attribute("timed");
				attributes_co.addElement(timed);
				attributes_co.addElement(new Attribute("CO",classVal));
				data_co = new Instances("TestInstances",attributes_co,0);
		        Instance inst_co = new Instance(data_co.numAttributes());
				
				inst_co.setValue(temperature, (String) temp_values.elementAt(clusterTemp(t,co)));
				inst_co.setValue(location, (String) loc_values.elementAt(clusterLoc(lat,lon,co)));
				inst_co.setValue(timed, (double) time%31556926);
		        data_co.add(inst_co);
		        data_co.setClassIndex(data_co.numAttributes() - 1);
			

				// Test the model, returns 0 if null value of pollutant received
				try {
					if(classify == 1) {
						result = CO_MAP[(int) cls_co.classifyInstance(data_co.instance(0))];
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
				
			StreamElement[] se = new StreamElement[1];
			// create output streamelement to be sent to application, neglect pollutant if index = -1
			se[0] = new StreamElement(OUTPUT_FIELDS , 
					new Byte[] {DataTypes.DOUBLE,DataTypes.INTEGER,  DataTypes.INTEGER,  DataTypes.DOUBLE} , 
					new Serializable [] {co,result, abs_ind, t} , time);
			
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
		
		private int clusterTemp(double temperature, double carbonmonoxide)
		{//clusters numeric temperature value to be used in the classifier
			
	        FastVector attributes = new FastVector();
			
	        Attribute temp_attr = new Attribute("temperature");
			attributes.addElement((Attribute)temp_attr);
			
	        Attribute co_attr = new Attribute("co");
			attributes.addElement((Attribute)co_attr);
		
			Instances cluster_data = new Instances("TestInstances",attributes,0);
	        Instance inst_co = new Instance(cluster_data.numAttributes());
			
			inst_co.setValue(temp_attr, temperature);
			inst_co.setValue(co_attr, carbonmonoxide);

	        cluster_data.add(inst_co);
			
			int result = 0; 
			
			try {
				result = kmeans_temp.clusterInstance(cluster_data.instance(0));
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return result;
		}
		
		private int clusterLoc(double latitude, double longitude, double carbonmonoxide)
		{//clusters numeric temperature value to be used in the classifier
			
	        FastVector attributes = new FastVector();
			
	        Attribute lat_attr = new Attribute("latitude");
			attributes.addElement((Attribute)lat_attr);
			Attribute lon_attr = new Attribute("longitude");
			attributes.addElement((Attribute)lon_attr);
			
	        Attribute co_attr = new Attribute("co");
			attributes.addElement((Attribute)co_attr);
		
			Instances cluster_data = new Instances("TestInstances",attributes,0);
	        Instance inst_co = new Instance(cluster_data.numAttributes());
			
			inst_co.setValue(lat_attr, (double) latitude);
			inst_co.setValue(lon_attr, (double) longitude);
			inst_co.setValue(co_attr, (double) carbonmonoxide);

	        cluster_data.add(inst_co);
			
			int result = 0; 
			
			try {
				result = kmeans_loc.clusterInstance(cluster_data.instance(0));
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return result;
		}

}
