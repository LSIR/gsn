package gsn.vsensor;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import javax.imageio.ImageIO;

import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;

import org.apache.log4j.Logger;

public class ThumbGetterVirtualSensor extends BridgeVirtualSensorPermasense {

	private static final transient Logger logger = Logger.getLogger(ThumbGetterVirtualSensor.class);

	private DataField[] dataField = {	new DataField("MODIFICATIONTIME", DataTypes.BIGINT),
						new DataField("FILE", DataTypes.VARCHAR),
						new DataField("JPEG_SCALED", DataTypes.BINARY)};
	
	
	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
		String relativfile = (String) data.getData("relativefile");
		String storagedirectory = (String) data.getData("storagedirectory");
		Serializable[] newdata = new Serializable[dataField.length];
		
		File file = new File(storagedirectory+relativfile);
		file = file.getAbsoluteFile();
		logger.debug("file name: " + file.getAbsolutePath());

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		
	    try {
	    	BufferedImage image = ImageIO.read(file); // Read from an input stream
	    	InputStream is = new BufferedInputStream( new FileInputStream(file));
	    	image = ImageIO.read(is);
			ImageIO.write(image, "jpeg", os);
	    } catch (IOException e) {
	    	logger.error(e.getMessage(), e);
	    }
		
	    newdata[0] = data.getData("modificationtime");
		newdata[1] = file.getAbsolutePath();
		newdata[2] = os.toByteArray();
		
		data = new StreamElement(
				dataField, 
				newdata);

		super.dataAvailable(inputStreamName, data);
	}
}
