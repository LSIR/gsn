package gsn.wrappers.backlog.plugins;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import gsn.beans.DataField;
import gsn.beans.InputInfo;
import gsn.wrappers.BackLogWrapper;


/**
 * This plugin listens for incoming LWB DPP messages.
 * 
 * @author Tonio Gsell
 */
public class DPPMessagePlugin extends AbstractPlugin {

	private static final String DPP_MESSAGE_CLASS = "message-classname";
	
	private static DataField[] headerDataField = {
			new DataField("TIMESTAMP", "BIGINT"),
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("GENERATION_TIME_MICROSEC", "BIGINT"),
			new DataField("DEVICE_ID", "INTEGER"),
			new DataField("MESSAGE_TYPE", "INTEGER"),
			new DataField("TARGET_ID", "INTEGER"),
			new DataField("SEQNR", "INTEGER"),
			new DataField("PAYLOAD_LENGTH", "INTEGER")};

	private DataField[] msgDataField;

	private final transient Logger logger = Logger.getLogger( DPPMessagePlugin.class );

	private Constructor<?> messageConstructor = null;

	private DPPMessageMultiplexer dppMsgMultiplexer = null;

	private gsn.wrappers.backlog.plugins.dpp.Message msgClass;

	@Override
	public boolean initialize(BackLogWrapper backlogwrapper, String coreStationName, String deploymentName) {
		activeBackLogWrapper = backlogwrapper;
		String p = getActiveAddressBean().getPredicateValue("priority");
		if (p == null)
			priority = null;
		else
			priority = Integer.valueOf(p);
		
		try {
			dppMsgMultiplexer = DPPMessageMultiplexer.getInstance(coreStationName, backlogwrapper.getBLMessageMultiplexer());
			
			// get the DPP message class for the specified DPP packet
			Class<?> classTemplate = Class.forName(getActiveAddressBean().getPredicateValueWithException(DPP_MESSAGE_CLASS));
			messageConstructor = classTemplate.getConstructor();
			
			msgClass = ((gsn.wrappers.backlog.plugins.dpp.Message) messageConstructor.newInstance());
			
			msgDataField = (DataField[])ArrayUtils.addAll(headerDataField, msgClass.getOutputFormat());
			
			dppMsgMultiplexer.registerListener(msgClass.getType(), this);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return false;
		}
		
		return true;
	}

	@Override
	public String getPluginName() {
		return "DPPMessagPlugin-"+messageConstructor.getName();
	}

	@Override
	public boolean messageReceived(int deviceId, long timestamp, Serializable[] data) {
		try {
			int device_id = toInteger(data[0]);
            boolean ext_msg = (Boolean)data[1];
            int type = toInteger(data[2]);
            int payload_len = toInteger(data[3]);
            ByteBuffer payload;
            Serializable[] header;
            if (ext_msg) {
	    		payload = ByteBuffer.wrap((byte[]) data[4]);
	    		header = new Serializable[] {timestamp, null, null, device_id, type, null, null, payload_len};
            }
            else {
            	int target_id = toInteger(data[4]);
	            int seq_no = toInteger(data[5]);
	            long generation_time = toLong(data[6]);
	    		payload = ByteBuffer.wrap((byte[]) data[7]);
	    		header = new Serializable[] {timestamp, (long)(generation_time/1000.0), generation_time, device_id, type, target_id, seq_no, payload_len};
	    		
            }
    		payload.order(ByteOrder.LITTLE_ENDIAN);
    		Serializable[] msg = (Serializable[])ArrayUtils.addAll(header, msgClass.receivePayload(payload));
    		
			if( dataProcessed(System.currentTimeMillis(), msg) ) {
				ackMessage(timestamp, super.priority);
				return true;
			} else {
				logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return false;
	}
	
	@Override
	public InputInfo sendToPlugin(String action, String[] paramNames, Object[] paramValues) {
		//TODO: implement upload functionality
		if (getDeviceID() != null) {
			//ret = sendRemote(System.currentTimeMillis(), new Serializable[] {packet}, super.priority);
		}
		return null;
	}

	@Override
	public short getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.DPP_MESSAGE_TYPE;
	}
	
	
	@Override
	public void dispose() {
		dppMsgMultiplexer.deregisterListener(msgClass.getType(), this);
	}

	@Override
	public DataField[] getOutputFormat() {
		return msgDataField;
	}

}
