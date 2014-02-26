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
* File: src/gsn/utils/models/ModelLoader.java
*
* @author Alexandru Arion
* @author Sofiane Sarni
*
*/

package gsn.utils.models;

import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;

public class ModelLoader {
	
	
	private Classifier classifier = null;

    public ModelLoader(String model) {
    	try{
    		classifier = (Classifier) weka.core.SerializationHelper.read(model);
    	}catch(Exception e)
    	{}
	}

	public Double predict(Instance i) {
		try{
		return new Double((classifier.classifyInstance(i)+4)*100);
		}catch(Exception e){
			return null;
		}
	}
}
