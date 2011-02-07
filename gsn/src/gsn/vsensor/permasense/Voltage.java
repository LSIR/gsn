package gsn.vsensor.permasense;

import java.io.Serializable;
import java.text.DecimalFormat;

import org.apache.log4j.Logger;

public class Voltage implements Converter {
	
	private static final DecimalFormat decimal3 = new DecimalFormat("0.000");
	
	private static final transient Logger logger = Logger.getLogger(Voltage.class);
	
	
	public String convert(Serializable input, String value) {
		String result = null;
		//long start = System.nanoTime();
		int v = ((Integer) input).intValue();
		result = decimal3.format(Double.parseDouble(value) * v);
		//if (logger.isDebugEnabled())
		//	logger.debug("voltageConversion: " + Long.toString((System.nanoTime() - start) / 1000) + " us");
		return result;
	}
	
}
