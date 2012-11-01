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
import java.util.TreeMap;
import java.util.Vector;

import javax.imageio.ImageIO;

import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.processor.ScriptletProcessor;
import gsn.utils.ParamParser;
import gsn.utils.Helpers;
import gsn.wrappers.DataMappingWrapper;

import org.apache.log4j.Logger;

/**
 *
 * @author Tonio Gsell
 * @author Mustafa Yuecel
 */
public class BridgeVirtualSensorPermasense extends ScriptletProcessor
{
	private static final int DEFAULT_WIDTH = 610;
	
	private static final transient Logger logger = Logger.getLogger(BridgeVirtualSensorPermasense.class);

	protected String deployment;
	private int width;
	private Vector<String> jpeg_scaled;
	private String rotate_image;
	private boolean position_mapping = false;
	private boolean sensortype_mapping = false;
	private boolean sensorvalue_conversion = false;
	private boolean gps_time_conversion = false;
	private boolean processScriptlet;

	@Override
	public boolean initialize() {
		String s;
		int i;
		VSensorConfig vsensor = getVirtualSensorConfiguration();
		TreeMap<String,String> params = vsensor.getMainClassInitialParams();
		
		deployment = vsensor.getName().split("_")[0].toLowerCase();
		
		width = ParamParser.getInteger(params.get("width"), DEFAULT_WIDTH);

		rotate_image = params.get("rotate_image");
		
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

		if (params.get("gps_time_conversion") != null)
			gps_time_conversion = true;
		
		processScriptlet = super.initialize();
		return true;
	}

	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
		String s;
		if (position_mapping && data.getData("device_id") != null) {
			if (data.getData("generation_time") != null) {
				Integer position = DataMappingWrapper.getPosition(((Integer) data.getData("device_id")).intValue(),
						((Long) data.getData("generation_time")).longValue(),
						deployment, getVirtualSensorConfiguration().getName(), inputStreamName);
				data = new StreamElement(data, 
						new String[]{"position"}, 
						new Byte[]{DataTypes.INTEGER}, 
						new Serializable[]{position});
				
			}
		}
		if (sensortype_mapping && 
				data.getData("position") != null &&	data.getData("generation_time") != null) {
			Serializable[] sensortype = DataMappingWrapper.getSensorType(((Integer) data.getData("position")).intValue(), 
					((Long) data.getData("generation_time")).longValue(),
					deployment, getVirtualSensorConfiguration().getName(), inputStreamName);
			data = new StreamElement(data, 
					new String[]{"sensortype", "sensortype_serialid"}, 
					new Byte[]{DataTypes.VARCHAR, DataTypes.BIGINT},
					sensortype);
		}
		if (sensorvalue_conversion && data.getData("position") != null &&	data.getData("generation_time") != null) {
			data = DataMappingWrapper.getConvertedValues(data, deployment, getVirtualSensorConfiguration().getName(), inputStreamName);
		}
		if (gps_time_conversion && data.getData("gps_time") != null && data.getData("gps_week") != null) {
			data = new StreamElement(data, 
					new String[]{"gps_unixtime"}, 
					new Byte[]{DataTypes.BIGINT},
					new Serializable[]{(long)(Helpers.convertGPSTimeToUnixTime((double)((Integer)data.getData("gps_time")/1000.0), (Short)data.getData("gps_week"))*1000.0)});
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

		if (processScriptlet)
			super.dataAvailable(inputStreamName, data);
		else
			dataProduced( data );
			
	}
	
	@Override
	public synchronized void dispose() {
		if (processScriptlet)
			super.dispose();
	}
}
