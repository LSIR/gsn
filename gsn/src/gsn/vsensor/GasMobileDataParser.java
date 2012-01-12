package gsn.vsensor;

import gsn.beans.DataField;
import gsn.beans.StreamElement;


import java.io.File;
import java.io.Serializable;
import java.io.BufferedReader;
import java.io.FileReader;

import org.apache.log4j.Logger;

public class GasMobileDataParser extends BridgeVirtualSensorPermasense {

	private static final transient Logger logger = Logger.getLogger(GasMobileDataParser.class);
	
	private String storage_directory = null;
	
	private static DataField[] dataField = {
			new DataField("POSITION", "INTEGER"),
			new DataField("DEVICE_ID", "INTEGER"),
			new DataField("GENERATION_TIME", "BIGINT"),

			new DataField("OZONE_PPB", "DOUBLE"),
			new DataField("RESISTANCE", "INTEGER"),
			new DataField("TEMPERATURE", "DOUBLE"),
			new DataField("HUMIDITY", "INTEGER"),
			new DataField("LATITUDE", "DOUBLE"),
			new DataField("LONGITUDE", "DOUBLE"),
			new DataField("SPEED", "DOUBLE"),
					
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
	
	private void parseData(File file, String inputStreamName, StreamElement data) {
		try
		{
			//create BufferedReader to read csv file
			BufferedReader br = new BufferedReader( new FileReader(file.getAbsolutePath()));
			String strLine = "";
			
			// skip first header line
			br.readLine();
			
			//read comma separated file line by line
			while( (strLine = br.readLine()) != null)
			{
				String[] tokens = strLine.split(",",-1);
				if (tokens.length != 8 && tokens.length != 9) {
				  logger.warn("Line <" + strLine + "> skipped. Line to be parsed has " + tokens.length + " instead of 8 or 9 elements");
				  continue;
				}
				int delta = 0;
				if (tokens.length == 9)
				  delta = 1;
				  
				Long timestamp;
				if (tokens[0].length() == 0)
					timestamp = null;
				else
					timestamp = Long.valueOf(tokens[0]);
				
				// convert strings to double and int values
				Double[] readingsDouble = new Double[5];
				int ind_d = 0;
				Integer[] readingsInt = new Integer[2];
				int ind_i = 0;
				for (int i = 1+delta; i < 8+delta; i++) {
					if (i == 2 || i == 4) {
						if (tokens[i].length() == 0)
					    readingsInt[ind_i] = null;
					  else
					    readingsInt[ind_i] = Integer.valueOf(tokens[i]);
						ind_i++;
					}
					else {
					  if (tokens[i].length() == 0)
					    readingsDouble[ind_d] = null;
					  else
					    readingsDouble[ind_d] = Double.valueOf(tokens[i]);
					}
				}
				
				// If there is no timestamp or location information is missing then skip this line.
				if (timestamp == null || timestamp == 0 ||
						readingsDouble[2] == null || readingsDouble[2] == 0 ||
						readingsDouble[3] == null || readingsDouble[3] == 0)
					continue;
				
				// Convert GPS coordinate.
				int lati = readingsDouble[2].intValue();
				readingsDouble[2] = ((readingsDouble[2]-lati)*60.0) + lati*100.0;
				int longi = readingsDouble[3].intValue();
				readingsDouble[3] = ((readingsDouble[3]-longi)*60.0) + longi*100.0;
				
				StreamElement curr_data = new StreamElement(dataField, new Serializable[] {data.getData(dataField[1].getName()), data.getData(dataField[1].getName()), timestamp, readingsDouble[0], readingsInt[0], readingsDouble[1], readingsInt[1], readingsDouble[2], readingsDouble[3], readingsDouble[4], null});
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
		
		parseData(file, inputStreamName, data);
		logger.debug("parsed new incoming file (" + file.getAbsolutePath() + ")");
	}
}
