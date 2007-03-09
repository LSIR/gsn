package gsn.storage;

import static gsn.Main.getContainerConfig;
import gsn.Main;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.utils.GSNRuntimeException;
import gsn.utils.ValidityTools;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * Date: Aug 5, 2005 <br>
 * Time: 9:29:47 AM <br>
 */
public class StorageManager {

	/**************************************************************************************************
	 *  Various HELPER METHODS.
	 **************************************************************************************************/
	/**
	 * Given a connection, gets the appropriate DATABASE object from the DATABASE Enum.
	 */
	public static DATABASE getDatabaseForConnection(Connection connection) throws SQLException {
		String name = connection.getMetaData().getDatabaseProductName();
		if (name.toLowerCase().indexOf("hsql")>=0)
			return DATABASE.HSQL;
		else if (name.toLowerCase().indexOf("mysql")>=0)
			return DATABASE.MYSQL;
		return null;
	}

	/**
	 * Returns false if the table doesnt exist.
	 * Uses the current default connection.
	 * 
	 * @param tableName
	 * @return False if the table doesn't exist in the current connection.
	 * @throws SQLException
	 */
	public boolean tableExists(CharSequence tableName) throws SQLException{
		return tableExists(tableName,new DataField[] {},getConnection());
	}
	/**
	 * Checks to see if the given tablename exists using the given connection.
	 * @param tableName
	 * @param connection
	 * @return
	 * @throws SQLException
	 */
	public static boolean tableExists(CharSequence tableName,Connection connection) throws SQLException{
		return tableExists(tableName,new DataField[] {},connection);
	}
	/**
	 * Returns false if the table doesnt exist. If the table exists but the structure is not compatible with the specified
	 * fields the method throws GSNRuntimeException.
	 * Note that this method doesn't close the connection
	 * @param tableName
	 * @param connection (this method will not close it and the caller is responsible for closing the connection)
	 * @return
	 * @throws SQLException
	 * @Throws GSNRuntimeException 
	 */

	public static boolean tableExists(CharSequence tableName, DataField[] fields,Connection connection)throws SQLException ,GSNRuntimeException{
		if (!ValidityTools.isValidJavaVariable(tableName))
			throw new GSNRuntimeException("Table name is not valid");
		StringBuilder sb = new StringBuilder("select * from ").append(tableName).append(" where false ");
		ResultSet rs = null;
		try{
			rs = executeQueryWithResultSet(sb,connection);
			ResultSetMetaData structure= rs.getMetaData();
			if (fields!=null && fields.length>0) 
				nextField : for (DataField field : fields) {
					for (int i=1;i<=structure.getColumnCount();i++) {
						String colName = structure.getColumnName(i);
						int colType = structure.getColumnType(i);
						if (field.getName().equalsIgnoreCase(colName))
							if (field.getDataTypeID()==DataTypes.convertFromJDBCToGSNFormat(colType))
								continue nextField;
							else 
								throw new GSNRuntimeException("The column : "+colName+" in the >"+tableName+"< is not compatible with type : "+field.getType()+". The actual type is : "+colType);
					}
					throw new GSNRuntimeException("The table "+tableName+" doesn't have the >"+field.getName()+"< column.");
				}
		}catch (SQLException e) {
			DATABASE database = getDatabaseForConnection(connection);
			if(e.getErrorCode()==database.getTableNotExistsErrNo())
				return false;
			else {
				logger.error(e.getErrorCode());
				throw e;
			}
		}finally{
			if (rs!=null)
				try {
					rs.close();
				} catch (SQLException e) {
					logger.error(e.getMessage(),e);
				}
		}
		return true;
	}
	public  boolean tableExists(CharSequence tableName, DataField[] fields)throws SQLException {
		return tableExists(tableName,fields,getConnection());
	}

	/**
	 * Returns true if the specified query has any result in it's result set.
	 * The created result set will be closed automatically.
	 * @param sqlQuery
	 * @return
	 */
	public boolean isThereAnyResult ( StringBuilder sqlQuery ) {
		boolean toreturn = false;
		PreparedStatement prepareStatement = null;
		try {
			prepareStatement = getConnection().prepareStatement( sqlQuery.toString());
			ResultSet resultSet =  prepareStatement.executeQuery();
			toreturn = resultSet.next( );
		} catch ( SQLException error ) {
			logger.error( error.getMessage( ) , error );
		} finally {
			close(prepareStatement);
		}
		return toreturn;
	}

