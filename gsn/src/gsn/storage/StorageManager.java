package gsn.storage;

import static gsn.Main.getContainerConfig;
import gsn.Main;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.http.datarequest.AbstractQuery;
import gsn.utils.GSNRuntimeException;
import gsn.utils.ValidityTools;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;

public class StorageManager {

	/***************************************************************************
	 * Various HELPER METHODS.
	 **************************************************************************/
	/**
	 * Given a connection, gets the appropriate DATABASE object from the
	 * DATABASE Enum.
	 */
	public static DATABASE getDatabaseForConnection(Connection connection)
	throws SQLException {
		String name = connection.getMetaData().getDatabaseProductName();
		if (name.toLowerCase().indexOf("h2") >= 0)
			return DATABASE.H2;
		else if (name.toLowerCase().indexOf("mysql") >= 0)
			return DATABASE.MYSQL;
		else if (name.toLowerCase().indexOf("oracle") >= 0)
			return DATABASE.ORACLE;
		else if (name.toLowerCase().indexOf("sql server") >= 0 || name.toLowerCase().indexOf("sqlserver") > 0)
			return DATABASE.SQLSERVER;
		else {
			return null;
		}
	}

	/**
	 * Returns false if the table doesnt exist. Uses the current default
	 * connection.
	 *
	 * @param tableName
	 * @return False if the table doesn't exist in the current connection.
	 * @throws SQLException
	 */
	public boolean tableExists(CharSequence tableName) throws SQLException {
		Connection connection = null;
		try{
			connection = getConnection();
			return tableExists(tableName, new DataField[] {}, connection);
		}finally{
			close(connection);
		}
	}

	/**
	 * Checks to see if the given tablename exists using the given connection.
	 *
	 * @param tableName
	 * @param connection
	 * @return
	 * @throws SQLException
	 */
	public static boolean tableExists(CharSequence tableName,
			Connection connection) throws SQLException {
		return tableExists(tableName, new DataField[] {}, connection);
	}


	public static DataField[] tableToStructure(CharSequence tableName,Connection connection) throws SQLException {
		StringBuilder sb = new StringBuilder("select * from ").append(tableName).append(" where 1=0 ");
		ResultSet rs = null;
		DataField[] toReturn = null;
		try {
			rs = executeQueryWithResultSet(sb, connection);
			ResultSetMetaData structure = rs.getMetaData();
			ArrayList<DataField> toReturnArr = new ArrayList<DataField>();
			for (int i=1;i<=structure.getColumnCount();i++) {
				String colName = structure.getColumnLabel(i);
				if (colName.equalsIgnoreCase("pk")) continue;
				int colType = structure.getColumnType(i);
				byte colTypeInGSN= getDatabaseForConnection(connection).convertLocalTypeToGSN(colType);
				toReturnArr.add(new DataField(colName,colTypeInGSN));
			}
			toReturn = toReturnArr.toArray(new DataField[] {});
		}finally {
			if (rs!=null)
				close(rs);
		}
		return toReturn;
	}

	/**
	 * Returns false if the table doesnt exist. If the table exists but the
	 * structure is not compatible with the specified fields the method throws
	 * GSNRuntimeException. Note that this method doesn't close the connection
	 *
	 * @param tableName
	 * @param connection
	 *            (this method will not close it and the caller is responsible
	 *            for closing the connection)
	 * @return
	 * @throws SQLException
	 * @Throws GSNRuntimeException
	 */

	public static boolean tableExists(CharSequence tableName, DataField[] fields, Connection connection) throws SQLException,
	GSNRuntimeException {
		if (!ValidityTools.isValidJavaVariable(tableName))
			throw new GSNRuntimeException("Table name is not valid");
		StringBuilder sb = new StringBuilder("select * from ").append(Main.tableNameGeneratorInString(tableName)).append(" where 1=0 ");
		ResultSet rs = null;
		try {
			rs = executeQueryWithResultSet(sb, connection);
			ResultSetMetaData structure = rs.getMetaData();
			if (fields != null && fields.length > 0)
				nextField: for (DataField field : fields) {
					for (int i = 1; i <= structure.getColumnCount(); i++) {
						String colName = structure.getColumnLabel(i);
						int colType = structure.getColumnType(i);
						int colTypeScale = structure.getScale(i);
						if (field.getName().equalsIgnoreCase(colName))
							if (field.getDataTypeID() == getDatabaseForConnection(connection).convertLocalTypeToGSN(colType,colTypeScale))
								continue nextField;
							else
								throw new GSNRuntimeException("The column : "
										+ colName + " in the >" + tableName
										+ "< table is not compatible with type : "
										+ field.getType()
										+ ". The actual type for this table (currently in the database): " + colType);
					}
					throw new GSNRuntimeException("The table " + tableName
							+ " in the database, doesn't have the >" + field.getName()
							+ "< column.");
				}
		} catch (SQLException e) {
			DATABASE database = getDatabaseForConnection(connection);
			if (e.getErrorCode() == database.getTableNotExistsErrNo() || e.getMessage().contains("does not exist"))
				return false;
			else {
				logger.error(e.getErrorCode());
				throw e;
			}
		} finally {
			close(rs);
		}
		return true;
	}

	public boolean tableExists(CharSequence tableName, DataField[] fields)
	throws SQLException {
		Connection conn = null;
		boolean to_return = true;
		try{
			conn =  getConnection();
			to_return=  tableExists(tableName, fields, conn);
		}finally {
			close(conn);
		}
		return to_return;
	}

