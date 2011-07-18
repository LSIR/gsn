package gsn.wrappers;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.OperationNotSupportedException;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;

import gsn.beans.DataField;

public class FileGetterWrapper extends AbstractWrapper {
	
	private DataField[] outputStructure;

	private final transient Logger logger = Logger.getLogger( FileGetterWrapper.class );

	private File deploymentBinaryDir = null;
	private String subdirectoryName = null;
	private Pattern[] filenamePatternArray;
	private boolean deviceIdFromFilename;
	final static private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HHmmss");

	@Override
	public boolean initialize() {
		String rootBinaryDir = null;
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		try {
			subdirectoryName = getActiveAddressBean().getPredicateValueWithException("subdirectory-name");
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

		int size = -1;
		for (int i=0; i<getActiveAddressBean().getVirtualSensorConfig().getWebinput().length; i++) {
			if (getActiveAddressBean().getVirtualSensorConfig().getWebinput()[i].getName().equalsIgnoreCase("files")) {
				size = getActiveAddressBean().getVirtualSensorConfig().getWebinput()[i].getParameters().length*2+2;
				break;
			}
		}
		if (size == -1) {
			logger.error("command name files has to be existing in the web-input section");
			return false;
		}
		outputStructure = new DataField[size];
		outputStructure[0] = new DataField("DEVICE_ID", "INTEGER");
		outputStructure[1] = new DataField("GENERATION_TIME", "BIGINT");
		filenamePatternArray = new Pattern [size-2];
		boolean hasRegex = false;
		for (int i=2; i<size; i++) {
			String regex = getActiveAddressBean().getPredicateValue("filename-regex" + (i-1));
			if (regex != null) {
				hasRegex = true;
				filenamePatternArray[i-2] = Pattern.compile(regex);
				logger.info("RELATIVE_FILE" + (i-1) + " has to match regular expression: " + regex);
				
				deviceIdFromFilename = Boolean.parseBoolean(getActiveAddressBean().getPredicateValue("deviceid-from-filename"));
				if (deviceIdFromFilename)
					logger.info("device id will be extracted from RELATIVE_FILE" + (i-1) + " using the first group from regular expresion: " + regex);
			}
			else
				filenamePatternArray[i-2] = null;
			
			outputStructure[i] = new DataField("RELATIVE_FILE" + (i-1), "VARCHAR(255)");
		}
		
		
		if (!hasRegex && Boolean.parseBoolean(getActiveAddressBean().getPredicateValue("deviceid-from-filename"))) {
			logger.error("deviceid-from-filename predicate key can only be used together with filename-regex");
			return false;
		}
		
		return true;
	}
	
	@Override
	public boolean sendToWrapper ( String action , String [ ] paramNames , Object [ ] paramValues ) throws OperationNotSupportedException {
		if( action.compareToIgnoreCase("files") == 0 ) {
			try {
				long gentime = System.currentTimeMillis();
				String deviceid = null;
				Vector<LoggerFile> inputFileItems = new Vector<LoggerFile>();
				
				for( int i=0; i<paramNames.length; i++ ) {
					String tmp = paramNames[i];
					if( tmp.compareToIgnoreCase("device_id") == 0 )
						deviceid = (String) paramValues[i];
					else if( tmp.endsWith("_file") )
						inputFileItems.add(new LoggerFile((FileItem) paramValues[i], tmp));
					else
						logger.warn("unknown upload field: " + tmp + " -> skip it");
				}
				
				FileItem lastFileItem = null;
				
				for (int i=0; i<inputFileItems.size(); i++) {
					if (!inputFileItems.get(i).getFileItem().getName().isEmpty() && filenamePatternArray[i] != null) {
						Matcher m = filenamePatternArray[i].matcher(inputFileItems.get(i).getFileItem().getName());
						if (!m.matches()) {
							logger.error("filename " + inputFileItems.get(i).getFileItem().getName() + " does not match regular expression " + filenamePatternArray[i].toString());
							return false;
						}
		
						if (deviceIdFromFilename) {
							String id = m.group(1);
							if (deviceid != null && deviceid.compareTo(id) != 0 && lastFileItem != null) {
								logger.error("the device id extracted from " + lastFileItem.getName() + " and " + inputFileItems.get(i).getFileItem().getName() + " are not equal");
								return false;
							}
							deviceid = id;
							lastFileItem = inputFileItems.get(i).getFileItem();
						}
					}
				}
				
				if (deviceid.equals("")) {
					logger.error("device_id argument has to be an integer between 0 and 65535");
					return false;
				}
				
				Integer id;
				try {
					id = Integer.parseInt(deviceid);
				} catch(Exception e) {
					logger.error("Could not interprete device_id argument (" + deviceid +") as integer");
					return false;
				}
				if (id < 0 || id > 65534) {
					logger.error("device_id argument has to be an integer between 0 and 65535");
					return false;
				}
				
				File storageDir = new File(new File(deploymentBinaryDir, id.toString()), subdirectoryName);
				if (!storageDir.exists()) {
			    	if (!storageDir.mkdirs()) {
			    		logger.error("could not mkdir >" + storageDir + "<");
			    		return false;
					}
			    	else
			    		logger.info("created new storage directory >" + storageDir + "<");
				}
				
				Serializable[] output = new Serializable[inputFileItems.size()+2];
				output[0] = id;
				output[1] = gentime;
				int i = 2;
				for (Iterator<LoggerFile> it = inputFileItems.iterator (); it.hasNext (); ) {
					LoggerFile inputLoggerFile = it.next ();
					if (inputLoggerFile.getFileItem().getSize() <= 0) {
						if (!inputLoggerFile.getFileItem().getName().isEmpty())
							logger.warn("uploaded file " + inputLoggerFile.getFileItem().getName() + " is empty => skip it");
						output[i] = null;
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
							return false;
						}
						output[i] = subdirectoryName + "/" + filename;
						i++;
						output[i] = outputFile.length();
					}
					i++;
				}
				
				return postStreamElement(output);
			} catch(Exception e) {
				logger.error(e.getMessage(), e);
				return false;
			}
		}
		else {
			logger.warn("action >" + action + "< not supported");
			return false;
		}
	}

	@Override
	public DataField[] getOutputFormat() {
		return outputStructure;
	}

	@Override
	public void dispose() { }

	@Override
	public String getWrapperName() {
		return "FileGetterWrapper";
	}

	class LoggerFile {
		private FileItem file;
		private String prefix;
		
		public LoggerFile(FileItem file, String prefix) {
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
