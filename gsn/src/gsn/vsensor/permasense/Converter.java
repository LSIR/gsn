package gsn.vsensor.permasense;

import java.io.Serializable;

public interface Converter {
	
	public String convert(Serializable signal_name, String value, Serializable input);
	
}
