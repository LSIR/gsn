package gsn.vsensor;

import gsn.Main;
import gsn.beans.DataField;
import gsn.beans.StreamElement;


import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import org.apache.log4j.Logger;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.Math;


@SuppressWarnings("unchecked")
public class OZ47Calibration extends BridgeVirtualSensorPermasense {

	private static final transient Logger logger = Logger.getLogger(OZ47Calibration.class);
	
	private static double ADJUSTMENT_WEIGHT = 0.35;
	private static double BIN_SIZE = 28;
	private static int NUM_BINS = 5;
	private static double kT = 0.0199999995529652;
	
	private HashMap<Integer, ArrayList<Double>[]> binMap = new HashMap<Integer, ArrayList<Double>[]>();
	private HashMap<Integer, ArrayList<Long>> binTimeMap = new HashMap<Integer, ArrayList<Long>>();
	
	private ArrayList<Integer> deviceList = new ArrayList<Integer>();
	
	private static double MAX_SPATIAL_DIFF = 0.1;  // degree minutes
	private static double MAX_TEMP_DIFF = 15;      // minutes
	private static int MAX_BUFFER_SIZE = 5000;
	
	private HashMap<Integer, ArrayList<StreamElement>> sensorBufferSchimmelstrMap = new HashMap<Integer, ArrayList<StreamElement>>();
	private HashMap<Integer, ArrayList<StreamElement>> referenceBufferSchimmelstrMap = new HashMap<Integer, ArrayList<StreamElement>>();
	private HashMap<Integer, ArrayList<StreamElement>> sensorBufferStampfenbachstrMap = new HashMap<Integer, ArrayList<StreamElement>>();
	private HashMap<Integer, ArrayList<StreamElement>> referenceBufferStampfenbachstrMap = new HashMap<Integer, ArrayList<StreamElement>>();
  
  // For linear regression
  private double[][] V;            // Least squares and var/covar matrix
  private double[] C;      // Coefficients

  
  public enum RefStations {
    NONE,
    SCHIMMELSTR,
    STAMPFENBACHSTR 
}
	
	private static DataField[] dataField = {
			new DataField("POSITION", "INTEGER"),
			new DataField("DEVICE_ID", "INTEGER"),
			new DataField("GENERATION_TIME", "BIGINT"),
			
			new DataField("BIN_0_REL_VAL", "DOUBLE"),
			new DataField("BIN_0_SENSOR_VAL", "DOUBLE"),
			new DataField("BIN_0_TIME_VAL", "BIGINT"),
			new DataField("BIN_0_AGE_VAL", "DOUBLE"),
			new DataField("BIN_1_REL_VAL", "DOUBLE"),
			new DataField("BIN_1_SENSOR_VAL", "DOUBLE"),
			new DataField("BIN_1_TIME_VAL", "BIGINT"),
			new DataField("BIN_1_AGE_VAL", "DOUBLE"),
			new DataField("BIN_2_REL_VAL", "DOUBLE"),
			new DataField("BIN_2_SENSOR_VAL", "DOUBLE"),
			new DataField("BIN_2_TIME_VAL", "BIGINT"),
			new DataField("BIN_2_AGE_VAL", "DOUBLE"),
			new DataField("BIN_3_REL_VAL", "DOUBLE"),
			new DataField("BIN_3_SENSOR_VAL", "DOUBLE"),
			new DataField("BIN_3_TIME_VAL", "BIGINT"),
			new DataField("BIN_3_AGE_VAL", "DOUBLE"),
			new DataField("BIN_4_REL_VAL", "DOUBLE"),
			new DataField("BIN_4_SENSOR_VAL", "DOUBLE"),
			new DataField("BIN_4_TIME_VAL", "BIGINT"),
			new DataField("BIN_4_AGE_VAL", "DOUBLE"),
			
			new DataField("CALIB_PARAM_0", "DOUBLE"),
			new DataField("CALIB_PARAM_1", "DOUBLE"),
			
			new DataField("DATA_IMPORT_SOURCE", "SMALLINT")};
	
