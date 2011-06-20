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
