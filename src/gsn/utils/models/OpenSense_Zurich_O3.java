package gsn.utils.models;

import weka.classifiers.Classifier;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;


public class OpenSense_Zurich_O3 {
	
	Instances data_o3;
	int classify = 0;
	
	double temperature;
	double humidity;
	double latitude;
	double longitude;
	double ozone;
	
	private static final int[] O3_MAP = new int [] {2,1,3,4,5};

	public OpenSense_Zurich_O3(double o3, double t, int h, double lat, double lon, long time)
	{
		FastVector classVal = new FastVector();
        classVal.addElement("(54.26528-108.52996]");
        classVal.addElement("(-inf-54.26528]");
        classVal.addElement("(108.52996-162.79464)");
        classVal.addElement("(162.79464-217.05932]");
        classVal.addElement("(217.05932-inf)");
		
		// Create attributes to be used with classifiers
		// Create instances for each pollutant with attribute values pollutant
		// Set instance's values for "pollutant concentration"
        //Check for null values by checking if pollutant values are >= 0.0, they are -1.0 if null returned from database
        
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
	        
	        FastVector loc_values = new FastVector(4);
	        loc_values.addElement("cluster0");
	        loc_values.addElement("cluster2");
	        loc_values.addElement("cluster3");
	        loc_values.addElement("cluster1");
	        Attribute location = new Attribute("location", loc_values);
	        
	        FastVector temp_values = new FastVector(4);
	        temp_values.addElement("cluster2");
	        temp_values.addElement("cluster1");
	        temp_values.addElement("cluster0");
	        temp_values.addElement("cluster3");
	        Attribute temperature = new Attribute("temperature", loc_values);
			
			Attribute timed = new Attribute("timed");
			attributes_o3.addElement((Attribute)timed);
			attributes_o3.addElement(new Attribute("ozone",classVal));
			data_o3 = new Instances("TestInstances",attributes_o3,0);
	        Instance inst_o3 = new Instance(data_o3.numAttributes());
			
			inst_o3.setValue(temperature, (Double) hum_values.elementAt(clusterTemp()));
			inst_o3.setValue(location, (Double) loc_values.elementAt(clusterLoc()));
			inst_o3.setValue(humidity, (Double) loc_values.elementAt(clusterHum()));
			inst_o3.setValue(timed, (double) time%31556926);
	        data_o3.add(inst_o3);
	        data_o3.setClassIndex(data_o3.numAttributes() - 1);
		}

	}
	
	public int clusterTemp()
	{//clusters numeric temperature value to be used in the classifier
		
        FastVector attributes = new FastVector();
		
        Attribute temp_attr = new Attribute("temperature");
		attributes.addElement((Attribute)temp_attr);
		
        Attribute o3_attr = new Attribute("ozone");
		attributes.addElement((Attribute)o3_attr);
	
		Instances cluster_data = new Instances("TestInstances",attributes,0);
        Instance inst_o3 = new Instance(cluster_data.numAttributes());
		
		inst_o3.setValue(temp_attr, (double) temperature);
		inst_o3.setValue(o3_attr, (double) ozone);

        cluster_data.add(inst_o3);
		
		int result = 0; 
		
		try {
			SimpleKMeans kmeans = (SimpleKMeans) weka.core.SerializationHelper.read("/home/jeberle/gsn/gsn-dev/models/ZURICH_O3_TEMP.model");
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
		
        Attribute co_attr = new Attribute("CO");
		attributes.addElement((Attribute)co_attr);
	
		Instances cluster_data = new Instances("TestInstances",attributes,0);
        Instance inst_co = new Instance(cluster_data.numAttributes());
		
		inst_co.setValue(lat_attr, (double) latitude);
		inst_co.setValue(lon_attr, (double) longitude);
		inst_co.setValue(co_attr, (double) ozone);

        cluster_data.add(inst_co);
		
		int result = 0; 
		
		try {
			SimpleKMeans kmeans = (SimpleKMeans) weka.core.SerializationHelper.read("/home/jeberle/gsn/gsn-dev/models/ZURICH_O3_LOC.model");
			result = kmeans.clusterInstance(cluster_data.instance(0));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}
	
	public int clusterHum()
	{//clusters numeric temperature value to be used in the classifier
		
        FastVector attributes = new FastVector();
		
        Attribute hum_attr = new Attribute("humidity");
		attributes.addElement((Attribute)hum_attr);
		
        Attribute o3_attr = new Attribute("ozone");
		attributes.addElement((Attribute)o3_attr);
	
		Instances cluster_data = new Instances("TestInstances",attributes,0);
        Instance inst_o3 = new Instance(cluster_data.numAttributes());
		
		inst_o3.setValue(hum_attr, (double) humidity);
		inst_o3.setValue(o3_attr, (double) ozone);

        cluster_data.add(inst_o3);
		
		int result = 0; 
		
		try {
			SimpleKMeans kmeans = (SimpleKMeans) weka.core.SerializationHelper.read("/home/jeberle/gsn/gsn-dev/models/ZURICH_O3_HUM.model");
			result = kmeans.clusterInstance(cluster_data.instance(0));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}
	
	public int applyModel()
	{
		Classifier cls_o3 = null;
		
		try {
			
			//Julien version
			cls_o3 = (Classifier) weka.core.SerializationHelper.read("/home/jeberle/gsn/gsn-dev/models/ZURICH_O3_IBK.model");

			//Erol version
//			cls_o3 = (Classifier) weka.core.SerializationHelper.read("C:/Users/baba tenor/Documents/EPFL/2012-2013 Fall/Master Thesis/ZURICH_O3_J48.model");
		
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		int result = -1;
		// Test the model, returns 0 if null value of pollutant received
		try {
			if(classify == 1) result = O3_MAP[(int) cls_o3.classifyInstance(data_o3.instance(0))];
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}
}