	/**
	 * Executes the query of the database. Returns the specified colIndex of the
	 * first row. Useful for image recovery of the web interface.
	 * 
	 * @param query The query to be executed.
	 * @return A resultset with only one row and one column. The user of the method
	 * should first call next on the result set to make sure that the row is there and then
	 * retrieve the value for the row.
	 * 
	 * @throws SQLException 
	 */

	public static ResultSet getBinaryFieldByQuery ( StringBuilder query , String colName , long pk ,Connection connection) throws SQLException {
		PreparedStatement 	ps = connection.prepareStatement(query.toString());
		ps.setLong( 1 , pk );
		return   ps.executeQuery( );
	}
	public ResultSet getBinaryFieldByQuery ( StringBuilder query , String colName , long pk ) throws SQLException {
		return getBinaryFieldByQuery(query, colName, pk,getConnection());
	}
	public static void closeStatement(Statement stmt) {
		try {
			if (stmt!=null )
				stmt.close();
		} catch (SQLException e) {
			logger.error(e.getMessage(),e);
		}
	}
	public static void close(ResultSet resultSet) {
		try {
			if (resultSet!=null )
				resultSet.close();
		} catch (SQLException e) {
			logger.error(e.getMessage(),e);
		}
	}
	public static void close(PreparedStatement preparedStatement) {
		try {
			if (preparedStatement!=null )
				preparedStatement.close();
		} catch (SQLException e) {
			logger.error(e.getMessage(),e);
		}
	}
	/**
	 * This method only works with HSQLDB database.
	 * If one doesn't close the HSQLDB properly with high probability, the DB goes into instable or in worst case becomes corrupted.
	 * 
	 * @throws SQLException
	 */public void shutdown ( ) throws SQLException {
		 if ( StorageManager.isHsql( ) ) {
			 getConnection().createStatement( ).execute( "SHUTDOWN" );
			 logger.warn( "Closing the database server (for HSqlDB) [done]." );
		 }
		 logger.warn( "Closing the connection pool [done]." );
	 }
	 /**************************************************************************************************
	  *  Various Statement Executors.
	  **************************************************************************************************/

	 public void executeRenameTable ( String oldName , String newName ) throws SQLException {
		 executeRenameTable(oldName, newName,getConnection());
	 }

	 public void executeRenameTable ( String oldName , String newName,Connection connection ) throws SQLException {
		 getConnection().prepareStatement( getStatementRenameTable(oldName, newName)).execute();
	 }

	 public void executeDropTable(CharSequence tableName ) throws SQLException {
		 executeDropTable(tableName,getConnection());
	 }
	 public void executeDropTable(CharSequence tableName ,Connection connection) throws SQLException{
		 if ( logger.isDebugEnabled( ) ) logger.debug( "Dropping table structure: " + tableName );
		 PreparedStatement prepareStatement = connection.prepareStatement(getStatementDropTable(tableName).toString());
		 prepareStatement.execute();
		 close(prepareStatement);
		 prepareStatement = connection.prepareStatement( getStatementDropIndex(tableName+"_INDEX", connection).toString());
		 prepareStatement.execute();
		 close(prepareStatement);
	 }

	 public void executeDropView(StringBuilder tableName ) throws SQLException {
		 executeDropView(tableName,getConnection());
	 }

