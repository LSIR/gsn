/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
* 
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/gsn/vsensor/ModellingVirtualSensor.java
*
* @author Julien Eberle
*
*/

package gsn.vsensor;

import java.util.TreeMap;

import org.apache.log4j.Logger;

import gsn.beans.StreamElement;
import gsn.utils.models.AbstractModel;


/**
 * This class is linked to an array of AbstractModels and keep them updated by pushing every StreamElement to them.
 * The model classes are defined by their class names separated by "," as a parameter of the VS.
 * If a model need some parameters before initializing, they can be specified in the VS parameters as "model.i.param",
 *  where i is the index of the model and param the parameter name.
 * @author jeberle
 *
 */
public class ModellingVirtualSensor extends AbstractVirtualSensor {
	
	private static final transient Logger logger = Logger.getLogger(ModellingVirtualSensor.class);
	
	private static final String PARAM_MODEL_CLASS = "model";
	private static final String PARAM_MODEL_PREFIX ="model";
	
	private String[] model;
	
	private AbstractModel[] am;
	

	@Override
	public boolean initialize() {

        TreeMap<String, String> params = getVirtualSensorConfiguration().getMainClassInitialParams();

        //get all the models
        String model_str = params.get(PARAM_MODEL_CLASS);

        if (model_str == null) {
            logger.warn("Parameter \"" + PARAM_MODEL_CLASS + "\" not provided in Virtual Sensor file");
            return false;
        }
        
        model = model_str.trim().split(",");
        
        am = new AbstractModel[model.length];
        
        for(int i=0;i<model.length;i++){
			try {
				//instantiate the models, ...
				 Class<?>  fc = Class.forName(model[i]);
				am[i] = (AbstractModel) fc.newInstance();
				//output structure of the models is the same as the one of the VS
				am[i].setOutputFields(getVirtualSensorConfiguration().getOutputStructure());
				//...set their parameters...			
				for (String k: params.navigableKeySet())
				{
					String prefix = PARAM_MODEL_PREFIX+"."+i+".";
					if (k.startsWith(prefix)){
						am[i].setParam(k.substring(prefix.length()),params.get(k));
					}	
				}
				am[i].setVirtualSensor(this);
				//... and initialize them.
				if (! am[i].initialize()){
					return false;
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

	}

	@Override
	public void dataAvailable(String inputStreamName, StreamElement streamElement) {
		StreamElement out = streamElement;
		if (am.length > 0){
		    out = am[0].pushData(streamElement); //by default returns the result from the first model
		}
		for(int i=1;i<am.length;i++){
			if (am[i] != null){
				am[i].pushData(streamElement);//push the data to all other models too
			}
		}
		if(out != null)
		    dataProduced(out);
	}
	
	
	/**
	 * Return the model corresponding to the given index
	 * @param index of the model
	 * @return the model if it exists or null if the index is out of bound
	 */
	public AbstractModel getModel(int index){
		if (index>=0 && index <am.length)
			return am[index];
		else
			return null;
	}

}
