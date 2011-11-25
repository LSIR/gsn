package gsn.wrappers.backlog.plugins;

import gsn.beans.DataField;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;

public class BackLogConfigPlugin extends AbstractPlugin {

	private final transient Logger logger = Logger.getLogger( BackLogConfigPlugin.class );
	
	private DataField[] dataField = {new DataField("DEVICE_ID", "INTEGER"),
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("MESSAGE", "VARCHAR(256)"),
			new DataField("CONFIGURATION", "binary")};

	@Override
	public String getPluginName() {
		return "BackLogConfigPlugin";
	}

	@Override
	public boolean messageReceived(int deviceId, long timestamp, Serializable[] data) {
		try {
			if(dataProcessed(System.currentTimeMillis(), new Serializable[] {deviceId, timestamp, (String)data[0], ((String)data[1]).getBytes("UTF-8")}))
				ackMessage(timestamp, super.priority);
			else
				return false;
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage(), e);
			return false;
		}
		return true;
	}

	@Override
	public byte getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.CONFIG_MESSAGE_TYPE;
	}

	@Override
	public DataField[] getOutputFormat() {
		return dataField;
	}

	@Override
	public boolean sendToPlugin(String action, String[] paramNames, Object[] paramValues) {
		if( action.compareToIgnoreCase("config_command") == 0 ) {
			byte [] configuration = null;
			int id = -1;
			long time = System.currentTimeMillis();
			for (int i = 0 ; i < paramNames.length ; i++) {
				if( paramNames[i].compareToIgnoreCase("configuration") == 0 ) {
					// store the configuration received from the web input in the database
					configuration = ((FileItem)paramValues[i]).get();
				}
				else if( paramNames[i].compareToIgnoreCase("core_station") == 0 ) {
					id = Integer.parseInt((String)paramValues[i]);
				}
			}
			
			// and try to send it to the deployment
			try {
				if (!sendRemote(System.currentTimeMillis(), new Serializable [] {configuration}, super.priority)) {
					dataProcessed(time, new Serializable[] {id, time, "no connection to the CoreStation: could not upload configuration -> try again later", configuration});
					logger.warn("no connection to the CoreStation: could not upload configuration -> try again later");
				}
			} catch (IOException e) {
				dataProcessed(time, new Serializable[] {id, time, e.getMessage() + ": could not upload configuration -> try again later", configuration});
				logger.info(e.getMessage() + ": could not upload configuration -> try again later");
			}

			return true;
		}
		else
			return false;
	}

}