	 public void executeDropView(StringBuilder tableName ,Connection connection) throws SQLException{
		 if ( logger.isDebugEnabled( ) ) logger.debug( "Dropping table structure: " + tableName );
		 PreparedStatement prepareStatement = connection.prepareStatement( getStatementDropView(tableName,connection).toString());
		 prepareStatement.execute();
		 close(prepareStatement);
	 }
	 public void executeCreateTable ( CharSequence tableName , DataField[] structure ) throws SQLException {
		 executeCreateTable(tableName,structure,getConnection());
	 }
	 /**
	  * Create a table with a index on the timed field.
	  * @param tableName
	  * @param structure
	  * @param connection
	  * @throws SQLException 
	  */
	 public static void executeCreateTable ( CharSequence tableName , DataField[] structure,Connection connection ) throws SQLException  {
		 StringBuilder sql = getStatementCreateTable(tableName, structure, connection);
		 if ( logger.isDebugEnabled() ) logger.debug( new StringBuilder( ).append( "The create table statement is : " ).append( sql ).toString( ) );
		 PreparedStatement prepareStatement = connection.prepareStatement( sql.toString());
		 prepareStatement.execute();
		 prepareStatement.close();

		 sql = getStatementCreateIndexOnTimed(tableName);
		 if ( logger.isDebugEnabled() ) logger.debug( new StringBuilder( ).append( "The create index statement is : " ).append( sql ).toString( ) );
		 prepareStatement = connection.prepareStatement( sql.toString());
		 prepareStatement.execute();
		 prepareStatement.close();

	 }

	 public ResultSet executeQueryWithResultSet ( StringBuilder query ) throws SQLException {
		 return executeQueryWithResultSet(query,getConnection());
	 }
	 public static ResultSet executeQueryWithResultSet ( StringBuilder query, Connection connection ) throws SQLException {
		 return connection.prepareStatement( query.toString()).executeQuery();
	 }
	 public static DataEnumerator executeQuery ( StringBuilder query , boolean binaryFieldsLinked ,Connection connection) throws SQLException {
		 if (logger.isDebugEnabled()) logger.debug("Executing query: "+query+"("+binaryFieldsLinked+")");
		 return new DataEnumerator(connection.prepareStatement(query.toString()), binaryFieldsLinked );
	 }

	 public DataEnumerator executeQuery ( StringBuilder query , boolean binaryFieldsLinked ) throws SQLException {
		 return executeQuery(query, binaryFieldsLinked,getConnection());
	 }
	 public void executeCreateView(CharSequence viewName , CharSequence selectQuery) throws SQLException {
		 executeCreateView(viewName, selectQuery,getConnection());
	 }
	 public void executeCreateView(CharSequence viewName , CharSequence selectQuery,Connection connection) throws SQLException {
		 StringBuilder statement =  getStatementCreateView(viewName, selectQuery);
		 if (logger.isDebugEnabled())
			 logger.debug("Creating a view:"+statement);
		 final PreparedStatement prepareStatement = connection.prepareStatement(statement.toString());
		 prepareStatement.execute();
		 prepareStatement.close();
	 }

	 /**
	  * This method executes the provided statement over the connection.
	  * If there is an error retruns -1 otherwise it returns the output of the executeUpdate method
	  * on the PreparedStatement class which reflects the number of changed rows in the underlying table.
	  * 
	  * @param updateStatement
	  * @param connection
	  * @return Number of effected rows or -1 if there is an error.
	  */
	 public static int executeUpdate ( StringBuilder updateStatement ,Connection connection) {
		 int toReturn =-1;
		 PreparedStatement prepareStatement = null ;
		 try {
			 prepareStatement = connection.prepareStatement( updateStatement.toString());
			 toReturn = prepareStatement.executeUpdate();
		 } catch ( SQLException error ) {
			 logger.error( error.getMessage( ) , error );
		 }finally {
			 close(prepareStatement);
		 }
		 return toReturn;
	 }
	 public  int executeUpdate ( StringBuilder updateStatement ) throws SQLException {
		 return executeUpdate(updateStatement,getConnection());
	 }

