package gsn.vsensor.permasense;

import java.io.Serializable;
import java.text.DecimalFormat;

public class Thermistor44005 implements Converter {
	
	private static final DecimalFormat decimal4 = new DecimalFormat("0.0000");
	
	
	public String convert(Serializable signal_name, String value, Serializable input) {
		if (signal_name == null)
			return null;
		
		String result = null;
		int v = ((Integer) signal_name).intValue();
		if (v < 64000 && v != 0) {
			double cal = 0.0;
			if (value != null)
				cal = Double.parseDouble(value);
			double ln_res = Math.log(10000.0 / ((64000.0 / v) - 1.0));
			//Math.pow(v, 3.0) needs more CPU instructions than (v * v * v)
			//double steinhart_eq = 0.0014051 + 0.0002369 * ln_res + 0.0000001019 * Math.pow(ln_res, 3);
			double tmp = 0.0014051 + (0.0002369 * ln_res) + (0.0000001019 * (ln_res * ln_res * ln_res));
			result = decimal4.format((1.0 / tmp) - 273.15 - cal);
		}
		return result;
	}
	
}
