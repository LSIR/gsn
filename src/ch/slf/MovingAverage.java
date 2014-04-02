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
* File: src/ch/slf/MovingAverage.java
*
* @author Timotee Maret
* @author Ali Salehi
*
*/

package ch.slf;

import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;

import java.io.Serializable;

import org.apache.log4j.Logger;

public class MovingAverage extends WindowAwareVS {

	private static final String VALUES = "AVERAGE";

	private static final DataField [] outputStructure = new DataField[] { 
		new DataField(VALUES, DataTypes.DOUBLE_NAME)
	};
	
	private static transient Logger logger  = Logger.getLogger ( MovingAverage.class );
	

	public boolean init() {
		return true;
	}

	public void process(double [] values , long[] timestampsInMSec) {

		if (logger.isDebugEnabled()) {
			logger.debug("INPUT MOVING AVERAGE DATA");
			for (int i = 0 ; i < values.length ; i++) {
				logger.debug(values[i] + "\n");
			}
		}
		
		long deltaTimeStampInSec = (timestampsInMSec[timestampsInMSec.length - 1] - timestampsInMSec[0]) / 1000;
		logger.debug("Delta Time Stamp in s: " + deltaTimeStampInSec);

		long middleTimeStamp = timestampsInMSec[0] + ((deltaTimeStampInSec / 2) * 1000);
		
		//
		double avg = 0;
		for (int i = 0 ; i < values.length ; i++) {
			avg += values[i]; 
		}
		avg /= values.length;
		
		//
		Serializable[] dataOut = new Serializable[1];
		// Average
		dataOut[0] = avg;
		
		//
		StreamElement se = new StreamElement (outputStructure, dataOut, middleTimeStamp);
		logger.debug("FFT StreamElement produced: " + se);
		dataProduced( se );
	}
}
