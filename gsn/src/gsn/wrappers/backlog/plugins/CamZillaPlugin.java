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
	
	private static final short TASK_MESSAGE = 0;
	private static final short POWER_MESSAGE = 1;

	private static final short PANORAMA_TASK = 0;
	private static final short PICTURE_TASK = 1;
	private static final short MODE_TASK = 2;
	private static final short CALIBRATION_TASK = 3;
	
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
		Serializable[] command = null;
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
			if (!sx.trim().isEmpty() && !sy.trim().isEmpty())
				str = "start("+sx+","+sy+") ";
			if (!px.trim().isEmpty() && !py.trim().isEmpty())
				str += "pictures("+px+","+py+") ";
			if (!rx.trim().isEmpty() && !ry.trim().isEmpty())
				str += "rotation("+rx+","+ry+") ";
			if (!d.trim().isEmpty())
				str += "delay("+d+") ";
			if (!g.trim().isEmpty())
				str += "gphoto2("+g+")";
			
			logger.info("uploading panorama picture task >" + str + "<");
			command = new Serializable[] {TASK_MESSAGE, PANORAMA_TASK, str};
		}
		else if ( action.compareToIgnoreCase("picture_now") == 0 ) {
			logger.info("uploading picture now command");
			command = new Serializable[] {TASK_MESSAGE, PICTURE_TASK};
		}
		else if ( action.compareToIgnoreCase("operating_mode") == 0 ) {
			short mode = 0;
			for (int i = 0 ; i < paramNames.length ; i++) {
				if( paramNames[i].compareToIgnoreCase("mode") == 0 )
					mode = 1;
			}
			if (mode == 0)
				logger.info("uploading automatic mode command");
			else
				logger.info("uploading manual mode command");
			command = new Serializable[] {TASK_MESSAGE, MODE_TASK, mode};
		}
		else if ( action.compareToIgnoreCase("calibration") == 0 ) {
			logger.info("uploading calibration command");
			String str = "";
			for (int i = 0 ; i < paramNames.length ; i++) {
				if( paramNames[i].compareToIgnoreCase("gphoto2_config") == 0 )
					str = (String) paramValues[i];
			}
			command = new Serializable[] {TASK_MESSAGE, CALIBRATION_TASK, str};
		}
		else if ( action.compareToIgnoreCase("power_settings") == 0 ) {
			short camRobot = 0;
			short heater = 0;
			for (int i = 0 ; i < paramNames.length ; i++) {
				if( paramNames[i].compareToIgnoreCase("robot_and_camera") == 0 )
					camRobot = 1;
				if( paramNames[i].compareToIgnoreCase("heater") == 0 )
					heater = 1;
			}
			if (heater == 0) {
				if (camRobot == 0)
					logger.info("uploading: robot and camera power off / heater off");
				else
					logger.info("uploading robot and camera power on / heater off");
			}
			else {
				if (camRobot == 0)
					logger.info("uploading robot and camera power off / heater on");
				else
					logger.info("uploading robot and camera power on / heater on");
			}
			command = new Serializable[] {POWER_MESSAGE, camRobot, heater};
		}
		else {
			logger.warn("unrecognized action >" + action + "<");
		}
		
		try {
			if( sendRemote(System.currentTimeMillis(), command, super.priority) ) {
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
		
		return true;
	}
}
