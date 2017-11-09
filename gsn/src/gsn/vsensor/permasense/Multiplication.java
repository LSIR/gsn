package gsn.vsensor.permasense;

import java.io.Serializable;
import java.text.DecimalFormat;

import org.apache.log4j.Logger;

public class Multiplication implements Converter {
	
	private static final DecimalFormat decimal3 = new DecimalFormat("0.000");
	
	private static final transient Logger logger = Logger.getLogger(Multiplication.class);
	
	
	public String convert(Serializable input, String value) {
		int v = ((Integer) input).intValue();
		if (value.trim().isEmpty() || v == 0xffff)
			return null;
		else
			return decimal3.format(Double.parseDouble(value) * v);
	}
	
}
