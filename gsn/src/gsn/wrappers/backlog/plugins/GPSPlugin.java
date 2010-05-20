package gsn.wrappers.backlog.plugins;

import java.io.Serializable;
import java.nio.*;

import org.apache.log4j.Logger;

import gsn.beans.DataField;
import gsn.wrappers.backlog.BackLogMessage;

public class GPSPlugin extends AbstractPlugin {

	private DataField[] dataField = {	new DataField("TIMESTAMP", "BIGINT"),
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("DEVICE_ID", "INTEGER"),
			new DataField("GPS_TIME", "INTEGER"),
			new DataField("GPS_WEEK", "SMALLINT"),
			new DataField("CARRIER_PHASE", "DOUBLE"),
			new DataField("PSEUDO_RANGE", "DOUBLE"),
			new DataField("DOPPLER", "DOUBLE"),
			new DataField("SPACE_VEHICLE", "SMALLINT"),
			new DataField("MEASUREMENT_QUALITY", "SMALLINT"),
			new DataField("SIGNAL_STRENGTH", "SMALLINT"),
			new DataField("LLI", "SMALLINT")
			};
	
	private final transient Logger logger = Logger.getLogger( GPSPlugin.class );
	
	@Override
	public byte getMessageType() {
		return BackLogMessage.GPS_MESSAGE_TYPE;
	}

	@Override
	public DataField[] getOutputFormat() {
		return dataField;
	}

	@Override
	public String getPluginName() {
		return "GPSPlugin";
	}

	@Override
	public boolean messageReceived(int deviceId, long timestamp, byte[] packet) {
		logger.debug("message received from CoreStation with DeviceId: " + deviceId);
		
		// Parse the Message
		ByteBuffer buffer = ByteBuffer.allocate(packet.length);
		buffer.put(packet);
		
		buffer.position(0);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		
		int gpsTime = buffer.getInt();
		short gpsWeek = buffer.getShort();
		double carrierPhase = buffer.getDouble();
		double pseudorange = buffer.getDouble();
		double doppler = (double) buffer.getFloat();
		short spaceVehicle = (short) buffer.get();
		short measurementQuality = buffer.getShort();
		short signalStrength = buffer.getShort();
		short lli = (short) buffer.get();
		
		Serializable[] data = {timestamp, timestamp, deviceId, gpsTime, gpsWeek, carrierPhase, pseudorange, doppler, spaceVehicle, measurementQuality, signalStrength, lli};
		
		if( dataProcessed(System.currentTimeMillis(), data) ) {
			ackMessage(timestamp, super.priority);
		} else {
			logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");
		}
		
		return true;
	}
}
