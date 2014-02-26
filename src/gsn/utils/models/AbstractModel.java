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
* File: src/gsn/utils/models/AbstractModel.java
*
* @author Julien Eberle
*
*/

package gsn.utils.models;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.vsensor.ModellingVirtualSensor;


/**
 * This class is the base class for all models that need to be linked to a virtual sensor for getting updated in real-time.
 * A reference to the VS allows for accessing the other models if needed.
 * @author jeberle
 *
 */
public abstract class AbstractModel {
	
	protected DataField[] outputfield;
	
	protected ModellingVirtualSensor vs;

	public DataField[] getOutputFields() {
		return outputfield;
	}

	public void setOutputFields(DataField[] outputStructure) {
		outputfield = outputStructure;
		
	}

	public abstract StreamElement pushData(StreamElement streamElement);



	public abstract StreamElement[] query(StreamElement params);
	

	public abstract void setParam(String k, String string);

	public boolean initialize() {
		return true;
	}
	
	public void setVirtualSensor(ModellingVirtualSensor v){
		vs = v;
	}
	
	public ModellingVirtualSensor getVirtualSensor(){
		
		return vs;
	}
	
	
	
}
