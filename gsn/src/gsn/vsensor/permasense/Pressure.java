package gsn.vsensor.permasense;

import java.io.Serializable;
import java.text.DecimalFormat;

public class Pressure implements Converter {
	
	private static final DecimalFormat decimal1 = new DecimalFormat("0.0");
	
	
	public String convert(Serializable signal_name, String value, Serializable input) {
		if (signal_name == null)
			return null;
		
		String result = null;
		int v = ((Integer) signal_name).intValue();
		if (v <= 64000) {
			result = decimal1.format((v / 64000.0) * 5000.0);
		}
		return result;
	}
	
}
