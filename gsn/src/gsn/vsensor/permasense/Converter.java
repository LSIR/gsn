package gsn.vsensor.permasense;

import java.io.Serializable;

public interface Converter {
	
	public String convert(Serializable input, String value);
	
}
