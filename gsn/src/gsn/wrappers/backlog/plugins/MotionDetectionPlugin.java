package gsn.wrappers.backlog.plugins;

import java.io.Serializable;

import org.apache.log4j.Logger;

import gsn.beans.DataField;


public class MotionDetectionPlugin extends AbstractPlugin {

	private static DataField[] dataField = {
		  new DataField("TIMESTAMP", "BIGINT"),
		  new DataField("GENERATION_TIME", "BIGINT"),
		  new DataField("DEVICE_ID", "INTEGER"),
		  
		  new DataField("THRESHOLD_RAW_DATA", "DOUBLE"),
		  new DataField("POLL_INT_RAW_DATA", "DOUBLE"),
		  new DataField("POLL_DURATION_RAW_DATA", "DOUBLE"),
		  new DataField("STD_X_RAW_DATA", "DOUBLE"),
		  new DataField("STD_Y_RAW_DATA", "DOUBLE"),
		  new DataField("STD_Z_RAW_DATA", "DOUBLE"),
		  new DataField("MOVING_RAW_DATA", "INTEGER")};

	private final transient Logger logger = Logger.getLogger( MotionDetectionPlugin.class );

	@Override
	public byte getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.MOTION_DETECTION_MESSAGE_TYPE;
	}

	@Override
	public String getPluginName() {
        return "MotionDetectionPlugin";
	}

	@Override
	public DataField[] getOutputFormat() {
    return dataField;
	}

	@Override
	public boolean messageReceived(int deviceId, long timestamp, Serializable[] data) {
		
		logger.debug("message received from CoreStation with DeviceId: " + deviceId);

		try {
			Serializable[] header = {timestamp, timestamp, deviceId};
			data = checkAndCastData(data, 0, dataField, 3);
			
			if( dataProcessed(System.currentTimeMillis(), concat(header, data)) )
				ackMessage(timestamp, super.priority);
			else
				logger.warn("The MotionDetection message with timestamp >" + timestamp + "< could not be stored in the database."); 
		} catch (Exception e) {
			logger.error("MotionDetection data could not be parsed: " + e.getMessage(), e);
			ackMessage(timestamp, super.priority);
			return true;
		}
		return true;
	}
}
