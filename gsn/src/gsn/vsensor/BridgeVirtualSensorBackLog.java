package gsn.vsensor;

import gsn.Main;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.TreeMap;
import java.util.Vector;
import java.lang.Double;

import javax.imageio.ImageIO;

import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.utils.ParamParser;
import gsn.wrappers.AbstractWrapper;

import org.apache.log4j.Logger;

/**
 * This virtual sensor takes the input field called as specified in the
 * IMAGE_INPUT_FIELD variable from the input stream,
 * interprets it as a jpeg and resizes it to the image width specified in
 * the virtual sensors XML file's SCALED_IMAGE_WIDTH_NAME field. Finally,
 * it forwards the result in a new stream with a field named as specified
 * in the IMAGE_OUTPUT_FIELD variable.
 *
 * @author Tonio Gsell
 * @author Mustafa Yuecel
 */
public class BridgeVirtualSensorBackLog extends BridgeVirtualSensor
{
	private static final int DEFAULT_WIDTH = 610;
	private static final String DEFAULT_STREAM_NAME = "data";
	private static final String DEFAULT_STREAM_SOURCE = "source";
	private static final String[] DEFAULT_TIMESTAMP_STRING = { "timestamp_string" };
	private static final String[] DEFAULT_JPEG_SCALED = { };
	
	private static final SimpleDateFormat datetimefm = new SimpleDateFormat(Main.getContainerConfig().getTimeFormat());
	
	private static final transient Logger logger = Logger.getLogger(BridgeVirtualSensorBackLog.class);

	private int width;
	private String stream_name = null;
	private String stream_source = null;
	private AbstractWrapper backlog = null;
	private Vector<String> timestamp_string;
	private Vector<String> jpeg_scaled;
	
	public boolean initialize() {
		String s;
		int i;
		VSensorConfig vsensor = getVirtualSensorConfiguration();
		TreeMap<String,String> params = vsensor.getMainClassInitialParams();
		
		width = ParamParser.getInteger(params.get("width"), DEFAULT_WIDTH);
		stream_name = params.get("stream-name");
		if (stream_name == null) stream_name = DEFAULT_STREAM_NAME;
		stream_source = params.get("stream-source");
		if (stream_source == null) stream_source = DEFAULT_STREAM_SOURCE;
		try {
			backlog = vsensor.getInputStream(stream_name).getSource(stream_source).getWrapper();
		} catch (Exception e) {
			logger.info("backlog wrapper instance not found in " + vsensor.getName() + "->" + stream_source);
		}

		ArrayList<DataField> outputstructure = new ArrayList<DataField>(Arrays.asList(vsensor.getOutputStructure()));
		
		String[] timestamp = null;
		s = params.get("timestamp_string");
		if (s != null)
			timestamp = s.split(",");
		if (timestamp == null) timestamp = DEFAULT_TIMESTAMP_STRING;
		timestamp_string = new Vector<String>(timestamp.length);
		for (i = 0; i < timestamp.length; i++) {
			s = timestamp[i].trim();
			if (outputstructure.contains(new DataField(s, DataTypes.VARCHAR)))
				timestamp_string.add(s);
		}
		
		String[] jpeg = null;
		s = params.get("jpeg_scaled");
		if (s != null)
			jpeg = s.split(",");
		if (jpeg == null) jpeg = DEFAULT_JPEG_SCALED;
		jpeg_scaled = new Vector<String>(jpeg.length);
		for (i = 0; i < jpeg.length; i++) {
			s = jpeg[i].trim();
			if (outputstructure.contains(new DataField(s, DataTypes.BINARY)))
				jpeg_scaled.add(s);
		}

		return true;
	}
	
	public void dataAvailable(String inputStreamName, StreamElement data) {
		String s;
		for (Enumeration<String> elem = timestamp_string.elements() ; elem.hasMoreElements() ; ) {
			s = elem.nextElement();
			if (data.getData(s) != null) {
				try {
					data.setData(s, datetimefm.format(new Date(((Double) data.getData(s)).longValue())), DataTypes.VARCHAR);
				} catch (IllegalArgumentException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
		for (Enumeration<String> elem = jpeg_scaled.elements() ; elem.hasMoreElements() ; ) {
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
		if (backlog != null) {
			try {
				return backlog.sendToWrapper(command, paramNames, paramValues);
			} catch (Exception e) {
				logger.warn(e.getMessage());
			}
		}
		return false;
	}
}
