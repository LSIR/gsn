package gsn.wrappers.backlog.plugins;

import gsn.beans.DataField;

import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;

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
	private static final short POSITIONING_TASK = 2;
	private static final short MODE_TASK = 3;
	private static final short CALIBRATION_TASK = 4;
	
	private static DataField[] dataField = {
			new DataField("TIMESTAMP", "BIGINT"),
			new DataField("GENERATION_TIME", "BIGINT"),
			new DataField("DEVICE_ID", "INTEGER"),

			new DataField("COMMAND", "VARCHAR(127)"),
			new DataField("INFO", "VARCHAR(255)"),
			new DataField("X", "VARCHAR(8)"),
			new DataField("Y", "VARCHAR(8)"),
			new DataField("START_X", "VARCHAR(8)"),
			new DataField("START_Y", "VARCHAR(8)"),
			new DataField("PICTURES_X", "SMALLINT"),
			new DataField("PICTURES_Y", "SMALLINT"),
			new DataField("ROTATION_X", "VARCHAR(8)"),
			new DataField("ROTATION_Y", "VARCHAR(8)"),
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
			DecimalFormat df = new DecimalFormat( "0.0" );
			String x = null, y = null, start_x = null, start_y = null, rotation_x = null, rotation_y = null;
			byte[] gphoto2conf = null;
			if (data[3] != null)
				x = df.format((Double)data[3]);
			if (data[4] != null)
				y = df.format((Double)data[4]);
			if (data[5] != null)
				start_x = df.format((Double)data[5]);
			if (data[6] != null)
				start_y = df.format((Double)data[6]);
			if (data[9] != null)
				rotation_x = df.format((Double)data[9]);
			if (data[10] != null)
				rotation_y = df.format((Double)data[10]);
			if (data[12] != null)
				gphoto2conf = ((String)data[12]).getBytes("UTF-8");
			
			if( dataProcessed(System.currentTimeMillis(), new Serializable[]{timestamp, toLong(data[0]), deviceId, (String)data[1], (String)data[2], x, y, start_x, start_y, toShort(data[7]), toShort(data[8]), rotation_x, rotation_y, toShort(data[11]), gphoto2conf}) ) {
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
			String sx = "", sy = "", px = "", py = "", rx = "", ry = "", d = "", aperture = "", shutter = "", iso = "", g = "";
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
				else if( paramNames[i].compareToIgnoreCase("aperture") == 0 )
					aperture = (String) paramValues[i];
				else if( paramNames[i].compareToIgnoreCase("shutter_speed") == 0 )
					shutter = (String) paramValues[i];
				else if( paramNames[i].compareToIgnoreCase("iso") == 0 )
					iso = (String) paramValues[i];
				else if( paramNames[i].compareToIgnoreCase("gphoto2_config") == 0 )
					g = (String) paramValues[i];
			}

			if (g.isEmpty())
				g = getD300sConfig(aperture, shutter, iso);
			else
				g = getD300sConfig(aperture, shutter, iso) + "," + g;

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
			String aperture = "", shutter = "", iso = "", g = "";
			for (int i = 0 ; i < paramNames.length ; i++) {
				if( paramNames[i].compareToIgnoreCase("aperture") == 0 )
					aperture = (String) paramValues[i];
				else if( paramNames[i].compareToIgnoreCase("shutter_speed") == 0 )
					shutter = (String) paramValues[i];
				else if( paramNames[i].compareToIgnoreCase("iso") == 0 )
					iso = (String) paramValues[i];
				else if( paramNames[i].compareToIgnoreCase("gphoto2_config") == 0 )
					g = (String) paramValues[i];
			}
			if (g.isEmpty())
				g = getD300sConfig(aperture, shutter, iso);
			else
				g = getD300sConfig(aperture, shutter, iso) + "," + g;
			command = new Serializable[] {TASK_MESSAGE, PICTURE_TASK, g};
		}
		else if ( action.compareToIgnoreCase("positioning") == 0 ) {
			double x = 0, y = 0;
			for (int i = 0 ; i < paramNames.length ; i++) {
				if( paramNames[i].compareToIgnoreCase("x") == 0 )
					x = new Double((String)paramValues[i]);
				else if( paramNames[i].compareToIgnoreCase("y") == 0 )
					y = new Double((String)paramValues[i]);
			}
			
			logger.info("uploading positioning task (x=" + x + ",y=" + y + ")");
			command = new Serializable[] {TASK_MESSAGE, POSITIONING_TASK, x, y};
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
			command = new Serializable[] {TASK_MESSAGE, CALIBRATION_TASK};
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
	
	
	private String getD300sConfig(String aperture, String shutter, String iso) {
		String ret = "";
		
		if (aperture.equalsIgnoreCase("auto")) {
			if (shutter.equalsIgnoreCase("auto"))
				ret += "/main/capturesettings/expprogram=P";
			else
				ret += "/main/capturesettings/expprogram=S,/main/capturesettings/shutterspeed=" + shutter;
		}
		else {
			if (shutter.equalsIgnoreCase("auto"))
				ret += "/main/capturesettings/expprogram=A,/main/capturesettings/f-number=" + aperture;
			else
				ret += "/main/capturesettings/expprogram=M,/main/capturesettings/shutterspeed=" + shutter + ",/main/capturesettings/f-number=" + aperture;
		}
		
		if (iso.equalsIgnoreCase("auto"))
			ret += ",/main/imgsettings/autoiso=On";
		else
			ret += ",/main/imgsettings/autoiso=Off,/main/imgsettings/iso=" + iso;
		
		return ret;
	}
}
