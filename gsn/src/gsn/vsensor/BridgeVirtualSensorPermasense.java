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

		if (position_mapping || sensortype_mapping || sensorvalue_conversion) {
			try {
				DataMapping.registerVS(this, deployment);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				return false;
			}
		}
		
		processScriptlet = super.initialize();
		return true;
	}

	@Override
	public void dataAvailable(String inputStreamName, StreamElement data) {
		String s;
		if (position_mapping && data.getData("device_id") != null) {
			if (data.getData("generation_time") != null) {
				Integer position = DataMapping.getPosition(((Integer) data.getData("device_id")).intValue(),
						new Timestamp(((Long) data.getData("generation_time")).longValue()),
						deployment, getVirtualSensorConfiguration().getName(), inputStreamName);
				data = new StreamElement(data, 
						new String[]{"position"}, 
						new Byte[]{DataTypes.INTEGER}, 
						new Serializable[]{position});
				
			}
		}
		if (sensortype_mapping && 
				data.getData("position") != null &&	data.getData("generation_time") != null) {
			Serializable[] sensortype = DataMapping.getSensorType(((Integer) data.getData("position")).intValue(), 
					new Timestamp(((Long) data.getData("generation_time")).longValue()),
					deployment, getVirtualSensorConfiguration().getName(), inputStreamName);
			data = new StreamElement(data, 
					new String[]{"sensortype", "sensortype_serialid"}, 
					new Byte[]{DataTypes.VARCHAR, DataTypes.BIGINT},
					sensortype);
		}
		if (sensorvalue_conversion && data.getData("position") != null &&	data.getData("generation_time") != null) {
			data = DataMapping.getConvertedValues(data, deployment, getVirtualSensorConfiguration().getName(), inputStreamName);
		}
		if (gps_time_conversion && data.getData("gps_time") != null && data.getData("gps_week") != null) {
			data = new StreamElement(data, 
					new String[]{"gps_unixtime"}, 
					new Byte[]{DataTypes.BIGINT},
					new Serializable[]{(long)(gps2unix((double)((Integer)data.getData("gps_time")/1000.0), (Short)data.getData("gps_week"))*1000.0)});
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
		DataMapping.removeVS(this);
	}

	
/*
	Modified version (by Tonio Gsell) of:
	
	gpstimeutil.js: a javascript library which translates between GPS and unix time
	
	Copyright (C) 2012  Jeffery Kline
	
	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	
	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.
	
	You should have received a copy of the GNU General Public License
	along with this program.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
	
	*/
	
	// 
	// v0.0: Sat May 19 22:24:16 CDT 2012
	//     initial release
	// v0.1: Sat May 19 22:24:31 CDT 2012
	//     fix bug converting negative fractional gps times
	//     fix bug converting negative fractional unix times
	//     introduce global variable
	// v0.2: Sat May 19 23:08:05 CDT 2012
	//     ensure that unix2gps/gps2unix treats all input/output as Number
	
	/*
	Javascript code is based on original at:
	http://www.andrews.edu/~tzs/timeconv/timealgorithm.html
	
	The difference between the original and this version is that this
	version handles the leap seconds using linear interpolation, not a
	discontinuity.  Linear interpolation guarantees a 1-1 correspondence
	between gps times and unix times.
	
	By contrast, for example, the original implementation maps both gps
	times 46828800.5 and 46828800 map to unix time 362793599.5 
	*/
	private static final long GPS_OFFSET = 315964800L;

	private static final long[] leaps = {46828800L, 78364801L, 109900802L, 173059203L, 252028804L,
		315187205L, 346723206L, 393984007L, 425520008L, 457056009L, 504489610L,
		551750411L, 599184012L, 820108813L, 914803214L, 1025136015L,
		1341118800L};
	
	private double gps2unix(double gpsSec, short gpsWeek) {
		double gpsTime = (double)(gpsWeek*604800 + gpsSec);
		
		if ( gpsTime < 0)
			return gpsTime + GPS_OFFSET;
		
		double fpart = gpsTime % 1;
		long ipart = (long) Math.floor(gpsTime);

		long leap = countleaps(ipart, false);
		double unixTime = (double)(ipart + GPS_OFFSET - leap);
		
		if (isleap(ipart + 1))
			unixTime = unixTime + fpart / 2;
		else if (isleap(ipart))
			unixTime = unixTime + (fpart + 1) / 2;
		else
			unixTime = unixTime + fpart;
		
		return unixTime;
	}
	
	private boolean isleap(long gpsTime) {
		boolean isLeap = false;
		for (int i = 0; i < leaps.length; i++) {
			if (gpsTime == leaps[i]) {
				isLeap = true;
				break;
			}
		}
		return isLeap;
	}
	
	private long countleaps(long gpsTime, boolean accum_leaps) {
		long nleaps = 0;
		
		if (accum_leaps) {
			for (int i = 0; i < leaps.length; i++)
				if (gpsTime + i >= leaps[i])
					nleaps++;
		}
		else {
			for (int i = 0; i < leaps.length; i++)
				if (gpsTime >= leaps[i])
					nleaps++;
		}
		
		return nleaps;
	}
}
