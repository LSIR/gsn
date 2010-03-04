package gsn.wrappers;

import org.apache.log4j.Logger;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import gsn.beans.DataField;
import gsn.beans.AddressBean;
import gsn.beans.StreamElement;
import gsn.beans.DataTypes;
import gsn.storage.StorageManager;
import gsn.storage.DataEnumerator;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * This JDBC remote wrapper enables one to reply the existing stream from a a table in a remote database.
 * parameters: table: table name, start-time: starting time to replay from
 * It uses the config file 'conf/permasense/remote-database.properties', which specifies the remote
 * database location
 */
public class JDBCRemoteWrapper extends AbstractWrapper {

    private static long DEFAULT_RATE = 30000;   // 10 minute in milliseconds
    private static long DEFAULT_BUFFER_SIZE = 1000;

    private transient Logger logger = Logger.getLogger(this.getClass());
    private DataField[] outputFormat;
    private static int threadCounter = 0;
    private String table_name;
    private long start_time;
    private long rate = DEFAULT_RATE;
    private long buffer_size = DEFAULT_BUFFER_SIZE;
    private long latest_timed;
	private final static String propertyfile = "conf/permasense/remote-database.properties";
	private static Properties props = null;
	private Connection connection = null;
	private PreparedStatement prStatment;
	
    String[] dataFieldNames;
    Byte[] dataFieldTypes;
    int dataFieldsLength;

    public String getWrapperName() {
        return "JDBCRemoteWrapper";
    }

    public void dispose() {
        threadCounter--;
        try {
        	connection.close();
		} catch (SQLException e) {
			logger.error(e);
		}
    }

    public DataField[] getOutputFormat() {
        return outputFormat;
    }

    public boolean initialize() {
		
        setName(getWrapperName() + "-" + (++threadCounter));
        AddressBean addressBean = getActiveAddressBean();
        table_name = addressBean.getPredicateValue("table-name").toLowerCase();
		
		synchronized (propertyfile) {
			if (props == null) {
				try {
					props = new Properties();
					props.load(new FileInputStream(propertyfile));
				} catch (Exception e) {
					logger.error("Could not load property file: " + propertyfile, e);
					return false;
				}
				
				if (props.getProperty("url") == null) {
					logger.error("Could not get property 'url' from property file: " + propertyfile);
					return false;
				}
				
				if (props.getProperty("user") == null) {
					logger.error("Could not get property 'user' from property file: " + propertyfile);
					return false;
				}
				
				if (props.getProperty("password") == null) {
					logger.error("Could not get property 'password' from property file: " + propertyfile);
					return false;
				}
				
//				if (props.getProperty("driver") == null) {
//					logger.error("Could not get property 'driver' from property file: " + propertyfile);
//					return false;
//				}
			}
		}
		
        if (table_name == null) {
            logger.warn("The > table-name < parameter is missing from the wrapper for VS " + this.getActiveAddressBean().getVirtualSensorName());
            return false;
        }


        //////////////////
        String time = addressBean.getPredicateValue("start-time");
        if (time == null) {
            logger.error("The > start-time < parameter is missing from the wrapper for VS " + this.getActiveAddressBean().getVirtualSensorName());
            return false;
        }

        if (time.equalsIgnoreCase("continue")) {
            latest_timed = getLatestProcessed();
            logger.info("Mode: continue => " + latest_timed);
        } else if (isISOFormat(time)) {

            try {
                DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
                start_time = fmt.parseDateTime(time).getMillis();
                latest_timed = start_time;
                logger.info("Mode: ISO => " + latest_timed);
            }
            catch (IllegalArgumentException e) {
                logger.error("The > start-time < parameter is malformed (looks like ISO8601) for VS " + this.getActiveAddressBean().getVirtualSensorName());
                return false;
            }
        } else if (isLong(time)) {
            try {
                latest_timed = Long.parseLong(time);
                logger.info("Mode: epoch => " + latest_timed);
            }
            catch (NumberFormatException e) {
                logger.error("The > start-time < parameter is malformed (looks like epoch) for VS " + this.getActiveAddressBean().getVirtualSensorName());
                return false;
            }
        } else {
            logger.error("Incorrectly formatted > start-time < accepted values are: 'continue' (from latest element in destination table), iso-date (e.g. 2009-11-02T00:00:00.000+00:00), or epoch (e.g. 1257946505000)");
            return false;
        }

//		logger.debug("Connecting to SQL database:");
//		logger.debug("  url: " + props.getProperty("url"));
//		logger.debug("  driver: " + props.getProperty("driver"));
//		logger.debug("  user: " + props.getProperty("user"));
//		logger.debug("  password: " + props.getProperty("password"));
//		
//		BasicDataSource pool = new BasicDataSource();
//		logger.debug("MaxActive: " + pool.getMaxActive());
//		logger.debug("MaxIdle: " + pool.getMaxIdle());
//		logger.debug("MaxOpenPreparedStatements: " + pool.getMaxOpenPreparedStatements());
//		logger.debug("MaxWait: " + pool.getMaxWait());
//	    pool.setDriverClassName(props.getProperty("driver").trim());
//	    pool.setUsername(props.getProperty("user").trim());
//	    pool.setPassword(props.getProperty("password").trim());
//	    pool.setUrl(props.getProperty("url").trim());
//	    pool.setc(-1);
//	    pool.setMaxIdle(-1);
//	    pool.setMaxOpenPreparedStatements(-1);
//	    pool.setMaxWait(-1);
	    
        try {
        	
            logger.info("Initializing the structure of JDBCRemoteWrapper with : " + table_name);
            connection = DriverManager.getConnection(props.getProperty("url").trim(), props.getProperty("user").trim(), props.getProperty("password").trim());
            outputFormat = StorageManager.tableToStructure(table_name, connection);
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            try {
            	if( connection != null )
            		connection.close();
			} catch (SQLException e1) {
				logger.debug(e1.getMessage(),e1);
			}
            return false;
        }

        dataFieldsLength = outputFormat.length;
        dataFieldNames = new String[dataFieldsLength];
        dataFieldTypes = new Byte[dataFieldsLength];

        for (int i = 0; i < outputFormat.length; i++) {
            dataFieldNames[i] = outputFormat[i].getName();
            dataFieldTypes[i] = outputFormat[i].getDataTypeID();
        }

        String querySelect = addressBean.getPredicateValueWithDefault("query-select", "*");

        String queryWhere = addressBean.getPredicateValueWithDefault("query-where", "");
        if(queryWhere != "")
        	queryWhere = " and " + queryWhere;
        
		StringBuilder query = new StringBuilder("select " + querySelect + " from ").append(table_name).append(" where timed > ? " + queryWhere + " order by timed asc limit 0," + buffer_size);

		logger.debug("SQL query: " + query);
		
		try {
			prStatment = connection.prepareStatement(query.toString());
		} catch (SQLException e) {
			logger.error(e);
			return false;
		}

        return true;
    }

