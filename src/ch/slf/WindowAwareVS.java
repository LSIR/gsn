package ch.slf;

import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.vsensor.AbstractVirtualSensor;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import org.apache.log4j.Logger;

public abstract class WindowAwareVS extends AbstractVirtualSensor {

	private static final String WINDOW_SIZE = "window-size";

	private static final String STEP_SIZE = "step-size";
	
	private static final String TIMESTAMP = "TIMESTAMP";
	
	private static final String DF = "DF";
	
	private static final String VALUES = "VALS";
	
	private static final DataField [] outputStructure = new DataField[] { 
		new DataField(TIMESTAMP, DataTypes.BIGINT_NAME), 
		new DataField(DF, DataTypes.DOUBLE_NAME), 
		new DataField(VALUES, "BLOB")
	};

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
		// Put the new stream element in the circular buffer and call the process method if the number of elements
		// in the buffer is more or equal to the step size
		try {
			circularBuffer.put(streamElement);
			
			logger.debug("Circular buffer for stream " + inputStreamName + " contains: " + circularBuffer.size() + " elements (" + circularBuffer.remainingCapacity() + " free places remains)");
			
			if (circularBuffer.size() >= stepSize) {

				double[][] values = new double[stepSize][streamElement.getData().length] ;
				long[] timestamps = new long[stepSize] ;
				
				StreamElement elt = null;
				double[] dataInDouble = null;
				for (int i = 0 ; i < stepSize ; i++) {
					try {
						elt = circularBuffer.take();
						dataInDouble = new double[elt.getData().length] ;
						for (int j = 0 ; j < dataInDouble.length ; j++) {
							try {
								dataInDouble[j] = ((Number)elt.getData()[j]).doubleValue() ;
							}
							catch (java.lang.ClassCastException e) {
								dataInDouble[j] = Double.NaN;
								logger.warn("The element +>" + elt.getData()[j] + "<+ is not a number. This element has been converted to NaN.");
							}
						}
						values[i] = dataInDouble;
						timestamps[i] = new Long(elt.getTimeStamp());
					} catch (InterruptedException e) {
						logger.warn(e.getMessage(), e) ;
					}
				}
				processNextWindow(values, timestamps);
			}		
		} catch (InterruptedException e) {
			logger.warn (e.getMessage(), e) ;
		}
	}

	public void finalize() {

	}

	public int getPredicateValueAsIntWithException (String parameter) {
		String param = getVirtualSensorConfiguration( ).getMainClassInitialParams( ).get(parameter);
		if (param == null) throw new  java.lang.RuntimeException ("The required parameter: >" + parameter + "<+ is missing.from the virtual sensor configuration file.");
		else return Integer.parseInt(param);
	}
	
	public void processNextWindow (double [][] values , long[] timestamps) {
		
		FFTOutputPacket fftOutputPacket = process(values, timestamps);
		
		Serializable[] data = new Serializable[3];
		data[0] = fftOutputPacket.getTimestamp();
		data[1] = fftOutputPacket.getDf();
		
		StringBuilder sb = new StringBuilder();
		int size = fftOutputPacket.getValues().size();
		if (size != 0) sb.append(fftOutputPacket.getValues().get(0));
		for (int i = 1 ; i < size ; i++) {
			sb.append("," + fftOutputPacket.getValues().get(i));
		}
		data[2] = sb.toString().getBytes();
		StreamElement se = new StreamElement (outputStructure, data) ;
		dataProduced( se );
	}

	public abstract FFTOutputPacket process(double [][] values , long[] timestamps);

	public abstract boolean init();
}
