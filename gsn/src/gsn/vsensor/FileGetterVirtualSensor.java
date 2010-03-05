package gsn.vsensor;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;

import org.apache.log4j.Logger;

public class FileGetterVirtualSensor extends BridgeVirtualSensorPermasense {

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
		filetype.toLowerCase();
		
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
		
		if (filetype == "jpeg" || filetype == "jpg") {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
		    try {
		    	BufferedImage image = ImageIO.read(file); // Read from an input stream
		    	InputStream is = new BufferedInputStream( new FileInputStream(file));
		    	image = ImageIO.read(is);
				ImageIO.write(image, "jpeg", os);
		    } catch (IOException e) {
		    	logger.error(e.getMessage(), e);
		    }
			
			data = new StreamElement(data, 
					new String[]{"jpeg_scaled"}, 
					new Byte[]{DataTypes.BINARY}, 
					new Serializable[]{os.toByteArray()});
		}
		else if (filetype == "nef") {
			
		}

		super.dataAvailable(inputStreamName, data);
	}
}
