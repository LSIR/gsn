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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
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
import org.h2.tools.Server;

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

	private static HashSet<String> deployments = new HashSet<String>();
	
	private String deployment;
	private Connection conn = null;
	private PreparedStatement position_query = null;
	private PreparedStatement sensortype_query = null;
	private PreparedStatement serialid_query = null;
	private Server web;
	private int width;
	private String stream_name = null;
	private String stream_source = null;
	private AbstractWrapper backlog = null;
	private Vector<String> timestamp_string;
	private Vector<String> jpeg_scaled;
	private boolean position_mapping = false;
	private boolean sensortype_mapping = false;
	
	public boolean initialize() {
		String s;
		int i;
		VSensorConfig vsensor = getVirtualSensorConfiguration();
		TreeMap<String,String> params = vsensor.getMainClassInitialParams();
		
		deployment = vsensor.getName().split("_")[0].toLowerCase();
		
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

		if (params.get("position_mapping") != null)
			position_mapping = true;
		
		if (params.get("sensortype_mapping") != null)
			sensortype_mapping = true;

		if (position_mapping || sensortype_mapping) {
			synchronized (deployments) {
				if (deployments.isEmpty()) {
					try {
						Class.forName("org.h2.Driver");
					} catch (ClassNotFoundException e) {
						logger.error(e.getMessage(), e);
						return false;
					}
					if (logger.isDebugEnabled()) {
						try {
							String [] args = {"-webPort", "8082", "-webAllowOthers", "true"};
							web = Server.createWebServer(args);
							web.start();
						} catch (SQLException e) {
							logger.warn(e.getMessage(), e);
						}
					}
				}

				try {
					conn = DriverManager.getConnection("jdbc:h2:mem:" + deployment + ";DB_CLOSE_DELAY=-1", "sa", "");
					conn.setAutoCommit(true);
					logger.info("connected to jdbc:h2:mem:" + deployment + "...");

					// check if table is already created
					if (!deployments.contains(deployment)) {
						Statement stat = conn.createStatement();
						stat.execute("CREATE TABLE positionmapping(node_id INT NOT NULL, begin DATETIME(23,0) NOT NULL, end DATETIME(23,0) NOT NULL, position INT NOT NULL, PRIMARY KEY(node_id, begin, end))");
						logger.info("create positionmapping table for " + deployment + " deployment");
						try {
							stat.execute("INSERT INTO positionmapping SELECT * FROM CSVREAD('conf/backlog/positionmapping-" + deployment + ".csv')");
						} catch (SQLException e) {
							logger.error("could not fill out locationmapping table", e);
						}
						stat.execute("CREATE TABLE sensortypemapping(position INT NOT NULL, begin DATETIME(23,0) NOT NULL, end DATETIME(23,0) NOT NULL, sensortype VARCHAR(30) NOT NULL, sensortype_args VARCHAR(255) NOT NULL, sensortype_serialid BIGINT, PRIMARY KEY(position, begin, end))");
						logger.info("create sensortypemapping table for " + deployment + " deployment");
						try {
							stat.execute("INSERT INTO sensortypemapping SELECT * FROM CSVREAD('conf/backlog/sensortypemapping-" + deployment + ".csv')");
						} catch (SQLException e) {
							logger.error("could not fill out sensortypemapping table", e);
						}
						ResultSet rs;
						rs = stat.executeQuery("SELECT node_id, position FROM positionmapping WHERE NOW() BETWEEN begin AND end");
						logger.info("position mapping for now:");
						while (rs.next())
							logger.info(rs.getInt(1) + "->" + rs.getInt(2));
						rs = stat.executeQuery("SELECT p.node_id AS node_id, p.position AS position, s.sensortype AS SENSORTYPE, s.sensortype_serialid AS sensortype_serialid FROM positionmapping AS p, sensortypemapping AS s WHERE p.position = s.position AND NOW() BETWEEN p.begin AND p.end AND NOW() BETWEEN s.begin AND s.end");
						logger.info("sensortype mapping for now:");
						while (rs.next())
							logger.info(rs.getInt(1) + "->" + rs.getInt(2) + "->" + rs.getString(3) + "(" + rs.getLong(4) + ")");
						deployments.add(deployment);
					}
					
					if (position_mapping)
						position_query = conn.prepareStatement("SELECT position FROM positionmapping WHERE node_id = ? AND ? BETWEEN begin AND end LIMIT 1");
					if (sensortype_mapping)
						sensortype_query = conn.prepareStatement("SELECT sensortype, sensortype_args, sensortype_serialid FROM sensortypemapping WHERE position = ? AND ? BETWEEN begin AND end LIMIT 1");
					
				} catch (SQLException e) {
					logger.error(e.getMessage(), e);
					return false;
				}
			}
		}
		
		return true;
	}
	
	public void dataAvailable(String inputStreamName, StreamElement data) {
		String s;
		if (conn != null) {
			if (position_mapping && data.getData("position") != null &&
					data.getData("HEADER_ORIGINATORID") != null && 
					data.getData("GENERATION_TIME") != null) {
				int position = getPosition(((Integer) data.getData("HEADER_ORIGINATORID")).intValue(),
						((Long) data.getData("GENERATION_TIME")).longValue());
				data.setData("position", position);
			}
			if (sensortype_mapping && data.getData("sensortype") != null &&
					data.getData("position") != null && data.getData("GENERATION_TIME") != null) {
				Serializable[] sensortype = getSensorType(((Integer) data.getData("position")).intValue(), 
						((Long) data.getData("GENERATION_TIME")).longValue());
				data.setData("sensortype", sensortype[0]);
				if (data.getData("sensortype_args") != null) {
					data.setData("sensortype_args", sensortype[1]);
				}
				if (data.getData("sensortype_serialid") != null) {
					data.setData("sensortype_serialid", sensortype[2]);
				}
			}
		}
		for (Enumeration<String> elem = timestamp_string.elements() ; elem.hasMoreElements() ; ) {
			// convert timestamps to human readable form
			s = elem.nextElement();
			if (data.getData(s) != null) {
				try {
					data.setData(s, datetimefm.format(new Date(((Long) data.getData(s)).longValue())), DataTypes.VARCHAR);
				} catch (IllegalArgumentException e) {
					logger.error(e.getMessage(), e);
				}
			}
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

	private Integer getPosition(int node_id, long generation_time) {
		Integer res = null;
		long start = 0;
		if (logger.isDebugEnabled())
			start = System.nanoTime();
		try {
			position_query.setInt(1, node_id);
			position_query.setTimestamp(2, new Timestamp(generation_time));
			ResultSet rs = position_query.executeQuery();
			if (rs.next())
				res = rs.getInt(1);
		} catch (SQLException e) {
			logger.warn(e.getMessage(), e);
		}
		if (logger.isDebugEnabled())
			logger.debug("getPosition: " + Long.toString((System.nanoTime() - start) / 1000) + " us");
		return res;
	}

	private Serializable[] getSensorType(int position, long generation_time) {
		Serializable[] res = new Serializable[]{null, null, null};
		long start = 0;
		if (logger.isDebugEnabled())
			start = System.nanoTime();
		try {
			sensortype_query.setInt(1, position);
			sensortype_query.setTimestamp(2, new Timestamp(generation_time));
			ResultSet rs = sensortype_query.executeQuery();
			if (rs.next())
				res = new Serializable[]{rs.getString(1), rs.getString(2), rs.getLong(3)};
		} catch (SQLException e) {
			logger.warn(e.getMessage(), e);
		}
		if (logger.isDebugEnabled())
			logger.debug("getSensortype: " + Long.toString((System.nanoTime() - start) / 1000) + " us");
		return res;
	}
	
	public void dispose() {
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) { /* ignore */	}
		}
		if (web != null) {
			web.shutdown();
		}
	}
}
