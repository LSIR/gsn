package gsn.vsensor.permasense;

import java.io.Serializable;
import java.text.DecimalFormat;

import org.apache.log4j.Logger;

public class RotationX implements Converter {
	
	private static final DecimalFormat decimal3 = new DecimalFormat("0.000");

	private static final transient Logger logger = Logger.getLogger(RotationX.class);

	@Override
	public String convert(Serializable signal_name, String value, Serializable input) {
		if (signal_name == null || input == null)
			return null;
		Double x, y;

		if (signal_name instanceof Integer ||
				signal_name instanceof Double ||
				signal_name instanceof Float ||
				signal_name instanceof Short)
			x = (Double) signal_name;
		else if (signal_name instanceof String)
			x = Double.parseDouble((String)signal_name);
		else {
			logger.error("signal_name is not of supported type");
			return null;
		}
		
		if (input instanceof Integer)
			y = (Double) input;
		else if (input instanceof String)
			y = Double.parseDouble((String)input);
		else {
			logger.error("input is not of supported type");
			return null;
		}
		
		double a;
		try { a = Double.parseDouble(value); } catch (NumberFormatException e) { return null; }
		
		return decimal3.format(x * Math.cos(Math.toRadians(a)) - y * Math.sin(Math.toRadians(a)));
	}

}
