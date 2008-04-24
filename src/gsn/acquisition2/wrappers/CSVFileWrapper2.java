package gsn.acquisition2.wrappers;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import org.apache.log4j.Logger;

public class CSVFileWrapper2 extends AbstractWrapper2 {

	private static final String CSV_SKIP_LINES = "csv-skip-lines";
	private static final String CSV_SKIP_LINES_DEFAULT = "0";
	private int csvSkipLines;
	
	private BufferedReader fileReader = null;
	
	private static int threadCounter = 0;

	private final transient Logger logger = Logger.getLogger( CSVFileWrapper2.class );

	public boolean initialize (  ) {
		logger.warn("cvsfile wrapper initialize started...");
		String path = null;
		try {
			path = getActiveAddressBean().getPredicateValue(CSVFileWrapperFormat.CSV_SOURCE_FILE_PATH);
			fileReader = new BufferedReader(new FileReader(path));
		} catch (FileNotFoundException e) {
			logger.error("The file "+path+" is not accessible!", e);
			return false;
		}
		csvSkipLines = Integer.parseInt(getActiveAddressBean().getPredicateValueWithDefault(CSV_SKIP_LINES, CSV_SKIP_LINES_DEFAULT));
		//
		setName( "CVSFileWrapper-Thread:" + ( ++threadCounter ) );
		logger.warn("cvsfile wrapper initialize completed ...");
		return true;
	}

	public void run ( ) {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		logger.warn("cvsfile wrapper run started...");
		// Produce the data by reading the file
		String nextLine = null;
		try {
			int skip_lines = csvSkipLines;
			while ((nextLine = fileReader.readLine()) != null) {
				if (skip_lines-- <= 0) {
					postStreamElement(nextLine, System.currentTimeMillis( ));
				}
			}
			fileReader.close();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		logger.warn("cvsfile wrapper run finished...");
	}

	public void finalize (  ) {
		threadCounter--;  
	}

	public String getWrapperName() {
		return "CSV File Wrapper";
	}
}
