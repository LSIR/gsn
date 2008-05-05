package gsn.acquisition2.wrappers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.apache.log4j.Logger;

public class CSVFileWrapper2 extends AbstractWrapper2 {

	private static final String CSV_SKIP_LINES = "csv-skip-lines";
	private static final String CSV_SKIP_LINES_DEFAULT = "0";
	private int csvSkipLines;

	private static final String CSV_UPDATE_DELAY = "csv-update-delay";
	private static final String CSV_UPDATE_DELAY_DEFAULT = "5000";
	private static long updateDelay;

	private File dataFile = null;

	private long nbOfProcessedLines = 0;

	private long lastModified = 0; 

	private static int threadCounter = 0;

	private String path = null;

	private final transient Logger logger = Logger.getLogger( CSVFileWrapper2.class );

	public boolean initialize (  ) {
		logger.warn("cvsfile wrapper initialize started...");

		path = getActiveAddressBean().getPredicateValue(CSVFileWrapperFormat.CSV_SOURCE_FILE_PATH);
		csvSkipLines = Integer.parseInt(getActiveAddressBean().getPredicateValueWithDefault(CSV_SKIP_LINES, CSV_SKIP_LINES_DEFAULT));
		updateDelay = Long.parseLong(getActiveAddressBean().getPredicateValueWithDefault(CSV_UPDATE_DELAY, CSV_UPDATE_DELAY_DEFAULT));
		dataFile = new File (path) ;
		if (! dataFile.isFile() || ! dataFile.exists()) {
			logger.error("The file >" + path + "< does not exists.");
			return false;
		}

		setName( "CVSFileWrapper-Thread:" + ( ++threadCounter ) );
		logger.warn("cvsfile wrapper initialize completed ...");
		return true;
	}

	public void run ( ) {
		logger.warn("cvsfile wrapper run started...");

		BufferedReader fileReader = null;
		String nextLine = null;

		while (true) {
			if (lastModified < dataFile.lastModified()) {
				lastModified = dataFile.lastModified();
				try {
					fileReader = new BufferedReader(new FileReader(path));
					// Produce the data by reading the file
					int skip_lines = csvSkipLines;
					int processed_lines = 0;
					while ((nextLine = fileReader.readLine()) != null) {
						if (skip_lines-- <= 0) {
							if (processed_lines > nbOfProcessedLines) {
								postStreamElement(nextLine, System.currentTimeMillis( ));
								nbOfProcessedLines = processed_lines;
							}
						}
						processed_lines++;
					}
					fileReader.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}
			else logger.debug("The data file has not been modified.");
			try {
				Thread.sleep(updateDelay);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	public void finalize (  ) {
		threadCounter--;  
	}

	public String getWrapperName() {
		return "CSV File Wrapper";
	}
}