	 public  void executeInsert ( CharSequence tableName , DataField[] fields, StreamElement streamElement ) throws SQLException {
		 executeInsert(tableName, fields, streamElement,getConnection());
	 }
	 public static void executeInsert ( CharSequence tableName , DataField[] fields, StreamElement streamElement , Connection connection) throws SQLException {
		 PreparedStatement ps = null;
		 try {	
			 ps= connection.prepareStatement( getStatementInsert(tableName, fields, streamElement).toString());
			 int counter;
			 for ( counter = 1 ; counter <= streamElement.getFieldTypes( ).length ; counter++ ) {
				 Serializable value = streamElement.getData( )[ counter - 1 ];
				 switch ( streamElement.getFieldTypes( )[ counter - 1 ] ) {
				 case DataTypes.VARCHAR :
					 if (value==null)
						 ps.setNull(counter, Types.VARCHAR);
					 else
						 ps.setString(counter, value.toString());
					 break;
				 case DataTypes.CHAR :
					 if (value==null)
						 ps.setNull(counter, Types.CHAR);
					 else
						 ps.setString(counter, value.toString());
					 break;
				 case DataTypes.INTEGER :
					 if (value==null)
						 ps.setNull(counter, Types.INTEGER);
					 else
						 ps.setInt( counter , ( Integer ) value );
					 break;
				 case DataTypes.SMALLINT :
					 if (value==null)
						 ps.setNull(counter, Types.SMALLINT);
					 else
						 ps.setShort( counter , ( Short ) value );
					 break;
				 case DataTypes.TINYINT :
					 if (value==null)
						 ps.setNull(counter, Types.TINYINT);
					 else
						 ps.setByte( counter , ( Byte ) value );
					 break;
				 case DataTypes.DOUBLE :
					 if (value==null)
						 ps.setNull(counter, Types.DOUBLE);
					 else 
						 ps.setDouble( counter , (Double)value );
					 break;
				 case DataTypes.BIGINT :
					 if (value==null)
						 ps.setNull(counter, Types.BIGINT);
					 else
						 ps.setLong( counter , ( Long )value );
					 break;
				 case DataTypes.BINARY :
					 if (value==null)
						 ps.setNull(counter, Types.BINARY);
					 else
						 ps.setBytes( counter , ( byte [ ] ) value );
					 break;
				 default :
					 logger.error( "The type conversion is not supported for : " + streamElement.getFieldNames( )[ counter - 1 ] + "(" + streamElement.getFieldTypes( )[ counter - 1 ] + ")" );
				 }
			 }
			 ps.setLong( counter , streamElement.getTimeStamp( ) );
			 ps.execute();
		 } catch ( GSNRuntimeException e ) {
			 if ( e.getType( ) == GSNRuntimeException.UNEXPECTED_VIRTUAL_SENSOR_REMOVAL ) {
				 if ( logger.isDebugEnabled() ) logger.debug( "An stream element dropped due to unexpected virtual sensor removal." , e );
			 } else
				 logger.warn( "Inserting a stream element failed : " + streamElement.toString( ) , e );
		 }finally{close(ps);} 
	 }

	 /**************************************************************************************************
	  *  Statement Generators
	  **************************************************************************************************/
	 /**
	  * Creates a sql statement which can be used for inserting the specified
	  * stream element in to the specified table.
	  * 
	  * @param tableName The table which the generated sql will pointing to.
	  * @param se The stream element for which the sql statement is generated.
	  * @return A sql statement which can be used for inserting the provided
	  * stream element into the specified table.
	  */
	 public static StringBuilder getStatementInsert ( CharSequence tableName ,DataField dataFields[], StreamElement se ) {
		 StringBuilder toReturn = new StringBuilder( "insert into " ).append( tableName ).append( " ( " );
		 int storedFieldCount = 1;
		 nextField:for ( String fieldName : se.getFieldNames( ) ) {
			 for (DataField dataField : dataFields)
				 if (dataField.getName().equalsIgnoreCase(fieldName)) {
					 toReturn.append( fieldName ).append( " ," );
					 storedFieldCount++;
					 continue nextField;
				 } 
			 if ( logger.isDebugEnabled( ) ) {
				 StringBuilder acceptedFields = new StringBuilder( "The field : " ).append( fieldName ).append( " is ignored, accepted fields are : " );
				 for (DataField dataField_Inner : dataFields )
					 acceptedFields.append( dataField_Inner.getName() ).append( ", " );
				 logger.debug( acceptedFields.toString( ) );
			 }
		 }
		 toReturn.append( " timed " ).append( " ) values (" );
		 for (int i=1;i<=storedFieldCount;i++)
			 toReturn.append("?,");
		 toReturn.replace(toReturn.length()-1, toReturn.length(), ")");
		 return toReturn;
	 }

