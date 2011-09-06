package gsn.vsensor;

import java.io.Serializable;

import org.apache.log4j.Logger;

import gsn.beans.DataField;
import gsn.beans.StreamElement;

public class AlphasenseVirtualSensor extends BridgeVirtualSensorPermasense {
	
	private static final transient Logger logger = Logger.getLogger(AlphasenseVirtualSensor.class);
	
	private static String MESSAGE_TYPE_STR = "message_type";

	private static String STATIC_NAMING_STR = "static";
	private static String DYNAMIC_NAMING_STR  = "dynamic";

	private static int STATIC_NAMING = 1;
	private static int DYNAMIC_NAMING = 2;


	private static DataField[] dynamicDataField = {	
            			new DataField("SENSOR_CURRENT", "DOUBLE"),
						new DataField("SENSOR_PPM_1", "DOUBLE"),
						new DataField("SENSOR_PPM_2", "DOUBLE"),
						new DataField("AMBIENT_TEMP", "DOUBLE"),
						new DataField("OFFSET_COMP", "DOUBLE"),
						new DataField("SENSITIVITY_COMP", "DOUBLE")
	};
  
	private static DataField[] staticDataField = {
						new DataField("PPM_AT_ZERO", "DOUBLE"),
						new DataField("CURRENT_AT_ZERO", "DOUBLE"),
						new DataField("PPM_AT_SPAN", "DOUBLE"),
						new DataField("CURRENT_AT_SPAN", "DOUBLE"),
						new DataField("CALIBRATION_DATE", "VARCHAR(16)")
	};
  
	private int messageType = -1;

