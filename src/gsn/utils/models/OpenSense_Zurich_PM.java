package gsn.utils.models;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;


public class OpenSense_Zurich_PM {
	
	Instances data_pm;
	int classify = 0;
	
	private static final int[] PM_MAP = new int [] {4,3,5,2,1};

	public OpenSense_Zurich_PM(double pm, double t, double lat, double lon, long time)
	{
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
		}

	}
	
	public int applyModel()
	{
		Classifier cls_pm = null;
		
		try {
			
			//Julien version
			cls_pm = (Classifier) weka.core.SerializationHelper.read("/home/jeberle/gsn/gsn-dev/models/ZURICH_PM_IBK.model");

			//Erol version
//			cls_o3 = (Classifier) weka.core.SerializationHelper.read("C:/Users/baba tenor/Documents/EPFL/2012-2013 Fall/Master Thesis/ZURICH_PM_IBK.model");
		
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		int result = -1;
		// Test the model, returns 0 if null value of pollutant received
		try {
			if(classify == 1) result = PM_MAP[(int) cls_pm.classifyInstance(data_pm.instance(0))];
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}
}
