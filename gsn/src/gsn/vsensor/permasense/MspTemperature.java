package gsn.vsensor.permasense;

import java.io.Serializable;
import java.text.DecimalFormat;

import org.apache.log4j.Logger;

public class MspTemperature implements Converter {
	
	private static final DecimalFormat decimal3 = new DecimalFormat("0.000");
	
	private static final transient Logger logger = Logger.getLogger(MspTemperature.class);
	
	
	public String convert(Serializable input, String value) {
		int v = ((Integer) input).intValue();
		if (v == 65535)
			return null;
		else
			return decimal3.format((new Double(v) * (1.5/4095) - 0.986) / 0.00355);
	}
	
}
