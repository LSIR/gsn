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
* File: src/weka/filters/unsupervised/instance/SubSample.java
*
* @author Julien Eberle
* @author Sofiane Sarni
*
*/

package weka.filters.unsupervised.instance;

import weka.core.Instances;
import weka.core.OptionHandler;
import weka.filters.UnsupervisedFilter;

/**
 * A filter that resample the instances uniformly.
 * @author jeberle
 *
 */
public class SubSample extends weka.filters.SimpleBatchFilter implements UnsupervisedFilter, OptionHandler{

	private static final long serialVersionUID = -6080185146245135909L;
	
	private int m_index = 0;
	
	private int m_ratio = 1;

	@Override
	public String globalInfo() {
		return "A filter that resample the instances uniformly.";
	}

	@Override
	protected Instances determineOutputFormat(Instances inputFormat)
			throws Exception {
		return inputFormat;
	}

	@Override
	protected Instances process(Instances instances) throws Exception {

		instances.sort(m_index);
		
		Instances output = new Instances(instances);
		
		if(instances.numInstances() <= m_ratio){return output;}
		
		for(int i=output.numInstances()-1;i>=0;i--){
			if((i+1) % m_ratio != 0){output.delete(i);}
		}
		//output.compactify();
		
		
		return output;
	}

	public int getM_index() {
		return m_index;
	}

	public void setM_index(int m_index) {
		this.m_index = m_index;
	}

	public int getRatio() {
		return m_ratio;
	}

	public void setRatio(int ratio) {
		this.m_ratio = ratio;
	}

}
