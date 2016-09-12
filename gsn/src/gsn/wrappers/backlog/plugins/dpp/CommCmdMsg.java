package gsn.wrappers.backlog.plugins.dpp;

import java.io.Serializable;
import java.nio.ByteBuffer;

import gsn.beans.DataField;

public class CommCmdMsg implements Message {
	
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
		return null;
	}

	@Override
	public int getType() {
		return gsn.wrappers.backlog.plugins.dpp.MessageTypes.MSG_TYPE_COMM_CMD;
	}
}