	/**
	 * Returns true if the specified query has any result in it's result set.
	 * The created result set will be closed automatically.
	 *
	 * @param sqlQuery
	 * @return
	 */
	public boolean isThereAnyResult(StringBuilder sqlQuery) {
		boolean toreturn = false;
		Connection connection = null;
		try {
			connection = getConnection();
			PreparedStatement prepareStatement= connection.prepareStatement(
					sqlQuery.toString());
			ResultSet resultSet = prepareStatement.executeQuery();
			toreturn = resultSet.next();
		} catch (SQLException error) {
			logger.error(error.getMessage(), error);
		} finally {
			close(connection);
		}
		return toreturn;
	}

	/**
	 * Executes the query of the database. Returns the specified colIndex of the
	 * first row. Useful for image recovery of the web interface.
	 *
	 * @param query
	 *            The query to be executed.
	 * @return A resultset with only one row and one column. The user of the
	 *         method should first call next on the result set to make sure that
	 *         the row is there and then retrieve the value for the row.
	 *
	 * @throws SQLException
	 */

	public static ResultSet getBinaryFieldByQuery(StringBuilder query,
			String colName, Long pk, Connection connection) throws SQLException {
		PreparedStatement ps = connection.prepareStatement(query.toString());
		if (pk != null)
			ps.setLong(1, pk);
		return ps.executeQuery();
	}

