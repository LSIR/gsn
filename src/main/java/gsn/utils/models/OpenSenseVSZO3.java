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


public class OpenSenseVSZO3 extends AbstractModel {
	
	private String MODEL_FOLDER;

	private static final transient Logger logger = LoggerFactory.getLogger( OpenSenseVSZO3.class );
	
	private static final String[] OUTPUT_FIELDS = new String [] {"TEMPERATURE","HUMIDITY","LATITUDE","LONGITUDE","O3_REL","O3_ABS","OZONE_PPB"};
	private static final double[] O3_THRESHOLDS = new double [] {0, 60, 120, 180, 240, Integer.MAX_VALUE};
	private static final int[] O3_MAP = new int [] {1,3,2,4,5};
	
	private Classifier cls_o3;
	private SimpleKMeans kmeans_temp;
	private SimpleKMeans kmeans_loc;
	private SimpleKMeans kmeans_h;

    @Override
	public boolean initialize() {
    	
		Properties modelconf = new Properties( );
		try {
			modelconf.load( new FileInputStream( "conf/model.properties" ) );
		} catch (Exception e) {
			e.printStackTrace();
		}
		MODEL_FOLDER=modelconf.getProperty("folder");
	    	
	    	try {

				cls_o3 = (Classifier) weka.core.SerializationHelper.read(MODEL_FOLDER + "ZURICH_O3_J48.model");
				kmeans_temp = (SimpleKMeans) weka.core.SerializationHelper.read(MODEL_FOLDER + "ZURICH_O3_TEMP.model");
				kmeans_loc = (SimpleKMeans) weka.core.SerializationHelper.read(MODEL_FOLDER + "ZURICH_O3_LOC.model");
				kmeans_h = (SimpleKMeans) weka.core.SerializationHelper.read(MODEL_FOLDER + "ZURICH_O3_HUM.model");

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
			double o3 = 0;
			double o3_ug = 0;
			long time = 0;
			int abs_ind = -1;
			int result = -1;
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
	                lat = ((Double) data.getData(fieldName))/100;
	            if (fieldName.equalsIgnoreCase("longitude"))
	                lon = ((Double) data.getData(fieldName))/100;
	            if (fieldName.equalsIgnoreCase("timed"))
	                time = (Long) data.getData(fieldName);
	        }	
						
			Instances data_o3;
			int classify = 0;

			FastVector classVal = new FastVector();
	        classVal.addElement("(54.26528-108.52996]");
	        classVal.addElement("(-inf-54.26528]");
	        classVal.addElement("(108.52996-162.79464)");
	        classVal.addElement("(162.79464-217.05932]");
	        classVal.addElement("(217.05932-inf)");
			
			// Create attributes to be used with classifiers
			// Create instances for each pollutant with attribute values pollutant
			// Set instance's values for "pollutant concentration"
	        // Check for null values by checking if pollutant values are >= 0.0, they are -1.0 if null returned from database
	        
			if(o3 >= 0.0)
			{
				classify = 1;
				
		        FastVector attributes_o3 = new FastVector();
				
		        FastVector hum_values = new FastVector(5);
		        hum_values.addElement("cluster0");
		        hum_values.addElement("cluster3");
		        hum_values.addElement("cluster2");
		        hum_values.addElement("cluster1");
		        hum_values.addElement("cluster4");
		        Attribute humidity = new Attribute("humidity", hum_values);
			    attributes_o3.addElement(humidity);
		        
		        FastVector loc_values = new FastVector(4);
		        loc_values.addElement("cluster0");
		        loc_values.addElement("cluster2");
		        loc_values.addElement("cluster3");
		        loc_values.addElement("cluster1");
		        Attribute location = new Attribute("location", loc_values);
			    attributes_o3.addElement(location);	        

		        FastVector temp_values = new FastVector(4);
		        temp_values.addElement("cluster2");
		        temp_values.addElement("cluster1");
		        temp_values.addElement("cluster0");
		        temp_values.addElement("cluster3");
		        Attribute temperature = new Attribute("temperature", temp_values);
			    attributes_o3.addElement(temperature);		
		
				Attribute timed = new Attribute("timed");
				attributes_o3.addElement((Attribute)timed);
				attributes_o3.addElement(new Attribute("ozone",classVal));
				data_o3 = new Instances("TestInstances",attributes_o3,0);
		        Instance inst_o3 = new Instance(data_o3.numAttributes());
				
				inst_o3.setValue(temperature, (String)  hum_values.elementAt(clusterTemp(t,o3)));
				inst_o3.setValue(location, (String) loc_values.elementAt(clusterLoc(lat,lon,o3)));
				inst_o3.setValue(humidity, (String) loc_values.elementAt(clusterHum(h,o3)));
				inst_o3.setValue(timed, (double) time%31556926);
		        data_o3.add(inst_o3);
		        data_o3.setClassIndex(data_o3.numAttributes() - 1);

				// Test the model, returns 0 if null value of pollutant received
				try {
					if(classify == 1){
						result = O3_MAP[(int) cls_o3.classifyInstance(data_o3.instance(0))];
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			// create output streamelement to be sent to application, neglect pollutant if index = -1
			
			StreamElement[] se = new StreamElement[1];
			
			se[0] = new StreamElement( OUTPUT_FIELDS , 
					new Byte[] {DataTypes.DOUBLE,  DataTypes.INTEGER, DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.INTEGER,  DataTypes.INTEGER, DataTypes.DOUBLE} , 
					new Serializable [] {t, h, lat, lon, result, abs_ind,o3} , time);
			
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
		
		private int clusterTemp(double temperature, double ozone)
		{//clusters numeric temperature value to be used in the classifier
			
	        FastVector attributes = new FastVector();
			
	        Attribute temp_attr = new Attribute("temperature");
			attributes.addElement((Attribute)temp_attr);
			
	        Attribute o3_attr = new Attribute("ozone");
			attributes.addElement((Attribute)o3_attr);
		
			Instances cluster_data = new Instances("TestInstances",attributes,0);
	        Instance inst_o3 = new Instance(cluster_data.numAttributes());
			
			inst_o3.setValue(temp_attr, temperature);
			inst_o3.setValue(o3_attr, ozone);

	        cluster_data.add(inst_o3);
			
			int result = 0; 
			
			try {
				
				result = kmeans_temp.clusterInstance(cluster_data.instance(0));
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return result;
		}
		
		private int clusterLoc(double latitude, double longitude, double ozone)
		{//clusters numeric temperature value to be used in the classifier
			
	        FastVector attributes = new FastVector();
			
	        Attribute lat_attr = new Attribute("latitude");
			attributes.addElement((Attribute)lat_attr);
			Attribute lon_attr = new Attribute("longitude");
			attributes.addElement((Attribute)lon_attr);
	        Attribute co_attr = new Attribute("CO");
			attributes.addElement((Attribute)co_attr);
		
			Instances cluster_data = new Instances("TestInstances",attributes,0);
	        Instance inst_co = new Instance(cluster_data.numAttributes());
			
			inst_co.setValue(lat_attr, latitude);
			inst_co.setValue(lon_attr, longitude);
			inst_co.setValue(co_attr, ozone);

	        cluster_data.add(inst_co);
			
			int result = 0; 
			
			try {
				result = kmeans_loc.clusterInstance(cluster_data.instance(0));
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return result;
		}
		
		private int clusterHum(double humidity, double ozone)
		{//clusters numeric temperature value to be used in the classifier
			
	        FastVector attributes = new FastVector();
	        Attribute hum_attr = new Attribute("humidity");
			attributes.addElement((Attribute)hum_attr);
	        Attribute o3_attr = new Attribute("ozone");
			attributes.addElement((Attribute)o3_attr);
			Instances cluster_data = new Instances("TestInstances",attributes,0);
	        Instance inst_o3 = new Instance(cluster_data.numAttributes());
			
			inst_o3.setValue(hum_attr,  humidity);
			inst_o3.setValue(o3_attr,  ozone);

	        cluster_data.add(inst_o3);
			
			int result = 0; 
			
			try {
				result = kmeans_h.clusterInstance(cluster_data.instance(0));
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return result;
		}
}