	@Override
	public boolean initialize() {
		String type = getVirtualSensorConfiguration().getMainClassInitialParams().get(MESSAGE_TYPE_STR);
		if (type == null) {
			logger.error(MESSAGE_TYPE_STR + " has to be specified");
			return false;
		}
		if (type.equalsIgnoreCase(STATIC_NAMING_STR))
			messageType = STATIC_NAMING;
		else if (type.equalsIgnoreCase(DYNAMIC_NAMING_STR))
			messageType = DYNAMIC_NAMING;
		else {
			logger.error(MESSAGE_TYPE_STR + " has to be " + STATIC_NAMING_STR + " or " + DYNAMIC_NAMING_STR);
			return false;
		}
		
		return super.initialize();
	}

	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
		try {
			short type = (Short) data.getData("MESSAGE_TYPE");
		    
		    String inputString = new String((String)data.getData("RAW_DATA"));
		    if (inputString.length() != 146 && inputString.length() != 140 && inputString.length() != 137 && inputString.length() != 134 && inputString.length() != 110 && inputString.length() != 131) {
		    	logger.warn("RAW_DATA has wrong length " + inputString.length());
		    	return;
		    }
		    String delims = "[ ]+";
		    String[] tokens = inputString.split(delims);
		    
			if (messageType == STATIC_NAMING && type == STATIC_NAMING) {
				
				String ppmAtZero = new String();
			    String currentAtZero = new String();
			    String ppmAtSpan = new String();
			    String currentAtSpan = new String();
			    StringBuffer calibrationDate = new StringBuffer();
			    
			    for (int i = tokens.length-1; i >= 0; i--) {
			      if (i >= 26 && i <= 33) {
			        int c = Integer.parseInt(tokens[i],16);
			        char chr = (char)c;
			        calibrationDate.append(chr);
			      }
			      else if (i >= 22 && i <= 25)
			        currentAtSpan = currentAtSpan.concat(tokens[i]);
			      else if (i >= 18 && i <= 21)
			        ppmAtSpan = ppmAtSpan.concat(tokens[i]);
			      else if (i >= 14 && i <= 17)
			        currentAtZero = currentAtZero.concat(tokens[i]);
			      else if (i >= 10 && i <= 13)
			        ppmAtZero = ppmAtZero.concat(tokens[i]);
			    }
			    calibrationDate.reverse();
			    
			    Long i;
			    Float _ppmAtZero, _currentAtZero, _ppmAtSpan, _currentAtSpan;

			    i = Long.parseLong(ppmAtZero, 16); 
			    _ppmAtZero = Float.intBitsToFloat(i.intValue()); 
			    i = Long.parseLong(currentAtZero, 16); 
			    _currentAtZero = Float.intBitsToFloat(i.intValue()); 
			    i = Long.parseLong(ppmAtSpan, 16); 
			    _ppmAtSpan = Float.intBitsToFloat(i.intValue()); 
			    i = Long.parseLong(currentAtSpan, 16); 
			    _currentAtSpan = Float.intBitsToFloat(i.intValue()); 
				
			    data = new StreamElement(data, staticDataField, new Serializable[] {_ppmAtZero, _currentAtZero, _ppmAtSpan, _currentAtSpan, calibrationDate.toString()});
			    super.dataAvailable(inputStreamName, data);
			}
			else if (messageType == DYNAMIC_NAMING && type == DYNAMIC_NAMING) {
				
				String sensorCurrent = new String();
			    String sensorPpm1 = new String();
			    String sensorPpm2 = new String();
			    String ambientTemp = new String();
			    String offsetComp = new String();
			    String sensitivityComp = new String();
			    
			    int p = 0;
			    if (inputString.length() == 137)
			      p = 1;
			    else if (inputString.length() == 134)
			      p = 2;
			    else if (inputString.length() == 131)
			      p = 3;

		    	for (int i = tokens.length-1; i >= 0; i--) {
			      if (i >= 14-p && i <= 17-p)
			        sensorCurrent = sensorCurrent.concat(tokens[i]);
			      else if (i >= 18-p && i <= 21-p)
			        sensorPpm1 = sensorPpm1.concat(tokens[i]);
			      else if (i >= 22-p && i <= 25-p)
			        sensorPpm2 = sensorPpm2.concat(tokens[i]);
			      else if (i >= 30-p && i <= 33-p)
			        ambientTemp = ambientTemp.concat(tokens[i]);
			      else if (i >= 34-p && i <= 37-p)
			        offsetComp = offsetComp.concat(tokens[i]);
			      else if (i >= 38-p && i <= 41-p)
			        sensitivityComp = sensitivityComp.concat(tokens[i]);
			    }

			    Long i;
			    Float _sensorCurrent, _sensorPpm1, _sensorPpm2, _ambientTemp, _offsetComp, _sensitivityComp;

			    i = Long.parseLong(sensorCurrent, 16); 
			    _sensorCurrent = Float.intBitsToFloat(i.intValue());
			    if (Float.isNaN(_sensorCurrent))
			    	_sensorCurrent = null;
			    i = Long.parseLong(sensorPpm1, 16); 
			    _sensorPpm1 = Float.intBitsToFloat(i.intValue());
			    if (Float.isNaN(_sensorPpm1))
			    	_sensorPpm1 = null;
			    i = Long.parseLong(sensorPpm2, 16); 
			    _sensorPpm2 = Float.intBitsToFloat(i.intValue());
			    if (Float.isNaN(_sensorPpm2))
			    	_sensorPpm2 = null;
			    i = Long.parseLong(ambientTemp, 16); 
			    _ambientTemp = Float.intBitsToFloat(i.intValue());
			    if (Float.isNaN(_ambientTemp))
			    	_ambientTemp = null;
			    i = Long.parseLong(offsetComp, 16); 
			    _offsetComp = Float.intBitsToFloat(i.intValue());
			    if (Float.isNaN(_offsetComp))
			    	_offsetComp = null;
			    i = Long.parseLong(sensitivityComp, 16); 
			    _sensitivityComp = Float.intBitsToFloat(i.intValue());
			    if (Float.isNaN(_sensitivityComp))
			    	_sensitivityComp = null;
			    
			    data = new StreamElement(data, dynamicDataField, new Serializable[] {_sensorCurrent, _sensorPpm1, _sensorPpm2, _ambientTemp, _offsetComp, _sensitivityComp});
			    super.dataAvailable(inputStreamName, data);
			}
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}
