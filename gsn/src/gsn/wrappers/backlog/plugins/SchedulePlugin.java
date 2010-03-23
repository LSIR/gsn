package gsn.wrappers.backlog.plugins;

import gsn.beans.DataField;

public class SchedulePlugin extends AbstractPlugin {
	
	private DataField[] dataField = {new DataField("TIMESTAMP", "BIGINT"),
			new DataField("SCHEDULE", "BINARY")};

	@Override
	public byte getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.SCHEDULE_MESSAGE_TYPE;
	}

	@Override
	public DataField[] getOutputFormat() {
		return dataField;
	}

	@Override
	public String getPluginName() {
		return "SchedulePlugin";
	}

	@Override
	public boolean messageReceived(long timestamp, byte[] payload) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean sendToPlugin(String action, String[] paramNames, Object[] paramValues) {
		// TODO Auto-generated method stub
		return false;
	}
}
