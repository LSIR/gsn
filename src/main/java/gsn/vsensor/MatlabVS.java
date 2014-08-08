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
* File: src/gsn/vsensor/MatlabVS.java
*
* @author Jerome Rousselot
* @author Ali Salehi
* @author Mehdi Riahi
*
*/

package gsn.vsensor;

import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.utils.MatlabEngine;

import java.io.IOException;
import java.io.Serializable;
import java.util.TreeMap;

import org.apache.log4j.Logger;

public class MatlabVS extends AbstractVirtualSensor {

	private final static transient Logger      logger         = Logger.getLogger( AbstractVirtualSensor.class );
	private MatlabEngine engine;
	
	
	private String[] fieldNames = {"Matlab_Result"};
	private Byte[] fieldTypes = {DataTypes.DOUBLE};
	
	private String functionName, defaultFunctionName = "myGSNMatlabFunction";
	private Integer nbArgs;
	private Double[] parameters;
	/* (non-Javadoc)
	 * @see gsn.vsensor.AbstractVirtualSensor#dataAvailable(java.lang.String, gsn.beans.StreamElement)
	 */
	@Override
	public void dataAvailable(String inputStreamName,
			StreamElement streamElement) {
	
		if(streamElement.getFieldTypes().length == nbArgs+1)
			for(int i = 0; i < nbArgs; i++)
				parameters[i] = (Double) streamElement.getData()[i];
		Double answer;
		try {
			String matlabCommand = functionName + "(" ;
			for(int i = 0; i < nbArgs; i++) {
				matlabCommand = matlabCommand + parameters[i].toString();
				if(i != nbArgs-1)
					matlabCommand = matlabCommand +",";
			}
			if(nbArgs > 0)
				matlabCommand = matlabCommand + ")";
			if(logger.isDebugEnabled())
				logger.debug("Calling matlab engine with command: " + matlabCommand);
			engine.evalString(matlabCommand);
			String matlabAnswer = engine.getOutputString(100);
			if(logger.isDebugEnabled())
				logger.debug("Received output from matlab: " + matlabAnswer +". Trying to interpret this"
						+ " answer as a Java Float object.");
			answer = Double.parseDouble(matlabAnswer);
			StreamElement result = new StreamElement(fieldNames, fieldTypes , new Serializable[] {answer});
			dataProduced(result);
		} catch (IOException e) {
			logger.warn(e);
		}

		
	}

	/* (non-Javadoc)
	 * @see gsn.vsensor.AbstractVirtualSensor#dispose()
	 */
	@Override
	public void dispose() {
		try {
			engine.close();
		} catch (InterruptedException e) {
			logger.warn(e);
		} catch (IOException e) {
			logger.warn(e);
		}
	}

	/* (non-Javadoc)
	 * @see gsn.vsensor.AbstractVirtualSensor#initialize()
	 */
	@Override
	public boolean initialize() {
		boolean success = false;
		VSensorConfig vsensor = getVirtualSensorConfiguration();
		TreeMap < String , String > params = vsensor.getMainClassInitialParams( );
		engine = new MatlabEngine();
        try {
                // Matlab start command:
                engine.open("matlab -nosplash -nojvm");
                // Display output:
                if(logger.isDebugEnabled())
                	logger.debug(engine.getOutputString(500));
                String functionName = params.get("function");
                if(functionName == null || functionName.trim().equals(""))
                	functionName = defaultFunctionName;
                if(logger.isDebugEnabled())
                	logger.debug("Function name configured to: " + functionName);
                nbArgs = Integer.parseInt(params.get("arguments"));
                if(nbArgs == null)
                	nbArgs = new Integer(0);
                else
                	parameters = new Double[nbArgs];
                if(logger.isDebugEnabled())
                	logger.debug("Number of arguments configured to: " + nbArgs);
                success = true;
        }
        catch (Exception e) {
                logger.warn(e);
        }
		
		
		return success;
	}

}
