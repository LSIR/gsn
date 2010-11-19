package gsn.vsensor;

import java.io.Serializable;

import gsn.beans.DataField;
import gsn.beans.StreamElement;

import org.apache.log4j.Logger;

public class VaisalaDemuxBridgeVirtualSensor extends BridgeVirtualSensorPermasense {
	
	private static final transient Logger logger = Logger.getLogger(VaisalaDemuxBridgeVirtualSensor.class);

	private static final DataField[] dataField = {	new DataField("TIMESTAMP", "BIGINT"),
						new DataField("GENERATION_TIME", "BIGINT"),
						new DataField("DEVICE_ID", "INTEGER"),
						new DataField("POSITION", "INTEGER"),
						new DataField("VAISALAWXT520_ADDRESS", "INTEGER"),
						new DataField("WIND_DIRECTION_MINIMUM", "VARCHAR(30)"),
						new DataField("WIND_DIRECTION_AVERAGE", "VARCHAR(30)"),
						new DataField("WIND_DIRECTION_MAXIMUM", "VARCHAR(30)"),
						new DataField("WIND_SPEED_MINIMUM", "VARCHAR(30)"),
						new DataField("WIND_SPEED_AVERAGE", "VARCHAR(30)"),
						new DataField("WIND_SPEED_MAXIMUM", "VARCHAR(30)"),
						new DataField("AIR_TEMPERATURE", "VARCHAR(30)"),
						new DataField("INTERNAL_TEMPERATURE", "VARCHAR(30)"),
						new DataField("RELATIVE_HUMIDITY", "VARCHAR(30)"),
						new DataField("AIR_PRESSURE", "VARCHAR(30)"),
						new DataField("RAIN_ACCUMULATION", "VARCHAR(30)"),
						new DataField("RAIN_DURATION", "VARCHAR(30)"),
						new DataField("RAIN_INTENSITY", "VARCHAR(30)"),
						new DataField("HAIL_ACCUMULATION", "VARCHAR(30)"),
						new DataField("HAIL_DURATION", "VARCHAR(30)"),
						new DataField("HAIL_INTENSITY", "VARCHAR(30)"),
						new DataField("RAIN_PEAK_INTENSITY", "VARCHAR(30)"),
						new DataField("HAIL_PEAK_INTENSITY", "VARCHAR(30)"),
						new DataField("HEATING_TEMPERATURE", "VARCHAR(30)"),
						new DataField("HEATING_VOLTAGE", "VARCHAR(30)"),
						new DataField("HEATING_MODE", "VARCHAR(1)"),
						new DataField("SUPPLY_VOLTAGE", "VARCHAR(30)"),
						new DataField("REF_VOLTAGE", "VARCHAR(30)")};
	
	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
		Serializable[] serialized_data = new Serializable[dataField.length];
		
		serialized_data[0] = data.getData(dataField[0].getName());
		serialized_data[1] = data.getData(dataField[1].getName());
		serialized_data[2] = data.getData(dataField[2].getName());
		serialized_data[3] = data.getData(dataField[3].getName());

		String[] r1 = ((String) data.getData("wu")).split(",");
		String[] r2 = ((String) data.getData("tu")).split(",");
		String[] r3 = ((String) data.getData("ru")).split(",");
		String[] r5 = ((String) data.getData("su")).split(",");

		int index = 5;
		try {
			serialized_data[4] = Integer.parseInt(r1[0].split("R")[0]);
			
			// check input correctness
			if (Integer.parseInt(r1[0].split("R")[1]) != 1)
				throw new Exception("wu should start with #R1:timestamp=" + data.getTimeStamp());
			if (Integer.parseInt(r2[0].split("R")[1]) != 2)
				throw new Exception("tu should start with #R2:timestamp=" + data.getTimeStamp());
			if (Integer.parseInt(r3[0].split("R")[1]) != 3)
				throw new Exception("ru should start with #R3:timestamp=" + data.getTimeStamp());
			if (Integer.parseInt(r5[0].split("R")[1]) != 5)
				throw new Exception("su should start with #R5:timestamp=" + data.getTimeStamp());
			
			for (int i=1; i<r1.length; i++) {
				if (r1[i].endsWith("#"))
					serialized_data[index++] = null;
				else
					serialized_data[index++] = r1[i].substring(3, r1[i].length()-1);
			}
			for (int i=1; i<r2.length; i++) {
				if (r2[i].endsWith("#"))
					serialized_data[index++] = null;
				else
					serialized_data[index++] = r2[i].substring(3, r2[i].length()-1);
			}
			for (int i=1; i<r3.length; i++) {
				if (r3[i].endsWith("#"))
					serialized_data[index++] = null;
				else
					serialized_data[index++] = r3[i].substring(3, r3[i].length()-1);
			}
			for (int i=1; i<r5.length; i++) {
				if (r5[i].endsWith("#"))
					serialized_data[index++] = null;
				else
					serialized_data[index++] = r5[i].substring(3, r5[i].length()-1);
				
				if (r5[i].startsWith("Vh")) {
					if (r5[i].endsWith("#"))
						serialized_data[index++] = null;
					else
						serialized_data[index++] = String.valueOf(r5[i].charAt(r5[i].length()-1));
				}
			}
		} catch (Exception e) {
			logger.error("incoming message could not be parsed properly (" + e.getMessage() + ") -> drop it");
			return;
		}

		data = new StreamElement(dataField, serialized_data, data.getTimeStamp());
		super.dataAvailable(inputStreamName, data);
	}
}
