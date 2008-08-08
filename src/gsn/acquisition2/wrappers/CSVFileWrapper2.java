package gsn.acquisition2.wrappers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;
import org.apache.log4j.Logger;

public class CSVFileWrapper2 extends AbstractWrapper2 {

	private File dataFile = null;

	private long lastModified = 0; 

	private static final int CHECK_POINT_DISTANCE = 100; // nb of lines between two check points

	private static int threadCounter = 0;

	private CSVFileWrapperParameters parameters = null;

	private CSVFileWrapperCheckPoints checkPoints = null;

	private ArrayList<String> lineBuffer = null;
	
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
			lineBuffer = new ArrayList<String> () ;
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
					Checksum checkSum = new Adler32 () ;
					FilterInputStream cis = new CheckedInputStream(new FileInputStream(parameters.getCsvSourceFilePath()), checkSum);
					fileReader = new BufferedReader (new InputStreamReader(cis)) ;
					// Produce the data by reading the file
					int read_lines = 0;
					while ((nextLine = fileReader.readLine()) != null) {
						read_lines++;
						if (! nextLine.matches("\\s*$")) {
							if (read_lines > parameters.getCsvSkipLines()) {
								lineBuffer.add(nextLine);
								if (read_lines % CHECK_POINT_DISTANCE == 0) {
									checkThisPoint (read_lines, checkSum) ;
								}
							}
							else logger.debug("Line skipped");
						}
						else logger.debug("Empty line skipped");
					}
					logger.debug("EOF reached");
					checkThisPoint (read_lines, checkSum) ;					
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
	
	private void sendBuffer () {
		// send all the lines from the line buffer
		Iterator<String> iter = lineBuffer.iterator();
		while (iter.hasNext()) {
			postStreamElement(iter.next(), System.currentTimeMillis());
		}
		lineBuffer.clear();
	}
	
	private void checkThisPoint(long read_lines, Checksum checkSum) {
		if (checkPoints.check(read_lines, checkSum)) {
			logger.warn("Check Point at line >" + read_lines + "< checked successfully. No new data to process from the last Check Point.");
			lineBuffer.clear();
		}
		else {
			logger.warn("Check Point at line >" + read_lines + "< doesn't match. Sending the data from the last Check Point.");
			sendBuffer();
			checkPoints.update(read_lines, checkSum);
		}
		checkSum.reset();
	}

	public void finalize (  ) {
		threadCounter--;  
	}

	public void setTableName(String tableName) {
		super.setTableName(tableName);
		checkPoints = new CSVFileWrapperCheckPoints ("csv" + getTableName() + ".checkpoints") ;
	}

	public String getWrapperName() {
		return "CSV File Wrapper";
	}
}