    public void run() {
    	
    	boolean isEmpty;
    	ResultSet resultSet = null;

    	while (isActive()) {
    		isEmpty = true;

			int setSize = 0;
    		try {
    			prStatment.setLong(1, latest_timed);
    			resultSet = prStatment.executeQuery();

    			while (resultSet.next()) {
    				isEmpty = false;

    				Serializable[] output = new Serializable[this.getOutputFormat().length];

    				long timed = resultSet.getLong(2);

    				for (int i = 1; i < dataFieldsLength; i++) {

    					switch (dataFieldTypes[i]) {
    					case DataTypes.VARCHAR:
    					case DataTypes.CHAR:
    						output[i] = resultSet.getString(i + 2);
    						break;
    					case DataTypes.INTEGER:
    						output[i] = resultSet.getInt(i + 2);
    						break;
    					case DataTypes.TINYINT:
    						output[i] = resultSet.getByte(i + 2);
    						break;
    					case DataTypes.SMALLINT:
    						output[i] = resultSet.getShort(i + 2);
    						break;
    					case DataTypes.DOUBLE:
    						output[i] = resultSet.getDouble(i + 2);
    						break;
    					case DataTypes.BIGINT:
    						output[i] = resultSet.getLong(i + 2);
    						break;
    					case DataTypes.BINARY:
    						output[i] = resultSet.getBytes(i + 2);
    						break;
    					}
    				}

    				StreamElement se = new StreamElement(dataFieldNames, dataFieldTypes, output, timed);

    				this.postStreamElement(se);

    				latest_timed = timed;

    				logger.debug(" Latest => " + latest_timed);
    				
    				setSize++;
    			}
    			if( setSize != DEFAULT_BUFFER_SIZE)
    				isEmpty = true;

    		} catch (SQLException e) {
    			logger.error(e.getMessage(), e);
    		}

    		try {
    			if (isEmpty)
    				Thread.sleep(rate);
    			else
    				Thread.yield();
    		} catch (InterruptedException e) {
    			logger.error(e.getMessage(), e);
    		}
    	}
    }

    public long getLatestProcessed() {
        DataEnumerator data;
        long latest = -1;
        StringBuilder query = new StringBuilder("select max(timed) from ").append(this.getActiveAddressBean().getVirtualSensorName());
        try {
            data = StorageManager.getInstance().executeQuery(query, false);
            logger.warn("Running query " + query);

            while (data.hasMoreElements()) {
                StreamElement se = data.nextElement();
                if (se.getData("max(timed)") != null)
                    latest = (Long) se.getData("max(timed)");
                logger.warn(" MAX ts = " + latest);
                logger.warn(se);

            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        } catch (NullPointerException e) {
            logger.error(e.getMessage(), e);
        }
        return latest;
    }


    public boolean isISOFormat(String time) {
        //Example: 2009-11-02T00:00:00.000+00:00
        String regexMask = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}[+-]\\d{2}:\\d{2}$";
        Pattern pattern = Pattern.compile(regexMask);
        Matcher matcher = pattern.matcher(time);
        logger.debug("Testing... " + time + " <==> " + regexMask);
        if (matcher.find()) {
            logger.debug(">>>>>    ISO FORMAT");
            return true;
        } else
            return false;
    }

    public boolean isLong(String time) {

        String regexMask = "^\\d+$";
        Pattern pattern = Pattern.compile(regexMask);
        Matcher matcher = pattern.matcher(time);
        logger.debug("Testing... " + time + " <==> " + regexMask);
        if (matcher.find()) {
            logger.debug(">>>>>    LONG number");
            return true;
        } else
            return false;
    }
}
