package gsn.vsensor;

import java.util.TreeMap;

import org.apache.log4j.Logger;

import gsn.beans.StreamElement;
import gsn.utils.models.AbstractModel;

public class ModellingVirtualSensor extends AbstractVirtualSensor {
	
	private static final transient Logger logger = Logger.getLogger(ModellingVirtualSensor.class);
	
	private static final String PARAM_MODEL_CLASS = "model";
	
	private static final String PARAM_SEC_MODEL_CLASS ="sec_model";
	
	private static final String PARAM_MODEL_PREFIX ="mmm";
	
	private static final String PARAM_MODEL_PREFIX2 = "mm2";
	
	
	
	private String model = "";
	
	private String model2 = "";
	
	private AbstractModel[] am = new AbstractModel[2];
	

	@Override
	public boolean initialize() {

        TreeMap<String, String> params = getVirtualSensorConfiguration().getMainClassInitialParams();

        String model_str = params.get(PARAM_MODEL_CLASS);

        if (model_str == null) {
            logger.warn("Parameter \"" + PARAM_MODEL_CLASS + "\" not provided in Virtual Sensor file");
            return false;
        } else {
            model = model_str.trim();
        }
        
		try {
			 Class<?>  fc = Class.forName(model);
			am[0] = (AbstractModel) fc.newInstance();
			am[0].setOutputFields(getVirtualSensorConfiguration().getOutputStructure());
						
			for (String k: params.navigableKeySet())
			{
				if (k.startsWith(PARAM_MODEL_PREFIX)){
					am[0].setParam(k.substring(PARAM_MODEL_PREFIX.length()),params.get(k));
				}	
			}
					
		} catch (Exception e) {
			logger.error( e.getMessage( ) , e );
			return false;
		}

		String model2_str = params.get(PARAM_SEC_MODEL_CLASS);

        if (model2_str != null) {
            model2 = model2_str.trim(); 
		try {
			 Class<?>  fc = Class.forName(model2);
			am[1] = (AbstractModel) fc.newInstance();
			am[1].setOutputFields(getVirtualSensorConfiguration().getOutputStructure());
			am[0].setNextModel(am[1]);
			
			for (String k: params.navigableKeySet())
			{
				if (k.startsWith(PARAM_MODEL_PREFIX2)){
					am[1].setParam(k.substring(PARAM_MODEL_PREFIX2.length()),params.get(k));
				}	
			}
			
		} catch (Exception e) {
			logger.error( e.getMessage( ) , e );
			return false;
		}
        }
        return true;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public void dataAvailable(String inputStreamName,
			StreamElement streamElement) {
		StreamElement out = am[0].pushData(streamElement);
		if (am[1] != null){
			am[1].pushData(streamElement);
		}
		
		if(out != null)
		    dataProduced(out);

	}
	
	
	public AbstractModel[] getModel(String name){
		return am;
	}

}
