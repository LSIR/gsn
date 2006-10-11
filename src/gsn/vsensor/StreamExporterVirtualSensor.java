/**
 * 
 * @author Jerome Rousselot
 */
package gsn.vsensor;

import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.utils.CaseInsensitiveComparator;
import gsn.utils.ParamParser;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;

/**
 * 
 * This virtual sensor saves its input stream to any JDBC accessible source.
 * 
 * @author Jerome Rousselot ( jeromerousselot@gmail.com )
 *
 */
public class StreamExporterVirtualSensor extends AbstractVirtualSensor {

	public static final String PARAM_USER="user", PARAM_PASSWD="password", PARAM_URL="url", PARAM_TABLE_PREFIX="table-prefix";
	private static final transient Logger logger = Logger.getLogger(StreamExporterVirtualSensor.class);
	
	StringBuilder sqlbuilder = new StringBuilder();
	private String sqlstart, table_prefix = "GSN_EXPORT_";
	private Connection connection;
	private Statement statement;

	private Vector<String> createdTables = new Vector<String>();
	
	public boolean initialize(HashMap map) {
		boolean status = false;
		if (super.initialize(map) == true) {
			TreeMap<String, String> params = virtualSensorConfiguration.getMainClassInitialParams();
			params.keySet();
			try {
				DriverManager.registerDriver(new com.mysql.jdbc.Driver());
				logger.debug("mysql driver loaded.");
				logger.debug("url="+params.get(PARAM_URL)+
								   ", user="+params.get(PARAM_USER)+
								   ", passwd="+params.get(PARAM_PASSWD));
				connection = DriverManager.getConnection(params.get(PARAM_URL), 
											params.get(PARAM_USER),
											params.get(PARAM_PASSWD));
				logger.debug("jdbc connection established.");
				if(params.get(PARAM_TABLE_PREFIX) != null)
					table_prefix = params.get(PARAM_TABLE_PREFIX);
				statement = connection.createStatement();
				status = true;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.warn("Could not connect StreamExporterVS to jdbc source at url: " + params.get(PARAM_URL));
				logger.info(e);
				status = false;
			}
		}
		return status;
	}

	/* (non-Javadoc)
	 * @see gsn.vsensor.VirtualSensor#dataAvailable(java.lang.String, gsn.beans.StreamElement)
	 */
	public void dataAvailable(String inputStreamName, StreamElement streamElement) {
		// TODO Auto-generated method stub
		String tableName = table_prefix + inputStreamName;
		ensureTableExistence(tableName, streamElement.getFieldNames(), streamElement.getFieldTypes());
		exportValues(tableName, streamElement);	
		
	}
	
	/*
	 *  After a call to this method, we are sure that the requested
	 *  table exists.
	 *  @param tableName The table name to check for.
	 */
	
	private void ensureTableExistence(String tableName, String[] fieldNames, Integer[] fieldTypes) {
		sqlbuilder = new StringBuilder();
		sqlbuilder.append("CREATE TABLE IF NOT EXISTS "); // Is this Mysql specific ?
		sqlbuilder.append(tableName);
		sqlbuilder.append(" (");
		// (COLNAME COLTYPE, COLNAME COLTYPE,...)
		boolean needsComma=false;
		for(int current = 0; current < fieldNames.length ; current++ ) {
			if(needsComma)
				sqlbuilder.append(", ");
			sqlbuilder.append(fieldNames[current]);
			sqlbuilder.append(" ");
			sqlbuilder.append(DataTypes.TYPE_NAMES[fieldTypes[current]]);
			needsComma = true;
		}
		sqlbuilder.append(");");
		try {
			logger.debug("Trying to run sql query:" + sqlbuilder.toString());
			statement.execute(sqlbuilder.toString());
		} catch (SQLException e) {
			logger.error("Could not create table for export in remote database: " + e);
		}
	}
	
	/*
	 *  Export all received values from a stream to the proposed
	 *  table name into the database selected by the
	 *   currently open connection.
	 */
	
	private void exportValues(String tableName, StreamElement streamElement) {
		sqlbuilder = new StringBuilder();
		sqlbuilder.append("INSERT INTO");
		sqlbuilder.append(tableName);
		sqlbuilder.append(" (");
		// (COLNAME1, COLNAME2,...) VALUES (bla1, bla2...) [, (bla1, bla2,...)
		boolean needsComma=false;
		for(int current=0; current < streamElement.getFieldNames().length; current++) {
			if(needsComma)
				sqlbuilder.append(", ");
			sqlbuilder.append(streamElement.getFieldNames()[current]);
			needsComma=true;
		}
		sqlbuilder.append(") VALUES (");
		needsComma=false;
		for(int current=0; current < streamElement.getData().length; current++) {
			if(needsComma)
				sqlbuilder.append(", ");
			sqlbuilder.append(streamElement.getData()[current]);
			needsComma=true;
		}
		
		sqlbuilder.append(");");
		
		try {
			logger.debug("Trying to run sql query:" + sqlbuilder.toString());
			statement.execute(sqlbuilder.toString());
		} catch (SQLException e) {
			logger.error("Could not insert values into remote table for export: " + e);
		}
	}
}
