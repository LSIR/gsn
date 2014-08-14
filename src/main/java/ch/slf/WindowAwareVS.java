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
* File: src/ch/slf/WindowAwareVS.java
*
* @author Timotee Maret
* @author Ali Salehi
* @author Mehdi Riahi
*
*/

package ch.slf;

import gsn.beans.StreamElement;
import gsn.vsensor.AbstractVirtualSensor;

import java.util.Hashtable;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.log4j.Logger;

/**
 * !! Get only the first value !! 
 * TODO extend to multiple dimensions
 */
public abstract class WindowAwareVS extends AbstractVirtualSensor {

	private static final String WINDOW_SIZE = "window-size";

	private static final String STEP_SIZE = "step-size";

	private static transient Logger logger = Logger.getLogger ( WindowAwareVS.class );

	private Hashtable<String, ArrayBlockingQueue<StreamElement>> circularBuffers;

	private int windowSize = -1;

	private int stepSize = -1;

	public boolean initialize() {	
		circularBuffers = new Hashtable<String, ArrayBlockingQueue<StreamElement>> () ;
		// Get the parameters from the XML configuration file
		TreeMap <  String , String > params = getVirtualSensorConfiguration( ).getMainClassInitialParams( ) ;
		String param = null;
		param = params.get(WINDOW_SIZE);
		if (param != null) windowSize = Integer.parseInt(param) ;
		else {
			logger.error("The required parameter: >" + WINDOW_SIZE + "<+ is missing.from the virtual sensor configuration file.");
			return false;
		}
		param = params.get(STEP_SIZE);
		if (param != null) stepSize = Integer.parseInt(param) ;
		else {
			logger.error("The required parameter: >" + STEP_SIZE + "<+ is missing.from the virtual sensor configuration file.");
			return false;
		}
		if (windowSize < stepSize) {
			logger.error("The parameter " + WINDOW_SIZE + " must be greater or equal to the parameter " + STEP_SIZE);
			return false;
		}
		return init();
	}

	public void dataAvailable(String inputStreamName, StreamElement streamElement) {
		ArrayBlockingQueue<StreamElement> circularBuffer = circularBuffers.get(inputStreamName) ;
		// Get the circular buffer that matches the input stream. Create a new one if none exists
		if (circularBuffer == null) {
			circularBuffer = new ArrayBlockingQueue<StreamElement> (windowSize);
			circularBuffers.put(inputStreamName, circularBuffer) ;
		}
		try {
			circularBuffer.put(streamElement);

			logger.debug("Circular buffer for stream " + inputStreamName + " contains: " + circularBuffer.size() + " elements (" + circularBuffer.remainingCapacity() + " free places remains)");

			if (circularBuffer.size() == windowSize ) {

				// Build the window
				double[] values = new double[windowSize] ;
				long[] timestamps = new long[windowSize] ;
				StreamElement elt = null;
				
				Object[] elts = circularBuffer.toArray();
				for (int i = 0 ; i < elts.length ; i++) {
					elt = (StreamElement) elts[i];
					values[i] = ((Number)elt.getData()[0]).doubleValue() ; //
					timestamps[i] = new Long(elt.getTimeStamp());
				}

				// Remove stepsize elements from the beginning of the buffer
				for (int i = 0 ; i < stepSize ; i++) {
					try {
						circularBuffer.take();
					} catch (InterruptedException e) {
						logger.warn(e.getMessage(), e) ;
					}
				}
				process(values, timestamps);
			}		
		} catch (InterruptedException e) {
			logger.warn (e.getMessage(), e) ;
		}
	}

	public void dispose() {
		
	}

	public int getPredicateValueAsIntWithException (String parameter) {
		String param = getVirtualSensorConfiguration( ).getMainClassInitialParams( ).get(parameter);
		if (param == null) throw new  java.lang.RuntimeException ("The required parameter: >" + parameter + "<+ is missing.from the virtual sensor configuration file.");
		else return Integer.parseInt(param);
	}

	public abstract void process(double[] values , long[] timestamps);

	public abstract boolean init();
}
