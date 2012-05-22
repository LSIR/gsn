package gsn.vsensor;

import gsn.beans.DataField;
import gsn.beans.StreamElement;


import java.io.File;
import java.io.Serializable;
import java.io.BufferedReader;
import java.io.FileReader;

import org.apache.log4j.Logger;

public class EMMobileDataParserGSM extends BridgeVirtualSensorPermasense {

	private static final transient Logger logger = Logger.getLogger(EMMobileDataParserGSM.class);
	
	private String storage_directory = null;
	
	private static DataField[] dataField = {
			new DataField("POSITION", "INTEGER"),
			new DataField("DEVICE_ID", "INTEGER"),
			new DataField("GENERATION_TIME", "BIGINT"),
			
			new DataField("AREA_CODE", "INTEGER"),
			new DataField("CELL_ID", "INTEGER"),
			new DataField("NETWORK_TYPE", "VARCHAR(256)"),
			new DataField("RSSI", "INTEGER"),
			new DataField("FIELD_STRENGTH", "DOUBLE"),

			new DataField("LATITUDE", "DOUBLE"),
			new DataField("LONGITUDE", "DOUBLE"),
					
			new DataField("DATA_IMPORT_SOURCE", "SMALLINT")};
	
	
	@Override
	public boolean initialize() {
		
		boolean ret = super.initialize();
		
		storage_directory = getVirtualSensorConfiguration().getStorage().getStorageDirectory();
		if (storage_directory != null) {
			storage_directory = new File(storage_directory, deployment).getPath();
		}
			
		return ret;
	}
	
  private double getFieldStrength(int rssi, long freq, double gain) {
    
    final double C = 299792458; // Speed of light
    final double Z0 = 376.73; // Free space impedance
    
    double powerW = Math.pow(10, (rssi/10.0))*0.001; // Convert dBm to Watt
    double wavelength = C/freq;
    double fieldStrength = Math.sqrt(powerW*Z0/(gain*Math.pow(wavelength,2)/(4*Math.PI)));

    return fieldStrength;
  }
	
	private void parseData(File file, String inputStreamName, StreamElement data) {
		try
		{
			//create BufferedReader to read csv file
			BufferedReader br = new BufferedReader( new FileReader(file.getAbsolutePath()));
			String strLine = "";
			
			//read comma separated file line by line
			while( (strLine = br.readLine()) != null)
			{
			  String[] tokens = strLine.split(",",-1);
        if (tokens.length != 7) {
          logger.warn("Line <" + strLine + "> skipped. Line to be parsed has " + tokens.length + " instead of 7 elements");
          continue;
        }
        
        Long timestamp;
        if (tokens[0].length() == 0)
          timestamp = null;
        else
          timestamp = Long.valueOf(tokens[0]);
        
        // convert strings to double and int values
        Double[] readingsDouble = new Double[2];
        int ind_d = 0;
        Integer[] readingsInt = new Integer[3];
        int ind_i = 0;
        for (int i = 1; i < tokens.length; i++) {
          if (i == 3 || i == 4 || i == 6) {
            if (tokens[i].length() == 0)
              readingsInt[ind_i] = null;
            else
              readingsInt[ind_i] = Integer.valueOf(tokens[i]);
            ind_i++;
          }
          else if (i == 1 || i == 2) {
            if (tokens[i].length() == 0)
              readingsDouble[ind_d] = null;
            else
              readingsDouble[ind_d] = Double.valueOf(tokens[i]);
            ind_d++;
          }
        }
			  
				// If there is no timestamp or location information is missing then skip this line.
				if (timestamp == null || timestamp == 0 ||
						readingsDouble[0] == null || readingsDouble[0] == 0 ||
						readingsDouble[1] == null || readingsDouble[1] == 0)
					continue;
				
				// Convert GPS coordinate.
				int lati = readingsDouble[0].intValue();
				readingsDouble[0] = ((readingsDouble[0]-lati)*60.0) + lati*100.0;
				int longi = readingsDouble[1].intValue();
				readingsDouble[1] = ((readingsDouble[1]-longi)*60.0) + longi*100.0;
				
				// Calculate field strength
        double fs = getFieldStrength(readingsInt[2], 900000000L, 1.0);
				
				StreamElement curr_data = new StreamElement(dataField, new Serializable[] {data.getData(dataField[1].getName()), data.getData(dataField[1].getName()), timestamp, readingsInt[0], readingsInt[1], tokens[5], readingsInt[2], fs, readingsDouble[0], readingsDouble[1], null});
				super.dataAvailable(inputStreamName, curr_data);
			}
		}
		catch(Exception e)
		{
			logger.error(e.getMessage(), e);
		}
		
	}
	
	
	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
		File file = new File(new File(storage_directory, Integer.toString((Integer)data.getData("device_id"))).getPath(), (String) data.getData("relative_file"));
		file = file.getAbsoluteFile();
		
		if (file.getAbsolutePath().indexOf("gsm") == -1)
      return;
		
		parseData(file, inputStreamName, data);
		logger.debug("parsed new incoming file (" + file.getAbsolutePath() + ")");
	}
}
