package gsn.utils.models;

import weka.classifiers.Classifier;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;


public class OpenSense_Zurich_CO {
	
	Instances data_co;
	
	double temperature;
	double latitude;
	double longitude;
	double carbonmonoxide;

	int classify;
	
	private static final int[] CO_MAP = new int [] {1,2,4,3,5};
	
	public OpenSense_Zurich_CO(double co, double lat, double lon, double t, long time)
	{
		
		temperature = t;
		latitude = lat;
		longitude = lon;
		carbonmonoxide = co;
		
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
	        
	        FastVector loc_values = new FastVector(3);
	        loc_values.addElement("cluster1");
	        loc_values.addElement("cluster0");
	        loc_values.addElement("cluster2");
	        Attribute location = new Attribute("location", loc_values);
			
			Attribute timed = new Attribute("timed");
			attributes_co.addElement(timed);
			attributes_co.addElement(new Attribute("CO",classVal));
			data_co = new Instances("TestInstances",attributes_co,0);
	        Instance inst_co = new Instance(data_co.numAttributes());
			
			inst_co.setValue(temperature, (String) temp_values.elementAt(clusterTemp()));
			inst_co.setValue(location, (String) loc_values.elementAt(clusterLoc()));
			inst_co.setValue(timed, (double) time%31556926);
	        data_co.add(inst_co);
	        data_co.setClassIndex(data_co.numAttributes() - 1);
		}
		

	}
	
	public int clusterTemp()
	{//clusters numeric temperature value to be used in the classifier
		
        FastVector attributes = new FastVector();
		
        Attribute temp_attr = new Attribute("temperature");
		attributes.addElement((Attribute)temp_attr);
		
        Attribute co_attr = new Attribute("co");
		attributes.addElement((Attribute)co_attr);
	
		Instances cluster_data = new Instances("TestInstances",attributes,0);
        Instance inst_co = new Instance(cluster_data.numAttributes());
		
		inst_co.setValue(temp_attr, (double) temperature);
		inst_co.setValue(co_attr, (double) carbonmonoxide);

        cluster_data.add(inst_co);
		
		int result = 0; 
		
		try {
			SimpleKMeans kmeans = (SimpleKMeans) weka.core.SerializationHelper.read("/home/jeberle/gsn/gsn-dev/models/ZURICH_CO_TEMP.model");
			result = kmeans.clusterInstance(cluster_data.instance(0));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}
	
	public int clusterLoc()
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
			SimpleKMeans kmeans = (SimpleKMeans) weka.core.SerializationHelper.read("/home/jeberle/gsn/gsn-dev/models/ZURICH_CO_LOC.model");
			result = kmeans.clusterInstance(cluster_data.instance(0));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}
	
	public int applyModel()
	{
		Classifier cls_co = null;

		try {
			
			//Julien version
			cls_co = (Classifier) weka.core.SerializationHelper.read("/home/jeberle/gsn/gsn-dev/models/ZURICH_CO_J48.model");
			
			//Erol version
//			cls_co = (Classifier) weka.core.SerializationHelper.read("C:/Users/baba tenor/Documents/EPFL/2012-2013 Fall/Master Thesis/ZURICH_CO_J48.model");
		
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		int result = -1;
		// Test the model, returns 0 if null value of pollutant received
		try {
			if(classify == 1) result = CO_MAP[(int) cls_co.classifyInstance(data_co.instance(0))];
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}
}
