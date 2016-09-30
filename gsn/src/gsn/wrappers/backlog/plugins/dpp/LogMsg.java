package gsn.wrappers.backlog.plugins.dpp;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import gsn.beans.DataField;

public class LogMsg implements Message {
	
	private static DataField[] dataField = {
			new DataField("COMPONENT_ID", "SMALLINT"),
			new DataField("TYPE", "SMALLINT"),
			new DataField("VALUE", "INTEGER"),
			new DataField("EXTRA_DATA", "VARCHAR(255)")};

	@Override
	public Serializable[] receivePayload(ByteBuffer payload) throws Exception {
		Short component_id = null;
		Short type = null;
		Integer value = null;
		String extra_data = null;
		
		try {
			component_id = (short) (payload.get() & 0xff); 					// uint8_t: component ID
			type = (short) (payload.get() & 0xff); 							// uint8_t: log type
			value = payload.getShort() & 0xffff; 							// int16_t: log value
			if (payload.hasRemaining()) {
				byte [] extra = new byte [payload.remaining()];
				payload.get(extra);
				extra_data = new String(extra, Charset.forName("UTF-8"));		// varchar: addition information
			}
		} catch (Exception e) {
		}
        
		return new Serializable[]{component_id, type, value, extra_data};
	}

	@Override
	public ByteBuffer sendPayload(String action, String[] paramNames, Object[] paramValues) throws Exception {
		throw new Exception("sendPayload not implemented");
	}

	@Override
	public DataField[] getOutputFormat() {
		return dataField;
	}

	@Override
	public int getType() {
		return gsn.wrappers.backlog.plugins.dpp.MessageTypes.MSG_TYPE_LOG;
	}

	@Override
	public boolean isExtended() {
		return false;
	}

	@Override
	public Serializable[] sendPayloadSuccess(boolean success) {
		return null;
	}
}
