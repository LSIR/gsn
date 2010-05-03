package gsn.vsensor;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.imageio.ImageIO;

import gsn.beans.DataTypes;
import gsn.beans.InputStream;
import gsn.beans.StreamElement;
import gsn.beans.StreamSource;
import gsn.beans.VSensorConfig;
import gsn.utils.ParamParser;
import gsn.vsensor.permasense.Converter;
import gsn.wrappers.AbstractWrapper;

import org.apache.log4j.Logger;
import org.h2.tools.Server;

/**
 *
 * @author Tonio Gsell
 * @author Mustafa Yuecel
 */
public class BridgeVirtualSensorPermasense extends BridgeVirtualSensor
{
	private static final int DEFAULT_WIDTH = 610;
	
	private static final MyFilenameFilter filter = new MyFilenameFilter();
	
	private static final transient Logger logger = Logger.getLogger(BridgeVirtualSensorPermasense.class);

	private static HashSet<String> deployments = new HashSet<String>();
	
	private String deployment;
	private Connection conn = null;
	private PreparedStatement position_query = null;
	private PreparedStatement sensortype_query = null;
	private PreparedStatement serialid_query = null;
	private PreparedStatement conversion_query = null;
	private Server web;
	private int width;
	private List<AbstractWrapper> backlogWrapperList = new LinkedList<AbstractWrapper>();
	private Vector<String> jpeg_scaled;
	private boolean position_mapping = false;
	private boolean sensortype_mapping = false;
	private boolean sensorvalue_conversion = false;

	private static Map<String,Converter> converterList = new TreeMap<String,Converter>();
	
	public boolean initialize() {
		String s;
		int i;
		VSensorConfig vsensor = getVirtualSensorConfiguration();
		TreeMap<String,String> params = vsensor.getMainClassInitialParams();
		
		deployment = vsensor.getName().split("_")[0].toLowerCase();
		
		width = ParamParser.getInteger(params.get("width"), DEFAULT_WIDTH);

		
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
							String [] args = {"-webPort", "8082", "-webAllowOthers", "false"};
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
						stat.execute("CREATE TABLE positionmapping(device_id INT NOT NULL, begin DATETIME(23,0) NOT NULL, end DATETIME(23,0) NOT NULL, position INT NOT NULL, comment CLOB, PRIMARY KEY(device_id, begin, end))");
						logger.info("create positionmapping table for " + deployment + " deployment");
						s = "conf/permasense/" + deployment + "-positionmapping.csv";
						if (new File(s).exists()) {
							stat.execute("INSERT INTO positionmapping SELECT * FROM CSVREAD('" + s + "')");
						} else {
							logger.warn("positionmapping not available");
						}

						stat.execute("CREATE TABLE sensormapping(position INT NOT NULL, begin DATETIME(23,0) NOT NULL, end DATETIME(23,0) NOT NULL, sensortype VARCHAR(30) NOT NULL, sensortype_args BIGINT, comment CLOB, PRIMARY KEY(position, begin, end, sensortype))");
						logger.info("create sensormapping table for " + deployment + " deployment");
						s = "conf/permasense/" + deployment + "-sensormapping.csv";
						if (new File(s).exists()) {
							stat.execute("INSERT INTO sensormapping SELECT * FROM CSVREAD('" + s + "')");
						} else {
							logger.warn("sensormapping not available");
						}
						
						// TODO: sensortype dont depends on deployment...
						stat.execute("CREATE TABLE sensortype(sensortype VARCHAR(30) NOT NULL, signal_name VARCHAR(30) NOT NULL, physical_signal VARCHAR(30) NOT NULL, conversion VARCHAR(30), input VARCHAR(30), PRIMARY KEY(sensortype, signal_name, physical_signal))");
						logger.info("create sensortype table for " + deployment + " deployment");
						String[] files = new File("conf/permasense/sensortype_templates").list(filter);
						ListIterator<String> filenames = Arrays.asList(files).listIterator();
						while (filenames.hasNext()) {
							s = filenames.next();
							stat.execute("INSERT INTO sensortype SELECT '" + s.substring(0, s.lastIndexOf('.')) + 
									"' AS sensortype, * FROM CSVREAD('conf/permasense/sensortype_templates/" + s + "')");
						}

						stat.execute("CREATE TABLE sensortype_args(sensortype_args BIGINT NOT NULL, physical_signal VARCHAR(30) NOT NULL, value VARCHAR(255) NOT NULL, comment CLOB, PRIMARY KEY(sensortype_args, physical_signal))");
						logger.info("create sensortype_args table for " + deployment + " deployment");
						s = "conf/permasense/" + deployment + "-sensortype_args.csv";
						if (new File(s).exists()) {
							stat.execute("INSERT INTO sensortype_args SELECT * FROM CSVREAD('" + s + "')");
						} else {
							logger.info("sensortype_args not available");
						}
						
						deployments.add(deployment);
					}
					