	 public static String getStatementRenameTable ( String oldName , String newName ) {
		 return  new StringBuilder( "alter table " ).append( oldName ).append( " rename to " ).append( newName ).toString();
	 }
	 public static StringBuilder getStatementDropTable(CharSequence tableName) {
		 return new StringBuilder( " " ).append( tableName );
	 }
	 /**
	  * First detects the appropriate DB Engine to use. Get's the drop index statement syntax (which is DB dependent)
	  * and executes it. 
	  * 
	  * @param indexName
	  * @param connection
	  * @return
	  * @throws SQLException
	  */
	 public static StringBuilder getStatementDropIndex(CharSequence indexName,Connection connection) throws SQLException {
		 DATABASE db = getDatabaseForConnection(connection);
		 return new StringBuilder(db.getStatementDropIndex().replace("#NAME", indexName));
	 }

	 public static StringBuilder getStatementDropView(CharSequence viewName,Connection connection) throws SQLException {
		 DATABASE db = getDatabaseForConnection(connection);
		 return new StringBuilder(db.getStatementDropView().replace("#NAME", viewName));
	 }
	 public static StringBuilder getStatementCreateIndexOnTimed ( CharSequence tableName) throws SQLException {
		 return new StringBuilder( "CREATE INDEX " ).append( tableName).append( "_INDEX ON " ).append( tableName).append( " (timed DESC)" );
	 }

	 public static StringBuilder getStatementCreateTable ( CharSequence tableName , DataField[] structure ,Connection connection) throws SQLException {
		 StringBuilder result = new StringBuilder( "CREATE " );
		 if ( isHsql( ) ) result.append( " CACHED " );
		 result.append( "TABLE " ).append( tableName );
		 if ( isHsql( ) ) result.append( " (PK BIGINT NOT NULL IDENTITY, timed BIGINT NOT NULL, " );
		 if ( isMysqlDB( ) ) result.append( " (PK BIGINT PRIMARY KEY NOT NULL AUTO_INCREMENT, timed BIGINT NOT NULL, " );
		 for ( DataField field : structure ) {
			 result.append( field.getName( ).toUpperCase( ) ).append( ' ' );
			 if ( StorageManager.isMysqlDB( ) ) {
				 switch ( field.getDataTypeID( ) ) {
				 case DataTypes.CHAR :
				 case DataTypes.VARCHAR :
					 // Because the parameter for the varchar is not
					 // optional.
					 result.append( field.getType( ) );
					 break;
				 case DataTypes.BINARY :
					 result.append( "BLOB" );
					 break;
				 default :
					 result.append( DataTypes.TYPE_NAMES[ field.getDataTypeID( ) ] );
				 break;
				 }
			 } else if ( StorageManager.isHsql( ) ) {
				 switch ( field.getDataTypeID( ) ) {
				 case DataTypes.CHAR :
				 case DataTypes.VARCHAR :
					 // Because the parameter for the varchar is not
					 // optional.
					 result.append( field.getType( ) );
					 break;
				 default :
					 result.append( DataTypes.TYPE_NAMES[ field.getDataTypeID( ) ] );
				 break;
				 }
			 }
			 result.append( " ," );
		 }
		 result.delete(result.length()-2,result.length());
		 result.append(")");
		 return result;
	 }

	 public static StringBuilder getStatementCreateView(CharSequence viewName , CharSequence selectQuery) {
		 return new StringBuilder( "create view " ).append( viewName ).append( " AS ( " ).append( selectQuery ).append(" ) ");
	 }

	 /**************************************************************************************************
	  *  INITIALIZATION PARAMETERS. SET IN THE FIRST TIME THIS CLASS USED.
	  **************************************************************************************************/
	 private static boolean mysql = false;

	 private static boolean hsql  = false;

	 public static enum DATABASE {

