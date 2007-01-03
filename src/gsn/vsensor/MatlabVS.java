package gsn.vsensor;

import java.io.IOException;
import java.io.Serializable;
import org.apache.log4j.Logger;

import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.utils.MatlabEngine;


/**
 * @author Jérôme Rousselot (jeromerousselot@gmail.com)
 *
 */
public class MatlabVS extends AbstractVirtualSensor {

	private final static transient Logger      logger         = Logger.getLogger( AbstractVirtualSensor.class );
	private MatlabEngine engine;
	private Integer threshold = new Integer(42);
	
	private String[] fieldNames = {"Matlab_Result"};
	private Byte[] fieldTypes = {DataTypes.INTEGER};
	
	/* (non-Javadoc)
	 * @see gsn.vsensor.AbstractVirtualSensor#dataAvailable(java.lang.String, gsn.beans.StreamElement)
	 */
	@Override
	public void dataAvailable(String inputStreamName,
			StreamElement streamElement) {
		Integer a = (Integer) streamElement.getData("A_VALUE");
		Integer b = (Integer) streamElement.getData("B_VALUE");
		Integer answer;
		try {
			engine.evalString("myMatlabFunction("+a.toString() + ","+b.toString()+")");
			answer = Integer.parseInt(engine.getOutputString(10));
			if(answer.compareTo(threshold) > 0) {
				StreamElement command = new StreamElement(fieldNames, fieldTypes , new Serializable[] {answer});
			}
		} catch (IOException e) {
			logger.warn(e);
		}

		
	}

	/* (non-Javadoc)
	 * @see gsn.vsensor.AbstractVirtualSensor#finalize()
	 */
	@Override
	public void finalize() {
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
        engine = new MatlabEngine();
        try {
                // Matlab start command:
                engine.open("matlab -nosplash -nojvm");
                // Display output:
                if(logger.isDebugEnabled())
                	logger.debug(engine.getOutputString(500));
                success = true;
        }
        catch (Exception e) {
                logger.warn(e);
        }
		
		
		return success;
	}

}
