package gsn.vsensor;

import java.awt.image.renderable.ParameterBlock;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;

import org.apache.log4j.Logger;

import com.sun.media.jai.codec.SeekableStream;

public class FileGetterVirtualSensor extends BridgeVirtualSensorPermasense {
	
	private static final String DCRAW = "/home/perma/bin/dcraw-x64/dcraw";

	private static final transient Logger logger = Logger.getLogger(FileGetterVirtualSensor.class);

	private String filetype = null;
	private String storage_directory = null;
	private String nef_extraction_type = null;
	private String dcraw_flip = null;
	private Double rotation = null;	
	
	@Override
	public boolean initialize() {
		boolean ret = super.initialize();
		VSensorConfig vsensor = getVirtualSensorConfiguration();
		TreeMap<String,String> params = vsensor.getMainClassInitialParams();

		storage_directory = vsensor.getStorage().getStorageDirectory();
		if (storage_directory != null) {
			storage_directory = new File(storage_directory, deployment).getPath();
		}
		filetype = params.get("file_type");
		if (filetype == null) {
			logger.error("file_type has to be defined in the virtual sensors xml file");
			return false;
		}
		if (filetype.equalsIgnoreCase("nef")) {
			nef_extraction_type = params.get("nef_extraction_type");
			if (nef_extraction_type == null || (!nef_extraction_type.equalsIgnoreCase("thumbnail") && !nef_extraction_type.equalsIgnoreCase("jpeg"))) {
				logger.error("nef_extraction_type has to be defined in the virtual sensors xml file (valid values are \"thumbnail\" or \"jpeg\")");
				return false;
			}
		}
		
		dcraw_flip = params.get("dcraw_flip");
		if (params.get("rotation")!=null)
			rotation = new Double(params.get("rotation"));
		return ret;
	}
	
	
	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
		File file;
		if (storage_directory == null) {
			String filename = (String) data.getData("file");
			if (filename != null) {
				file = new File(filename);
				if (!file.exists()) {
					logger.error(file.getAbsolutePath() + " does not exist");
					return;
				}
			}
			else {
				logger.error("no file specified");
				return;
			}
		}
		else {
			file = new File(new File(storage_directory, Integer.toString((Integer)data.getData("device_id"))).getPath(), (String) data.getData("relative_file"));
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
			Process p = null;
			StringBuffer cmd = new StringBuffer(DCRAW + " -c");
			if (nef_extraction_type.equalsIgnoreCase("thumbnail"))
				cmd.append(" -e");
			if (dcraw_flip != null)
				cmd.append(" -t "+dcraw_flip);
			cmd.append(" "+file.getAbsolutePath());
			logger.debug("exec "+cmd);
			try {
				p = Runtime.getRuntime().exec(cmd.toString());
				is = p.getInputStream();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
		    	return;
			}
		}
		
		SeekableStream s = SeekableStream.wrapInputStream(is, true);
		RenderedOp image = JAI.create("stream", s);
		if (rotation!=null) {
			// rotate
			logger.debug("rotate " + file.getAbsolutePath() + " by "+rotation);
			// 	rotation center
			float centerX = (float)image.getWidth() / 2;
			float centerY = (float)image.getHeight() / 2;
			ParameterBlock pb = new ParameterBlock();
			pb.addSource(image);
			pb.add(centerX);
			pb.add(centerY);
			pb.add((float)(rotation / 180d * Math.PI));
			pb.add(new javax.media.jai.InterpolationBicubic(10));
			// create a new, rotated image
			image = JAI.create("rotate", pb);
		}
		try {
			ImageIO.write(image.getAsBufferedImage(), "jpg", os);
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
