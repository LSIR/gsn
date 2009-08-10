package gsn.operators;

import gsn.beans.Operator;
import gsn.beans.StreamElement;
import gsn.beans.DataField;
import gsn.channels.DataChannel;
import gsn.core.OperatorConfig;

import java.io.File;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.Rserve.RConnection;

public class RVirtualSensor implements Operator {
	
	public void process ( String inputStreamName , List<StreamElement> data ) {
		for (StreamElement se: data)
			process(inputStreamName, se);
	}

  public DataField[] getStructure() {
    return new DataField[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void start() {}
	public void stop() {}


	private static final String WINDOW_SIZE = "window_size";
	private static final String STEP_SIZE = "step_size";
	private static final String OUTPUT_PLOT = "gsn_plot";
	private static final String SCRIPT = "script";
	private static final String SERVER = "server";
	private static final String PORT = "port";
	public String script = null;
	public String stype = null;

	private static transient Logger logger = Logger.getLogger(RVirtualSensor.class);

	private Hashtable<String, ArrayBlockingQueue<StreamElement>> circularBuffers;

	private int windowSize = -1;
	private int stepSize = -1;

	public RConnection rc;
	public REXP xp;

	public TreeMap<String, String> params = null;
	
//	private DataField[] outStructure;
	private DataChannel outputChannel;

	public RVirtualSensor(OperatorConfig config,DataChannel outputChannel ) {
		this.outputChannel = outputChannel;
		
		circularBuffers = new Hashtable<String, ArrayBlockingQueue<StreamElement>>();
		// Get the parameters from the XML configuration file

		windowSize  = config.getParameters().getValueAsIntWithException(WINDOW_SIZE);
		stype = config.getParameters().getValueWithDefault("script_type", "computation");
		stepSize =  config.getParameters().getValueAsIntWithException(STEP_SIZE);
		
		if (windowSize < stepSize) 
			throw new RuntimeException("The parameter " + WINDOW_SIZE + " must be greater or equal to the parameter " + STEP_SIZE);
//		outStructure = config.getOutputFormat();
	}

	public void process(String inputStreamName,StreamElement streamElement) {
		ArrayBlockingQueue<StreamElement> circularBuffer = circularBuffers
				.get(inputStreamName);

		// Get the circular buffer that matches the input stream. Create a new
		// one
		// if none exists
		if (circularBuffer == null) {
			circularBuffer = new ArrayBlockingQueue<StreamElement>(windowSize);
			circularBuffers.put(inputStreamName, circularBuffer);
		}
		try {
			circularBuffer.put(streamElement);

			logger.debug("Window for " + inputStreamName + " contains: "
					+ circularBuffer.size() + " of " + windowSize);

			if (circularBuffer.size() == windowSize) {
				logger.info("Window for " + inputStreamName + " contains: "
						+ circularBuffer.size() + " of " + windowSize);

				// Connect to Rserve and assign global variables
				try {
					rc = new RConnection(params.get(SERVER), Integer
							.parseInt(params.get(PORT)));

					logger.info("Connected to R server " + params.get(SERVER)
							+ ":" + params.get(PORT));

					String[] fieldname = streamElement.getFieldNames();

					logger.info("Sending " + fieldname.length
							+ " data attributes to R server.");

					// Assign R vector variables prior the script
					for (int n = 0; n < fieldname.length; n++) {
						// Build the window
						double[] values = new double[windowSize];
						StreamElement elt = null;

						// convert the circular buffer to an array
						Object[] elts = circularBuffer.toArray();
						for (int i = 0; i < elts.length; i++) {
							elt = (StreamElement) elts[i];
//							values[i] = ((Number) elt.getValue()[n])
//									.doubleValue(); //
						}

						// assign vectors as R variables
						rc.assign("gsn_" + fieldname[n].toLowerCase(), values);
					}

					logger.info("Done.");

					// read the R script
					// open the script file every time we do some processing
					// (this can be
					// improved).
					File file = new File(params.get(SCRIPT).toString());
					script = FileUtils.readFileToString(file, "UTF-8");

					logger.info("Sending R script.");

					// evaluate the R script
					rc.voidEval(script);
					logger.info("Done.");

					// get the output timestamp
					logger
							.info("Performing computation in R server (please wait).");

					// collect vector values after computation
				
//					String[] plotFieldName = new String[outStructure.length];
//					Byte[] plotFieldType = new Byte[outStructure.length];
//
//					for (int w = 0; w < outStructure.length; w++) {
//						plotFieldName[w] = outStructure[w].getName();
//						plotFieldType[w] = outStructure[w].getDataTypeID();
//					}

					Serializable[] outputData = null;
					StreamElement se = null;

//					Byte[] fieldType = streamElement.getFieldTypes();

					// check if we have defined more attributes in the output
					// structure
//					if (outStructure.length > fieldname.length) {
//						outputData = new Serializable[outStructure.length];
//					} else {
//						outputData = new Serializable[fieldname.length];
//					}

//					for (int n = 0; n < fieldname.length; n++) {
						// evaluate/get attribute data from R server
//						xp = rc.parseAndEval(fieldname[n].toLowerCase());

//						if (fieldType[n] == DataTypes.NUMERIC) {
//							double[] b1 = xp.asDoubles();
//							outputData[n] = b1[b1.length - 1];
//						}
//
//						if (fieldType[n] == DataTypes.NUMERIC) {
//							int[] b1 = xp.asIntegers();
//							outputData[n] = b1[b1.length - 1];
//						}
//
//						if (fieldType[n] == DataTypes.TIME) {
//							int[] b1 = xp.asIntegers();
//							outputData[n] = (long) b1[b1.length - 1];
//						}
//					}

//					int len1 = outStructure.length;
//					int len2 = fieldname.length;
//
//					// check if we have defined more attributes in the output
//					// structure
//					if (len1 > len2) {
//						if (stype.equals("plot")) {
//							xp = rc.parseAndEval("gsn_plot");
//							outputData[len2] = xp.asBytes();
//
////							se = new StreamElement(plotFieldName,
////									plotFieldType, outputData);
//						}
//					} else {
//						se = new StreamElement(fieldname, fieldType, outputData);
//					}
//
//					logger.info("Computation finished.");
//
//					outputChannel.write(se);
//					logger.debug("Stream published: "
//							+ se.toString().toLowerCase());
//
//					// Close connection to R server
//					rc.close();
//					logger.info("Connection to R server closed.");

				} catch (Exception e) {
					logger.warn(e);
					// Close connection to R server
					logger.info("Connection to R server closed.");
					rc.close();
				}

				// Remove step size elements from the beginning of the buffer
				for (int i = 0; i < stepSize; i++) {
					try {
						circularBuffer.take();
					} catch (InterruptedException e) {
						logger.warn(e.getMessage(), e);
					}
				}

			}

			// end if if for window
		} catch (InterruptedException e) {
			logger.warn(e.getMessage(), e);
		}

	}

	public void dispose() {

	}

}
