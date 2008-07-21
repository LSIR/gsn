package gsn.acquisition2.wrappers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.TreeMap;
import java.util.zip.Checksum;
import org.apache.log4j.Logger;

public class CSVFileWrapperCheckPoints{

	private final transient Logger logger = Logger.getLogger( CSVFileWrapperCheckPoints.class );

	private File checkPointsFile;

	public CSVFileWrapperCheckPoints (String path) {
		checkPointsFile = new File (path);
	}

	public void update (long lineNumber, Checksum checkSum) {
		TreeMap<Long,Long> checkPoints = readCheckPoints ();
		checkPoints.put(lineNumber, checkSum.getValue());
		writeCheckPoints(checkPoints);
	}

	
	public boolean check (long lineNumber, Checksum checkSum) {
		TreeMap<Long,Long> checkPoints = readCheckPoints ();
		Long checkPoint = checkPoints.get(lineNumber);
		if (checkPoint == null) return false; 
		else return checkPoint.doubleValue() == checkSum.getValue();
	}

	private void writeCheckPoints (TreeMap<Long, Long> checkPoints) {
		try {
			ObjectOutput oos = new ObjectOutputStream (new BufferedOutputStream (new FileOutputStream (checkPointsFile))) ;
			oos.writeObject(checkPoints);
			oos.flush();
			if (oos != null) oos.close();	
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage());
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private @SuppressWarnings("unchecked") TreeMap<Long, Long> readCheckPoints () {
		TreeMap<Long, Long> checkPoints = null;
		try {
			// Get the list of check points from the file or create a new list if the file is new
			if (! checkPointsFile.createNewFile()) {			
				ObjectInput ois = new ObjectInputStream (new BufferedInputStream (new FileInputStream(checkPointsFile))) ;
				checkPoints = (TreeMap<Long, Long>) ois.readObject();
				if (ois != null) ois.close();
			}
			else {
				checkPoints = new TreeMap<Long, Long> () ;
				writeCheckPoints (checkPoints);
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		}
		return checkPoints;
	}
}
