package gsn.vsensor;

import java.io.Serializable;
import org.apache.log4j.Logger;
import gsn.beans.DataField;
import gsn.beans.StreamElement;

public class CONO2VirtualSensor extends BridgeVirtualSensorPermasense {
	
	private static final transient Logger logger = Logger.getLogger(CONO2VirtualSensor.class);


	private static DataField[] cono2DataField = {	
						new DataField("RESISTANCE_CO", "INTEGER"),
						new DataField("RESISTANCE_NO2", "INTEGER"),
						new DataField("TEMPERATURE", "DOUBLE"),
						new DataField("HUMIDITY", "INTEGER"),
						new DataField("MEASUREMENT_ID", "BIGINT")
  };

	@Override
	public boolean initialize() {
		
		return super.initialize();
	}

	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
		try {
		    
		    // Convert the coded values in the string to hex values
		    String str = new String((String)data.getData("RAW_DATA"));
		    
		    if (str.length() != 18) {
		      logger.warn("Input size " + str.length() + " instead of 18");
		      return;
		    }
		    
		    for (int i = 0; i < str.length(); i++) {
		      char c = str.charAt(i);
		      if (c > 57 && c < 64) {
		        char r = (char)((int)c + 7);
		        str = replaceCharAt(str,i,r);
		      }
		    }
		    
				// split string:
				// pos 6-8: resistance CO
				// pos 9-11: resistance NO2
				// pos 12-14: temp
				// pos 15-16: humidity
				int r1= Integer.parseInt(str.substring(6,9),16);
				int r2= Integer.parseInt(str.substring(9,12),16);
				int _t= Integer.parseInt(str.substring(12,15),16);
				double t = _t/10-40;
				int h= Integer.parseInt(str.substring(15,17),16);
				
				data = new StreamElement(data, cono2DataField, new Serializable[] {r1, r2, t, h, data.getData("MEASUREMENT_ID")});
				super.dataAvailable(inputStreamName, data);
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private static String replaceCharAt(String s, int pos, char c) {
		return s.substring(0,pos) + c + s.substring(pos+1);
	}
}
