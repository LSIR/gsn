package gsn.wrappers.backlog.plugins.dpp;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import gsn.beans.DataField;

public class CommCmdMsg implements Message {
	
	private static final String DPP_CMD_TYPE = "type";
	private static final String DPP_CMD_VALUE = "value";

	private static final short DPP_CMD_TYPE_MAX = 255;
	private static final int DPP_CMD_VALUE_MAX = 65535;
	
	private Short type = null;
	private Integer value = null;
	
	private static DataField[] dataField = {
			new DataField("TYPE", "SMALLINT"),
			new DataField("VALUE", "INTEGER")};

	@Override
	public Serializable[] receivePayload(ByteBuffer payload) throws Exception {
		short type = (short) (payload.get() & 0xff); 	// uint8_t: command type
		int value = payload.getShort() & 0xffff; 		// int16_t: value
        
		return new Serializable[]{type, value};
	}

	@Override
	public DataField[] getOutputFormat() {
		return dataField;
	}

	@Override
	public ByteBuffer sendPayload(String action, String[] paramNames, Object[] paramValues) throws Exception {
		if( action.compareToIgnoreCase("dppmsg") == 0 ) {
			for (int i=0; i<paramNames.length;i++) {
				if (paramNames[i].trim().compareToIgnoreCase(DPP_CMD_TYPE) == 0) {
		            type = new Short((String)paramValues[i]);
				}
				else if (paramNames[i].trim().compareToIgnoreCase(DPP_CMD_VALUE) == 0) {
		            value = new Integer((String)paramValues[i]);
				}
			}
			if (type == null)
				throw new Exception("type field has to be specified");
			else if (value == null)
				throw new Exception("value field has to be specified");
			else if (type < 0 || type > DPP_CMD_TYPE_MAX)
				throw new Exception("type field has to be an integer between 0 and " + DPP_CMD_TYPE_MAX);
			else if (value < 0 || value > DPP_CMD_VALUE_MAX)
				throw new Exception("value field has to be an integer between 0 and " + DPP_CMD_VALUE_MAX);
			else {
				ByteBuffer bb = ByteBuffer.wrap(new byte[3]);
				bb.order(ByteOrder.LITTLE_ENDIAN);
				bb.put((byte)(type & 0xff));
				bb.putShort((short)(value & 0xffff));
				return bb;
			}
		}
		else
			throw new Exception("Unknown action");
	}

	@Override
	public Serializable[] sendPayloadSuccess(boolean success) {
		if (success)
			return new Serializable[] {type, value};
		else
			return null;
	}

	@Override
	public int getType() {
		return gsn.wrappers.backlog.plugins.dpp.MessageTypes.MSG_TYPE_COMM_CMD;
	}

	@Override
	public boolean isExtended() {
		return false;
	}
}
