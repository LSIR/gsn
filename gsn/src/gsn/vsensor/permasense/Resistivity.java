package gsn.vsensor.permasense;

import java.io.Serializable;
import java.text.DecimalFormat;

public class Resistivity implements Converter {
	
	private static final DecimalFormat decimal3 = new DecimalFormat("0.000");
	
	
	public String convert(Serializable signal_name, String value, Serializable input) {
		if (signal_name == null)
			return null;
		
		String result = null;
		int v = ((Integer) signal_name).intValue();
		if (v <= 64000 && v != 0) {
			result = decimal3.format(((64000.0 / v) - 1.0));
		}		
		return result;
	}

}
