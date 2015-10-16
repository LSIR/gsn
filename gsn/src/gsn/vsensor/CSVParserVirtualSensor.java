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

	private final static String DEVICE_ID_FIELD_NAME = "device_id";
	private final static String FILE_FIELD_NAME = "file";

    private final transient Logger logger = Logger.getLogger(CSVParserVirtualSensor.class);

    private CSVHandler handler = new CSVHandler();

    //TODO: make configurable in VS xml file
	private static final DataField[] copiedDataFields = {
		new DataField("DEVICE_ID", "INTEGER"),
		new DataField("DEVICE_TYPE", "VARCHAR(32)"),
		new DataField("FILE", "VARCHAR(255)"),
		new DataField("SIZE", "BIGINT")
	};
    
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
		if (data.getData(FILE_FIELD_NAME) == null) {
			logger.error("the received stream element from input stream \"" + inputStreamName + "\" does not contain the field: " + FILE_FIELD_NAME + ". Cannot process this element.");
			return;
		}
		
		File file = new File("");
		try {
			String relativeFile = (String) data.getData(FILE_FIELD_NAME);
			if (relativeFile != null) {
				file = new File(new File(storage_directory, Integer.toString((Integer)data.getData(DEVICE_ID_FIELD_NAME))).getPath(), relativeFile);
				file = file.getAbsoluteFile();
			
		        FileReader reader = new FileReader(file);
		        ArrayList<TreeMap<String, Serializable>> output = handler.work(reader, null);
		        Serializable [] s = new Serializable[copiedDataFields.length];
		        for (int i=0; i<copiedDataFields.length; i++) {
		        	s[i] = data.getData(copiedDataFields[i].getName());
		        }
		        
		        for (TreeMap<String, Serializable> se : output)
		            super.dataAvailable(inputStreamName, new StreamElement(new StreamElement(se, handler.getDataFields()), copiedDataFields, s));
			}
		} catch (Exception e) {
            logger.error(e.getMessage() + " :: " + file.getAbsolutePath(), e);
		}
	}

}
