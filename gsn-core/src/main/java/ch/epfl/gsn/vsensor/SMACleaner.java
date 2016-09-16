/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2016, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
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
* File: src/ch/epfl/gsn/vsensor/SMACleaner.java
*
* @author Ali Salehi
*
*/

package ch.epfl.gsn.vsensor;

import java.io.Serializable;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import ch.epfl.gsn.beans.DataField;
import ch.epfl.gsn.beans.StreamElement;
import ch.epfl.gsn.vsensor.AbstractVirtualSensor;
import ch.epfl.gsn.vsensor.SensorscopeVS;

import org.slf4j.Logger;

public class SMACleaner extends AbstractVirtualSensor {
	
	static int index = 0;
	static double values[] ;
	static private double error_threshold;
	
	private static final transient Logger logger = LoggerFactory.getLogger(SensorscopeVS.class);
	
	public void dataAvailable(String inputStreamName,StreamElement in) {
		Double input = (Double) in.getData()[0];
		
		if (index>=values.length) {
			double sum = 0;
			for (double v:values)
				sum+=v;
			double sma = sum/values.length;
			
			StreamElement se ;
			boolean isAcceptable =  (Math.abs(input - sma)/input <= error_threshold );
			se= new StreamElement(
					new DataField[] {new DataField("raw_value","double" ), new DataField("acceptable","integer")},
					new Serializable[] {input,(isAcceptable == false ? 0 : 1)},
					in.getTimeStamp());
			dataProduced(se);
		}
		values[index++%values.length]= input;
	}

	public void dispose() {
		
	}

	public boolean initialize() {
		TreeMap <  String , String > params = getVirtualSensorConfiguration( ).getMainClassInitialParams( );
		int size = Integer.parseInt(params.get("size"));
		error_threshold = Double.parseDouble(params.get("error-threshold"));
		values = new double[size];
		return true;
	}
}
