package gsn.vsensor;

import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.vsensor.AbstractVirtualSensor;

import java.awt.Image;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import org.apache.log4j.Logger;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.Rserve.RConnection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class RVirtualSensor extends AbstractVirtualSensor {

	private static final String WINDOW_SIZE = "window_size";
	private static final String STEP_SIZE = "step_size";
  private static final String INPUT_DATA = "input_data";
  private static final String INPUT_TIMESTAMPS = "input_timestamps";
  private static final String OUTPUT_DATA = "output_data";
  private static final String OUTPUT_PLOT = "output_plot";  
  private static final String OUTPUT_TIMESTAMP = "output_timestamp";    
  private static final String SCRIPT = "script"; 
  private static final String SERVER = "server"; 
  
	private static transient Logger logger = Logger.getLogger ( RVirtualSensor.class );

	private Hashtable<String, ArrayBlockingQueue<StreamElement>> circularBuffers;

	private int windowSize = -1;

	private int stepSize = -1;

  private static final String [] fieldNames = new String [] { OUTPUT_DATA, OUTPUT_PLOT};
  
  private static final Byte [ ] fieldTypes = new Byte [ ] { DataTypes.DOUBLE, DataTypes.BINARY};
  
  private Serializable [ ] outputData = new Serializable [ fieldNames.length ];	

  public RConnection rc;
  public REXP xp;
  
  public TreeMap <  String , String > params = null;
  
  public String script = null;

	public boolean initialize()
	{	
	  params = getVirtualSensorConfiguration().getMainClassInitialParams();
	  
		circularBuffers = new Hashtable<String, ArrayBlockingQueue<StreamElement>> () ;
		// Get the parameters from the XML configuration file

		String param = null;
		param = params.get(WINDOW_SIZE);
		
		if (param != null)
		{
		  windowSize = Integer.parseInt(param) ;
		}
		else
		{
			logger.error("The required parameter: >" + WINDOW_SIZE + "<+ is missing.from the virtual sensor configuration file.");
			return false;
		}
		param = params.get(STEP_SIZE);
		
		if (param != null)
		{
		  stepSize = Integer.parseInt(param) ;
		}
		else
		{
			logger.error("The required parameter: >" + STEP_SIZE + "<+ is missing.from the virtual sensor configuration file.");
			return false;
		}
		if (windowSize < stepSize)
		{
			logger.error("The parameter " + WINDOW_SIZE + " must be greater or equal to the parameter " + STEP_SIZE);
			return false;
		}
		
    // connect to Rserve and assign global variables
    try
    {
      rc = new RConnection(params.get(SERVER));
      
      rc.assign(WINDOW_SIZE, params.get(WINDOW_SIZE).toString());
      rc.assign(STEP_SIZE, params.get(STEP_SIZE).toString() );         
      
      rc.voidEval("window_size <- as.numeric(window_size);");
      rc.voidEval("step_size <- as.numeric(step_size);");      
    }
    catch (Exception e)
    { 
      logger.warn(e);
    }		
		
		return init();
	}

	public void dataAvailable(String inputStreamName, StreamElement streamElement)
	{
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
				
				// convert the circular buffer to an array
				Object[] elts = circularBuffer.toArray();
				for (int i = 0 ; i < elts.length ; i++) {
					elt = (StreamElement) elts[i];
					values[i] = ((Number)elt.getData()[0]).doubleValue() ; //
					timestamps[i] = new Long(elt.getTimeStamp());
				}

				// Remove step size elements from the beginning of the buffer
				for (int i = 0 ; i < stepSize ; i++) {
					try {
						circularBuffer.take();
					} catch (InterruptedException e) {
						logger.warn(e.getMessage(), e) ;
					}
				}
				
				// the the data processing (e.g. run the algorithm on the data)
				process(values, timestamps);
			}		
		} catch (InterruptedException e) {
			logger.warn (e.getMessage(), e) ;
		}
	}

  public void finalize()
  {
    // close RConnection
    rc.close();
  }

	public int getPredicateValueAsIntWithException (String parameter) {
		String param = getVirtualSensorConfiguration( ).getMainClassInitialParams( ).get(parameter);
		if (param == null) throw new  java.lang.RuntimeException ("The required parameter: >" + parameter + "<+ is missing.from the virtual sensor configuration file.");
		else return Integer.parseInt(param);
	}

  public boolean init()
  {
    return true;
  }

  // takes an array of data values and their timestamps
  public void process(double [] values , long[] timestampsInMSec)
  {
    int[] r_timestamps = new int[timestampsInMSec.length];
    //long epoch = 0;
    
    //String[] epoch_str = new String[timestampsInMSec.length];
    
    // cast timestamps from long to int
    for (int i = 0 ; i < values.length ; i++)
    {
      r_timestamps[i] = (int)(timestampsInMSec[i] / 1000);
      
      //epoch = r_timestamps[i];
      
      //epoch_str[i] = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date (epoch*1000));
    }
    
    double[] output_data = null;
    long output_timestamp = 0;
    
    // read the R script
    // open the script file everytime we do some processing
    // to allow changes to the R script in run-time
    // very useful for testing the R scripts with real-time data
    try
    {
      File file = new File(params.get(SCRIPT).toString());
      script = FileUtils.readFileToString(file, "UTF-8");

    } catch (Exception e)
    {
        logger.error("Could not open R script. ", e);
    }    
    
    try
    {
      // Assign R vector variables prior the script
      rc.assign(INPUT_DATA, values);
      rc.assign(INPUT_TIMESTAMPS, r_timestamps);
      
      //rc.assign(INPUT_TIMESTAMPS, epoch_str);
      
      // evaluate the R script
      rc.voidEval(script);
      
      // get the output_data
      xp = rc.parseAndEval(OUTPUT_DATA);      
      output_data = xp.asDoubles();
      
      // get the output timestamp
      xp = rc.parseAndEval(OUTPUT_TIMESTAMP);      
      output_timestamp = ((long)xp.asInteger()) * 1000L;
      
      // get the output timestamp
      xp = rc.parseAndEval("output_plot");      
      byte[] image = xp.asBytes();
      
      // create and post the stream element 
      StreamElement se = new StreamElement (fieldNames , fieldTypes , new Serializable []{output_data[0], image}, output_timestamp);
      logger.info("R Proxy - StreamElement produced: " + se);
      dataProduced( se );
      
    } 
    catch (Exception e)
    {
      // Error: output timestamp is not set in your R script.
      logger.error(e);
    }
    
  }
  
}
