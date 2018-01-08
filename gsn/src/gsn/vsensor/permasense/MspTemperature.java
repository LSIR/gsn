package gsn.vsensor.permasense;

import java.io.Serializable;
import java.text.DecimalFormat;

public class MspTemperature implements Converter {
	
	private static final DecimalFormat decimal3 = new DecimalFormat("0.000");
	
	
	public String convert(Serializable signal_name, String value, Serializable input) {
		if (signal_name == null)
			return null;
		
		int v = ((Integer) signal_name).intValue();
		if (v == 65535)
			return null;
		else
			return decimal3.format((new Double(v) * (1.5/4095) - 0.986) / 0.00355);
	}
	
}
