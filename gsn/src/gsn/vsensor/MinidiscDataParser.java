package gsn.vsensor;

import gsn.Main;
import gsn.beans.DataField;
import gsn.beans.StreamElement;


import java.io.File;
import java.io.Serializable;
import java.io.BufferedReader;
import java.io.FileReader;

import java.util.TimeZone;
import java.util.Date;
import java.sql.Connection;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;

public class MinidiscDataParser extends BridgeVirtualSensorPermasense {

	private static final transient Logger logger = Logger.getLogger(MinidiscDataParser.class);
	
	private String storage_directory = null;
	private long last_db_timestamp = 0;
	
	private static DataField[] dataField = {
			new DataField("POSITION", "INTEGER"),
			new DataField("DEVICE_ID", "INTEGER"),
			new DataField("GENERATION_TIME", "BIGINT"),

			new DataField("VALID", "DOUBLE"),
			new DataField("NUMBER", "DOUBLE"),
			new DataField("DIAM", "DOUBLE"),
			new DataField("LDSA", "DOUBLE"),
			new DataField("DIFFUSION", "DOUBLE"),
			new DataField("FILTER", "DOUBLE"),
			new DataField("UCOR", "DOUBLE"),
			new DataField("FLOW", "DOUBLE"),
			new DataField("TEMP", "DOUBLE"),
			new DataField("LAT", "DOUBLE"),
			new DataField("LON", "DOUBLE"),
			new DataField("RAW_DATA", "VARCHAR(256)"),
			
			new DataField("DATA_IMPORT_SOURCE", "SMALLINT")};
	
	
	@Override
	public boolean initialize() {
		
		boolean ret = super.initialize();
		
		storage_directory = getVirtualSensorConfiguration().getStorage().getStorageDirectory();
		if (storage_directory != null) {
			storage_directory = new File(storage_directory, deployment).getPath();
		}
		
		// Get the latest timestamp from the database
		Connection conn = null;
		ResultSet rs = null;
		try {
			conn = Main.getStorage(getVirtualSensorConfiguration().getName()).getConnection();
			StringBuilder query = new StringBuilder();
			query.append("select generation_time from ").append(getVirtualSensorConfiguration().getName()).append(" order by generation_time desc limit 1");
			rs = Main.getStorage(getVirtualSensorConfiguration().getName()).executeQueryWithResultSet(query, conn);
			
			if (rs.next()) {
				// get timestamp
				last_db_timestamp = rs.getLong("generation_time");
			} else {
				last_db_timestamp = 0;
				logger.warn("no timestamp available in the database, parse all data");
			}
			rs.close();
			conn.close();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		
		return ret;
	}
	
	private void parseData(File file, String inputStreamName, StreamElement data) {
		try
		{
			// define date format and set time zone used by the MiniDiSC
			SimpleDateFormat df = new SimpleDateFormat( "dd.MM.yyyy HH:mm:ss z" );
		  df.setTimeZone( TimeZone.getTimeZone( "utc" ) );
		
			//create BufferedReader to read csv file
			BufferedReader br = new BufferedReader( new FileReader(file.getAbsolutePath()));
			String strLine = "";
			Date dt;
			
			// skip first header line
			br.readLine();
			
			//read comma separated file line by line
			while( (strLine = br.readLine()) != null)
			{
				String[] tokens = strLine.split(";",-1);
				
				dt = df.parse(tokens[0]);
				// Use same time format as on the core station (UTC+1h)
				long curr_timestamp = dt.getTime() + 3600 * 1000;
				
				// convert strings to double values
				Double[] readings = new Double[11];
				for (int i = 1; i < 12; i++) {
				  if (tokens[i].length() == 0)
				    readings[i-1] = null;
				  else
				    readings[i-1] = Double.valueOf(tokens[i]);
				}
				
				// Only push data to database if the timestamp is bigger than the one from the latest entry
				if (curr_timestamp > last_db_timestamp) {
					StreamElement curr_data = new StreamElement(dataField, new Serializable[] {data.getData(dataField[0].getName()), data.getData(dataField[1].getName()), curr_timestamp, readings[0], readings[1], readings[2], readings[3], readings[4], readings[5], readings[6], readings[7], readings[8], readings[9], readings[10], strLine, data.getData(dataField[15].getName())});
					last_db_timestamp = curr_timestamp;
					super.dataAvailable(inputStreamName, curr_data);
				}
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
