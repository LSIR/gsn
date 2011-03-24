package gsn.wrappers.backlog.plugins;

import java.io.Serializable;
import java.math.*;

import org.apache.log4j.Logger;

import gsn.beans.DataField;


public class ECVQEK3Plugin extends AbstractPlugin {

	private DataField[] dataField = {	new DataField("TIMESTAMP", "BIGINT"),
						new DataField("GENERATION_TIME", "BIGINT"),
						new DataField("DEVICE_ID", "INTEGER"),
						new DataField("BIAS_VOLTAGE", "BIGINT"),
						new DataField("MEASURED_CONCENTRATION", "DOUBLE"),
						new DataField("SENSOR_OUTPUT_CURRENT", "BIGINT"),
						new DataField("ELECTROCHEM_SPAN_CONCENTRATION", "DOUBLE"),
						new DataField("SPAN_CURRENT", "BIGINT"),
						new DataField("TEMPERATURE", "DOUBLE"),
						new DataField("CURRENT_RANGE", "VARCHAR(3)"),
						new DataField("SELECTED_RANGE", "VARCHAR(3)")
  };

	private final transient Logger logger = Logger.getLogger( ECVQEK3Plugin.class );

	@Override
	public byte getMessageType() {
		return gsn.wrappers.backlog.BackLogMessage.ECVQEK3_MESSAGE_TYPE;
	}


	@Override
	public String getPluginName() {
		return "ECVQEK3Plugin";
	}

	@Override
	public DataField[] getOutputFormat() {
		return dataField;
	}

	@Override
	public boolean messageReceived(int deviceId, long timestamp, Serializable[] data) {

		if (data.length != 1) {
			logger.error("The message with timestamp >" + timestamp + "< seems unparsable.(length: " + data.length + ")");
      return true;
		}

    try {

      String str = new String((String)data[0]);
    
		  // split string:
	    // pos 10-13: bias voltage
	    // pos 16-23: measured concentration
	    // pos 26-33: sensor output current
	    // pos 36-43: electrochem span concentration
	    // pos 46-53: span current
	    // pos 55-59: temp
	    // pos 61-63: current range
	    // pos 65-67: selected range
      Long bv = Long.parseLong(str.substring(10,14),16);
      Long mc = Long.parseLong(str.substring(16,24),16);
      Long sc = Long.parseLong(str.substring(26,34),16);
      Long es = Long.parseLong(str.substring(36,44),16);
      Long spc = Long.parseLong(str.substring(46,54),16);
      double t = Double.parseDouble(str.substring(55,59));

		  if( dataProcessed(System.currentTimeMillis(), new Serializable[] {timestamp, timestamp, deviceId, bv, mc.doubleValue()/100.0, sc, es.doubleValue()/100.0, spc, t, str.substring(61,64), str.substring(65,68)}) )
			  ackMessage(timestamp, super.priority);
		  else
			  logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");

    } catch (Exception e) {
      logger.warn("ECVQEK3 data could not be parsed.");
			//logger.warn(e.getMessage(), e);


		    if( dataProcessed(System.currentTimeMillis(), new Serializable[] {timestamp, timestamp, deviceId, null, null, null, null, null, null, null, null}) )
			    ackMessage(timestamp, super.priority);
		    else
			    logger.warn("The message with timestamp >" + timestamp + "< could not be stored in the database.");
			return true;
		}

		return true;
	}
}
