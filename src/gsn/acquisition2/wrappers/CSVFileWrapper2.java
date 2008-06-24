package gsn.acquisition2.wrappers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.apache.log4j.Logger;

public class CSVFileWrapper2 extends AbstractWrapper2 {

	private File dataFile = null;

	private long nbOfProcessedLines = 0;

	private long lastModified = 0; 

	private static int threadCounter = 0;

	private CSVFileWrapperParameters parameters = null;

	private final transient Logger logger = Logger.getLogger( CSVFileWrapper2.class );

	public boolean initialize (  ) {
		logger.warn("cvsfile wrapper initialize started...");
		try {
			parameters = new CSVFileWrapperParameters () ;
			parameters.initParameters(getActiveAddressBean());
			dataFile = new File (parameters.getCsvSourceFilePath()) ;
			if (! dataFile.isFile() || ! dataFile.exists()) {
				logger.error("The file >" + parameters.getCsvSourceFilePath() + "< does not exists.");
				return false;
			}
		}
		catch (RuntimeException e) {
			logger.error(e.getMessage(), e);
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
					fileReader = new BufferedReader(new FileReader(parameters.getCsvSourceFilePath()));
					// Produce the data by reading the file
					int read_lines = 0;
					while ((nextLine = fileReader.readLine()) != null) {
						read_lines++;
						if (! nextLine.matches("\\s*$")) {
							if (read_lines > parameters.getCsvSkipLines()) {
								if (read_lines > nbOfProcessedLines) {
									logger.debug("Next line: " + nextLine);
									postStreamElement(nextLine, System.currentTimeMillis( ));
									nbOfProcessedLines = read_lines;
								}
								else logger.debug("Line already processed");
							}
							else logger.debug("Line skipped");
						}
						else logger.debug("Empty line skipped");
					}
					logger.debug("EOF reached");
					fileReader.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}
			else logger.debug("The data file has not been modified.");
			try {
				Thread.sleep(parameters.getCsvUpdateDelay());
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
