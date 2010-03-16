package gsn.vsensor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.TreeMap;

import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;

import org.apache.log4j.Logger;

public class FileGetterVirtualSensor extends BridgeVirtualSensorPermasense {
	
	private static final String DCRAW = "/home/perma/dcraw-x64/dcraw";

	private static final transient Logger logger = Logger.getLogger(FileGetterVirtualSensor.class);
	
	private String filetype = null;
	
	
	@Override
	public boolean initialize() {
		VSensorConfig vsensor = getVirtualSensorConfiguration();
		TreeMap<String,String> params = vsensor.getMainClassInitialParams();
		
		filetype = params.get("file_type");
		if (filetype == null) {
			logger.error("file_type has to be defined in the virtual sensors xml file");
			return false;
		}
		
		return super.initialize();
	}
	
	
	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
		File file = new File((String) data.getData("file"));
		if (!file.exists()) {
			logger.error(file.getAbsolutePath() + " does not exist");
			return;
		}
		file = file.getAbsoluteFile();

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		InputStream is = null;
		
		if (filetype.equalsIgnoreCase("jpeg")  || filetype.equalsIgnoreCase("jpg")) {
			logger.debug("getting jpeg: " + file.getAbsolutePath());
		    try {
		    	is = new FileInputStream(file);
		    } catch (IOException e) {
		    	logger.error(e.getMessage(), e);
		    	return;
		    }
		}
		else if (filetype.equalsIgnoreCase("nef")) {
			logger.debug("exctracting jpeg from: " + file.getAbsolutePath());
			try {
				Process p = Runtime.getRuntime().exec(DCRAW + " -c -e " + file.getAbsolutePath());
				is = p.getInputStream();
			} catch (IOException e) {
		    	logger.error(e.getMessage(), e);
		    	return;
			}
		}
		
		byte[] b = new byte[1024];
		int n;
		try {
			while ((n = is.read(b)) != -1)
				os.write(b, 0, n);
		} catch (IOException e) {
	    	logger.error(e.getMessage(), e);
	    	return;
		}
		
		data = new StreamElement(data, 
				new String[]{"jpeg_scaled"},
				new Byte[]{DataTypes.BINARY},
				new Serializable[]{os.toByteArray()});

		super.dataAvailable(inputStreamName, data);
	}
}
