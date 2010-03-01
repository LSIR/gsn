package gsn.vsensor.permasense;

import java.io.Serializable;
import java.text.DecimalFormat;

import org.apache.log4j.Logger;

public class Selfpotential implements Converter {
	
	private static final DecimalFormat decimal3 = new DecimalFormat("0.000");
	
	private static final transient Logger logger = Logger.getLogger(Selfpotential.class);
	
	
	public String convert(Serializable input, String value) {
		String result = null;
		//long start = System.nanoTime();
		int v = ((Integer) input).intValue();
		if (v <= 64000) {
			result = decimal3.format(v * 2500.0 / 64000.0);
		}
		//if (logger.isDebugEnabled())
		//	logger.debug("dilatationConversion: " + Long.toString((System.nanoTime() - start) / 1000) + " us");
		return result;
	}
	
}
