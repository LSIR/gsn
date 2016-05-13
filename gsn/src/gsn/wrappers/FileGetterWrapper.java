package gsn.wrappers;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.OperationNotSupportedException;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;

import gsn.Main;
import gsn.beans.DataField;
import gsn.beans.InputInfo;

public class FileGetterWrapper extends AbstractWrapper {
	
	private ArrayList<DataField> outputStructure;

	private final transient Logger logger = Logger.getLogger( FileGetterWrapper.class );

	private File deploymentBinaryDir = null;
	private String subdirectoryName = null;
	private Pattern[] filenamePatternArray;
	private boolean deviceIdFromFilename;
	final static private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HHmmss");

    private static final String PARAM_SUBDIRECTORY_NAME = "subdirectory-name";
    private static final String PARAM_FILENAME_REGEX = "filename-regex";
    private static final String PARAM_DEVICEID_FROM_FILENAME = "deviceid-from-filename";
    
    private static final String COMMAND_NAME = "files";
    
    private static final String DATAFIELD_GENERATION_TIME = "GENERATION_TIME";
    private static final String DATAFIELD_DEVICE_ID = "DEVICE_ID";
    private static final String DATAFIELD_RELATIVE_FILE = "RELATIVE_FILE";
    private static final String DATAFIELD_SIZE_FILE = "SIZE_FILE";
    
    private static final String DATAFIELD_BINARY_SUFFIX = "_file";

	@Override
	public boolean initialize() {
		String rootBinaryDir = null;
		format.setTimeZone(Main.getContainerConfig().getTimeZone());
		outputStructure = new ArrayList<DataField>();
		
		try {
			subdirectoryName = getActiveAddressBean().getPredicateValueWithException(PARAM_SUBDIRECTORY_NAME);
		} catch (Exception e){
			logger.error(e.getMessage());
			return false;
		}
		
		try {
			rootBinaryDir = getActiveAddressBean().getVirtualSensorConfig().getStorage().getStorageDirectory();
		} catch (NullPointerException e){
			logger.error(e.getMessage());
			return false;
		}
		
		File f = new File(rootBinaryDir);
		if (!f.isDirectory()) {
			logger.error(rootBinaryDir + " is not a directory");
			return false;
		}
		
		if (!f.canWrite()) {
			logger.error(rootBinaryDir + " is not writable");
			return false;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("binary root directory: " + rootBinaryDir);
		}
		
		deploymentBinaryDir = new File(rootBinaryDir, getActiveAddressBean().getVirtualSensorName().split("_")[0].toLowerCase());
		if (!deploymentBinaryDir.exists()) {
	    	if (!deploymentBinaryDir.mkdirs()) {
	    		logger.error("could not mkdir >" + deploymentBinaryDir + "<");
	    		return false;
			}
	    	else
	    		logger.info("created new storage directory >" + deploymentBinaryDir + "<");
		}

		DataField[] commandEntries = null;
		for (int i=0; i<getActiveAddressBean().getVirtualSensorConfig().getWebinput().length; i++) {
			if (getActiveAddressBean().getVirtualSensorConfig().getWebinput()[i].getName().equalsIgnoreCase(COMMAND_NAME)) {
				commandEntries = getActiveAddressBean().getVirtualSensorConfig().getWebinput()[i].getParameters();
				break;
			}
		}
		if (commandEntries == null) {
			logger.error("command name " + COMMAND_NAME + " has to be existing in the web-input section");
			return false;
		}
		
		outputStructure.add(new DataField(DATAFIELD_DEVICE_ID, "INTEGER"));
		outputStructure.add(new DataField(DATAFIELD_GENERATION_TIME, "BIGINT"));
		
		int fileCnt = 0;
		for (DataField field: commandEntries) {
			if (field.getName().endsWith(DATAFIELD_BINARY_SUFFIX))
				fileCnt++;
			else {
				for (DataField outputField: getActiveAddressBean().getVirtualSensorConfig().getOutputStructure()) {
					if (outputField.getName().equalsIgnoreCase(field.getName())
							&& !outputField.getName().equalsIgnoreCase(DATAFIELD_DEVICE_ID)
							&& !outputField.getName().equalsIgnoreCase(DATAFIELD_GENERATION_TIME)) {
						outputStructure.add(outputField);
						break;
					}
				}
			}
		}
		
		filenamePatternArray = new Pattern [fileCnt];
		boolean hasRegex = false;
		for (int i=0; i<fileCnt; i++) {
			String regex = getActiveAddressBean().getPredicateValue(PARAM_FILENAME_REGEX + (i+1));
			if (regex != null) {
				hasRegex = true;
				filenamePatternArray[i] = Pattern.compile(regex);
				logger.info(DATAFIELD_RELATIVE_FILE + (i+1) + " has to match regular expression: " + regex);
				
				deviceIdFromFilename = Boolean.parseBoolean(getActiveAddressBean().getPredicateValue(PARAM_DEVICEID_FROM_FILENAME));
				if (deviceIdFromFilename)
					logger.info("device id will be extracted from " + DATAFIELD_RELATIVE_FILE + (i+1) + " using the first group from regular expresion: " + regex);
			}
			else
				filenamePatternArray[i] = null;
			
			outputStructure.add(new DataField(DATAFIELD_RELATIVE_FILE + (i+1), "VARCHAR(255)"));
			outputStructure.add(new DataField(DATAFIELD_SIZE_FILE + (i+1), "BIGINT"));
		}
		
		
		if (!hasRegex && Boolean.parseBoolean(getActiveAddressBean().getPredicateValue(PARAM_DEVICEID_FROM_FILENAME))) {
			logger.error(PARAM_DEVICEID_FROM_FILENAME + " parameter can only be used together with filename-regex");
			return false;
		}
		
		return true;
	}
	
