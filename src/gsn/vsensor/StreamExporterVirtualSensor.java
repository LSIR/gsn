/**
 * 
 * @author Jerome Rousselot
 */
package gsn.vsensor;

import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.storage.StorageManager.DATABASE;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;
import org.apache.log4j.Logger;
import java.sql.PreparedStatement;
/**
 * This virtual sensor saves its input stream to any JDBC accessible source.
 * 
 * @author Jerome Rousselot ( jeromerousselot@gmail.com )
 */
public class StreamExporterVirtualSensor extends AbstractVirtualSensor {

	public static final String            PARAM_USER    = "user" , PARAM_PASSWD = "password" , PARAM_URL = "url" , PARAM_TABLE_PREFIX = "table";

	private static final transient Logger logger        = Logger.getLogger( StreamExporterVirtualSensor.class );

	StringBuilder                         sqlbuilder    = new StringBuilder( );

	private String                        sqlstart ;

	private Connection                    connection;

	private Statement                     statement;

	private Vector < String >             createdTables = new Vector < String >( );

	private String table_name;

	public boolean initialize ( ) {
		VSensorConfig vsensor = getVirtualSensorConfiguration( );
		TreeMap < String , String > params = vsensor.getMainClassInitialParams( );
		params.keySet( );
		if ( params.get( PARAM_URL ) != null && params.get( PARAM_USER ) != null && params.get( PARAM_PASSWD ) != null ) {
			try {
				// identify database
				for ( DATABASE db : DATABASE.values( ) )
					if ( params.get( PARAM_URL ).startsWith( db.getJDBCPrefix( ) ) ) {
						db.loadDriver( );
						logger.info( "driver for " + db.toString( ) + " loaded." );
					}
				logger.debug( "url=" + params.get( PARAM_URL ) + ", user=" + params.get( PARAM_USER ) + ", passwd=" + params.get( PARAM_PASSWD ) );
				connection = DriverManager.getConnection( params.get( PARAM_URL ) , params.get( PARAM_USER ) , params.get( PARAM_PASSWD ) );
				logger.debug( "jdbc connection established." );
				if ( params.get( PARAM_TABLE_PREFIX ) != null ) 
					table_name = params.get( PARAM_TABLE_PREFIX );
				statement = connection.createStatement( );

			} catch ( SQLException e ) {
				// TODO Auto-generated catch block
				logger.error( "Could not connect StreamExporterVS to jdbc source at url: " + params.get( PARAM_URL ) );
				if(logger.isDebugEnabled())
					logger.debug( e );
				return false;
			}
		}
		return true;
	}

	public void dataAvailable ( String inputStreamName , StreamElement streamElement ) {
		ensureTableExistence( table_name , streamElement.getFieldNames( ) , streamElement.getFieldTypes( ) );
		exportValues( table_name , streamElement );

	}


	/*
	 * Creates a table with the requested name and structure, with the addition of the
	 * GSN_TIMESTAMP field.
	 */
	private void createTable(String tableName , String [ ] fieldNames , Byte [ ] fieldTypes) {
		sqlbuilder = new StringBuilder();
		sqlbuilder.append( "CREATE TABLE " ); 
		sqlbuilder.append( tableName );
		sqlbuilder.append( " ( GSN_TIMESTAMP ");
		sqlbuilder.append("TIMESTAMP");  //SQL 2 standard data type. Should be fairly portable.
		// (COLNAME COLTYPE, COLNAME COLTYPE,...)
		// We must convert gsn data type to db data type
		for ( int current = 0 ; current < fieldNames.length ; current++ ) {
			sqlbuilder.append(", ");
			sqlbuilder.append( fieldNames[ current ] );
			sqlbuilder.append( " " );
			sqlbuilder.append( DataTypes.TYPE_NAMES[ fieldTypes[ current ] ] );
		}
		sqlbuilder.append( ");" );
		try {
			if(logger.isDebugEnabled())
				logger.debug( "Trying to run sql query:" + sqlbuilder.toString( ) );
			statement.execute( sqlbuilder.toString( ) );
		} catch ( SQLException e ) {
			if(logger.isDebugEnabled())
				logger.error( "Could not create table for export in remote database : " + e );
		}

	}
	/*
	 * After a call to this method, we are sure that the requested table exists.
	 * @param tableName The table name to check for.
	 */

	private void ensureTableExistence ( String tableName , String [ ] fieldNames , Byte [ ] fieldTypes ) {
		sqlbuilder = new StringBuilder( );
		sqlbuilder.append("SELECT * FROM " + tableName);
		if(logger.isDebugEnabled())
			logger.debug( "Trying to run sql query:" + sqlbuilder.toString());
		boolean finished = false;
		try  {
			statement.execute(sqlbuilder.toString());
		} catch(Exception sqlException) {
			// We assume that the table does not exist.
			createTable(tableName, fieldNames, fieldTypes);
			finished = true;
		}
		// Table exists. Is it the same structure ?
		if(!finished) {
			try {
				ResultSetMetaData meta = statement.getResultSet().getMetaData();
				boolean structureLooksOk = false;
				ArrayList<String> fields2 = new ArrayList<String>();
				for(int i = 1; i <  meta.getColumnCount() + 1; i++)
					fields2.add(meta.getColumnName(i));
				if(logger.isDebugEnabled())
					logger.debug("fields2="+fields2);
				StringBuilder t = new StringBuilder();
				for(String field: fieldNames)
					t.append(field+" ");
				if(logger.isDebugEnabled())
					logger.debug("fieldNames="+t.toString());
				if(fields2.containsAll(Arrays.asList(fieldNames)) && fields2.contains("GSN_TIMESTAMP"))
					structureLooksOk = true;
				if(!structureLooksOk)
					logger.error("A table named " + tableName + " already exists in the database, " +
					"with a different structure ! Aborting stream export operation.");

			} catch(Exception sqlException) {
				logger.error("An SQL error occured while checking table structure: " + sqlException);
			}
		}
	}


	/*
	 * Export all received values from a stream to the proposed table name into
	 * the database selected by the currently open connection.
	 */

	private void exportValues ( String tableName , StreamElement streamElement ) {
		sqlbuilder = new StringBuilder( );
		sqlbuilder.append( "INSERT INTO " );
		sqlbuilder.append( tableName );
		sqlbuilder.append( " (GSN_TIMESTAMP" );
		// (COLNAME1, COLNAME2,...) VALUES (bla1, bla2...) [, (bla1, bla2,...)
		for ( int current = 0 ; current < streamElement.getFieldNames( ).length ; current++ ) {
			sqlbuilder.append( ", " );
			sqlbuilder.append( streamElement.getFieldNames( )[ current ] );
		}
		sqlbuilder.append( ") VALUES (?" );

		for ( int current = 0 ; current < streamElement.getData( ).length ; current++ ) {
			sqlbuilder.append( ", " );
			sqlbuilder.append( streamElement.getData( )[ current ] );
		}

		sqlbuilder.append( ");" );

		try {
			if(logger.isDebugEnabled())
				logger.debug( "Trying to run sql query:" + sqlbuilder.toString( ) );
			PreparedStatement s = connection.prepareStatement(sqlbuilder.toString());
			s.setTimestamp(1, new java.sql.Timestamp(streamElement.getTimeStamp()));
			s.execute();
		} catch ( SQLException e ) {
			logger.error( "Could not insert values into remote table for export: " + e );
		}
	}

	public void finalize ( ) {

	}
}
