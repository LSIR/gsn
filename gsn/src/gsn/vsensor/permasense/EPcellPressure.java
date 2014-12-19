package gsn.vsensor.permasense;

import java.io.Serializable;
import java.text.DecimalFormat;

import org.apache.log4j.Logger;

public class EPcellPressure implements Converter {
	
	private static final DecimalFormat decimal1 = new DecimalFormat("0.0");
	
	private static final transient Logger logger = Logger.getLogger(EPcellPressure.class);
	
	
	public String convert(Serializable input, String value) {
		String result = null;
		//long start = System.nanoTime();
		int v = ((Integer) input).intValue();
		if (v <= 64000) {
			result = decimal1.format(v * 2.5 * 2 / 1000);
		}
		//if (logger.isDebugEnabled())
		//	logger.debug("pressureConversion: " + Long.toString((System.nanoTime() - start) / 1000) + " us");
		return result;
	}
	
}