		 MYSQL ( "jdbc:mysql:" ,"com.mysql.jdbc.Driver") {

			 public int getTableNotExistsErrNo() {
				 return 1146;
			 }

			 /*
			  * Returns the MySQL data type that can store this gsn datafield.
			  * @param field The datafield to be converted. @return convertedType
			  * the data type used by Mysql.
			  */
			 public String convertGSNTypeToLocalType ( DataField field ) {
				 String convertedType;
				 switch ( field.getDataTypeID( ) ) {
				 case DataTypes.CHAR :
				 case DataTypes.VARCHAR :
					 // Because the parameter for the varchar is not
					 // optional.
					 convertedType = field.getType( );
					 break;
				 case DataTypes.BINARY :
					 convertedType = "BLOB";
					 break;
				 default :
					 convertedType = DataTypes.TYPE_NAMES[ field.getDataTypeID( ) ];
				 break;
				 }
				 return convertedType;
			 }
			 public String getStatementDropIndex() {
				 return "DROP TABLE IF EXISTS #NAME";
			 }
			 public String getStatementDropView() {
				 return "DROP VIEW IF EXISTS #NAME";
			 }
		 } ,
		 HSQL ( "jdbc:hsql:" ,"org.hsqldb.jdbcDriver") {

			 public int getTableNotExistsErrNo() {
				 return -22;
			 }

			 /*
			  * Returns the HSQLDB data type that can store this gsn datafield.
			  * @param field The datafield to be converted. @return convertedType
			  * the data type used by hsql.
			  */
			 public String convertGSNTypeToLocalType ( DataField field ) {
				 String convertedType = null;
				 switch ( field.getDataTypeID( ) ) {
				 case DataTypes.CHAR :
				 case DataTypes.VARCHAR :
					 // Because the parameter for the varchar is not
					 // optional.
					 convertedType = field.getType( );
					 break;
				 default :
					 convertedType = DataTypes.TYPE_NAMES[ field.getDataTypeID( ) ];
				 break;
				 }
				 return convertedType;
			 }
			 public String getStatementDropIndex() {
				 return "DROP INDEX #NAME IF EXISTS";
			 }
			 public String getStatementDropView() {
				 return "DROP VIEW #NAME IF EXISTS";
			 }
		 };

		 private final String jdbcPrefix;

		 private final String driver;

		 DATABASE ( String jdbcPrefix, String driver ) {
			 this.jdbcPrefix = jdbcPrefix;
			 this.driver=driver;
			 try {
				 Class.forName(driver);
			 } catch (ClassNotFoundException e) {
				 logger.error("Error in loading the database driver. !");
				 logger.error(e.getMessage(),e);
			 }
		 }

		 /**
		  * The prefix is in lower case
		  * @return
		  */
		 public String getJDBCPrefix ( ) {
			 return jdbcPrefix;
		 }

		 public String getJDBCDriverClass() {
			 return driver;
		 }

		 /*
		  * Converts from internal GSN data types to a supported DB data type.
		  * @param field The DataField to be converted @return convertedType The
		  * datatype name used by the target database.
		  */

		 public abstract String convertGSNTypeToLocalType ( DataField field );
		 public abstract String getStatementDropIndex();
		 public abstract String getStatementDropView();
		 public abstract int getTableNotExistsErrNo();
	 };

	 /**
	  * Determinse if the database used is MySql.
	  * 
	  * @return Returns true if the database used is mysql.
	  */
	 public static boolean isMysqlDB ( ) {
		 return mysql;
	 }

	 /**
	  * Determining if the database used is HSqlDB.
	  * 
	  * @return true if the database used is HSqlDB.
	  */
	 public static boolean isHsql ( ) {
		 return hsql;
	 }

	 private static StorageManager                  singleton                   = new StorageManager( );

	 private static final transient Logger          logger                        = Logger.getLogger( StorageManager.class );

	 public static final int               DEFAULT_STORAGE_POOL_SIZE        = 100;

	 private static int MAX_DB_CONNECTIONS;

	 private String databaseURL;

	 private Properties                                    dbConnectionProperties = new Properties( );

	 private Connection connection= null;

	 public static StorageManager getInstance ( ) {
		 return singleton;
	 }

	 private StorageManager ( ) {}


