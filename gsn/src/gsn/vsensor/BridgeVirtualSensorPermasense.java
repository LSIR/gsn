package gsn.vsensor;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import javax.imageio.ImageIO;

import gsn.beans.DataTypes;
import gsn.beans.InputStream;
import gsn.beans.StreamElement;
import gsn.beans.StreamSource;
import gsn.beans.VSensorConfig;
import gsn.utils.ParamParser;
import gsn.wrappers.AbstractWrapper;

import org.apache.log4j.Logger;

/**
 *
 * @author Tonio Gsell
 * @author Mustafa Yuecel
 */
public class BridgeVirtualSensorPermasense extends BridgeVirtualSensor
{
	private static final int DEFAULT_WIDTH = 610;
	
	private static final transient Logger logger = Logger.getLogger(BridgeVirtualSensorPermasense.class);

	private String deployment;
	private int width;
	private List<AbstractWrapper> backlogWrapperList = new LinkedList<AbstractWrapper>();
	private Vector<String> jpeg_scaled;
	private String rotate_image;
	private boolean position_mapping = false;
	private boolean sensortype_mapping = false;
	private boolean sensorvalue_conversion = false;

	public boolean initialize() {
		String s;
		int i;
		VSensorConfig vsensor = getVirtualSensorConfiguration();
		TreeMap<String,String> params = vsensor.getMainClassInitialParams();
		
		deployment = vsensor.getName().split("_")[0].toLowerCase();
		
		width = ParamParser.getInteger(params.get("width"), DEFAULT_WIDTH);

		rotate_image = params.get("rotate_image");
		
		Iterator<InputStream> streams = vsensor.getInputStreams().iterator();
		while (streams.hasNext()) {
			StreamSource[] sources = streams.next().getSources();
			for (int j=0; j<sources.length; j++) {
				backlogWrapperList.add(sources[j].getWrapper());
			}
		}
		
		String[] jpeg = null;
		s = params.get("jpeg_scaled");
		if (s != null)
			jpeg = s.split(",");
		if (jpeg == null) jpeg = new String[]{ };
		jpeg_scaled = new Vector<String>(jpeg.length);
		for (i = 0; i < jpeg.length; i++) {
			jpeg_scaled.addElement(jpeg[i].trim().toLowerCase());
		}

		if (params.get("position_mapping") != null)
			position_mapping = true;

		if (params.get("sensortype_mapping") != null)
			sensortype_mapping = true;

		if (params.get("sensorvalue_conversion") != null)
			sensorvalue_conversion = true;

		if (position_mapping || sensortype_mapping || sensorvalue_conversion) {
			try {
				DataMapping.registerVS(this, deployment);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				return false;
			}
		}
		return true;
	}
	
	public void dataAvailable(String inputStreamName, StreamElement data) {
		String s;
		if (position_mapping && data.getData("device_id") != null) {
			if (data.getData("generation_time") != null) {
				Integer position = DataMapping.getPosition(deployment, ((Integer) data.getData("device_id")).intValue(),
						new Timestamp(((Long) data.getData("generation_time")).longValue()));
				data = new StreamElement(data, 
						new String[]{"position"}, 
						new Byte[]{DataTypes.INTEGER}, 
						new Serializable[]{position});
			}
		}
		if (sensortype_mapping && 
				data.getData("position") != null &&	data.getData("generation_time") != null) {
			Serializable[] sensortype = DataMapping.getSensorType(deployment, ((Integer) data.getData("position")).intValue(), 
					new Timestamp(((Long) data.getData("generation_time")).longValue()));
			data = new StreamElement(data, 
					new String[]{"sensortype", "sensortype_serialid"}, 
					new Byte[]{DataTypes.VARCHAR, DataTypes.BIGINT},
					sensortype);
		}
		if (sensorvalue_conversion && data.getData("position") != null &&	data.getData("generation_time") != null) {
			data = DataMapping.getConvertedValues(deployment, data);
		}

		for (Enumeration<String> elem = jpeg_scaled.elements() ; elem.hasMoreElements() ; ) {
			// scale image to given width
			s = elem.nextElement();
		    if (data.getData(s) != null) {
		    	try {
		    		BufferedImage image;
		    		try {
		    			image = ImageIO.read(new ByteArrayInputStream((byte[]) data.getData(s)));
		    		} catch ( IOException e ) {
		    			logger.error("Could not read image: skip image!", e);
		    			return;
		    		}

		    		// use Graphics2D for scaling -> make usage of GPU
		    		double factor = (float) width / image.getWidth();
		    		BufferedImage scaled = new BufferedImage(width, (int) (image.getHeight() * factor), BufferedImage.TYPE_INT_RGB);
		    		Graphics2D g = scaled.createGraphics();
		    		if (rotate_image != null) {
		    			g.rotate(Math.toRadians(Integer.parseInt(rotate_image)), scaled.getWidth() / 2d, scaled.getHeight() / 2d);
		    		}
		    		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		    		AffineTransform at = AffineTransform.getScaleInstance(factor, factor);
		    		g.drawRenderedImage(image,at);
		    		g.dispose();

		    		ByteArrayOutputStream os = new ByteArrayOutputStream();
		    		ImageIO.write(scaled, "jpeg", os);

		    		data.setData(s, os.toByteArray());
		    	} catch (Exception e) {
		    		logger.error(e.getMessage(), e);
		    	}
		    }
		}

		super.dataAvailable(inputStreamName, data);
	}
	
	@Override
	public boolean dataFromWeb(String command, String[] paramNames, Serializable[] paramValues) {
		boolean ret = false;
		
		Iterator<AbstractWrapper> wrappers = backlogWrapperList.iterator();
		while (wrappers.hasNext()) {
			try {
				if (wrappers.next().sendToWrapper(command, paramNames, paramValues))
					ret = true;
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		
		return ret;
	}
	
	public synchronized void dispose() {
		DataMapping.removeVS(this);
	}
}
