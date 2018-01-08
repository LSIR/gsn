package gsn.vsensor.permasense;

import java.io.Serializable;
import java.text.DecimalFormat;

public class Multiplication implements Converter {
	
	private static final DecimalFormat decimal3 = new DecimalFormat("0.000");
	
	
	public String convert(Serializable signal_name, String value, Serializable input) {
		if (signal_name == null)
			return null;
		
		int v = ((Integer) signal_name).intValue();
		if (value.trim().isEmpty() || v == 0xffff)
			return null;
		else
			return decimal3.format(Double.parseDouble(value) * v);
	}
	
}