	@Override
	public boolean initialize() {
		
		boolean ret = super.initialize();
		
		// Add all supported device IDs
		deviceList.add(3);
		deviceList.add(4);
		deviceList.add(5);
		deviceList.add(6);
		deviceList.add(7);
		deviceList.add(8);
		
		// Create bins and buffers for each device
		for (int d = 0; d < deviceList.size(); d++) {
		  ArrayList<Double>[] bins = new ArrayList[NUM_BINS];
		  for(int i=0; i<NUM_BINS; i++)
		    bins[i] = new ArrayList<Double>();
		  binMap.put(deviceList.get(d), bins);
		  binTimeMap.put(deviceList.get(d), new ArrayList<Long>());
		  
		  sensorBufferSchimmelstrMap.put(deviceList.get(d), new ArrayList<StreamElement>());
		  referenceBufferSchimmelstrMap.put(deviceList.get(d), new ArrayList<StreamElement>());
		  sensorBufferStampfenbachstrMap.put(deviceList.get(d), new ArrayList<StreamElement>());
		  referenceBufferStampfenbachstrMap.put(deviceList.get(d), new ArrayList<StreamElement>());
		}
		
		// Get latest bin values
		Connection conn = null;
		ResultSet rs = null;
		try {
			conn = Main.getStorage(getVirtualSensorConfiguration().getName()).getConnection();
			
			for (int d = 0; d < deviceList.size(); d++) {
			  ArrayList<Double>[] bins = binMap.get(deviceList.get(d));
			  ArrayList<Long> binTime = binTimeMap.get(deviceList.get(d));
			  
			  if (bins == null || binTime == null) {
			    logger.error("Device " + deviceList.get(d) + ": init failed, no bin data available for this device");
			    return ret;
			  }
			  
  			StringBuilder query = new StringBuilder();
  			query.append("select * from ").append(getVirtualSensorConfiguration().getName()).append(" where device_id = " + deviceList.get(d) + " order by generation_time desc limit 1");
  			rs = Main.getStorage(getVirtualSensorConfiguration().getName()).executeQueryWithResultSet(query, conn);
  			
  			if (rs.next()) {
  				// get bin data
  				bins[0].add(rs.getDouble("bin_0_rel_val")); bins[0].add(rs.getDouble("bin_0_sensor_val")); bins[0].add(rs.getDouble("bin_0_age_val")); binTime.add(rs.getLong("bin_0_time_val"));
  				bins[1].add(rs.getDouble("bin_1_rel_val")); bins[1].add(rs.getDouble("bin_1_sensor_val")); bins[1].add(rs.getDouble("bin_1_age_val")); binTime.add(rs.getLong("bin_1_time_val"));
  				bins[2].add(rs.getDouble("bin_2_rel_val")); bins[2].add(rs.getDouble("bin_2_sensor_val")); bins[2].add(rs.getDouble("bin_2_age_val")); binTime.add(rs.getLong("bin_2_time_val"));
  				bins[3].add(rs.getDouble("bin_3_rel_val")); bins[3].add(rs.getDouble("bin_3_sensor_val")); bins[3].add(rs.getDouble("bin_3_age_val")); binTime.add(rs.getLong("bin_3_time_val"));
  				bins[4].add(rs.getDouble("bin_4_rel_val")); bins[4].add(rs.getDouble("bin_4_sensor_val")); bins[4].add(rs.getDouble("bin_4_age_val")); binTime.add(rs.getLong("bin_4_time_val"));
  				logger.info("Device " + deviceList.get(d) + ": current calibration in DB: ("+bins[0].get(0)+","+bins[0].get(1)+")"+"("+bins[1].get(0)+","+bins[1].get(1)+")"+"("+bins[2].get(0)+","+bins[2].get(1)+")"+"("+bins[3].get(0)+","+bins[3].get(1)+")"+"("+bins[4].get(0)+","+bins[4].get(1)+")");
  			} else {
  				// use default values
  				bins[0].add(15.0); bins[0].add(420.0); bins[0].add(0.0); binTime.add(1316124000000L);
  				bins[1].add(44.0); bins[1].add(1500.0); bins[1].add(0.0); binTime.add(1316124000000L);
  				bins[2].add(67.0); bins[2].add(2350.0); bins[2].add(0.0); binTime.add(1316124000000L);
  				bins[3].add(null); bins[3].add(null); bins[3].add(null); binTime.add(null);
  				bins[4].add(null); bins[4].add(null); bins[4].add(null); binTime.add(null);
  
  				logger.warn("Device " + deviceList.get(d) + ": no calibration data in the database, use default");
  				logger.info("Device " + deviceList.get(d) + ": default calibration: ("+bins[0].get(0)+","+bins[0].get(1)+")"+"("+bins[1].get(0)+","+bins[1].get(1)+")"+"("+bins[2].get(0)+","+bins[2].get(1)+")"+"("+bins[3].get(0)+","+bins[3].get(1)+")"+"("+bins[4].get(0)+","+bins[4].get(1)+")");
  			}
  			rs.close();
			}
			
			conn.close();
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		
		return ret;
	}
	
	
	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
	  
	  try {
	  
  	  long gTime = ((Long)data.getData("GENERATION_TIME")).longValue();
  	  ArrayList<StreamElement> checkBuffer;
      ArrayList<StreamElement> streamBuffer;
  	  
      // If incoming stream belongs to sensor data, check whether measurement location is near
      // a reference station and build calibration tuples if possible.
  	  if (inputStreamName.equalsIgnoreCase("sensordata")) {
  	    
  	    int devId = ((Integer)data.getData("DEVICE_ID")).intValue();
        RefStations r = checkVicinity(data);
        
        if (r == RefStations.NONE) return;
        
        if (r == RefStations.STAMPFENBACHSTR) {
          checkBuffer = referenceBufferStampfenbachstrMap.get(devId);
          streamBuffer = sensorBufferStampfenbachstrMap.get(devId);
        }
        else if (r == RefStations.SCHIMMELSTR) {
          checkBuffer = referenceBufferSchimmelstrMap.get(devId);
          streamBuffer = sensorBufferSchimmelstrMap.get(devId);
        }
        else {
          logger.error("RefStation unknown");
          return;
        }
        
        if (checkBuffer == null || streamBuffer == null) {
          logger.error("Device " + devId + ": instant calibration failed, no buffer found for this device");
          return;
        }
        
        // Build calibration tuples and use them to update calibration
        buildTuples(gTime, checkBuffer, streamBuffer, inputStreamName, data);
        
      }
  	  else if (inputStreamName.equalsIgnoreCase("refdata_stampfenbachstrasse") || inputStreamName.equalsIgnoreCase("refdata_schimmelstrasse")) {
  	    
    	  if (inputStreamName.equalsIgnoreCase("refdata_stampfenbachstrasse")) {
    	    if (data.getData("OZONE_PPB") == null) {
            logger.warn("OZONE_PPB field returned null from stream " + inputStreamName);
            return;
          }
    	    
    	    for (int d = 0; d < deviceList.size(); d++) {
    	      checkBuffer = sensorBufferStampfenbachstrMap.get(deviceList.get(d));
    	      streamBuffer = referenceBufferStampfenbachstrMap.get(deviceList.get(d));
    	      
    	      if (checkBuffer == null || streamBuffer == null) {
              logger.error("Device " + deviceList.get(d) + ": instant calibration failed, no buffer found for this device");
              continue;
            }
    	      
    	      // Build calibration tuples and use them to update calibration
            buildTuples(gTime, checkBuffer, streamBuffer, inputStreamName, data);
    	    }
    	  }
    	  else if (inputStreamName.equalsIgnoreCase("refdata_schimmelstrasse")) {
    	    if (data.getData("OZONE_PPB") == null) {
            logger.warn("OZONE_PPB field returned null from stream " + inputStreamName);
            return;
          }
    	    
    	    for (int d = 0; d < deviceList.size(); d++) {
            checkBuffer = sensorBufferSchimmelstrMap.get(deviceList.get(d));
            streamBuffer = referenceBufferSchimmelstrMap.get(deviceList.get(d));
            
            if (checkBuffer == null || streamBuffer == null) {
              logger.error("Device " + deviceList.get(d) + ": instant calibration failed, no buffer found for this device");
              continue;
            }
            
            // Build calibration tuples and use them to update calibration
            buildTuples(gTime, checkBuffer, streamBuffer, inputStreamName, data);
          }
    	  }
  	  }
  	  else {
        logger.error("inputStreamName unkown " + inputStreamName);
        return;
      }
      
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
	}
	
	private void buildTuples(long g_time, ArrayList<StreamElement> checkBuffer, ArrayList<StreamElement> streamBuffer, String inputStreamName, StreamElement data) {
	  // Check whether we can build tuples using the buffered data.
    // Search for entries with generation_times +- 15min.
    ArrayList<Integer> removeEntries = new ArrayList<Integer>();
    for (int i = 0; i < checkBuffer.size(); i++) {
      
      if (((Long)checkBuffer.get(i).getData("GENERATION_TIME")).longValue() >= g_time - MAX_TEMP_DIFF*60*1000 && ((Long)checkBuffer.get(i).getData("GENERATION_TIME")).longValue() <= g_time + MAX_TEMP_DIFF*60*1000) {
        
        // Remove entry from the buffer, update bins, and push data to db
        StreamElement bufData = checkBuffer.get(i);
        removeEntries.add(i);
        
        int devId;
        double resistance, reference;
        if (inputStreamName.contains("refdata")) {
          
          if (data == null) {
            logger.warn("data equals null from stream " + inputStreamName);
            return;
          }
          else if (data.getData("OZONE_PPB") == null) {
            logger.warn("OZONE_PPB field returned null from stream " + inputStreamName + ":" + data.getFieldNames().toString() );
            return;
          }
          devId = ((Integer)bufData.getData("DEVICE_ID")).intValue();
          reference = ((Double)data.getData("OZONE_PPB")).doubleValue();
          resistance = ((Integer)bufData.getData("RESISTANCE_1")).intValue() * Math.exp(kT * (((Double)bufData.getData("TEMPERATURE")).doubleValue() - 25));
        }
        else if (inputStreamName.contains("sensordata")) {
          devId = ((Integer)data.getData("DEVICE_ID")).intValue();
          reference = ((Double)bufData.getData("OZONE_PPB")).doubleValue();
          resistance = ((Integer)data.getData("RESISTANCE_1")).intValue() * Math.exp(kT * (((Double)data.getData("TEMPERATURE")).doubleValue() - 25));
        }
        else {
          logger.error("inputStreamName unkown " + inputStreamName);
          return;
        }
        
        int bin_index = (int)Math.ceil(reference/BIN_SIZE)-1;
        
        if (bin_index > NUM_BINS-1) {
          logger.warn("no bin available for the reference ozone concentration of " + reference + " ppb");
          return;
        }
        
        // Get bin array of the corresponding device
        ArrayList<Double>[] bins = binMap.get(devId);
        ArrayList<Long> binTime = binTimeMap.get(devId);
        
        if (bins == null || binTime == null) {
          logger.error("Device " + devId + ": instant calibration failed, no bin data available for this device");
          return;
        }
        
        // Adjust bin data with the new values and calculate new calibration curve
        if (bins[bin_index].get(0) != null && bins[bin_index].get(0) != 0.0) {
          bins[bin_index].set(0, (1-ADJUSTMENT_WEIGHT)*bins[bin_index].get(0) + ADJUSTMENT_WEIGHT*reference);
          bins[bin_index].set(1, (1-ADJUSTMENT_WEIGHT)*bins[bin_index].get(1) + ADJUSTMENT_WEIGHT*resistance);
          binTime.set(bin_index, (long)((1-ADJUSTMENT_WEIGHT)*binTime.get(bin_index) + ADJUSTMENT_WEIGHT*g_time));
        }
        else {
          bins[bin_index].set(0, reference);
          bins[bin_index].set(1, resistance);
          binTime.set(bin_index, g_time);
        }
        
        // update all ages
        for (int j = 0; j < NUM_BINS; j++) {
          if (bins[j].get(2) == null)
            continue;
          bins[j].set(2, (g_time-binTime.get(j))/1000.0/60.0/60.0/24.0);
        }
        
        Regression(bins);
        
        if (inputStreamName.contains("refdata"))
          data = new StreamElement(dataField, new Serializable[] {bufData.getData("POSITION"), bufData.getData("DEVICE_ID"), bufData.getData("GENERATION_TIME"), bins[0].get(0), bins[0].get(1), binTime.get(0), bins[0].get(2), bins[1].get(0), bins[1].get(1), binTime.get(1), bins[1].get(2), bins[2].get(0), bins[2].get(1), binTime.get(2), bins[2].get(2), bins[3].get(0), bins[3].get(1), binTime.get(3), bins[3].get(2), bins[4].get(0), bins[4].get(1), binTime.get(4), bins[4].get(2),  C[0], C[1], null});
        else
          data = new StreamElement(dataField, new Serializable[] {data.getData("POSITION"), data.getData("DEVICE_ID"), data.getData("GENERATION_TIME"), bins[0].get(0), bins[0].get(1), binTime.get(0), bins[0].get(2), bins[1].get(0), bins[1].get(1), binTime.get(1), bins[1].get(2), bins[2].get(0), bins[2].get(1), binTime.get(2), bins[2].get(2), bins[3].get(0), bins[3].get(1), binTime.get(3), bins[3].get(2), bins[4].get(0), bins[4].get(1), binTime.get(4), bins[4].get(2), C[0], C[1], null});
        
        logger.debug("Instant calibration: bin pair updated");
        super.dataAvailable(inputStreamName, data);
      }
    }
    
    // Remove elements which were used above.
    for (int i = 0; i < removeEntries.size(); i++)
      checkBuffer.remove(removeEntries.get(i).intValue());
    
    if (removeEntries.size() > 0)
      return;
  
    // No match found, add stream element to the streamBuffer and check buffer size
    if (streamBuffer.size() == MAX_BUFFER_SIZE) {
      logger.debug("Remove buffered element from " + inputStreamName + " buffer");
      streamBuffer.remove(0);
    }
    streamBuffer.add(data);
	}
	
	private static RefStations checkVicinity(StreamElement data) {
	  
	  Double _lat = (Double)data.getData("LATITUDE");
	  Double _lon = (Double)data.getData("LONGITUDE");
	  
	  if (_lat == null || _lon == null) {
	    //logger.warn("Parsing data from OpenSense_OZ47_Uncalibrated: LATITUDE or LONGITUDE is null");
	    return RefStations.NONE;
	  }
	  
	  double lat = _lat.doubleValue();
	  double lon = _lon.doubleValue();
	  
	  // Check whether the measurement is near Stampfenbachstr
	  if (lat >= 4723.1396-MAX_SPATIAL_DIFF && lon >= 832.3892-MAX_SPATIAL_DIFF && lat <= 4723.1396+MAX_SPATIAL_DIFF && lon <= 832.4892+MAX_SPATIAL_DIFF)
	    return RefStations.STAMPFENBACHSTR;
	  else if (lat >= 4722.275-MAX_SPATIAL_DIFF && lon >= 831.4106-MAX_SPATIAL_DIFF && lat <= 4722.275+MAX_SPATIAL_DIFF && lon <= 831.4106+MAX_SPATIAL_DIFF)
      return RefStations.SCHIMMELSTR;
	  else
	    return RefStations.NONE;
	}
	
	
	private void Regression(ArrayList<Double>[] bins)
  {
	  int n = NUM_BINS;
	  for (int i = 0; i < NUM_BINS; i++) {
	    if (bins[i].get(0) == null || bins[i].get(0) == 0)
	      n--;
	  }
	  
    // Y[j]   = j-th observed data point
    // X[i,j] = j-th value of the i-th independent variable
    // W[j]   = j-th weight value
	  
	  double[] Y = new double[n];
    double[][] X = new double[2][n];
    double[] W = new double[n];
    
    for (int i = 0; i < n; i++) {
      Y[i] = bins[i].get(0);
      X[0][i] = 1.0;
      X[1][i] = bins[i].get(1);
      
      W[i] = 1/(1+bins[i].get(2));
    }
    
    

    int M = Y.length;             // M = Number of data points
    int N = (X.length*X[0].length) / M;         // N = Number of linear terms
    int NDF = M - N;              // Degrees of freedom

    // If not enough data, don't attempt regression
    if (NDF < 1)
      System.out.println("not enough data");

    V = new double[N][N];
    C = new double[N];
    double[] B = new double[N];   // Vector for LSQ

    // Clear the matrices to start out
    for (int i = 0; i < N; i++) {
      for (int j = 0; j < N; j++)
        V[i][j] = 0;
    }

    // Form Least Squares Matrix
    for (int i = 0; i < N; i++)
    {
      for (int j = 0; j < N; j++)
      {
        V[i][j] = 0;
        for (int k = 0; k < M; k++)
          V[i][j] = V[i][j] + W[k] * X[i][k] * X[j][k];
      }
      B[i] = 0;
      for (int k = 0; k < M; k++)
        B[i] = B[i] + W[k] * X[i][k] * Y[k];
    }
    // V now contains the raw least squares matrix
    if (!SymmetricMatrixInvert(V))
    {
      System.out.println("Could not invert matrix");
      return;
    }
    // V now contains the inverted least square matrix
    // Matrix multpily to get coefficients C = VB
    for (int i = 0; i < N; i++)
    {
      C[i] = 0;
      for (int j = 0; j < N; j++)
        C[i] = C[i] + V[i][j] * B[j];
    }
  }
  
  private boolean SymmetricMatrixInvert(double[][] V)
  {
    int N = (int)Math.sqrt(V.length*V[0].length);
    double[] t = new double[N];
    double[] Q = new double[N];
    double[] R = new double[N];
    double AB;
    int K, L, M;

    // Invert a symetric matrix in V
    for (M = 0; M < N; M++)
      R[M] = 1;
    K = 0;
    for (M = 0; M < N; M++)
    {
        double Big = 0;
        for (L = 0; L < N; L++)
        {
          AB = Math.abs(V[L][L]);
          if ((AB > Big) && (R[L] != 0))
          {
            Big = AB;
            K = L;
          }
        }
        if (Big == 0)
        {
          return false;
        }
        R[K] = 0;
        Q[K] = 1 / V[K][K];
        t[K] = 1;
        V[K][K] = 0;
        if (K != 0)
        {
          for (L = 0; L < K; L++)
          {
            t[L] = V[L][K];
            if (R[L] == 0)
              Q[L] = V[L][K] * Q[K];
            else
              Q[L] = -V[L][K] * Q[K];
            V[L][K] = 0;
          }
        }
        if ((K + 1) < N)
        {
          for (L = K + 1; L < N; L++)
          {
            if (R[L] != 0)
              t[L] = V[K][L];
            else
              t[L] = -V[K][L];
            Q[L] = -V[K][L] * Q[K];
            V[K][L] = 0;
          }
        }
        for (L = 0; L < N; L++)
          for (K = L; K < N; K++)
            V[L][K] = V[L][K] + t[L] * Q[K];
    }
    M = N;
    L = N - 1;
    for (K = 1; K < N; K++)
    {
      M = M - 1;
      L = L - 1;
      for (int J = 0; J <= L; J++)
        V[M][J] = V[J][M];
    }
    return true;
  }
	
}
