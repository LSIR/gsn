package gsn.wrappers;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import javax.naming.OperationNotSupportedException;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;

import gsn.beans.DataField;

public class FileGetterWrapper extends AbstractWrapper {
	
	private final static DataField[] outputStructure = new DataField[] {
				new DataField("DEVICE_ID", "INTEGER"),
				new DataField("GENERATION_TIME", "BIGINT"),
				new DataField("RELATIVE_FILE", "VARCHAR(255)")};

	private final transient Logger logger = Logger.getLogger( FileGetterWrapper.class );

	private File deploymentBinaryDir = null;
	private String subdirectoryName = null;
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
		
		return true;
	}
	
	@Override
	public boolean sendToWrapper ( String action , String [ ] paramNames , Object [ ] paramValues ) throws OperationNotSupportedException {
		if( action.compareToIgnoreCase("logger_file") == 0 ) {
			long gentime = System.currentTimeMillis();
			String deviceid = null;
			FileItem inputFileItem = null;
			if( paramNames.length != 2 ) {
				logger.error("logger_file action must have two parameter names: 'device_id' and 'file'");
				return false;
			}
			if( paramValues.length != 2 ) {
				logger.error("logger_file action must have two parameter values");
				return false;
			}
			
			for( int i=0; i<2; i++ ) {
				try {
					String tmp = paramNames[i];
					if( tmp.compareToIgnoreCase("device_id") == 0 )
						deviceid = (String) paramValues[i];
					else if( tmp.compareToIgnoreCase("file") == 0 )
						inputFileItem = (FileItem) paramValues[i];
				} catch(Exception e) {
					logger.error("Could not interprete logger_file arguments: " + e.getMessage());
					return false;
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
				logger.error("Could not interprete device_id argument: " + e.getMessage(), e);
				return false;
			}
			if (id < 0 || id > 65534) {
				logger.error("device_id argument has to be an integer between 0 and 65535");
				return false;
			}
			
			File storageDir = new File(new File(deploymentBinaryDir, deviceid), subdirectoryName);
			if (!storageDir.exists()) {
		    	if (!storageDir.mkdirs()) {
		    		logger.error("could not mkdir >" + storageDir + "<");
		    		return false;
				}
		    	else
		    		logger.info("created new storage directory >" + storageDir + "<");
			}
			
			if (inputFileItem.getSize() <= 0) {
				logger.warn("uploaded file is empty => skip it");
				return false;
			}
				
			int pos = inputFileItem.getName().lastIndexOf('.');
			String suffix = "";
			if (pos > 0 && pos < inputFileItem.getName().length() - 1)
				suffix = inputFileItem.getName().substring(pos);
			String filename = format.format(new java.util.Date(gentime))+suffix;
			File outputFile = new File(storageDir, filename);
			try {
				inputFileItem.write(outputFile);
			} catch (Exception e) {
				logger.error(e.getMessage());
				return false;
			}
			
			return postStreamElement(new Serializable[]{Integer.parseInt(deviceid), gentime, subdirectoryName + "/" + filename});
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

}
