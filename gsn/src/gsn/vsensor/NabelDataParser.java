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

public class NabelDataParser extends BridgeVirtualSensorPermasense {

	private static final transient Logger logger = Logger.getLogger(NabelDataParser.class);

	private String storage_directory = null;
	private long last_nabel_timestamp = 0;
	
	private static DataField[] dataField = {			
			new DataField("NABEL_TIMESTAMP", "BIGINT"),
			new DataField("OZONE_PPB", "DOUBLE"),
			new DataField("CO_PPM", "DOUBLE"),
			new DataField("RAW_DATA", "VARCHAR(64)")};
	
	@Override
	public boolean initialize() {
		boolean ret = super.initialize();
		storage_directory = getVirtualSensorConfiguration().getStorage().getStorageDirectory();
		if (storage_directory != null) {
			storage_directory = new File(storage_directory, deployment).getPath();
		}
		
		// Get the newest nabel_timestamp from the database
		Connection conn = null;
		ResultSet rs = null;
		try {
			conn = Main.getStorage(getVirtualSensorConfiguration().getName()).getConnection();
			StringBuilder query = new StringBuilder();
			query.append("select nabel_timestamp from ").append(getVirtualSensorConfiguration().getName()).append(" order by nabel_timestamp desc limit 1");
			rs = Main.getStorage(getVirtualSensorConfiguration().getName()).executeQueryWithResultSet(query, conn);
			
			if (rs.next()) {
				// get nabel_timestamp
				last_nabel_timestamp = rs.getLong("nabel_timestamp");
			} else {
				last_nabel_timestamp = 0;
				logger.warn("no last nabel_timestamp available in the database");
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		
		return ret;
	}
	
	
	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
		File file = new File(new File(storage_directory, Integer.toString((Integer)data.getData("device_id"))).getPath(), (String) data.getData("relative_file"));
		file = file.getAbsoluteFile();
		
		logger.info("new incoming file (" + file.getAbsolutePath() + ")");
		
		try
		{
			// define date format and set time zone used by NABEL
			SimpleDateFormat df = new SimpleDateFormat( "dd.MM.yy HH:mm" );
		    df.setTimeZone( TimeZone.getTimeZone( "cest" ) );
		
			//create BufferedReader to read csv file
			BufferedReader br = new BufferedReader( new FileReader(file.getAbsolutePath()));
			String strLine = "";
			Date dt;
			
			// skip first two header lines
			br.readLine();
			br.readLine();
			
			//read comma separated file line by line
			while( (strLine = br.readLine()) != null)
			{
				String[] tokens = strLine.split(";",-1);
				
				dt = df.parse(tokens[0]);
				// Use same time format as on the core station (UTC+1h)
				long curr_nabel_timestamp = dt.getTime() - 3600 * 1000;
				
				// Only push data to database if its timestamp is bigger than the one from the last entry
				if (curr_nabel_timestamp > last_nabel_timestamp) {
					data = new StreamElement(data, dataField, new Serializable[] {curr_nabel_timestamp, tokens[1], tokens[2], strLine});
					last_nabel_timestamp = curr_nabel_timestamp;
					super.dataAvailable(inputStreamName, data);
				}
			}
		}
		catch(Exception e)
		{
			logger.error(e.getMessage(), e);
		}
	}
}
