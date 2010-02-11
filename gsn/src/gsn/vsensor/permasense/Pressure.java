package gsn.vsensor.permasense;

import java.io.Serializable;
import java.text.DecimalFormat;

import org.apache.log4j.Logger;

public class Pressure implements Converter {
	
	private static final DecimalFormat decimal1 = new DecimalFormat("0.0");
	
	private static final transient Logger logger = Logger.getLogger(Pressure.class);
	
	
	public String convert(Serializable input, String value) {
		String result = null;
		//long start = System.nanoTime();
		int v = ((Integer) input).intValue();
		if (v <= 64000) {
			result = decimal1.format((v / 64000.0) * 5000.0);
		}
		//if (logger.isDebugEnabled())
		//	logger.debug("pressureConversion: " + Long.toString((System.nanoTime() - start) / 1000) + " us");
		return result;
	}
	
}
