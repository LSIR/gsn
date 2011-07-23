package gsn.vsensor;

import gsn.Main;
import gsn.beans.DataField;
import gsn.beans.StreamElement;


import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import org.apache.log4j.Logger;
import java.util.ArrayList;
import java.lang.Math;


@SuppressWarnings("unchecked")
public class OZ47Calibration extends BridgeVirtualSensorPermasense {

	private static final transient Logger logger = Logger.getLogger(OZ47Calibration.class);
	
	private static String MESSAGE_TYPE_STR = "message_type";

	private static String DUE_NAMING_STR = "due";
	private static String ZUE_NAMING_STR  = "zue";
	private static int DUE_NAMING = 1;
	private static int ZUE_NAMING = 2;

	private static double ADJUSTMENT_WEIGHT = 0.35;
	private static double BIN_SIZE = 28;
	private static int NUM_BINS = 5;
	private static double kT = 0.0199999995529652;
	
	private ArrayList<Double>[] bins = new ArrayList[NUM_BINS];
	private Connection conn = null;
	
	private static DataField[] dataField = {
			new DataField("POSITION", "INTEGER"),
			new DataField("DEVICE_ID", "INTEGER"),
			new DataField("GENERATION_TIME", "BIGINT"),

			new DataField("ADJ_WEIGHT", "DOUBLE"),
			new DataField("BIN_SIZE", "DOUBLE"),
			
			new DataField("BIN_0_REL_VAL", "DOUBLE"),
			new DataField("BIN_0_SENSOR_VAL", "DOUBLE"),
			new DataField("BIN_1_REL_VAL", "DOUBLE"),
			new DataField("BIN_1_SENSOR_VAL", "DOUBLE"),
			new DataField("BIN_2_REL_VAL", "DOUBLE"),
			new DataField("BIN_2_SENSOR_VAL", "DOUBLE"),
			new DataField("BIN_3_REL_VAL", "DOUBLE"),
			new DataField("BIN_3_SENSOR_VAL", "DOUBLE"),
			new DataField("BIN_4_REL_VAL", "DOUBLE"),
			new DataField("BIN_4_SENSOR_VAL", "DOUBLE"),
			
			new DataField("CALIB_PARAM_0", "DOUBLE"),
			new DataField("CALIB_PARAM_1", "DOUBLE"),
			
			new DataField("DATA_IMPORT_SOURCE", "SMALLINT")};
	
	private int messageType = -1;
	
