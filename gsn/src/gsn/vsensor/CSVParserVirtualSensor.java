package gsn.vsensor;

import java.io.File;
import java.io.FileReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.wrappers.general.CSVHandler;

public class CSVParserVirtualSensor extends BridgeVirtualSensorPermasense {
	
	private final static String FILE_FIELD_NAME = "file";

    private final transient Logger logger = Logger.getLogger(CSVParserVirtualSensor.class);

    private CSVHandler handler = new CSVHandler();
    
	private String storage_directory = null;

	@Override
	public boolean initialize() {
		boolean ret = super.initialize();
		storage_directory = getVirtualSensorConfiguration().getStorage().getStorageDirectory();
		if (storage_directory != null) {
			storage_directory = new File(storage_directory, deployment).getPath();
		}
		
        String csvFields = getVirtualSensorConfiguration().getMainClassInitialParams().get("fields");
        if (csvFields == null) {
        	logger.error("csvFields has to be specified");
        	return false;
        }
        String csvFormats = getVirtualSensorConfiguration().getMainClassInitialParams().get("formats");
        if (csvFormats == null) {
        	logger.error("csvFormats has to be specified");
        	return false;
        }
        //String csvSeparator = addressBean.getPredicateValueWithDefault("separator",",");
        String value = getVirtualSensorConfiguration().getMainClassInitialParams().get("separator");
        String csvSeparator = (value == null || value.length() == 0) ? "," : value;
        value = getVirtualSensorConfiguration().getMainClassInitialParams().get("quote");
        String csvStringQuote = (value == null) ? "\"" : value;
        value = getVirtualSensorConfiguration().getMainClassInitialParams().get("skip-first-lines");
        int skipFirstXLine = (value == null) ? 0 : Integer.parseInt(value);
        value = getVirtualSensorConfiguration().getMainClassInitialParams().get("timezone");
        String timezone = (value == null) ? CSVHandler.LOCAL_TIMEZONE_ID : value;
        value = getVirtualSensorConfiguration().getMainClassInitialParams().get("bad-values");
        String nullValues = (value == null) ? "" : value;
        
        boolean hasFileField = false;
		for (DataField d: getVirtualSensorConfiguration().getOutputStructure()) {
			if (d.getName().equalsIgnoreCase(FILE_FIELD_NAME))
				hasFileField = true;
		}
		if (!hasFileField) {
			logger.error("the output structure has to contain the field: " + FILE_FIELD_NAME);
			return false;
		}

        if (csvSeparator != null && csvSeparator.length() != 1) {
            logger.warn("The provided CSV separator:>" + csvSeparator + "< should only have  1 character, thus ignored and instead \",\" is used.");
            csvSeparator = ",";
        }

        if (csvStringQuote.length() != 1) {
            logger.warn("The provided CSV quote:>" + csvSeparator + "< should only have 1 character, thus ignored and instead '\"' is used.");
            csvStringQuote = "\"";
        }

        try {
            if (!handler.initialize(csvFields, csvFormats, csvSeparator.toCharArray()[0], csvStringQuote.toCharArray()[0], skipFirstXLine, nullValues, timezone))
                return false;

        } catch (Exception e) {
            logger.error("Loading the csv-wrapper failed:" + e.getMessage(), e);
            return false;
        }

        return ret;
	}
	
	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
		File file = new File("");
		try {
			String relativeFile = (String) data.getData(FILE_FIELD_NAME);
			if (relativeFile != null) {
				file = new File(storage_directory, relativeFile);
				file = file.getAbsoluteFile();
			
		        FileReader reader = new FileReader(file);
		        ArrayList<TreeMap<String, Serializable>> output = handler.work(reader, null);
		        
		        for (TreeMap<String, Serializable> se : output) {
		            StreamElement streamElement = new StreamElement(se, handler.getDataFields());
		            super.dataAvailable(inputStreamName, streamElement);
		        }
			}
		} catch (Exception e) {
            logger.error(e.getMessage() + " :: " + file.getAbsolutePath(), e);
		}
	}

}
