package gsn.vsensor.permasense;

import java.io.Serializable;
import java.text.DecimalFormat;

import org.apache.log4j.Logger;

public class Sht11Temperature implements Converter {
	
	private static final DecimalFormat decimal3 = new DecimalFormat("0.000");
	
	private static final transient Logger logger = Logger.getLogger(Sht11Temperature.class);
	
	
	public String convert(Serializable input, String value) {
		int v = ((Integer) input).intValue();
		if (v == 0xffff)
			return null;
		else
			return decimal3.format(0.01*v - 39.63);
	}
	
}