	public static void closeStatement(Statement stmt) {
		try {
			if (stmt != null){
				stmt.close();
			}
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static void close(ResultSet resultSet) {
		try {
			if (resultSet != null){
				resultSet.getStatement().close();
			}
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static void close(PreparedStatement preparedStatement) {
		try {
			if (preparedStatement != null){
				preparedStatement.close();
			}
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static void close(Connection conn) {
		try{
			if (conn!=null & !conn.isClosed()){
				conn.close();
				connectionCount--;
			}
		}catch (SQLException e){
			logger.debug(e.getMessage(),e);
		}
	}

	/**
	 * This method only works with HSQLDB database. If one doesn't close the
	 * HSQLDB properly with high probability, the DB goes into instable or in
	 * worst case becomes corrupted.
	 *
	 * @throws SQLException
	 */
	public void shutdown() throws SQLException {
		if (StorageManager.isH2()) {
			getConnection().createStatement().execute("SHUTDOWN");
			logger.warn("Closing the database server (for HSqlDB) [done].");
		}
		logger.warn("Closing the connection pool [done].");
	}

	/***************************************************************************
	 * Various Statement Executors.
	 **************************************************************************/

	public void executeRenameTable(String oldName, String newName)
	throws SQLException {
		Connection conn =null;
		try {
			conn=getConnection();
			executeRenameTable(oldName, newName, conn);
		}finally{
			close(conn);
		}

	}

	public void executeRenameTable(String oldName, String newName, Connection connection) throws SQLException {
		PreparedStatement prepareStatement = null;
		try{
			prepareStatement = connection.prepareStatement(getStatementRenameTable(oldName, newName));
			prepareStatement.execute();
		}finally{
			close(prepareStatement);
		}

	}

	public void executeDropTable(CharSequence tableName) throws SQLException {
		Connection conn =null;
		try {
			conn =  getConnection();
			executeDropTable(tableName,conn );
		}finally{
			close(conn);
		}
	}

	public void executeDropTable(CharSequence tableName, Connection connection) {
		PreparedStatement prepareStatement = null;
		try{
			//String	stmt = getStatementDropIndex(Main.tableNamePostFixAppender(tableName, "_INDEX"), tableName,connection).toString();
			//if (logger.isDebugEnabled())
			//	logger.debug("Dropping table index on " + Main.tableNameGeneratorInString(tableName)+ " With query: "+stmt);
			//prepareStatement = connection.prepareStatement(stmt);
			//prepareStatement.execute();
			//close(prepareStatement);
			String stmt = getStatementDropTable(tableName,connection).toString();
			if (logger.isDebugEnabled())
				logger.debug("Dropping table structure: " + tableName+ " With query: "+stmt);
			prepareStatement= connection.prepareStatement(stmt);
			prepareStatement.execute();
			if (isOracle()) {
				executeCommand("drop sequence "+Main.tableNamePostFixAppender(tableName,"_SEQ"),connection);
				executeCommand("drop trigger "+Main.tableNamePostFixAppender(tableName,"_TRIG"),connection);
			}
		}catch (SQLException e) {
			logger.info(e.getMessage(),e);
		}
	}

	public void executeDropView(StringBuilder tableName) throws SQLException {
		Connection conn = null;
		try {
			conn = getConnection();
			executeDropView(tableName, conn);
		}finally{
			close(conn);
		}
	}

	public void executeDropView(StringBuilder tableName, Connection connection)	 throws SQLException {
		if (logger.isDebugEnabled())
			logger.debug("Dropping table structure: " + tableName);
		PreparedStatement prepareStatement = connection.prepareStatement(getStatementDropView(tableName, connection).toString());
		prepareStatement.execute();
		close(prepareStatement);
	}

	public void executeCreateTable(CharSequence tableName, DataField[] structure,boolean unique) throws SQLException {
		Connection conn = null;
		try{
			conn=getConnection();
			executeCreateTable(tableName, structure,unique, conn);
		}finally {
			close(conn);
		}
	}

	/**
	 * Create a table with a index on the timed field.
	 *
	 * @param tableName
	 * @param structure
	 * @param unique , setting this true cause the system to create a unique index on time.
	 * @param connection
	 * @throws SQLException
	 */
	public static void executeCreateTable(CharSequence tableName, DataField[] structure,boolean unique, Connection connection) throws SQLException {
		StringBuilder sql = getStatementCreateTable(tableName, structure,connection);
		if (logger.isDebugEnabled())
			logger.debug(new StringBuilder().append("The create table statement is : ").append(sql).toString());

		PreparedStatement prepareStatement = connection.prepareStatement(sql.toString());
		prepareStatement.execute();
		prepareStatement.close();

		if (isOracle()) { // need to make a sequence and trigger.
			String oracleSeq = "create sequence "+Main.tableNamePostFixAppender(tableName,"_SEQ");
			String oracleTrigger = "create or replace trigger "+Main.tableNamePostFixAppender(tableName, "_TRIG")+" before insert on "+tableName+" for each row begin select "+Main.tableNamePostFixAppender(tableName,"_SEQ")+".nextval into :NEW.pk from dual; end;";
			logger.debug(oracleSeq);
			logger.debug(oracleTrigger);
			executeCommand(oracleSeq,connection);
			executeCommand(oracleTrigger, connection);

		}

		sql = getStatementCreateIndexOnTimed(tableName,unique);
		if (logger.isDebugEnabled())
			logger.debug(new StringBuilder().append(
			"The create index statement is : ").append(sql).toString());
		prepareStatement = connection.prepareStatement(sql.toString());
		prepareStatement.execute();

	}

	public static ResultSet executeQueryWithResultSet(StringBuilder query,
			Connection connection) throws SQLException {
		return connection.prepareStatement(query.toString()).executeQuery();
	}

	public ResultSet executeQueryWithResultSet(AbstractQuery abstractQuery,Connection c) throws SQLException {
		if (abstractQuery.getLimitCriterion() == null) {
			return executeQueryWithResultSet(abstractQuery.getStandardQuery(), c);
		}
		DATABASE db = getDatabaseForConnection(c);
		String query = db.addLimit(abstractQuery.getStandardQuery().toString(), abstractQuery.getLimitCriterion().getSize(), abstractQuery.getLimitCriterion().getOffset());
		return executeQueryWithResultSet(new StringBuilder(query),c);
	}

	public static DataEnumerator executeQuery(StringBuilder query,boolean binaryFieldsLinked, Connection connection) throws SQLException {
		if (logger.isDebugEnabled())
			logger.debug("Executing query: " + query + "( Binary Field Linked:" + binaryFieldsLinked	+ ")");
		return new DataEnumerator(connection.prepareStatement(query.toString()),binaryFieldsLinked);
	}

	/**
	 * Attention: Caller should close the connection.
	 * @param abstractQuery
	 * @param binaryFieldsLinked
	 * @param connection
	 * @return
	 * @throws SQLException
	 */
	public static DataEnumerator executeQuery(AbstractQuery abstractQuery,boolean binaryFieldsLinked, Connection connection) throws SQLException {
		if (abstractQuery.getLimitCriterion() == null) {
			return executeQuery(abstractQuery.getStandardQuery(), binaryFieldsLinked, connection);
		}
		DATABASE db = getDatabaseForConnection(connection);
		String query = db.addLimit(abstractQuery.getStandardQuery().toString(), abstractQuery.getLimitCriterion().getSize(), abstractQuery.getLimitCriterion().getOffset());
		if (logger.isDebugEnabled())
			logger.debug("Executing query: " + query + "(" + binaryFieldsLinked	+ ")");
		return new DataEnumerator(connection.prepareStatement(query.toString()),binaryFieldsLinked);
	}

	public DataEnumerator executeQuery(StringBuilder query,boolean binaryFieldsLinked) throws SQLException {
		return executeQuery(query, binaryFieldsLinked, getConnection());
	}

	public void executeCreateView(CharSequence viewName, CharSequence selectQuery) throws SQLException {
		Connection connection = null;
		try{
			connection= getConnection();
			executeCreateView(viewName, selectQuery, connection);
		}finally{
			close(connection);
		}
	}

	public void executeCreateView(CharSequence viewName,CharSequence selectQuery, Connection connection) throws SQLException {
		StringBuilder statement = getStatementCreateView(viewName, selectQuery);
		if (logger.isDebugEnabled())
			logger.debug("Creating a view:" + statement);
		final PreparedStatement prepareStatement = connection.prepareStatement(statement.toString());
		prepareStatement.execute();
		close(prepareStatement);
	}

	/**
	 * This method executes the provided statement over the connection. If there
	 * is an error retruns -1 otherwise it returns the output of the
	 * executeUpdate method on the PreparedStatement class which reflects the
	 * number of changed rows in the underlying table.
	 *
	 * @param sql
	 * @param connection
	 * @return Number of effected rows or -1 if there is an error.
	 */

	public static void executeCommand(String sql, Connection connection) {
		Statement stmt = null;
		try {
			stmt = connection.createStatement();
			stmt.execute(sql);
		}catch (SQLException error) {
			if (isOracle() && (
					(sql.toLowerCase().contains("drop trigger") && error.getMessage().contains("does not exist")) ||
					(sql.toLowerCase().contains("create sequence") && error.getMessage().contains("name is already used"))
			)
			)
				// ignore it for oracle
				;
			else
				logger.error(error.getMessage()+" FOR: "+sql, error);
		}finally {
			try {
				if (stmt !=null && !stmt.isClosed())
					stmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	public static int executeUpdate(String updateStatement, Connection connection) {
		int toReturn = -1;
		PreparedStatement prepareStatement = null;
		try {
			prepareStatement = connection.prepareStatement(updateStatement);
			toReturn = prepareStatement.executeUpdate();
		} catch (SQLException error) {
			logger.error(error.getMessage(), error);
		}
		return toReturn;
	}
	public static int executeUpdate(StringBuilder updateStatement, Connection connection) {
		int to_return = -1;
		to_return = executeUpdate(updateStatement.toString(), connection);
		return to_return;
	}

	public int executeUpdate(StringBuilder updateStatement) throws SQLException {
		Connection connection = null;
		try{
			connection=getConnection();
			return executeUpdate(updateStatement, connection);
		}finally{
			close(connection);
		}
	}

	public void executeInsert(CharSequence tableName, DataField[] fields,StreamElement se) throws SQLException {
		Connection connection =null;
		try{
			connection= getConnection();
			executeInsert(tableName, fields, se, connection);
		}finally{
			close(connection);
		}
	}

	public static void executeInsert(CharSequence tableName,DataField[] fields, StreamElement streamElement,Connection connection) throws SQLException {
		PreparedStatement ps = null;
		String query = getStatementInsert(tableName, fields).toString();
		try {
			ps = connection.prepareStatement(query);
			int counter=1;
			for (DataField dataField : fields) {
				if (dataField.getName().equalsIgnoreCase("timed"))
					continue;
				Serializable value = streamElement.getData(dataField.getName());
				
				switch (dataField.getDataTypeID()) {
				case DataTypes.VARCHAR:
					if (value == null)
						ps.setNull(counter, Types.VARCHAR);
					else
						ps.setString(counter, value.toString());
					break;
				case DataTypes.CHAR:
					if (value == null)
						ps.setNull(counter, Types.CHAR);
					else
						ps.setString(counter, value.toString());
					break;
				case DataTypes.INTEGER:
					if (value == null)
						ps.setNull(counter, Types.INTEGER);
					else
						ps.setInt(counter, ((Number) value).intValue());
					break;
				case DataTypes.SMALLINT:
					if (value == null)
						ps.setNull(counter, Types.SMALLINT);
					else
						ps.setShort(counter, ((Number) value).shortValue());
					break;
				case DataTypes.TINYINT:
					if (value == null)
						ps.setNull(counter, Types.TINYINT);
					else
						ps.setByte(counter, ((Number) value).byteValue());
					break;
				case DataTypes.DOUBLE:
					if (value == null)
						ps.setNull(counter, Types.DOUBLE);
					else
						ps.setDouble(counter, ((Number) value).doubleValue());
					break;
				case DataTypes.BIGINT:
					if (value == null)
						ps.setNull(counter, Types.BIGINT);
					else
						ps.setLong(counter, ((Number) value).longValue());
					break;
				case DataTypes.BINARY:
					if (value == null)
						ps.setNull(counter, Types.BINARY);
					else
						ps.setBytes(counter, (byte[]) value);
					break;
				default:
					logger.error("The type conversion is not supported for : "
							+ streamElement.getFieldNames()[counter - 1] + "("
							+ streamElement.getFieldTypes()[counter - 1] + ")");
				}
				counter++;
			}
			ps.setLong(counter, streamElement.getTimeStamp());
			ps.execute();
		} catch (GSNRuntimeException e) {
			if (e.getType() == GSNRuntimeException.UNEXPECTED_VIRTUAL_SENSOR_REMOVAL) {
				if (logger.isDebugEnabled())
					logger.debug("An stream element dropped due to unexpected virtual sensor removal. (Stream element: "+streamElement.toString()+")+ Query: "+query,	e);
			} else
				logger.warn("Inserting a stream element failed : "
						+ streamElement.toString(), e);
		}catch (SQLException e) {
			if (e.getMessage().toLowerCase().contains("duplicate entry"))
				logger.info("Error occurred on inserting data to the database, an stream element dropped due to: "+e.getMessage()+". (Stream element: "+streamElement.toString()+")+ Query: "+query);
			else
				logger.warn("Error occurred on inserting data to the database, an stream element dropped due to: "+e.getMessage()+". (Stream element: "+streamElement.toString()+")+ Query: "+query);
			throw e;
		}
		finally {
			close(ps);
		}
	}

	/***************************************************************************
	 * Statement Generators
	 **************************************************************************/
	/**
	 * Creates a sql statement which can be used for inserting the specified
	 * stream element in to the specified table.
	 *
	 * @param tableName
	 *            The table which the generated sql will pointing to.
	 * @param fields
	 *            The stream element for which the sql statement is generated.
	 * @return A sql statement which can be used for inserting the provided
	 *         stream element into the specified table.
	 */
	public static StringBuilder getStatementInsert(CharSequence tableName, DataField fields[]) {
		StringBuilder toReturn = new StringBuilder("insert into ").append( tableName).append(" ( ");
		int numberOfQuestionMarks = 1; //Timed is always there.
		for (DataField dataField : fields) {
			if (dataField.getName().equalsIgnoreCase("timed"))
				continue;
			numberOfQuestionMarks++;
			toReturn.append(dataField.getName()).append(" ,");
		}
		toReturn.append(" timed ").append(" ) values (");
		for (int i = 1; i <= numberOfQuestionMarks; i++)
			toReturn.append("?,");
		toReturn.deleteCharAt(toReturn.length()-1);
		toReturn.append(")");
		return toReturn;
	}

	public static String getStatementRenameTable(String oldName, String newName) {
		return new StringBuilder("alter table ").append(oldName).append(
		" rename to ").append(newName).toString();
	}

	public static StringBuilder getStatementDropTable(CharSequence tableName,Connection conn) throws SQLException {
		StringBuilder sb = new StringBuilder("Drop table ");
		DATABASE db = getDatabaseForConnection(conn);
		if (db.getDBType()==MYSQL_DB || db.getDBType()== H2_DB)
			sb.append(" if exists " );
		sb.append(tableName);
		return sb;
	}

	/**
	 * First detects the appropriate DB Engine to use. Get's the drop index
	 * statement syntax (which is DB dependent) and executes it.
	 *
	 * @param indexName
	 * @param connection
	 * @return
	 * @throws SQLException
	 */
	public static StringBuilder getStatementDropIndex(CharSequence indexName, CharSequence tableName,
			Connection connection) throws SQLException {
		DATABASE db = getDatabaseForConnection(connection);
		return new StringBuilder(db.getStatementDropIndex().replace("#NAME",
				indexName).replace("#TABLE",tableName ));
	}

	public static StringBuilder getStatementDropView(CharSequence viewName,
			Connection connection) throws SQLException {
		DATABASE db = getDatabaseForConnection(connection);
		return new StringBuilder(db.getStatementDropView().replace("#NAME",
				viewName));
	}

	public static StringBuilder getStatementCreateIndexOnTimed(
			CharSequence tableName,boolean unique) throws SQLException {
		StringBuilder toReturn = new StringBuilder("CREATE ");
		if (unique)
			toReturn.append(" UNIQUE ");
		toReturn.append(" INDEX ").append(Main.tableNamePostFixAppender(tableName, "_INDEX")).append(" ON ").append(tableName).append(" (timed DESC)");
		return toReturn;
	}

	public static StringBuilder getStatementCreateTable(CharSequence tableName,
			DataField[] structure, Connection connection) throws SQLException {
		StringBuilder result = new StringBuilder("CREATE TABLE ").append(tableName);
		DATABASE db = getDatabaseForConnection(connection);
		if (db.getDBType()== SQLSERVER_DB || db.getDBType()==H2_DB)
			result.append(" (PK BIGINT NOT NULL IDENTITY, timed BIGINT NOT NULL, ");
		else if (db.getDBType()==MYSQL_DB)
			result.append(" (PK BIGINT PRIMARY KEY NOT NULL AUTO_INCREMENT, timed BIGINT NOT NULL, ");
		else if (db.getDBType() == ORACLE_DB)
			result.append(" (PK number(38) PRIMARY KEY, timed number(38) NOT NULL, ");

		for (DataField field : structure) {
			if (field.getName().equalsIgnoreCase("pk")||field.getName().equalsIgnoreCase("timed")) continue;
			result.append(field.getName().toUpperCase()).append(' ');
			result.append(db.convertGSNTypeToLocalType(field));
			result.append(" ,");
		}
		result.delete(result.length() - 2, result.length());
		result.append(")");
		return result;
	}

	public static boolean isSqlServer() {
		return sqlserver;
	}

	public static StringBuilder getStatementCreateView(CharSequence viewName,
			CharSequence selectQuery) {
		return new StringBuilder("create view ").append(viewName).append(
		" AS ( ").append(selectQuery).append(" ) ");
	}

	/***************************************************************************
	 * INITIALIZATION PARAMETERS. SET IN THE FIRST TIME THIS CLASS USED.
	 **************************************************************************/
	private static boolean mysql = false;
	private static boolean h2 = false;
	private static boolean oracle = false;

	public static final int MYSQL_DB=1;
	public static final int SQLSERVER_DB=2;
	public static final int H2_DB=4;
	public static final int ORACLE_DB=5;


	public static enum DATABASE {
		MYSQL("jdbc:mysql:", "com.mysql.jdbc.Driver") {

			public int getTableNotExistsErrNo() {
				return 1146;
			}

			/*
			 * Returns the MySQL data type that can store this gsn datafield.
			 * @param field The datafield to be converted. @return convertedType
			 * the data type used by Mysql.
			 */
			public String convertGSNTypeToLocalType(DataField field) {
				String convertedType;
				switch (field.getDataTypeID()) {
				case DataTypes.CHAR:
				case DataTypes.VARCHAR:
					// Because the parameter for the varchar is not
					// optional.
					if(field.getType().trim().equalsIgnoreCase("string"))
						convertedType="TEXT";
					else
						convertedType = field.getType();
					break;
				case DataTypes.BINARY:
					convertedType = "LONGBLOB";
					break;
				case DataTypes.DOUBLE:
					convertedType = "double precision";
					break;
				default:
					convertedType = DataTypes.TYPE_NAMES[field.getDataTypeID()];
				break;
				}
				return convertedType;
			}

			public String getStatementDropIndex() {
				if (isSqlServer())
					return "DROP TABLE #NAME";
				else
					return "DROP TABLE IF EXISTS #NAME";
			}

			public String getStatementDropView() {
				if (isSqlServer())
					return "DROP VIEW #NAME";
				else
					return "DROP VIEW IF EXISTS #NAME";
			}
			public  int getDBType() {return MYSQL_DB;}
			public String addLimit(String query,int limit,int offset) {
				return query+" LIMIT "+limit+" OFFSET "+offset;
			}

			public byte convertLocalTypeToGSN(int colTypeInJDBCFormat,int colPrecision) {
				switch ( colTypeInJDBCFormat ) {
				case Types.BIGINT :
					return DataTypes.BIGINT;
				case Types.INTEGER :
					return DataTypes.INTEGER;
				case Types.SMALLINT :
					return DataTypes.SMALLINT;
				case Types.TINYINT :
					return DataTypes.TINYINT;
				case Types.VARCHAR :
					return DataTypes.VARCHAR;
				case Types.CHAR :
					return DataTypes.CHAR;
				case Types.DOUBLE :
				case Types.DECIMAL:	// This is needed for doing aggregates in datadownload servlet.
					return DataTypes.DOUBLE;
				case Types.BINARY :
				case Types.BLOB :
				case Types.VARBINARY:
				case Types.LONGVARBINARY :
					return DataTypes.BINARY;
				default :
					logger.error( "The type can't be converted to GSN form : " + colTypeInJDBCFormat );
				break;
				}
				return -100;
			}

		},
		H2("jdbc:h2:", "org.h2.Driver") {
			public int getTableNotExistsErrNo() {
				return 42102;
			}

			/*
			 * Returns the HSQLDB data type that can store this gsn datafield.
			 * @param field The datafield to be converted. @return convertedType
			 * the data type used by hsql.
			 */
			public String convertGSNTypeToLocalType(DataField field) {
				String convertedType = null;
				switch (field.getDataTypeID()) {
				case DataTypes.CHAR:
				case DataTypes.VARCHAR:
					// Because the parameter for the varchar is not
					// optional.
					convertedType = field.getType();
					break;
				default:
					convertedType = DataTypes.TYPE_NAMES[field.getDataTypeID()];
				break;
				}
				return convertedType;
			}

			public String getStatementDropIndex() {
				if (StorageManager.isH2()||StorageManager.isSqlServer())
					return "DROP INDEX #NAME";
				if (StorageManager.isMysqlDB())
					return "DROP INDEX #NAME IF EXISTS";
				return null;
			}

			public String getStatementDropView() {
				return "DROP VIEW #NAME IF EXISTS";
			}
			public  int getDBType() {return H2_DB;}

			public String addLimit(String query,int limit,int offset) {
				return query+" LIMIT "+limit+" OFFSET "+offset;
			}

			public byte convertLocalTypeToGSN(int colTypeInJDBCFormat,int colScale) {
				switch ( colTypeInJDBCFormat ) {
				case Types.BIGINT :
					return DataTypes.BIGINT;
				case Types.INTEGER :
					return DataTypes.INTEGER;
				case Types.SMALLINT :
					return DataTypes.SMALLINT;
				case Types.TINYINT :
					return DataTypes.TINYINT;
				case Types.VARCHAR :
					return DataTypes.VARCHAR;
				case Types.CHAR :
					return DataTypes.CHAR;
				case Types.DOUBLE :
				case Types.DECIMAL:	// This is needed for doing aggregates in datadownload servlet.
					return DataTypes.DOUBLE;
				case Types.BINARY :
				case Types.BLOB :
				case Types.VARBINARY:
				case Types.LONGVARBINARY :
					return DataTypes.BINARY;
				default :
					logger.error( "The type can't be converted to GSN form : " + colTypeInJDBCFormat );
				break;
				}
				return -100;
			}
		},
		ORACLE("jdbc:oracle:thin:", "oracle.jdbc.driver.OracleDriver") {
			public int getTableNotExistsErrNo() {
				return 208; //java.sql.SQLException: Invalid object name
			}

			/*
			 * Returns the HSQLDB data type that can store this gsn datafield.
			 * @param field The datafield to be converted. @return convertedType
			 * the data type used by hsql.
			 */
			public String convertGSNTypeToLocalType(DataField field) {
				String convertedType = null;
				switch (field.getDataTypeID()) {
				case DataTypes.BIGINT:
				case DataTypes.SMALLINT:
				case DataTypes.INTEGER:
				case DataTypes.TINYINT:
					convertedType="number(38,0)";
					break;
				case DataTypes.DOUBLE:
					convertedType="number(38,16)";
					break;
				case DataTypes.CHAR:
				case DataTypes.VARCHAR:
					// Because the parameter for the varchar is not
					// optional.
					convertedType = field.getType();
					convertedType = convertedType.toLowerCase().replace("varchar", "varchar2");
					break;
				case DataTypes.BINARY:
					convertedType="LONG RAW";
					break;
				default:
					convertedType = DataTypes.TYPE_NAMES[field.getDataTypeID()];
				break;
				}
				return convertedType;
			}

			public String getStatementDropIndex() {
				return "DROP INDEX #NAME ON #TABLE";
			}

			public String getStatementDropView() {
				return "DROP VIEW #NAME";
			}
			public  int getDBType() {return ORACLE_DB;}

			public String addLimit(String query,int limit,int offset) {
				String toAppend = "";
				if (offset==0)
					toAppend=" ROWNUM <= "+limit;
				else
					toAppend=" ROWNUM BETWEEN "+offset+" AND "+(limit+offset)+" ";

				int indexOfWhere = SQLUtils.getWhereIndex(query);
				int indexOfGroupBy = SQLUtils.getGroupByIndex(query);
				int indexOfOrder = SQLUtils.getOrderByIndex(query);

				StringBuilder toReturn = new StringBuilder(query);
				if (indexOfGroupBy<0 && indexOfWhere<0 && indexOfOrder<0)
					return query+" WHERE "+toAppend;
				if (indexOfWhere<0 && indexOfOrder>0)
					return toReturn.insert(indexOfOrder, " WHERE "+toAppend).toString();
				if (indexOfWhere<0 && indexOfGroupBy>0)
					return toReturn.insert(indexOfGroupBy, " WHERE "+toAppend).toString();
				if (indexOfWhere>0) {
					StringBuilder tmp = toReturn.insert(indexOfWhere + " WHERE ".length(),toAppend+" AND (");
					int endIndex = tmp.length();
					if (indexOfGroupBy>0)
						endIndex=SQLUtils.getGroupByIndex(tmp);
					else if (indexOfOrder>0)
						endIndex=SQLUtils.getOrderByIndex(tmp);
					tmp.insert(endIndex, ")");
					return tmp.toString();
				}

				return query+" LIMIT "+limit+" OFFSET "+offset;
			}

			public byte convertLocalTypeToGSN(int colTypeInJDBCFormat,int colPrecision) {
				switch ( colTypeInJDBCFormat ) {
				case Types.NUMERIC:
					if (colPrecision==0)
						return DataTypes.BIGINT;
					else
						return DataTypes.DOUBLE;
				case Types.VARCHAR :
					return DataTypes.VARCHAR;
				case Types.CHAR :
					return DataTypes.CHAR;
				case Types.BINARY :
				case Types.BLOB :
				case Types.VARBINARY:
				case Types.LONGVARBINARY :
					return DataTypes.BINARY;
				default :
					logger.error( "The type can't be converted to GSN form : " + colTypeInJDBCFormat );
				break;
				}
				return -100;
			}
		},
		SQLSERVER("jdbc:jtds:sqlserver:", "net.sourceforge.jtds.jdbc.Driver") {

			public int getTableNotExistsErrNo() {
				return 208; //java.sql.SQLException: Invalid object name
			}

			/*
			 * Returns the HSQLDB data type that can store this gsn datafield.
			 * @param field The datafield to be converted. @return convertedType
			 * the data type used by hsql.
			 */
			public String convertGSNTypeToLocalType(DataField field) {
				String convertedType = null;
				switch (field.getDataTypeID()) {
				case DataTypes.CHAR:
				case DataTypes.VARCHAR:
					// Because the parameter for the varchar is not
					// optional.
					convertedType = field.getType();
					break;
				default:
					convertedType = DataTypes.TYPE_NAMES[field.getDataTypeID()];
				break;
				}
				return convertedType;
			}

			public String getStatementDropIndex() {
				return "DROP INDEX #NAME ON #TABLE";
			}

			public String getStatementDropView() {
				return "DROP VIEW #NAME";
			}
			public  int getDBType() {return SQLSERVER_DB;}

			public String addLimit(String query,int limit,int offset) {
				// FIXME, INCORRECT !
				return query+" LIMIT "+limit+" OFFSET "+offset;
			}

			public byte convertLocalTypeToGSN(int colTypeInJDBCFormat,int colPrecision) {
				switch ( colTypeInJDBCFormat ) {
				case Types.BIGINT :
					return DataTypes.BIGINT;
				case Types.INTEGER :
					return DataTypes.INTEGER;
				case Types.SMALLINT :
					return DataTypes.SMALLINT;
				case Types.TINYINT :
					return DataTypes.TINYINT;
				case Types.VARCHAR :
					return DataTypes.VARCHAR;
				case Types.CHAR :
					return DataTypes.CHAR;
				case Types.DOUBLE :
				case Types.DECIMAL:	// This is needed for doing aggregates in datadownload servlet.
					return DataTypes.DOUBLE;
				case Types.BINARY :
				case Types.BLOB :
				case Types.VARBINARY:
				case Types.LONGVARBINARY :
					return DataTypes.BINARY;
				default :
					logger.error( "The type can't be converted to GSN form : " + colTypeInJDBCFormat );
				break;
				}
				return -100;
			}

		};

		private final String jdbcPrefix;

		private final String driver;

		DATABASE(String jdbcPrefix, String driver) {
			this.jdbcPrefix = jdbcPrefix;
			this.driver = driver;
			//    try {
			//    Class.forName(driver);
			//    } catch (ClassNotFoundException e) {
			//    logger.error("Error in loading the database driver. !");
			//    logger.error(e.getMessage(), e);
			//    }
		}


		/**
		 * The prefix is in lower case
		 *
		 * @return
		 */
		public String getJDBCPrefix() {
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

		public abstract String convertGSNTypeToLocalType(DataField gsnType);

		public byte convertLocalTypeToGSN(int jdbcType) {
			return convertLocalTypeToGSN(jdbcType,0);
		}

		public abstract byte convertLocalTypeToGSN(int jdbcType,int precision);

		public abstract String getStatementDropIndex();

		public abstract String getStatementDropView();

		public abstract int getTableNotExistsErrNo();

		public abstract int getDBType();

		public abstract String addLimit(String query,int limit,int offset);

	};

	/**
	 * Determinse if the database used is MySql.
	 *
	 * @return Returns true if the database used is mysql.
	 */
	public static boolean isMysqlDB() {
		return mysql;
	}

	/**
	 * Determining if the database used is HSqlDB.
	 *
	 * @return true if the database used is HSqlDB.
	 */
	public static boolean isH2() {
		return h2;
	}
	public static boolean isOracle() {
		return oracle;
	}

	private static StorageManager singleton = new StorageManager();

	private static final transient Logger logger = Logger.getLogger(StorageManager.class);

	public static final int DEFAULT_STORAGE_POOL_SIZE = 100;


	private static boolean sqlserver;

	public static StorageManager getInstance() {
		return singleton;
	}

	private StorageManager() {
	}

	private String databaseURL;

	private Properties dbConnectionProperties = new Properties();

	private BasicDataSource pool;

	public void init(String databaseDriver, String username,
			String password, String databaseURL) {
		this.databaseURL = databaseURL;
		if (databaseDriver.trim().equalsIgnoreCase( DATABASE.H2.getJDBCDriverClass()))
			h2 = true;
		else if (databaseDriver.trim().equalsIgnoreCase( DATABASE.MYSQL.getJDBCDriverClass()))
			mysql = true;
		else if (databaseDriver.trim().equalsIgnoreCase(DATABASE.SQLSERVER.getJDBCDriverClass()))
			sqlserver = true;
		else  if (databaseDriver.trim().equalsIgnoreCase(DATABASE.ORACLE.getJDBCDriverClass()))
			oracle = true;
		else {
			logger.error(new StringBuilder().append("The GSN doesn't support the database driver : ").append(databaseDriver).toString());
			logger.error(new StringBuilder().append("Please check the storage element in the file : ").append(getContainerConfig().getContainerFileName()).toString());
		}

	
		logger.info("Initializing the access to the database server ...");

		Connection con = null;
		pool = new BasicDataSource();
	        pool.setDriverClassName(databaseDriver);
	        pool.setUsername(username);
	        pool.setPassword(password);
	        pool.setUrl(databaseURL);
			pool.setMaxActive(-1);
			pool.setMaxWait(30000);
		try {
		
			con = getConnection();
			Statement stmt = con.createStatement();
			if (StorageManager.isH2()) {
				stmt.execute("SET REFERENTIAL_INTEGRITY FALSE");
				stmt.execute("CREATE ALIAS IF NOT EXISTS NOW_MILLIS FOR \"java.lang.System.currentTimeMillis\";");
			} else if (StorageManager.isMysqlDB()) {
				ResultSet rs = stmt.executeQuery("select version();");
				rs.next();
				String versionInfo = rs.getString(1);
				if (!versionInfo.trim().startsWith("5.")) {
					logger.error(new StringBuilder().append(
					"You are using MySQL version : ").append(
							versionInfo).toString());
					logger
					.error("To run GSN using MySQL, you need version 5.0 or later.");
					System.exit(1);
				}
			} else if (sqlserver || oracle) {
				// Do the configurations related to sql server here.
			} else {
				logger.error("Unknow database server provided. GSN runs only under MySQL, HSqlDB and Oracle.");
			}
		} catch (Exception e) {
			logger.error(new StringBuilder().append("Connecting to the database with the following properties failed :")
					.append("\n\t UserName :").append(username).append("\n\t Password : ").append(password).append("\n\t Driver class : ").append(databaseDriver).append("\n\t Database URL : ").append(databaseURL).toString());
			logger.error(new StringBuilder().append(e.getMessage()).append(", Please refer to the logs for more detailed information.").toString());
			logger.error("Make sure in the : " + getContainerConfig().getContainerFileName() + " file, the <storage ...> element is correct.");
			e.printStackTrace();
			if (logger.isInfoEnabled())
				logger.info(e.getMessage(), e);
		} finally {
			close(con);
		}
	}

	static int connectionCount = 0;

	/**
	 * Obtains the default database connection.
	 * The conneciton comes from the data source which is configured through gsn.xml file.
	 * @return
	 * @throws SQLException
	 */
	public Connection getConnection() throws SQLException {
		//        if (this.connection == null || this.connection.isClosed())
		//            this.connection = DriverManager.getConnection(databaseURL,
		//                    dbConnectionProperties);
		//        return connection;
		if (logger.isDebugEnabled())
			logger.debug("Asking for connections to default DB=> busy: "+pool.getNumActive()+", max-size:"+pool.getMaxActive()+", idle:"+pool.getNumIdle());
		// try{ // used for tracking the calls to this method.
		//     throw new RuntimeException("Trackeeer");
		// }   catch (Exception e){
		//     logger.fatal(e.getMessage(),e);
		// }
		return pool.getConnection();
		//		connectionCount++;
		//		logger.debug("Asking for a new connection. current count : " + connectionCount);
		//		return DriverManager.getConnection(databaseURL, dbConnectionProperties);
	}


	/**
	 * Retruns an approximation of the difference between the current time of the DB and that of the local system
	 * @return
	 */
	public long getTimeDifferenceInMillis(){
		StringBuilder query = new StringBuilder();
		if (StorageManager.isH2())
			query.append("call NOW_MILLIS()");
		else if (StorageManager.isMysqlDB())
			query.append("select  UNIX_TIMESTAMP()*1000");
		else if (StorageManager.isSqlServer()) {
			query.append("select convert(bigint,datediff(second,'1/1/1970',current_timestamp))*1000 ");
		}
		Connection connection = null;
		try {
			connection = getConnection();
			PreparedStatement  prepareStatement = connection.prepareStatement(query.toString());
			long time1 = System.currentTimeMillis();
			ResultSet resultSet;
			resultSet = prepareStatement.executeQuery();
			resultSet.next();
			long time2 = System.currentTimeMillis();
			return resultSet.getLong(1) - time2 + (time2 - time1) / 2;
		} catch (SQLException error) {
			logger.error(error.getMessage(), error);
		} finally {
			close(connection);
		}
		return 0;
	}

	public ArrayList<String> getInternalTables() throws SQLException {
		ArrayList<String> toReturn = new ArrayList<String>();
		Connection c = null;
		try{
			c = getConnection();
			ResultSet rs = null;
			if (isMysqlDB()) {
				rs = executeQueryWithResultSet(new StringBuilder("show tables"),c );
			}
			if (rs!=null)
				while (rs.next())
					if (rs.getString(1).startsWith("_"))
						toReturn.add(rs.getString(1));
		}finally{
			close(c);
		}
		return toReturn;
	}
}
