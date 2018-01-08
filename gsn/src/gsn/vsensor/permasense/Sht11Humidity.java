package gsn.vsensor.permasense;

import java.io.Serializable;
import java.text.DecimalFormat;

public class Sht11Humidity implements Converter {
	
	private static final DecimalFormat decimal3 = new DecimalFormat("0.000");

	@Override
	public String convert(Serializable signal_name, String value, Serializable input) {
		if (signal_name == null || input == null)
			return null;
		
		int v = ((Integer) signal_name).intValue();
		if (v == 0xffff)
			return null;
		else {
			int i = ((Integer) input).intValue();
			if (i == 0xffff)
				return null;
			else
				return decimal3.format(((0.01*v)-64.63)*(0.01+(0.00008*i))+((0.0405*i)-4-(0.0000028*i*i)));
		}
	}

}
