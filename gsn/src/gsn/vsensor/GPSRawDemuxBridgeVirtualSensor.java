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
						new DataField("GSN_TIMESTAMP", "BIGINT"),
						new DataField("DEVICE_ID", "INTEGER"),
						new DataField("POSITION", "INTEGER"),
						new DataField("DEVICE_TYPE", "VARCHAR(16)"),
						new DataField("GPS_RAW_DATA_VERSION", "SMALLINT"),
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
		// gsn_timestamp
		long gsn_timestamp = (Long) data.getData(dataField[2].getName());
		// device_id
		serialized_data[3] = data.getData(dataField[3].getName());
		// position
		serialized_data[4] = data.getData(dataField[4].getName());
		// sensor type
		serialized_data[5] = data.getData(dataField[5].getName());
		// gps data version
		serialized_data[6] = data.getData(dataField[6].getName());

		Short version = (Short)data.getData("gps_raw_data_version");
		if (version == null)
			logger.error("gps_raw_data_version data should not be NULL");
		else if (version == 1) {
			// gps sats
			Integer sats = (Integer) data.getData("GPS_SATS");
			serialized_data[7] = sats;
			
			ByteBuffer bbuffer = ByteBuffer.wrap((byte[]) data.getData("gps_raw_data"));
			bbuffer.order(ByteOrder.LITTLE_ENDIAN);
			
			try {
				if (sats.compareTo((int) bbuffer.get(12) & 0xFF) != 0)
					logger.error("number of statellites (GPS_SATS=" + sats + ") is not equal to the number of satellites in GPS_RAW_DATA (" + ((int) bbuffer.get(12) & 0xFF) + ")");
				else if (!checksum(bbuffer, sats))
					logger.error("RXM RAW GPS checksum did not match in packet with generation_time " + data.getData(dataField[1].getName()));
				else {
					// skip header and payload length
					bbuffer.position(bbuffer.position()+6);
					// get GPS time
					serialized_data[8] = bbuffer.getInt();
					// get GPS week
					serialized_data[9] = bbuffer.getShort();
	
					// get number of satellites
					int s = (int) bbuffer.get() & 0xFF;
					// skip reserved byte
					bbuffer.position(bbuffer.position()+1);
					for (int i=0; i<s; i++) {
						// set gsn_timestamp
						serialized_data[2] = gsn_timestamp;
						gsn_timestamp++;
						// get Carrier Phase
						serialized_data[10] = bbuffer.getDouble();
						// get Pseudorange
						serialized_data[11] = bbuffer.getDouble();
						// get Doppler
						serialized_data[12] = (double)bbuffer.getFloat();
						// get SV nbr
						serialized_data[13] = (short) ((int)bbuffer.get() & 0xFF);
						// get Quality
						serialized_data[14] = (short) bbuffer.get();
						// get C/No
						serialized_data[15] = (short) bbuffer.get();
						// get LLI
						serialized_data[16] = (short) ((int)bbuffer.get() & 0xFF);
	
						data = new StreamElement(dataField, serialized_data);
						super.dataAvailable(inputStreamName, data);
					}
				}
			}
			catch (Exception e) {
				logger.error(e);
			}
		}
		else
			logger.warn("gps_raw_data_version data version not supported (gps_raw_data_version=" + version + ")");
	}

	private boolean checksum(ByteBuffer bbuffer, int sats) {
		// RXM-RAW Checksum
		byte CK_A = 0;
		byte CK_B = 0;
		for (int i=2; i<14+24*sats; i++) {
			CK_A += bbuffer.get(i);
			CK_B += CK_A;
		}
		if (bbuffer.get(14+24*sats) == CK_A && bbuffer.get(15+24*sats) == CK_B)
			return true;
		else
			return false;
	}
}
