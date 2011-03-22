package gsn.vsensor;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import gsn.beans.DataField;
import gsn.beans.StreamElement;

import org.apache.log4j.Logger;

public class GPSRawDemuxBridgeVirtualSensor extends BridgeVirtualSensorPermasense {
	
	private static final transient Logger logger = Logger.getLogger(GPSRawDemuxBridgeVirtualSensor.class);

	private static final DataField[] dataField = {
						new DataField("TIMESTAMP", "BIGINT"),
						new DataField("GENERATION_TIME", "BIGINT"),
						new DataField("DEVICE_ID", "INTEGER"),
						new DataField("POSITION", "INTEGER"),
						new DataField("GPS_RAW_DATA_VERSION", "SMALLINT"),
					    new DataField("GPS_SAMPLE_COUNT", "INTEGER"),
					    new DataField("GPS_SATS", "INTEGER"),
						new DataField("GPS_TIME", "INTEGER"),
						new DataField("GPS_WEEK", "SMALLINT"),
						new DataField("CARRIER_PHASE", "DOUBLE"),
						new DataField("PSEUDO_RANGE", "DOUBLE"),
						new DataField("DOPPLER", "DOUBLE"),
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
		// gps data version
		serialized_data[4] = data.getData(dataField[4].getName());

		if (((Short)data.getData("gps_raw_data_version")) == 1) {
			// gps sample count
			serialized_data[5] = data.getData(dataField[5].getName());
			// gps sats
			Integer sats = (Integer) data.getData("GPS_SATS");
			serialized_data[6] = sats;
			
			ByteBuffer bbuffer = ByteBuffer.wrap((byte[]) data.getData("gps_raw_data"));
			bbuffer.order(ByteOrder.LITTLE_ENDIAN);
			
			try {
				// skip header and payload length
				bbuffer.position(bbuffer.position()+6);
				// get GPS time
				serialized_data[7] = bbuffer.getInt();
				// get GPS week
				serialized_data[8] = bbuffer.getShort();

				// get number of satellites
				int s = (int) bbuffer.get() & 0xFF;
				// skip reserved byte
				bbuffer.position(bbuffer.position()+1);
				if (sats.compareTo(s) == 0) {
					for (int i=0; i<s; i++) {
						// get Carrier Phase
						serialized_data[9] = bbuffer.getDouble();
						// get Pseudorange
						serialized_data[10] = bbuffer.getDouble();
						// get Doppler
						serialized_data[11] = (double)bbuffer.getFloat();
						// get SV nbr
						serialized_data[12] = (short) ((int)bbuffer.get() & 0xFF);
						// get Quality
						serialized_data[13] = (short) bbuffer.get();
						// get C/No
						serialized_data[14] = (short) bbuffer.get();
						// get LLI
						serialized_data[15] = (short) ((int)bbuffer.get() & 0xFF);

						data = new StreamElement(dataField, serialized_data, data.getTimeStamp());
						super.dataAvailable(inputStreamName, data);
					}
				}
				else
					logger.error("number of statellites (GPS_SATS=" + sats + ") is not equal to the number of satellites in GPS_RAW_DATA (" + s + ")");
			}
			catch (Exception e) {
				logger.error(e);
			}
		}
	}
}
