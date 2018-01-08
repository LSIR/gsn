package gsn.vsensor.permasense;

import java.io.Serializable;
import java.text.DecimalFormat;

public class Selfpotential implements Converter {
	
	private static final DecimalFormat decimal3 = new DecimalFormat("0.000");
	
	
	public String convert(Serializable signal_name, String value, Serializable input) {
		if (signal_name == null)
			return null;
		
		String result = null;
		int v = ((Integer) signal_name).intValue();
		if (v <= 64000) {
			result = decimal3.format(v * 320.0 / 64000.0);
		}
		return result;
	}
	
}
