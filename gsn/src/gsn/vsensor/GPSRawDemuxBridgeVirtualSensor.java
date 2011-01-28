package gsn.vsensor;

import java.io.Serializable;

import gsn.beans.DataField;
import gsn.beans.StreamElement;

import org.apache.log4j.Logger;

public class GPSRawDemuxBridgeVirtualSensor extends BridgeVirtualSensorPermasense {
	
	private static final transient Logger logger = Logger.getLogger(GPSRawDemuxBridgeVirtualSensor.class);

	private static final DataField[] dataField = {	new DataField("TIMESTAMP", "BIGINT"),
						new DataField("GENERATION_TIME", "BIGINT"),
						new DataField("DEVICE_ID", "INTEGER"),
						new DataField("POSITION", "INTEGER"),
						new DataField("GPS_TIME", "INTEGER"),
						new DataField("GPS_WEEK", "SMALLINT"),
						new DataField("SVS", "SMALLINT"),
						new DataField("CARRIER_PHASE", "DOUBLE"),
						new DataField("PSEUDO_RANGE", "DOUBLE"),
						new DataField("DOPPLER", "INTEGER"),
						new DataField("SPACE_VEHICLE", "SMALLINT"),
						new DataField("MEASUREMENT_QUALITY", "SMALLINT"),
						new DataField("SIGNAL_STRENGTH", "SMALLINT"),
						new DataField("LLI", "SMALLINT")};
	
	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
		Serializable[] serialized_data = new Serializable[dataField.length];
		
		// timestamp
		serialized_data[0] = data.getData(dataField[0].getName());
		// generation_time
		serialized_data[1] = data.getData(dataField[1].getName());
		// device_id
		serialized_data[2] = data.getData(dataField[2].getName());
		// position
		serialized_data[3] = data.getData(dataField[3].getName());

		if (((Integer)data.getData("gps_raw_data_version")) == 1) {
			byte[] binaryData = (byte[]) data.getData("gps_raw_data");
			// TODO: parse GPS RAW binary data and put it into serialized_data[4] to serialized_data[13]
		}

		data = new StreamElement(dataField, serialized_data, data.getTimeStamp());
		super.dataAvailable(inputStreamName, data);
	}
}
