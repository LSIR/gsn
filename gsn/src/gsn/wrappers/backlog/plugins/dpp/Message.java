package gsn.wrappers.backlog.plugins.dpp;

import java.io.Serializable;
import java.nio.ByteBuffer;

import gsn.beans.DataField;

public interface Message {

	int getType();
	
	boolean isExtended();

	Serializable[] receivePayload(ByteBuffer payload) throws Exception;

	ByteBuffer sendPayload(String action, String[] paramNames, Object[] paramValues) throws Exception;
	
	Serializable[] sendPayloadSuccess(boolean success);

	DataField[] getOutputFormat();
}
