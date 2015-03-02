package gsn.vsensor;

import gsn.Main;
import gsn.beans.DataField;
import gsn.beans.StreamElement;


import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import org.apache.log4j.Logger;
import java.lang.Math;


public class OZ47InstantCalibration extends BridgeVirtualSensorPermasense {

	private static final transient Logger logger = Logger.getLogger(OZ47InstantCalibration.class);
	
	private static double kT = 0.0199999995529652;
	private double[] defaultParam = new double[2];
	
	private static DataField[] dataField = {
			new DataField("OZONE_PPB", "DOUBLE"),
			new DataField("CALIB_PARAM_0", "DOUBLE"),
			new DataField("CALIB_PARAM_1", "DOUBLE")};
	
	@Override
	public boolean initialize() {
		
		boolean ret = super.initialize();
		
		defaultParam[0] = 3.635901;
		defaultParam[1] = 0.026868;
		
		return ret;
	}
	
	
	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
		
		Long time = (Long)data.getData("GENERATION_TIME");
		Integer resistance = (Integer)data.getData("RESISTANCE_1");
		Double temp = (Double)data.getData("TEMPERATURE");
		
		if (time == null || resistance == null || temp == null)
		  return;
		
		Integer dev_id = (Integer)data.getData("DEVICE_ID");
		
		double ozone_calib;
		// No calibration if there is no sensor data
		if (resistance.intValue() == 0) {
			logger.info("measured resistance is zero, no final calibration applied (time=" + time + ")");
			return;
    }
		
		// Get calibration parameters
		Connection conn = null;
		ResultSet rs = null;
		try {
			conn = Main.getStorage(getVirtualSensorConfiguration()).getConnection();
			StringBuilder query = new StringBuilder();
			query.append("select calib_param_0, calib_param_1 from ostest_oz47_param_instant_cal where generation_time <= ").append(time.longValue()).append(" and device_id = ").append(dev_id.intValue()).append(" order by generation_time desc limit 1");
			
			rs = Main.getStorage(getVirtualSensorConfiguration()).executeQueryWithResultSet(query, conn);
			
			if(rs.next()) {
        ozone_calib = rs.getDouble("calib_param_0") + rs.getDouble("calib_param_1") * resistance.doubleValue() * Math.exp(kT * (temp.doubleValue() - 25));
        if (ozone_calib < 0) ozone_calib = 0;
			  data = new StreamElement(data, dataField, new Serializable[] {ozone_calib, rs.getDouble("calib_param_0"), rs.getDouble("calib_param_1")});
			}
			else {
			  logger.debug("no calibration found, using default parameters (time=" + time + ")");
			  ozone_calib = defaultParam[0] + defaultParam[1] * resistance.doubleValue() * Math.exp(kT * (temp.doubleValue() - 25));
			  if (ozone_calib < 0) ozone_calib = 0;
			  data = new StreamElement(data, dataField, new Serializable[] {ozone_calib, defaultParam[0], defaultParam[1]});
			}
			rs.close();
			conn.close();
			
			super.dataAvailable(inputStreamName, data);
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}		
	}
}
