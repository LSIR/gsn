package gsn.vsensor.permasense;

import java.io.Serializable;
import java.text.DecimalFormat;

public class Sht21Humidity implements Converter {
	
	private static final DecimalFormat decimal3 = new DecimalFormat("0.000");

	@Override
	public String convert(Serializable signal_name, String value, Serializable input) {
		if (signal_name == null)
			return null;
		
		int v = ((Integer) signal_name).intValue();
		if (v == 0xffff)
			return null;
		else
			return decimal3.format(-6.0 + 125.0 * v / 4096);
	}

}
