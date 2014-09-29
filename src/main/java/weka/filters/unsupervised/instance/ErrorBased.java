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
* File: src/weka/filters/unsupervised/instance/ErrorBased.java
*
* @author Julien Eberle
* @author Sofiane Sarni
*
*/

package weka.filters.unsupervised.instance;


import java.util.Arrays;

import java.util.Random;

import weka.core.Instances;
import weka.core.OptionHandler;
import weka.filters.UnsupervisedFilter;

/**
 * A filter that resample the instances according to their errors.
 * The error array must have the same size (and no offset) than the instances !!
 * @author jeberle
 *
 */
public class ErrorBased extends weka.filters.SimpleBatchFilter implements UnsupervisedFilter, OptionHandler{

	private static final long serialVersionUID = -6080185146245135909L;
	
	private double[] m_errors ;
	
	private int m_ratio = 100;

	@Override
	public String globalInfo() {
		return "A filter that resample the instances according to their errors.";
	}

	@Override
	protected Instances determineOutputFormat(Instances inputFormat)
			throws Exception {
		return inputFormat;
	}

	@Override
	protected Instances process(Instances instances) throws Exception {
		
		Instances output = new Instances(instances);
		
		double[] dif = m_errors.clone();
		Arrays.sort(dif);
		double quantil = dif[(int)(dif.length*(1-1.0/m_ratio))];
		int after = (int)(dif.length*(1-1.0/m_ratio));
		int middle = (int)(dif.length*(1-1.0/m_ratio));
		int before = (int)(dif.length*(1-1.0/m_ratio));
		while (after < dif.length && dif[after] == quantil){after++;}
		while (before >= 0 && dif[before] == quantil){before--;}
		Random r = new Random();
		if(instances.numInstances() <= m_ratio){return output;}

		for(int i=output.numInstances()-1;i>=0;i--){
			if(output.numInstances() <= m_ratio){break;}
			if(m_errors.length > i && m_errors[i] < quantil){output.delete(i);}
			if(m_errors.length > i && m_errors[i]==quantil && r.nextInt(after-before)>middle-before){
				output.delete(i);
			}
			
			

		}		
		
		return output;
	}

	public double[] getM_errors() {
		return m_errors;
	}

	public void setM_errors(double[] m_errors) {
		this.m_errors = m_errors;
	}
	public int getM_ratio() {
		return m_ratio;
	}

	public void setM_ratio(int m_ratio) {
		this.m_ratio = m_ratio;
	}
	
}