	 public void initialize ( String databaseDriver , String username , String password , String databaseURL ) {
		 this.databaseURL = databaseURL;
		 if ( databaseDriver.trim( ).equalsIgnoreCase( DATABASE.HSQL.getJDBCDriverClass() ) )
			 hsql = true;
		 else if ( databaseDriver.trim( ).equalsIgnoreCase( DATABASE.MYSQL.getJDBCDriverClass() ) )
			 mysql = true;
		 else {
			 logger.error( new StringBuilder( ).append( "The GSN doesn't support the database driver : " ).append( databaseDriver ).toString( ) );
			 logger.error( new StringBuilder( ).append( "Please check the storage element in the file : " ).append( getContainerConfig( ).getContainerFileName( ) ).toString( ) );
			 System.exit( 1 );
		 }
		 if (Main.getContainerConfig()==null || Main.getContainerConfig().getStoragePoolSize()<=0)
			 MAX_DB_CONNECTIONS=DEFAULT_STORAGE_POOL_SIZE;
		 else
			 MAX_DB_CONNECTIONS = Main.getContainerConfig( ).getStoragePoolSize( );

		 dbConnectionProperties.put( "user" , username );
		 dbConnectionProperties.put( "username" , username );
		 dbConnectionProperties.put( "password" , password );
		 dbConnectionProperties.put( "ignorecase" , "true" );
		 dbConnectionProperties.put( "autocommit" , "true" );
		 dbConnectionProperties.put( "useUnicode" , "true" );
		 dbConnectionProperties.put( "autoReconnect" , "true" );
		 dbConnectionProperties.put( "autoReconnectForPools" , "true" );
		 dbConnectionProperties.put( "cacheCallableStmts" , "true" );
		 dbConnectionProperties.put( "cachePrepStmts" , "true" );
		 dbConnectionProperties.put( "cacheResultSetMetadata" , "true" );
		 dbConnectionProperties.put( "cacheResultSetMetadata" , "true" );
		 dbConnectionProperties.put( "defaultFetchSize" , "5" );
		 dbConnectionProperties.put( "useLocalSessionState" , "true" );
		 dbConnectionProperties.put( "characterEncoding" , "sjis" );
		 dbConnectionProperties.put( "useLocalSessionState" , "true" );
		 dbConnectionProperties.put( "useServerPrepStmts" , "false" );
		 dbConnectionProperties.put( "prepStmtCacheSize" , "512" );		

		 logger.info( "Initializing the access to the database server ..." );

		 Connection con ;
		 Statement stmt = null ;
		 try {
			 con =  getConnection();
			 stmt= con.createStatement();
			 if ( StorageManager.isHsql( ) ) {
				 stmt.execute( "SET REFERENTIAL_INTEGRITY FALSE" );
				 stmt.execute( "CREATE ALIAS NOW_MILLIS FOR \"java.lang.System.currentTimeMillis\";" );
			 } else if ( StorageManager.isMysqlDB( ) ) {
				 ResultSet rs = stmt.executeQuery( "select version();" );
				 rs.next( );
				 String versionInfo = rs.getString( 1 );
				 if ( !versionInfo.trim( ).startsWith( "5." ) ) {
					 logger.error( new StringBuilder( ).append( "You are using MySQL version : " ).append( versionInfo ).toString( ) );
					 logger.error( "To run GSN using MySQL, you need version 5.0 or later." );
					 System.exit( 1 );
				 }
			 }else {
				 logger.error( "Unknow database server provided. GSN runs only under MySQL or HSqlDB" );
				 System.exit( 1 );
			 }
		 } catch ( SQLException e ) {
			 logger.error( new StringBuilder( ).append( "Connecting to the database with the following properties failed :" ).append( "\n\t UserName :" ).append( username ).append( "\n\t Password : " )
					 .append( password ).append( "\n\t Driver class : " ).append( databaseDriver ).append( "\n\t Database URL : " ).append( databaseURL ).toString( ) );
			 logger.error( new StringBuilder( ).append( e.getMessage( ) ).append( ", Please refer to the logs for more detailed information." ).toString( ) );
			 logger.error( "Make sure in the : " + getContainerConfig( ).getContainerFileName( ) + " file, the <storage ...> element is correct." );
			 e.printStackTrace( );
			 if ( logger.isInfoEnabled( ) ) logger.info( e.getMessage( ) , e );
			 System.exit( 1 );
		 }finally {
			 closeStatement(stmt);
		 }
	 }

	 public Connection getConnection() throws SQLException {
		 if (this.connection==null || this.connection.isClosed())
			 this.connection=DriverManager.getConnection(databaseURL,dbConnectionProperties);
		 return connection;
	 }
}
