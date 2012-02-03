package gsn.vsensor;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.h2.tools.Server;

import com.vividsolutions.jts.geom.Coordinate;

import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.vsensor.permasense.Converter;

public class DataMapping {
	
	private static final transient Logger logger = Logger.getLogger(DataMapping.class);
	private static DataMapping mappings = new DataMapping();
	
	private static final FilenameFilter filter = mappings.new MappingFilenameFilter();
	
	private static HashMap<String, Mappings> deployments = new HashMap<String, Mappings>();
	private static HashMap<AbstractVirtualSensor, String> vsmappings = new HashMap<AbstractVirtualSensor, String>();
	private static Server web;
	
	/**
	 * This method registers the vs for mappings on the specified deployment. The virtual sensor has to call the
	 * removeVS method in its dispose method.
	 * 
	 * @param vs
	 * @param deployment
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 */
	public static void registerVS(AbstractVirtualSensor vs, String deployment) throws SQLException, ClassNotFoundException {
		String s;
		Connection conn;
		synchronized (deployments) {
			if (deployments.isEmpty()) {
				Class.forName("org.h2.Driver");
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

			// check if table is already created
			if (!deployments.containsKey(deployment)) {
				conn = DriverManager.getConnection("jdbc:h2:mem:" + deployment + ";DB_CLOSE_DELAY=-1", "sa", "");
				conn.setAutoCommit(true);
				logger.info("connected to jdbc:h2:mem:" + deployment + "...");

				Statement stat = conn.createStatement();
				stat.execute("DROP TABLE IF EXISTS positionmapping");				
				stat.execute("CREATE TABLE positionmapping(device_id INT NOT NULL, begin DATETIME(23,0) NOT NULL, end DATETIME(23,0) NOT NULL, position INT NOT NULL, comment CLOB, PRIMARY KEY(device_id, begin, end))");
				logger.info("create positionmapping table for " + deployment + " deployment");
				s = "conf/permasense/" + deployment + "-positionmapping.csv";
				if (new File(s).exists()) {
					stat.execute("INSERT INTO positionmapping SELECT * FROM CSVREAD('" + s + "')");
				} else {
					logger.warn("positionmapping not available");
				}
				
				stat.execute("DROP TABLE IF EXISTS geomapping");
				stat.execute("CREATE TABLE geomapping(position INT NOT NULL, longitude DOUBLE, latitude DOUBLE, altitude DOUBLE, comment CLOB, PRIMARY KEY(position))");
				logger.info("create geomapping table for " + deployment + " deployment");
				s = "conf/permasense/" + deployment + "-geomapping.csv";
				if (new File(s).exists()) {
					stat.execute("INSERT INTO geomapping SELECT * FROM CSVREAD('" + s + "')");
				} else {
					logger.warn("Geographic coordinate not available");
				}

				stat.execute("DROP TABLE IF EXISTS sensormapping");
				stat.execute("CREATE TABLE sensormapping(position INT NOT NULL, begin DATETIME(23,0) NOT NULL, end DATETIME(23,0) NOT NULL, sensortype VARCHAR(30) NOT NULL, sensortype_args BIGINT, comment CLOB, PRIMARY KEY(position, begin, end, sensortype))");
				logger.info("create sensormapping table for " + deployment + " deployment");
				s = "conf/permasense/" + deployment + "-sensormapping.csv";
				if (new File(s).exists()) {
					stat.execute("INSERT INTO sensormapping SELECT * FROM CSVREAD('" + s + "')");
				} else {
					logger.warn("sensormapping not available");
				}

				// TODO: sensortype dont depends on deployment...
				stat.execute("DROP TABLE IF EXISTS sensortype");
				stat.execute("CREATE TABLE sensortype(sensortype VARCHAR(30) NOT NULL, signal_name VARCHAR(30) NOT NULL, physical_signal VARCHAR(30) NOT NULL, conversion VARCHAR(30), input VARCHAR(30), PRIMARY KEY(sensortype, signal_name, physical_signal))");
				logger.info("create sensortype table for " + deployment + " deployment");
				String[] files = new File("conf/permasense/sensortype_templates").list(filter);
				ListIterator<String> filenames = Arrays.asList(files).listIterator();
				while (filenames.hasNext()) {
					s = filenames.next();
					stat.execute("INSERT INTO sensortype SELECT '" + s.substring(0, s.lastIndexOf('.')) + 
							"' AS sensortype, * FROM CSVREAD('conf/permasense/sensortype_templates/" + s + "')");
				}

				stat.execute("DROP TABLE IF EXISTS sensortype_args");
				stat.execute("CREATE TABLE sensortype_args(sensortype_args BIGINT NOT NULL, physical_signal VARCHAR(30) NOT NULL, value VARCHAR(255) NOT NULL, comment CLOB, PRIMARY KEY(sensortype_args, physical_signal))");
				logger.info("create sensortype_args table for " + deployment + " deployment");
				s = "conf/permasense/" + deployment + "-sensortype_args.csv";
				if (new File(s).exists()) {
					stat.execute("INSERT INTO sensortype_args SELECT * FROM CSVREAD('" + s + "')");
				} else {
					logger.info("sensortype_args not available");
				}
				// queries
				Mappings m = mappings.new Mappings(conn);
				deployments.put(deployment, m);
			}
			vsmappings.put(vs, deployment);
		}

	}
	
	public static void removeVS (AbstractVirtualSensor vs) {
		String vsdeployment;
		synchronized (deployments) {
			if(vsmappings.containsKey(vs)) {
				if (deployments.containsKey(vsmappings.get(vs))) {
					vsdeployment = vsmappings.get(vs);					
					vsmappings.remove(vs);
					if (logger.isDebugEnabled()) {
						for (AbstractVirtualSensor v: vsmappings.keySet()) {
							logger.debug("vs using mappings for deployment "+vsdeployment+":"+v.getVirtualSensorConfiguration().getName());
						}
					}
					if (!vsmappings.containsValue(vsdeployment)) {
						// this was the last vs that needed this resource
						logger.info("remove deployment mappings for " + vsdeployment);
						Connection conn = deployments.get(vsdeployment).conn;
						if (conn != null) {
							try {
								conn.close();
							} catch (Exception e) {
							}
						}
						deployments.remove(vsdeployment);
						if (deployments.isEmpty() && web != null) {
							logger.debug("shut down h2 webserver");
							web.shutdown();
							web.stop();
							web = null;
						}
					}
				}
			}
		}		
	}
	
	// mapping queries
	public static Integer getPosition(int device_id, Timestamp generation_time, String deployment, String vsName, String inputStreamName) {
		Integer res = null;
		long start = System.nanoTime();
		try {
			synchronized (deployments) {
				if (!deployments.containsKey(deployment)) {
					logger.error(vsName+"["+inputStreamName+"]: Position mapping data not available for deployment "+deployment);
					return null;
				}
				PreparedStatement position_query = deployments.get(deployment).position_query;
				position_query.setInt(1, device_id);
				position_query.setTimestamp(2, generation_time);
				ResultSet rs = position_query.executeQuery();
				if (rs.next()) {
					res = rs.getInt(1);
					if (rs.wasNull())
						res = null;
				}
				if (res==null)
					logger.warn(vsName+"["+inputStreamName+"]: No position mapping available for deployment "+deployment+" device-id "+device_id);
			}
		} catch (SQLException e) {
			logger.warn(e.getMessage(), e);
		}
		if (logger.isDebugEnabled())
			logger.debug(vsName+"["+inputStreamName+"]: getPosition: " + Long.toString((System.nanoTime() - start) / 1000) + " us");
		return res;
	}
	
	public static Coordinate getCoordinate(int position, String deployment, String vsName, String inputStreamName) {
		Coordinate res = null;
		long start = System.nanoTime();
		try {
			synchronized (deployments) {
				if (!deployments.containsKey(deployment)) {
					logger.error(vsName+"["+inputStreamName+"]: Geographic coordinate mapping data not available for deployment "+deployment);
					return null;
				}
				PreparedStatement coordinate_query = deployments.get(deployment).coordinate_query;
				coordinate_query.setInt(1, position);
				ResultSet rs = coordinate_query.executeQuery();
				if (rs.next()) {
					res = new Coordinate(rs.getDouble(1), rs.getDouble(2), rs.getDouble(3));
					if (rs.wasNull())
						res = null;
				}
				if (res==null)
					logger.warn(vsName+"["+inputStreamName+"]: No coordinate mapping available for deployment "+deployment+" position "+position);
			}
		} catch (SQLException e) {
			logger.warn(e.getMessage(), e);
		}
		if (logger.isDebugEnabled())
			logger.debug(vsName+"["+inputStreamName+"]: getCoordinate: " + Long.toString((System.nanoTime() - start) / 1000) + " us");
		return res;
	}

	public static Serializable[] getSensorType(int position, Timestamp generation_time, String deployment, String vsName, String inputStreamName) {
		Long serialid = null;
		String s = null;
		ResultSet rs;
		StringBuffer sb = new StringBuffer();
		Serializable[] res = new Serializable[]{null, null, null};
		long start = System.nanoTime();
		try {
			synchronized (deployments) {
				if (!deployments.containsKey(deployment)) {
					logger.error(vsName+"["+inputStreamName+"]: Sensor mapping data not available for deployment "+deployment);
					return null;
				}
				PreparedStatement sensortype_query = deployments.get(deployment).sensortype_query;
				PreparedStatement serialid_query = deployments.get(deployment).serialid_query;
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
			}
			res = new Serializable[]{s, serialid};
		} catch (SQLException e) {
			logger.warn(e.getMessage(), e);
		}
		if (logger.isDebugEnabled())
			logger.debug(vsName+"["+inputStreamName+"]: getSensortype: " + Long.toString((System.nanoTime() - start) / 1000) + " us");
		return res;
	}
	
	public static StreamElement getConvertedValues(StreamElement data, String deployment, String vsName, String inputStreamName) {
		String s, physical, conversion, input, value;
		Converter converter;
		HashMap<String, Serializable> map = new HashMap<String, Serializable>();
		long start = System.nanoTime();
		ListIterator<String> list = Arrays.asList(data.getFieldNames()).listIterator();
		try {
			synchronized (deployments) {
				if (!deployments.containsKey(deployment)) {
					logger.error(vsName+"["+inputStreamName+"]: Sensor conversion mapping data not available for deployment "+deployment);
					return null;
				}
				PreparedStatement conversion_query = deployments.get(deployment).conversion_query;
				Map<String,Converter> converterList = deployments.get(deployment).converterList;
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
							logger.debug(vsName+"["+inputStreamName+"]: physical_signal:" + physical + " conversion:" + conversion +
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
							logger.debug(vsName+"["+inputStreamName+"]: no conversion found for >" + s + "< (" + s.substring("payload_".length()) + ")");
						}
					} else {
						logger.debug(vsName+"["+inputStreamName+"]: ignoring >" + s + "<");
					}
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
			logger.debug(vsName+"["+inputStreamName+"]: conversion: " + Long.toString((System.nanoTime() - start) / 1000) + " us");
		return data;
	}
	
	class Mappings {
		public Connection conn = null;
		public PreparedStatement position_query = null;
		public PreparedStatement coordinate_query = null;
		public PreparedStatement sensortype_query = null;
		public PreparedStatement serialid_query = null;
		public PreparedStatement conversion_query = null;
		public Map<String,Converter> converterList = new TreeMap<String,Converter>();

		public Mappings(Connection conn) throws SQLException {
			this.conn = conn;
			position_query = this.conn.prepareStatement("SELECT position FROM positionmapping WHERE device_id = ? AND ? BETWEEN begin AND end LIMIT 1");
			coordinate_query = this.conn.prepareStatement("SELECT longitude, latitude, altitude FROM geomapping WHERE position = ? LIMIT 1");
			sensortype_query = this.conn.prepareStatement("SELECT sensortype, sensortype_args FROM sensormapping WHERE position = ? AND ? BETWEEN begin AND end AND sensortype != 'serialid'");
			serialid_query = this.conn.prepareStatement("SELECT sensortype_args AS sensortype_serialid FROM sensormapping WHERE position = ? AND ? BETWEEN begin AND end AND sensortype = 'serialid' LIMIT 1");
			conversion_query = this.conn.prepareStatement("SELECT st.physical_signal AS physical_signal, st.conversion AS conversion, st.input as input, CASEWHEN(st.input IS NULL OR sm.sensortype_args IS NULL,NULL,sta.value) as value " +
						"FROM sensormapping AS sm, sensortype AS st, sensortype_args AS sta WHERE sm.position = ? AND ? BETWEEN sm.begin AND sm.end AND sm.sensortype = st.sensortype " +
				"AND st.signal_name = ? AND CASEWHEN(st.input IS NULL OR sm.sensortype_args IS NULL,TRUE,sm.sensortype_args = sta.sensortype_args AND sta.physical_signal = st.physical_signal) LIMIT 1");
		}
	}
	
	class MappingFilenameFilter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			return name.endsWith(".csv");
		}
	}
}




