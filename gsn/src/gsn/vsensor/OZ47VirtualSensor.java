package gsn.vsensor;

import java.io.Serializable;

import org.apache.log4j.Logger;

import gsn.beans.DataField;
import gsn.beans.StreamElement;

public class OZ47VirtualSensor extends BridgeVirtualSensorPermasense {
	
	private static final transient Logger logger = Logger.getLogger(OZ47VirtualSensor.class);
	
	private static String MESSAGE_TYPE_STR = "message_type";

	private static String STATIC_NAMING_STR = "static";
	private static String DYNAMIC_NAMING_STR  = "dynamic";

	private static int STATIC_NAMING = 1;
	private static int DYNAMIC_NAMING = 2;


	private static DataField[] dynamicDataField = {	
            			new DataField("OZONE_SENSOR_1", "INTEGER"),
						new DataField("OZONE_SENSOR_2", "INTEGER"),
						new DataField("RESISTANCE_1", "INTEGER"),
						new DataField("RESISTANCE_2", "INTEGER"),
						new DataField("TEMPERATURE", "DOUBLE"),
						new DataField("HUMIDITY", "INTEGER")
  };
  
  private static DataField[] staticDataField = {
						new DataField("LOT_NO", "INTEGER"),
						new DataField("CELL_NO", "INTEGER"),
						new DataField("CALIB_WEEK", "INTEGER"),
						new DataField("CALIB_YEAR", "INTEGER"),
						new DataField("TIMER_PRE_HEAT", "INTEGER"),
						new DataField("AUTO_CALIB", "INTEGER"),
						new DataField("TIMER_DELAY", "INTEGER"),
						new DataField("TIMER_CYCLE", "INTEGER"),
						new DataField("TIMER_PULSE", "INTEGER"),
						new DataField("OFFSET_MEM", "INTEGER"),
						new DataField("OFFSET_VERIF", "INTEGER"),
						new DataField("OFFSET", "INTEGER"),
						new DataField("CALIB_PARAM_X0_1", "DOUBLE"),
						new DataField("CALIB_PARAM_X1_1", "DOUBLE"),
						new DataField("CALIB_PARAM_X2_1", "DOUBLE"),
						new DataField("CALIB_PARAM_X3_1", "DOUBLE"),
						new DataField("CALIB_PARAM_KT_1", "DOUBLE"),
						new DataField("CALIB_PARAM_X0_2", "DOUBLE"),
						new DataField("CALIB_PARAM_X1_2", "DOUBLE"),
						new DataField("CALIB_PARAM_X2_2", "DOUBLE"),
						new DataField("CALIB_PARAM_X3_2", "DOUBLE"),
						new DataField("CALIB_PARAM_KT_2", "DOUBLE")
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
		    
		    // Convert the coded values in the string to hex values
		    String str = new String((String)data.getData("RAW_DATA"));
		    for (int i = 0; i < str.length(); i++) {
		      char c = str.charAt(i);
		      if (c > 57 && c < 64) {
		        char r = (char)((int)c + 7);
		        str = replaceCharAt(str,i,r);
		      }
		    }
		    
			if (messageType == STATIC_NAMING && type == STATIC_NAMING) {
				int lotNo = Integer.parseInt(str.substring(4,6),16);
				int cellNo = Integer.parseInt(str.substring(6,8),16);
				int calibWeek = Integer.parseInt(str.substring(8,10),16);
				int calibYear = Integer.parseInt(str.substring(10,12),16);
				int gTimerPreHeat = Integer.parseInt(str.substring(17,19),16);
				int gAutoCalib = Integer.parseInt(str.substring(19,21),16);
				int gTimerDelay = Integer.parseInt(str.substring(30,32),16);
				int gTimerCycle = Integer.parseInt(str.substring(32,36),16);
				int gTimerPulse = Integer.parseInt(str.substring(36,38),16);
				int offsetMem = Integer.parseInt(str.substring(43,45),16) - 127;
				int offsetVerif = Integer.parseInt(str.substring(45,47),16) - 127;
				int gOffset = Integer.parseInt(str.substring(56,58),16) - 127;
				
				long v = Long.parseLong(str.substring(69,77),16);
				double x0_1 = hex2double(v);
				v = Long.parseLong(str.substring(82,90),16);
				double x1_1 = hex2double(v);
				v = Long.parseLong(str.substring(95,103),16);
				double x2_1 = hex2double(v);
				v = Long.parseLong(str.substring(108,116),16);
				double x3_1 = hex2double(v);
				v = Long.parseLong(str.substring(121,129),16);
				double kt_1 = hex2double(v);
				v = Long.parseLong(str.substring(134,142),16);
				double x0_2 = hex2double(v);
				v = Long.parseLong(str.substring(147,155),16);
				double x1_2 = hex2double(v);
				v = Long.parseLong(str.substring(160,168),16);
				double x2_2 = hex2double(v);
				v = Long.parseLong(str.substring(173,181),16);
				double x3_2 = hex2double(v);
				v = Long.parseLong(str.substring(186,194),16);
				double kt_2 = hex2double(v);
				
				data = new StreamElement(data, staticDataField, new Serializable[] {lotNo, cellNo, calibWeek, calibYear, gTimerPreHeat, gAutoCalib, gTimerDelay, gTimerCycle, gTimerPulse, offsetMem, offsetVerif, gOffset, x0_1, x1_1, x2_1, x3_1, kt_1, x0_2, x1_2, x2_2, x3_2, kt_2});
			}
			else if (messageType == DYNAMIC_NAMING && type == DYNAMIC_NAMING) {
				// split string:
				// pos 2-3: sensor 1
				// pos 4-5: sensor 2
				// pos 6-8: resistance 1
				// pos 9-11: resistance 2
				// pos 12-14: temp
				// pos 15-16: humidity
				int s1= Integer.parseInt(str.substring(2,4),16);
				int s2= Integer.parseInt(str.substring(4,6),16);
				int r1= Integer.parseInt(str.substring(6,9),16);
				int r2= Integer.parseInt(str.substring(9,12),16);
				int _t= Integer.parseInt(str.substring(12,15),16);
				double t = _t/10-40;
				int h= Integer.parseInt(str.substring(15,17),16);
	
				data = new StreamElement(data, dynamicDataField, new Serializable[] {s1, s2, r1, r2, t, h});
	
				super.dataAvailable(inputStreamName, data);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private static String replaceCharAt(String s, int pos, char c) {
		return s.substring(0,pos) + c + s.substring(pos+1);
	}
	  
	private static double  hex2double(long valueHex) {
		double exp = (double)((valueHex >>23 & 255)-127);
		double sign = (double)(valueHex>>31);

		if (sign == 0)
			sign = 1;

		double man = ((double)1.0f + (double)((valueHex & 8388607)/(double)8388608));
		double expVal = Math.pow(2.0,exp);
		double ans = sign * expVal * man;
		return ans;
	}
}
