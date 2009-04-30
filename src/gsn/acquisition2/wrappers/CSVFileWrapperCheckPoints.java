package gsn.acquisition2.wrappers;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;
import org.apache.log4j.Logger;
import gsn.acquisition2.SafeStorageDB;

public class CSVFileWrapperCheckPoints{

	private final transient Logger logger = Logger.getLogger( CSVFileWrapperCheckPoints.class );
	
    private String dbUrl = null;

	private Connection connection = null;
	
	private PreparedStatement psReadCheckpoint = null;
	
	private PreparedStatement psUpdateCheckPoint = null;
	
	private PreparedStatement psCleanCheckPoint = null;
	
	private PreparedStatement psListCheckpoint = null;

	private long csvFilePathHash;
		
	public CSVFileWrapperCheckPoints (String csvFilePath, String requestername) {
		// We use a hash of the file path instead of the actual path concatenated to the requestername
		Checksum checkSum = new Adler32 () ;
		FilterInputStream cis = new CheckedInputStream(new ByteArrayInputStream((csvFilePath + requestername).getBytes()), checkSum);
		BufferedReader reader = new BufferedReader (new InputStreamReader(cis)) ;
		try {
			while (reader.read() != -1) ;
		} catch (IOException e) {
			logger.debug(e.getMessage());
		}
		this.csvFilePathHash = checkSum.getValue();
		//
		prepareConnectionAndTableIfNeeded();
	}

	public void update (long line, long checksum) {
		if (connection == null) return;		
		try {
			psUpdateCheckPoint.setLong(1, line);
			psUpdateCheckPoint.setLong(2, checksum);
			psUpdateCheckPoint.executeUpdate();
		} catch (SQLException e) {
			logger.debug(e.getMessage());
		}

	}
	
	public boolean check (long line, long checksum) {
		if (connection == null) return false;		
		try {
			psReadCheckpoint.setLong(1, line);
			ResultSet rs = psReadCheckpoint.executeQuery();
			if (rs.next()) {
				return rs.getLong("checksum") == checksum;
			}
		} catch (SQLException e) {
			logger.debug(e.getMessage());
		}
		return false;
	}
	
	public void clean (long fromLine, long toLine) {
		if (connection == null) return;
		try {
			psCleanCheckPoint.setLong(1, toLine);
			psCleanCheckPoint.setLong(2, fromLine);
			psCleanCheckPoint.executeUpdate();
		} catch (SQLException e) {
			logger.debug(e.getMessage());
		}
	}
	
	public ArrayList<Long> list () {
		ArrayList<Long> list = new ArrayList<Long> ();
		if (connection == null) return list;
		try {
			ResultSet checkpoints = psListCheckpoint.executeQuery();
			while (checkpoints.next()) {
				list.add(checkpoints.getLong("line"));
			}
		} catch (SQLException e) {
			logger.debug(e.getMessage());
		}
		return list;
	}
	
	private void prepareConnectionAndTableIfNeeded () {
        try {
			Class.forName("org.h2.Driver");

            dbUrl = SafeStorageDB.getDBUrl("csv_checkpoints");

            logger.warn("Connecting to : " + dbUrl);

			if (connection ==null || connection.isClosed()) connection = DriverManager.getConnection(dbUrl, "sa", "");
			// Create checkpoints table if not exists
			connection.prepareStatement("create table if not exists checkpoints (pk bigint not null identity primary key, csvfilepathHash bigint not null, line bigint not null, checksum bigint not null)").executeUpdate();
			//
			psReadCheckpoint = connection.prepareStatement("select checksum from checkpoints where csvfilepathHash = " + csvFilePathHash + " and line = ?");
			psUpdateCheckPoint = connection.prepareStatement("merge into checkpoints key(line, csvfilepathHash) values(null," + csvFilePathHash + ",?,?)");
			psCleanCheckPoint = connection.prepareStatement("delete from checkpoints where csvfilepathHash = " + csvFilePathHash + " and line < ? and line > ? ");
			psListCheckpoint = connection.prepareStatement("select line from checkpoints where csvfilepathHash = " + csvFilePathHash);
		} catch (ClassNotFoundException e) {
			connection = null;
			logger.warn("Unable to create the CSV checkpoint storage, wrapper will run without checkpoints.");
		} catch (SQLException e) {
			logger.warn(e.getMessage());
			connection = null;
		}
	}
}
