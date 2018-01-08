package gsn.vsensor.permasense;

import java.io.Serializable;
import java.text.DecimalFormat;

public class Voltage implements Converter {
	
	private static final DecimalFormat decimal3 = new DecimalFormat("0.000");
	
	
	public String convert(Serializable signal_name, String value, Serializable input) {
		if (signal_name == null)
			return null;
		
		String result = null;
		int v = ((Integer) signal_name).intValue();
		result = decimal3.format(Double.parseDouble(value) * v);
		return result;
	}
	
}
