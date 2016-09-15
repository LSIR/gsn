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
	
	private static final String DPP_HEADER_TARGET_ID = "target_id";
	
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
	            int seqnr = toInteger(data[5]);
	            long generation_time = toLong(data[6]);
	    		payload = ByteBuffer.wrap((byte[]) data[7]);
	    		header = new Serializable[] {timestamp, (long)(generation_time/1000.0), generation_time, device_id, type, target_id, seqnr, payload_len};
	    		
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
        Long timestamp = System.currentTimeMillis();
		InputInfo inputInfo;
		Serializable [] header;
		Serializable[] processPayload;
		if (msgClass.isExtended())
			header = new Serializable[] {timestamp, null, null, getDeviceID(), null};
		else
			header = new Serializable[] {timestamp, null, null, getDeviceID(), null, null, null, null};
		
		if (getDeviceID() != null) {
			boolean ret = false;
			try {
				if (logger.isDebugEnabled())
					logger.debug("action: " + action);
	
	            Serializable[] message;
	            if (msgClass.isExtended())
	            	message = new Serializable[5];
	            else
	            	message = new Serializable[8];
	            
	            int device_id = getDeviceID(); // device_id
	            boolean ext_msg = msgClass.isExtended(); // ext_msg
	            int type = msgClass.getType(); // type
	            Integer target_id = null;
	            Integer seqnr = null;
	            Long generation_time = null;
	            
	            if (!msgClass.isExtended()) {
					for (int i=0; i<paramNames.length;i++) {
						if (paramNames[i].trim().compareToIgnoreCase(DPP_HEADER_TARGET_ID) == 0) {
							target_id = new Integer((String)paramValues[i]); // target_id
						}
					}
					if (target_id == null)
						throw new Exception("target_id missing");

		            seqnr = dppMsgMultiplexer.getNextSequenceNumber(); // seqnr
		            generation_time = timestamp*1000; // generation_time
	            }
				
	            byte[] payload;
	            try {
	            	payload = msgClass.sendPayload(action, paramNames, paramValues).array();
				}
				catch (Exception e) {
					return inputInfo = new InputInfo(getActiveAddressBean().toString(), "DPP message upload not successfull: " + e.getMessage(), false);
				}
	            
	            int payload_len = (byte)(payload.length & 0xff); // payload_len
				
	            message[0] = (short)(device_id & 0xffff);
	            message[1] = ext_msg;
	            message[2] = (byte)(type & 0xff);
	            message[3] = (byte)(payload_len & 0xff);
	            if (msgClass.isExtended()) {
	            	message[4] = payload;
		    		header = new Serializable[] {timestamp, null, null, device_id, type, null, null, payload_len};
	            }
	            else {
		            message[4] = (short)(target_id & 0xffff);
		            message[5] = (short)(seqnr & 0xffff);
		            message[6] = generation_time;
	            	message[7] = payload;
		    		header = new Serializable[] {timestamp, (long)(generation_time/1000.0), generation_time, device_id, type, target_id, seqnr, payload_len};
	            }
	            
	            ret = sendRemote(timestamp, message, super.priority);
				processPayload = msgClass.sendPayloadSuccess(ret);
				
				if (ret)
					inputInfo = new InputInfo(getActiveAddressBean().toString(), "MIG message upload successfull", ret);
				else
					inputInfo = new InputInfo(getActiveAddressBean().toString(), "MIG message upload not successfull", ret);
			}
			catch (Exception e) {
				processPayload = msgClass.sendPayloadSuccess(false);
				inputInfo = new InputInfo(getActiveAddressBean().toString(), "DPP message upload not successfull: " + e.getMessage(), false);
			}
		}
		else {
			processPayload = msgClass.sendPayloadSuccess(false);
			inputInfo = new InputInfo(getActiveAddressBean().toString(), "device ID is null ", false);
		}

		if (processPayload != null)
			dataProcessed(timestamp, (Serializable[])ArrayUtils.addAll(header, processPayload));
		
		return inputInfo;
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
