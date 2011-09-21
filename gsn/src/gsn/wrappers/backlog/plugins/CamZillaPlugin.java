package gsn.wrappers.backlog.plugins;

import gsn.beans.DataField;

import java.io.IOException;
import java.io.Serializable;

import org.apache.log4j.Logger;


/**
 * This plugin listens for incoming CamZilla messages and offers the functionality to
 * upload CamZilla tasks.
 * 
 * @author Tonio Gsell
 */
public class CamZillaPlugin extends AbstractPlugin {
	
	private static DataField[] dataField = {
			new DataField("TIMESTAMP", "BIGINT"),
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("DEVICE_ID", "INTEGER"),

			new DataField("START_X", "SMALLINT"),
			new DataField("START_Y", "SMALLINT"),
			new DataField("PICTURES_X", "SMALLINT"),
			new DataField("PICTURES_Y", "SMALLINT"),
			new DataField("ROTATION_X", "SMALLINT"),
			new DataField("ROTATION_Y", "SMALLINT"),
			new DataField("DELAY", "SMALLINT"),
			new DataField("GPHOTO2_CONFIG", "BINARY")};

	private final transient Logger logger = Logger.getLogger( CamZillaPlugin.class );

	@Override
	public byte getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.CAMZILLA_MESSAGE_TYPE;
	}

	@Override
	public DataField[] getOutputFormat() {
		return dataField;
	}

	@Override
	public String getPluginName() {
		return "CamZillaPlugin";
	}

	@Override
	public boolean messageReceived(int deviceId, long timestamp, Serializable[] data) {
		try {
			if( dataProcessed(System.currentTimeMillis(), new Serializable[]{timestamp, toLong(data[0]), deviceId, toShort(data[1]), toShort(data[2]), toShort(data[3]), toShort(data[4]), toShort(data[5]), toShort(data[6]), toShort(data[7]), ((String)data[8]).getBytes("UTF-8")}) ) {
				ackMessage(timestamp, super.priority);
				return true;
			} else {
				logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return false;
	}
	
	
	@Override
	public boolean sendToPlugin(String action, String[] paramNames, Object[] paramValues) {
		if( action.compareToIgnoreCase("panorama_picture") == 0 ) {
			String sx = "", sy = "", px = "", py = "", rx = "", ry = "", d = "", g = "";
			for (int i = 0 ; i < paramNames.length ; i++) {
				if( paramNames[i].compareToIgnoreCase("start_x") == 0 )
					sx = (String) paramValues[i];
				else if( paramNames[i].compareToIgnoreCase("start_y") == 0 )
					sy = (String) paramValues[i];
				else if( paramNames[i].compareToIgnoreCase("pictures_x") == 0 )
					px = (String) paramValues[i];
				else if( paramNames[i].compareToIgnoreCase("pictures_y") == 0 )
					py = (String) paramValues[i];
				else if( paramNames[i].compareToIgnoreCase("rotation_x") == 0 )
					rx = (String) paramValues[i];
				else if( paramNames[i].compareToIgnoreCase("rotation_y") == 0 )
					ry = (String) paramValues[i];
				else if( paramNames[i].compareToIgnoreCase("delay") == 0 )
					d = (String) paramValues[i];
				else if( paramNames[i].compareToIgnoreCase("gphoto2_config") == 0 )
					g = (String) paramValues[i];
			}

			String str = "";
			if (sx.trim() != "" && sy.trim() != "")
				str = "start("+sx+","+sy+") ";
			if (px.trim() != "" && py.trim() != "")
				str += "pictures("+px+","+py+") ";
			if (rx.trim() != "" && ry.trim() != "")
				str += "rotation("+rx+","+ry+") ";
			if (d.trim() != "")
				str += "delay("+d+") ";
			if (g.trim() != "")
				str += "gphoto2("+g+")";
			
			logger.info("uploading panorama picture task >" + str + "<");
			Serializable[] task = {str};
			try {
				if( sendRemote(System.currentTimeMillis(), task, super.priority) ) {
					if (logger.isDebugEnabled())
						logger.debug("Panorama picture task sent to CoreStation");
				}
				else {
					logger.warn("Panorama picture task could not be sent to CoreStation");
					return false;
				}
			} catch (IOException e) {
				logger.warn(e.getMessage());
			}
		}
		
		return true;
	}
}
