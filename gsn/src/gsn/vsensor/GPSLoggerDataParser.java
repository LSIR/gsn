package gsn.vsensor;

import gsn.beans.StreamElement;
import java.io.File;

import org.apache.log4j.Logger;

public class GPSLoggerDataParser extends BridgeVirtualSensorPermasense {

	private static final transient Logger logger = Logger.getLogger(GPSLoggerDataParser.class);

	private String storage_directory = null;
	
	@Override
	public boolean initialize() {
		boolean ret = super.initialize();
		storage_directory = getVirtualSensorConfiguration().getStorage().getStorageDirectory();
		if (storage_directory != null) {
			storage_directory = new File(storage_directory, deployment).getPath();
		}
		return ret;
	}
	
	
	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
		File file = new File(new File(storage_directory, Integer.toString((Integer)data.getData("device_id"))).getPath(), (String) data.getData("relative_file"));
		file = file.getAbsoluteFile();
		
//		data = new StreamElement(data, 
//				new String[]{"jpeg_scaled"},
//				new Byte[]{DataTypes.BINARY},
//				new Serializable[]{os.toByteArray()});

		super.dataAvailable(inputStreamName, data);
	}
}
