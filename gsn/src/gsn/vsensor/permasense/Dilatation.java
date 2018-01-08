package gsn.vsensor.permasense;

import java.io.Serializable;
import java.text.DecimalFormat;

public class Dilatation implements Converter {
	
	private static final DecimalFormat decimal4 = new DecimalFormat("0.0000");
	
	
	public String convert(Serializable signal_name, String value, Serializable input) {
		if (signal_name == null)
			return null;
		
		String result = null;
		int v = ((Integer) signal_name).intValue();
		if (v <= 64000) {
			result = decimal4.format((v / 64000.0) * Double.parseDouble(value));
		}
		return result;
	}
	
}