					if (position_mapping) {
						position_query = conn.prepareStatement("SELECT position FROM positionmapping WHERE device_id = ? AND ? BETWEEN begin AND end LIMIT 1");
					}
					if (sensortype_mapping) {
						sensortype_query = conn.prepareStatement("SELECT sensortype, sensortype_args FROM sensormapping WHERE position = ? AND ? BETWEEN begin AND end AND sensortype != 'serialid'");
						serialid_query = conn.prepareStatement("SELECT sensortype_args AS sensortype_serialid FROM sensormapping WHERE position = ? AND ? BETWEEN begin AND end AND sensortype = 'serialid' LIMIT 1");
					}
					if (sensorvalue_conversion) {
						conversion_query = conn.prepareStatement("SELECT st.physical_signal AS physical_signal, st.conversion AS conversion, st.input as input, CASEWHEN(st.input IS NULL OR sm.sensortype_args IS NULL,NULL,sta.value) as value " +
								"FROM sensormapping AS sm, sensortype AS st, sensortype_args AS sta WHERE sm.position = ? AND ? BETWEEN sm.begin AND sm.end AND sm.sensortype = st.sensortype " +
								"AND st.signal_name = ? AND CASEWHEN(st.input IS NULL OR sm.sensortype_args IS NULL,TRUE,sm.sensortype_args = sta.sensortype_args AND sta.physical_signal = st.physical_signal) LIMIT 1");
					}	
				} catch (Exception e) {
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
			if (position_mapping && data.getData("device_id") != null) {
				if (data.getData("generation_time") != null) {
					Integer position = getPosition(((Integer) data.getData("device_id")).intValue(),
							new Timestamp(((Long) data.getData("generation_time")).longValue()));
					data = new StreamElement(data, 
							new String[]{"position"}, 
							new Byte[]{DataTypes.INTEGER}, 
							new Serializable[]{position});
				}
			}
			if (sensortype_mapping && 
					data.getData("position") != null &&	data.getData("generation_time") != null) {
				Serializable[] sensortype = getSensorType(((Integer) data.getData("position")).intValue(), 
						new Timestamp(((Long) data.getData("generation_time")).longValue()));
				data = new StreamElement(data, 
						new String[]{"sensortype", "sensortype_serialid"}, 
						new Byte[]{DataTypes.VARCHAR, DataTypes.BIGINT},
						sensortype);
			}
			if (sensorvalue_conversion &&
					data.getData("position") != null &&	data.getData("generation_time") != null) {
				String physical, conversion, input, value;
				Converter converter;
				HashMap<String, Serializable> map = new HashMap<String, Serializable>();
				long start = System.nanoTime();
				ListIterator<String> list = Arrays.asList(data.getFieldNames()).listIterator();
				try {
					conversion_query.setInt(1, ((Integer) data.getData("position")).intValue());
					conversion_query.setTimestamp(2, new Timestamp(((Long) data.getData("generation_time")).longValue()));
					while (list.hasNext()) {
						s = list.next().toLowerCase();
						if (s.startsWith("payload_") && !s.startsWith("payload_sample_") && data.getData(s) != null) {
							conversion_query.setString(3, s.substring("payload_".length()));
							ResultSet rs = conversion_query.executeQuery();
							if (rs.next()) {
								physical = rs.getString(1);
								conversion = rs.getString(2);
								input = rs.getString(3);
								value = rs.getString(4);
								// physical_signal, conversion, input, value
								logger.debug("physical_signal:" + physical + " conversion:" + conversion +
										" input:" + input + " value:" + value);
								
								try {
									synchronized (converterList) {
										if (!converterList.containsKey(conversion)) {
											String className = "gsn.vsensor.permasense." + conversion.substring(0,1).toUpperCase() + conversion.substring(1);
											logger.info("Instantiating converter '" + className);
											Class<?> classTemplate = Class.forName(className);
											converterList.put(conversion, (Converter)classTemplate.getConstructor().newInstance());
										}
										converter = converterList.get(conversion);
									}	
									map.put(physical, converter.convert(data.getData(s), value));
								} catch (Exception e) {
									logger.error(e.getMessage(), e);
								}
							} else {
								logger.debug("no conversion found for >" + s + "< (" + s.substring("payload_".length()) + ")");
							}
						} else {
							logger.debug("ignoring >" + s + "<");
						}
					}
					if (!map.isEmpty()) {
						Byte[] types = new Byte[map.size()];
						Arrays.fill(types, DataTypes.VARCHAR);
						data = new StreamElement(data, map.keySet().toArray(new String[]{}), 
								types, map.values().toArray(new Serializable[]{}));
					}
				} catch (SQLException e) {
					logger.warn(e.getMessage(), e);
				}
				if (logger.isDebugEnabled())
					logger.debug("conversion: " + Long.toString((System.nanoTime() - start) / 1000) + " us");				
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
		boolean ret = false;
		
		Iterator<AbstractWrapper> wrappers = backlogWrapperList.iterator();
		while (wrappers.hasNext()) {
			try {
				if (wrappers.next().sendToWrapper(command, paramNames, paramValues))
					ret = true;
			} catch (Exception e) {
				logger.warn(e.getMessage());
			}
		}
		
		return ret;
	}
	
	private Integer getPosition(int device_id, Timestamp generation_time) {
		Integer res = null;
		long start = System.nanoTime();
		try {
			position_query.setInt(1, device_id);
			position_query.setTimestamp(2, generation_time);
			ResultSet rs = position_query.executeQuery();
			if (rs.next()) {
				res = rs.getInt(1);
				if (rs.wasNull())
					res = null;
			}
		} catch (SQLException e) {
			logger.warn(e.getMessage(), e);
		}
		if (logger.isDebugEnabled())
			logger.debug("getPosition: " + Long.toString((System.nanoTime() - start) / 1000) + " us");
		return res;
	}

	private Serializable[] getSensorType(int position, Timestamp generation_time) {
		Long serialid = null;
		String s = null;
		ResultSet rs;
		StringBuffer sb = new StringBuffer();
		Serializable[] res = new Serializable[]{null, null, null};
		long start = System.nanoTime();
		try {
			sensortype_query.setInt(1, position);
			sensortype_query.setTimestamp(2, generation_time);			
			rs = sensortype_query.executeQuery();
			if (rs.first()) {
				do {
					s = rs.getString(1);
					sb.append(" " + s.substring(s.indexOf('_') + 1) + ":" + rs.getString(2));
				} while (rs.next());
				s = sb.toString();
			}
			
			serialid_query.setInt(1, position);
			serialid_query.setTimestamp(2, generation_time);
			rs = serialid_query.executeQuery();
			if (rs.next()) {
				serialid = rs.getLong(1);
				if (rs.wasNull())
					serialid = null;
			}
			res = new Serializable[]{s, serialid};
		} catch (SQLException e) {
			logger.warn(e.getMessage(), e);
		}
		if (logger.isDebugEnabled())
			logger.debug("getSensortype: " + Long.toString((System.nanoTime() - start) / 1000) + " us");
		return res;
	}
	
	public synchronized void dispose() {
		if (conn != null) {
			try {
				conn.close();
			} catch (Exception e) {
			} finally {
				conn = null;
			}
		}
		if (web != null) {
			web.shutdown();
			web = null;
		}
	}
}

class MyFilenameFilter implements FilenameFilter {
	public boolean accept(File dir, String name) {
		return name.endsWith(".csv");
	}
}
