package ch.slf;

import gsn.beans.Operator;
import gsn.beans.StreamElement;
import gsn.core.OperatorConfig;

import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.log4j.Logger;

/**
 * !! Get only the first value !! 
 * TODO extend to multiple dimensions
 */
public abstract class WindowAwareVS implements Operator {
	
	public void process ( String inputStreamName , List<StreamElement> data ) {
		for (StreamElement se: data)
			process(inputStreamName, se);
	}

	public void start() {}
	public void stop() {}


	private static final String WINDOW_SIZE = "window-size";

	private static final String STEP_SIZE = "step-size";

	private static transient Logger logger = Logger.getLogger ( WindowAwareVS.class );

	private Hashtable<String, ArrayBlockingQueue<StreamElement>> circularBuffers;

	private int windowSize = -1;

	private int stepSize = -1;

	public void initialize(OperatorConfig config) {
		circularBuffers = new Hashtable<String, ArrayBlockingQueue<StreamElement>> () ;
		windowSize = config.getParameters().getValueAsIntWithException(WINDOW_SIZE);
		stepSize= config.getParameters().getValueAsIntWithException(STEP_SIZE);
		
		if (windowSize < stepSize) 
			throw new RuntimeException("The parameter " + WINDOW_SIZE + " must be greater or equal to the parameter " + STEP_SIZE);
	}

	public void process(String inputStreamName, StreamElement streamElement) {
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
					values[i] = ((Number)elt.getValue(elt.getFieldNames()[0])).doubleValue() ; //
					timestamps[i] = new Long(elt.getTimeInMillis());
				}

				// Remove stepsize elements from the beginning of the buffer
				for (int i = 0 ; i < stepSize ; i++) {
					try {
						circularBuffer.take();
					} catch (InterruptedException e) {
						logger.warn(e.getMessage(), e) ;
					}
				}
				handle(values, timestamps);
			}		
		} catch (InterruptedException e) {
			logger.warn (e.getMessage(), e) ;
		}
	}

	public void dispose() {
		
	}

	public abstract void handle(double[] values , long[] timestamps);

}