	@Override
	public boolean initialize() {
		
		String type = getVirtualSensorConfiguration().getMainClassInitialParams().get(MESSAGE_TYPE_STR);
		if (type == null) {
			logger.error(MESSAGE_TYPE_STR + " has to be specified");
			return false;
		}
		if (type.equalsIgnoreCase(DUE_NAMING_STR))
			messageType = DUE_NAMING;
		else if (type.equalsIgnoreCase(ZUE_NAMING_STR))
			messageType = ZUE_NAMING;
		else {
			logger.error(MESSAGE_TYPE_STR + " has to be " + DUE_NAMING_STR + " or " + ZUE_NAMING_STR);
			return false;
		}
		
		boolean ret = super.initialize();
		
		for(int i=0; i<NUM_BINS; i++)
			bins[i] = new ArrayList<Double>();
		
		// Get latest bin values
		ResultSet rs = null;
		try {
			conn = Main.getStorage(getVirtualSensorConfiguration().getName()).getConnection();
			StringBuilder query = new StringBuilder();
			query.append("select bin_0_rel_val, bin_0_sensor_val, bin_1_rel_val, bin_1_sensor_val, bin_2_rel_val, bin_2_sensor_val, bin_3_rel_val, bin_3_sensor_val, bin_4_rel_val, bin_4_sensor_val from ").append(getVirtualSensorConfiguration().getName()).append(" order by generation_time desc limit 1");
			rs = Main.getStorage(getVirtualSensorConfiguration().getName()).executeQueryWithResultSet(query, conn);
			
			if (rs.next()) {
				// get bin data
				bins[0].add(rs.getDouble("bin_0_rel_val")); bins[0].add(rs.getDouble("bin_0_sensor_val"));
				bins[1].add(rs.getDouble("bin_1_rel_val")); bins[1].add(rs.getDouble("bin_1_sensor_val"));
				bins[2].add(rs.getDouble("bin_2_rel_val")); bins[2].add(rs.getDouble("bin_2_sensor_val"));
				bins[3].add(rs.getDouble("bin_3_rel_val")); bins[3].add(rs.getDouble("bin_3_sensor_val"));
				bins[4].add(rs.getDouble("bin_4_rel_val")); bins[4].add(rs.getDouble("bin_4_sensor_val"));
			} else {
				// use default values
				bins[0].add(15.6320); bins[0].add(275.6500);
				bins[1].add(44.1915); bins[1].add(616.4092);
				bins[2].add(67.4688); bins[2].add(906.6337);
				bins[3].add(null); bins[3].add(null);
				bins[4].add(null); bins[4].add(null);

				logger.warn("no calibration data in the database, use default");
			}
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		
		return ret;
	}
	
	
	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
		
		long time = (Long)data.getData("GENERATION_TIME");
		Double ozone_rel = (Double)data.getData("OZONE_PPB");
		Double ozone_sensor = 0.0;
		
		// No calibration if there is no NABEL data
		if (ozone_rel == null)
			return;
		
		// Get sensor readings from the Duebendorf node which were measured at the same time (last 10 minutes) as the NABEL measurement
		// Get latest bin values
		ResultSet rs = null;
		try {
			StringBuilder query = new StringBuilder();
			if (getVirtualSensorConfiguration().getName().contains("ostest_"))
				query.append("select resistance_1, temperature from ostest_oz47_dynamic__mapped where generation_time >= ").append(time-10*60*1000).append(" and generation_time <= ").append(time).append(" and position = 1 and sensor_id = 2");
			else
				query.append("select resistance_1, temperature from opensense_oz47_dynamic__mapped where generation_time >= ").append(time-10*60*1000).append(" and generation_time <= ").append(time).append(" and position = 1 and sensor_id = 2");
			
			rs = Main.getStorage(getVirtualSensorConfiguration().getName()).executeQueryWithResultSet(query, conn);
			
			// TODO: Clean sensor readings before using them to adjust calibration curve!
			int num = 0;
			while(rs.next()) {
        if (rs.getInt("resistance_1") == 0)
          continue
        ozone_sensor += rs.getInt("resistance_1") * Math.exp(kT * (rs.getDouble("temperature") - 25));
				num++;
			}
			if (num == 0)
				logger.warn("no sensor readings for the reliable measurement at time " + time);
				return;
			else
				ozone_sensor /= num;
			
			int bin_index = (int)Math.ceil(ozone_rel/BIN_SIZE)-1;
			if (bin_index > NUM_BINS-1) {
				logger.warn("no bin available for ozone concentration of " + ozone_rel + " ppb");
				return;
			}
			
			// Adjust bin data with the new values and calculate new calibration curve
			if (bins[bin_index].get(0) != null) {
				bins[bin_index].set(0, (1-ADJUSTMENT_WEIGHT)*bins[bin_index].get(0) + ADJUSTMENT_WEIGHT*ozone_rel);
				bins[bin_index].set(1, (1-ADJUSTMENT_WEIGHT)*bins[bin_index].get(1) + ADJUSTMENT_WEIGHT*ozone_sensor);
			}
			else {
				bins[bin_index].set(0, ozone_rel);
				bins[bin_index].set(1, ozone_sensor);
			}
			
			double[] param = new double[2];
			leastSquare(param, bins);
			
			StreamElement curr_data = new StreamElement(dataField, new Serializable[] {1, 1, time, ADJUSTMENT_WEIGHT, BIN_SIZE, bins[0].get(0), bins[0].get(1), bins[1].get(0), bins[1].get(1),bins[2].get(0), bins[2].get(1),bins[3].get(0), bins[3].get(1),bins[4].get(0), bins[4].get(1), param[0], param[1], null});
			super.dataAvailable(inputStreamName, curr_data);
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}		
	}
	
	public static void leastSquare(double [] parameters, ArrayList<Double>[] bins){

		double s=0.0,sx=0.0,sy=0.0,sxx=0.0,sxy=0.0,del;


	    s = NUM_BINS;
	    for(int i=0; i < NUM_BINS; i++){
	        if (bins[i].get(0) == null) {
	          s = s-1;          
	          continue;
	        }
	        sx  += bins[i].get(1);
	        sy  += bins[i].get(0);
	        sxx += bins[i].get(1)*bins[i].get(1);
	        sxy += bins[i].get(1)*bins[i].get(0);
	    }

	    del = s*sxx - sx*sx;

	     // Intercept
	    parameters[0] = (sxx*sy -sx*sxy)/del;
	    // Slope
	    parameters[1] = (s*sxy -sx*sy)/del;
	}
}
