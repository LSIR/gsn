package gsn.vsensor;

import gsn.Main;
import gsn.beans.DataField;
import gsn.beans.StreamElement;


import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import org.apache.log4j.Logger;
import java.lang.Math;


public class OZ47Concentration extends BridgeVirtualSensorPermasense {

	private static final transient Logger logger = Logger.getLogger(OZ47Concentration.class);
	
	private static double kT = 0.0199999995529652;
	private Connection conn = null;
	private double[] defaultParam = new double[2];
	
	private static DataField[] dataField = {
			new DataField("POSITION", "INTEGER"),
			new DataField("DEVICE_ID", "INTEGER"),
			new DataField("GENERATION_TIME", "BIGINT"),

			new DataField("OZONE_PPB", "DOUBLE"),
			
			new DataField("CALIB_PARAM_0", "DOUBLE"),
			new DataField("CALIB_PARAM_1", "DOUBLE"),
			
			new DataField("DATA_IMPORT_SOURCE", "SMALLINT")};
	
	@Override
	public boolean initialize() {
		
		boolean ret = super.initialize();
		
		defaultParam[0] = -5.809267002965658;
		defaultParam[1] = 0.07945008689940476;
		
		try {
			conn = Main.getStorage(getVirtualSensorConfiguration().getName()).getConnection();
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		
		return ret;
	}
	
	
	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
		
		long time = (Long)data.getData("GENERATION_TIME");
		Integer resistance = (Integer)data.getData("RESISTANCE_1");
		Double temp = (Double)data.getData("TEMPERATURE");
		double ozone_calib;
		// No calibration if there is no NABEL data
		if (resistance == 0) {
			logger.warn("measured resistance is zero, no final calibration applied (time=" + time + ")");
			return;
    }
		
		// Get calibration parameters
		ResultSet rs = null;
		try {
			StringBuilder query = new StringBuilder();
			if (getVirtualSensorConfiguration().getName().contains("ostest_"))
				query.append("select calib_param_0, calib_param_1 from ostest_oz47_calibration_due where generation_time <= ").append(time).append(" and position = 1 and sensor_id = 2 order by generation_time desc limit 1");
			else
				query.append("select calib_param_0, calib_param_1 from opensense_oz47_calibration_due where generation_time <= ").append(time).append(" and position = 1 and sensor_id = 2 order by generation_time desc limit 1");
			
			rs = Main.getStorage(getVirtualSensorConfiguration().getName()).executeQueryWithResultSet(query, conn);
			
			StreamElement curr_data;
			if(rs.next()) {
        ozone_calib = rs.getDouble("calib_param_0") + rs.getDouble("calib_param_1") * resistance * Math.exp(kT * (temp - 25));
			  curr_data = new StreamElement(dataField, new Serializable[] {data.getData("POSITION"), data.getData("DEVICE_ID"), data.getData("GENERATION_TIME"), ozone_calib, rs.getDouble("calib_param_0"), rs.getDouble("calib_param_1"), data.getData("DATA_IMPORT_SOURCE")});
			}
			else {
			  logger.warn("no calibration found, using default parameters (time=" + time + ")");
			  ozone_calib = defaultParam[0] + defaultParam[1] * resistance * Math.exp(kT * (temp - 25));
			  curr_data = new StreamElement(dataField, new Serializable[] {data.getData("POSITION"), data.getData("DEVICE_ID"), data.getData("GENERATION_TIME"), ozone_calib, defaultParam[0], defaultParam[1], data.getData("DATA_IMPORT_SOURCE")});
			}
			
			super.dataAvailable(inputStreamName, curr_data);
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}		
	}
}