	@Override
	public InputInfo sendToWrapper ( String action , String [ ] paramNames , Serializable [ ] paramValues ) throws OperationNotSupportedException {
		if( action.equalsIgnoreCase(COMMAND_NAME) ) {
			ArrayList<Serializable> output = new ArrayList<Serializable>();
			ArrayList<Serializable> additionalFields = new ArrayList<Serializable>();
			try {
				long gentime = System.currentTimeMillis();
				String deviceid = null;
				Vector<BinaryFile> inputFileItems = new Vector<BinaryFile>();
				
				for( int i=0; i<paramNames.length; i++ ) {
					String tmp = paramNames[i];
					if( tmp.equalsIgnoreCase(DATAFIELD_DEVICE_ID) )
						deviceid = (String) paramValues[i];
					else if( tmp.equalsIgnoreCase(DATAFIELD_GENERATION_TIME) ) {
						try {
							gentime = Long.parseLong((String) paramValues[i]);
						} catch(Exception e) {
							logger.error("Could not interprete " + DATAFIELD_GENERATION_TIME + " argument (" + ((String) paramValues[i]) +") as integer");
							return new InputInfo(getActiveAddressBean().toString(), "Could not interprete " + DATAFIELD_GENERATION_TIME + " argument (" + ((String) paramValues[i]) +") as integer", false);
						}
					}
					else if( tmp.endsWith(DATAFIELD_BINARY_SUFFIX) )
						inputFileItems.add(new BinaryFile((FileItem) paramValues[i], tmp));
					else {
						boolean hit = false;
						for (DataField df: getOutputFormat()) {
							if (tmp.equalsIgnoreCase(df.getName())) {
								additionalFields.add(paramValues[i]);
								hit = true;
								break;
							}
						}
						if (!hit)
							logger.warn("unknown upload field: " + tmp + " -> skip it");
					}
				}
				
				FileItem lastFileItem = null;
				
				for (int i=0; i<inputFileItems.size(); i++) {
					if (!inputFileItems.get(i).getFileItem().getName().isEmpty() && filenamePatternArray[i] != null) {
						Matcher m = filenamePatternArray[i].matcher(inputFileItems.get(i).getFileItem().getName());
						if (!m.matches()) {
							logger.error("filename " + inputFileItems.get(i).getFileItem().getName() + " does not match regular expression " + filenamePatternArray[i].toString());
							return new InputInfo(getActiveAddressBean().toString(), "filename " + inputFileItems.get(i).getFileItem().getName() + " does not match regular expression " + filenamePatternArray[i].toString(), false);
						}
		
						if (deviceIdFromFilename) {
							String id = m.group(1);
							if (deviceid != null && deviceid.compareTo(id) != 0 && lastFileItem != null) {
								logger.error("the device id extracted from " + lastFileItem.getName() + " and " + inputFileItems.get(i).getFileItem().getName() + " are not equal");
								return new InputInfo(getActiveAddressBean().toString(), "the device id extracted from " + lastFileItem.getName() + " and " + inputFileItems.get(i).getFileItem().getName() + " are not equal", false);
							}
							deviceid = id;
							lastFileItem = inputFileItems.get(i).getFileItem();
						}
					}
				}
				
				if (deviceid.equals("")) {
					logger.error(DATAFIELD_DEVICE_ID + " argument has to be an integer between 0 and 65535");
					return new InputInfo(getActiveAddressBean().toString(), DATAFIELD_DEVICE_ID + " argument has to be an integer between 0 and 65535", false);
				}
				
				Integer id;
				try {
					id = Integer.parseInt(deviceid);
				} catch(Exception e) {
					logger.error("Could not interprete " + DATAFIELD_DEVICE_ID + " argument (" + deviceid +") as integer");
					return new InputInfo(getActiveAddressBean().toString(), "Could not interprete " + DATAFIELD_DEVICE_ID + " argument (" + deviceid +") as integer", false);
				}
				if (id < 0 || id > 65534) {
					logger.error(DATAFIELD_DEVICE_ID + " argument has to be an integer between 0 and 65535");
					return new InputInfo(getActiveAddressBean().toString(), DATAFIELD_DEVICE_ID + " argument has to be an integer between 0 and 65535", false);
				}
				
				File storageDir = new File(new File(deploymentBinaryDir, id.toString()), subdirectoryName);
				if (!storageDir.exists()) {
			    	if (!storageDir.mkdirs()) {
			    		logger.error("could not mkdir >" + storageDir + "<");
			    		return new InputInfo(getActiveAddressBean().toString(), "could not mkdir >" + storageDir + "<", false);
					}
			    	else
			    		logger.info("created new storage directory >" + storageDir + "<");
				}
				output.add(id);
				output.add(gentime);
				for (Serializable s: additionalFields)
					output.add(s);
				for (Iterator<BinaryFile> it = inputFileItems.iterator (); it.hasNext (); ) {
					BinaryFile inputLoggerFile = it.next ();
					if (inputLoggerFile.getFileItem().getSize() <= 0) {
						if (!inputLoggerFile.getFileItem().getName().isEmpty())
							logger.warn("uploaded file " + inputLoggerFile.getFileItem().getName() + " is empty => skip it");
						output.add(null);
						output.add(null);
						
					}
					else {
						int pos = inputLoggerFile.getFileItem().getName().lastIndexOf('.');
						String suffix = "";
						if (pos > 0 && pos < inputLoggerFile.getFileItem().getName().length() - 1)
							suffix = inputLoggerFile.getFileItem().getName().substring(pos);
						String filename = inputLoggerFile.getPrefix()+"_"+format.format(new java.util.Date(gentime))+suffix;
						File outputFile = new File(storageDir, filename);
						try {
							inputLoggerFile.getFileItem().write(outputFile);
						} catch (Exception e) {
							logger.error(e.getMessage());
							return new InputInfo(getActiveAddressBean().toString(), e.getMessage(), false);
						}
						output.add(subdirectoryName + "/" + filename);
						output.add(outputFile.length());
					}
				}
				
				if (postStreamElement(output.toArray(new Serializable[output.size()])))
					return new InputInfo(getActiveAddressBean().toString(), "file successfully uploaded", true);
				else
					return new InputInfo(getActiveAddressBean().toString(), "file could not be uploaded", false);
			} catch(Exception e) {
				logger.error(e.getMessage(), e);
				return new InputInfo(getActiveAddressBean().toString(), e.getMessage(), false);
			}
		}
		else {
			logger.warn("action >" + action + "< not supported");
			return new InputInfo(getActiveAddressBean().toString(), "action >" + action + "< not supported", false);
		}
	}

	@Override
	public DataField[] getOutputFormat() {
		return outputStructure.toArray(new DataField[outputStructure.size()]);
	}

	@Override
	public void dispose() { }

	@Override
	public String getWrapperName() {
		return "FileGetterWrapper";
	}

	class BinaryFile {
		private FileItem file;
		private String prefix;
		
		public BinaryFile(FileItem file, String prefix) {
			this.file = file;
			this.prefix = prefix;
		}
		
		public String getPrefix() {
			return prefix;
		}
		
		public FileItem getFileItem() {
			return file;
		}
	}
}
