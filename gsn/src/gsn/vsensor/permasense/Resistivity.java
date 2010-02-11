package gsn.vsensor.permasense;

import java.io.Serializable;
import java.text.DecimalFormat;

import org.apache.log4j.Logger;

public class Resistivity implements Converter {
	
	private static final DecimalFormat decimal4 = new DecimalFormat("0.0000");
	
	private static final transient Logger logger = Logger.getLogger(Resistivity.class);
	
	
	public String convert(Serializable input, String value) {
		String result = null;
		long start = System.nanoTime();
		int v = ((Integer) input).intValue();
		if (v <= 64000 && v != 0) {
			result = decimal4.format(1000000.0 * ((64000.0 / v) - 1.0));
		}
		if (logger.isDebugEnabled())
			logger.debug("resistivityConversion: " + Long.toString((System.nanoTime() - start) / 1000) + " us");				
		return result;
	}

}
