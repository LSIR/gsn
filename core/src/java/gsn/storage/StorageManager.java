package gsn.storage;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import gsn.Main;
import static gsn.Main.getContainerConfig;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.http.datarequest.AbstractQuery;
import gsn.utils.GSNRuntimeException;
import gsn.utils.ValidityTools;
import org.apache.log4j.Logger;

import java.beans.PropertyVetoException;
import java.io.Serializable;
import java.sql.*;
import java.util.ArrayList;
import java.util.Properties;

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
        return DATABASE.H2;
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
        return tableExists(tableName, new DataField[]{}, getConnection());
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
        return tableExists(tableName, new DataField[]{}, connection);
    }


    public static DataField[] tableToStructure(CharSequence tableName, Connection connection) throws SQLException {
        StringBuilder sb = new StringBuilder("select * from ").append(tableName).append(" where 1=0 ");
        ResultSet rs = null;
        DataField[] toReturn = null;
        try {
            rs = executeQueryWithResultSet(sb, connection);
            ResultSetMetaData structure = rs.getMetaData();
            ArrayList<DataField> toReturnArr = new ArrayList<DataField>();
            for (int i = 1; i <= structure.getColumnCount(); i++) {
                String colName = structure.getColumnName(i);
                if (colName.equalsIgnoreCase("pk")) continue;
                int colType = structure.getColumnType(i);
                byte colTypeInGSN = getDatabaseForConnection(connection).convertLocalTypeToGSN(colType);
                toReturnArr.add(new DataField(colName, colTypeInGSN));
            }
            toReturn = toReturnArr.toArray(new DataField[]{});
        } finally {
            if (rs != null)
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
     * @param connection (this method will not close it and the caller is responsible
     *                   for closing the connection)
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
                nextField:for (DataField field : fields) {
                    for (int i = 1; i <= structure.getColumnCount(); i++) {
                        String colName = structure.getColumnName(i);
                        int colType = structure.getColumnType(i);
                        int colTypeScale = structure.getScale(i);
                        if (field.getName().equalsIgnoreCase(colName))
                            if (field.getDataTypeID() == getDatabaseForConnection(connection).convertLocalTypeToGSN(colType, colTypeScale))
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
        Connection conn = getConnection();
        boolean to_return = true;
        try {
            to_return = tableExists(tableName, fields, conn);
        } finally {
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
        PreparedStatement prepareStatement = null;
        try {
            prepareStatement = getConnection().prepareStatement(
                    sqlQuery.toString());
            ResultSet resultSet = prepareStatement.executeQuery();
            toreturn = resultSet.next();
        } catch (SQLException error) {
            logger.error(error.getMessage(), error);
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
     * @return A resultset with only one row and one column. The user of the
     *         method should first call next on the result set to make sure that
     *         the row is there and then retrieve the value for the row.
     * @throws SQLException
     */

    public static ResultSet getBinaryFieldByQuery(StringBuilder query,
                                                  String colName, long pk, Connection connection) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(query.toString());
        ps.setLong(1, pk);
        return ps.executeQuery();
    }

    public ResultSet getBinaryFieldByQuery(StringBuilder query, String colName,
                                           long pk) throws SQLException {
        return getBinaryFieldByQuery(query, colName, pk, getConnection());
    }

    public static void closeStatement(Statement stmt) {
        try {
            if (stmt != null)
                stmt.getConnection().close();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static void close(ResultSet resultSet) {
        try {
            if (resultSet != null)
                resultSet.getStatement().getConnection().close();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static void close(PreparedStatement preparedStatement) {
        try {
            if (preparedStatement != null)
                preparedStatement.getConnection().close();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
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
        getConnection().createStatement().execute("SHUTDOWN");
        logger.warn("Closing the database server (for H2) [done].");

    }

    /**
     * ************************************************************************
     * Various Statement Executors.
     * ************************************************************************
     */

    public void executeRenameTable(String oldName, String newName)
            throws SQLException {
        Connection conn = getConnection();
        try {
            executeRenameTable(oldName, newName, conn);
        } finally {
            close(conn);
        }

    }

    public void executeRenameTable(String oldName, String newName,
                                   Connection connection) throws SQLException {
        connection.prepareStatement(getStatementRenameTable(oldName, newName)).execute();
    }

    public void executeDropTable(CharSequence tableName) throws SQLException {
        Connection conn = getConnection();
        try {
            executeDropTable(tableName, conn);
        } finally {
            close(conn);
        }
    }

    private void close(Connection conn) {
        try {
            if (conn != null & !conn.isClosed())
                conn.close();
        } catch (SQLException e) {
            logger.debug(e.getMessage(), e);
        }
    }

    public void executeDropTable(CharSequence tableName, Connection connection) {
        PreparedStatement prepareStatement = null;
        try {
            //String	stmt = getStatementDropIndex(Main.tableNamePostFixAppender(tableName, "_INDEX"), tableName,connection).toString();
            //if (logger.isDebugEnabled())
            //	logger.debug("Dropping table index on " + Main.tableNameGeneratorInString(tableName)+ " With query: "+stmt);
            //prepareStatement = connection.prepareStatement(stmt);
            //prepareStatement.execute();
            //close(prepareStatement);
            String stmt = getStatementDropTable(tableName, connection).toString();
            if (logger.isDebugEnabled())
                logger.debug("Dropping table structure: " + tableName + " With query: " + stmt);
            prepareStatement = connection.prepareStatement(stmt);
            prepareStatement.execute();

        } catch (SQLException e) {
            logger.info(e.getMessage(), e);
        }
    }

    public void executeDropView(StringBuilder tableName) throws SQLException {
        Connection conn = getConnection();
        try {
            executeDropView(tableName, conn);
        } finally {
            close(conn);
        }
    }

    public void executeDropView(StringBuilder tableName, Connection connection)
            throws SQLException {
        if (logger.isDebugEnabled())
            logger.debug("Dropping table structure: " + tableName);
        PreparedStatement prepareStatement = connection
                .prepareStatement(getStatementDropView(tableName, connection)
                        .toString());
        prepareStatement.execute();
        close(prepareStatement);
    }

    public void executeCreateTable(CharSequence tableName, DataField[] structure, boolean unique)
            throws SQLException {
        Connection conn = getConnection();
        try {
            executeCreateTable(tableName, structure, unique, conn);
        } finally {
            close(conn);
        }
    }

    /**
     * Create a table with a index on the timed field.
     *
     * @param tableName
     * @param structure
     * @param unique     , setting this true cause the system to create a unique index on time.
     * @param connection
     * @throws SQLException
     */
    public static void executeCreateTable(CharSequence tableName, DataField[] structure, boolean unique, Connection connection) throws SQLException {
        StringBuilder sql = getStatementCreateTable(tableName, structure, connection);
        if (logger.isDebugEnabled())
            logger.debug(new StringBuilder().append("The create table statement is : ").append(sql).toString());

        PreparedStatement prepareStatement = connection.prepareStatement(sql.toString());
        prepareStatement.execute();
        prepareStatement.close();

        sql = getStatementCreateIndexOnTimed(tableName, unique);
        if (logger.isDebugEnabled())
            logger.debug(new StringBuilder().append("The create index statement is : ").append(sql).toString());
        prepareStatement = connection.prepareStatement(sql.toString());
        prepareStatement.execute();

    }

    public ResultSet executeQueryWithResultSet(StringBuilder query)
            throws SQLException {
        return executeQueryWithResultSet(query, getConnection());
    }

    public static ResultSet executeQueryWithResultSet(StringBuilder query,
                                                      Connection connection) throws SQLException {
        return connection.prepareStatement(query.toString()).executeQuery();
    }

    public ResultSet executeQueryWithResultSet(AbstractQuery abstractQuery, Connection c) throws SQLException {
        if (abstractQuery.getLimitCriterion() == null) {
            return executeQueryWithResultSet(abstractQuery.getStandardQuery(), c);
        }
        DATABASE db = getDatabaseForConnection(c);
        String query = db.addLimit(abstractQuery.getStandardQuery().toString(), abstractQuery.getLimitCriterion().getSize(), abstractQuery.getLimitCriterion().getOffset());
        return executeQueryWithResultSet(new StringBuilder(query), c);
    }

    public static DataEnumerator executeQuery(StringBuilder query, boolean binaryFieldsLinked, Connection connection) throws SQLException {
        if (logger.isDebugEnabled())
            logger.debug("Executing query: " + query + "( Binary Field Linked:" + binaryFieldsLinked + ")");
        return new DataEnumerator(connection.prepareStatement(query.toString()), binaryFieldsLinked);
    }

    public static DataEnumerator executeQuery(AbstractQuery abstractQuery, boolean binaryFieldsLinked, Connection connection) throws SQLException {
        if (abstractQuery.getLimitCriterion() == null) {
            return executeQuery(abstractQuery.getStandardQuery(), binaryFieldsLinked, connection);
        }
        DATABASE db = getDatabaseForConnection(connection);
        String query = db.addLimit(abstractQuery.getStandardQuery().toString(), abstractQuery.getLimitCriterion().getSize(), abstractQuery.getLimitCriterion().getOffset());
        if (logger.isDebugEnabled())
            logger.debug("Executing query: " + query + "(" + binaryFieldsLinked + ")");
        return new DataEnumerator(connection.prepareStatement(query.toString()), binaryFieldsLinked);
    }

    public DataEnumerator executeQuery(StringBuilder query, boolean binaryFieldsLinked) throws SQLException {
        return executeQuery(query, binaryFieldsLinked, getConnection());
    }

    public void executeCreateView(CharSequence viewName,
                                  CharSequence selectQuery) throws SQLException {
        executeCreateView(viewName, selectQuery, getConnection());
    }

    public void executeCreateView(CharSequence viewName,
                                  CharSequence selectQuery, Connection connection)
            throws SQLException {
        StringBuilder statement = getStatementCreateView(viewName, selectQuery);
        if (logger.isDebugEnabled())
            logger.debug("Creating a view:" + statement);
        final PreparedStatement prepareStatement = connection
                .prepareStatement(statement.toString());
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
        } catch (SQLException error) {
            logger.error(error.getMessage() + " FOR: " + sql, error);
        } finally {
            try {
                if (stmt != null && !stmt.isClosed())
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
        try {
            to_return = executeUpdate(updateStatement.toString(), connection);
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return to_return;
    }

    public int executeUpdate(StringBuilder updateStatement) throws SQLException {
        return executeUpdate(updateStatement, getConnection());
    }

    public void executeInsert(CharSequence tableName, DataField[] fields, StreamElement se) throws SQLException {
        executeInsert(tableName, fields, se, getConnection());
    }

    public static void executeInsert(CharSequence tableName, DataField[] fields, StreamElement streamElement, Connection connection) throws SQLException {
        PreparedStatement ps = null;
        String query = getStatementInsert(tableName, fields).toString();
        try {
            ps = connection.prepareStatement(query);
            int counter = 1;
            for (DataField dataField : fields) {
                if (dataField.getName().equalsIgnoreCase("timed"))
                    continue;
                Serializable value = streamElement.getData(dataField.getName());
                switch (dataField.getDataTypeID()) {
                    case DataTypes.STRING:
                        if (value == null)
                            ps.setNull(counter, Types.VARCHAR);
                        else
                            ps.setString(counter, value.toString());
                        break;

                    case DataTypes.NUMERIC:
                        if (value == null)
                            ps.setNull(counter, Types.NUMERIC);
                        else
                            ps.setDouble(counter, (Double) value);
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
                    logger.debug("An stream element dropped due to unexpected virtual sensor removal. (Stream element: " + streamElement.toString() + ")+ Query: " + query, e);
            } else
                logger.warn("Inserting a stream element failed : "
                        + streamElement.toString(), e);
        } catch (SQLException e) {
            if (e.getMessage().toLowerCase().contains("duplicate entry"))
                logger.info("Error occurred on inserting data to the database, an stream element dropped due to: " + e.getMessage() + ". (Stream element: " + streamElement.toString() + ")+ Query: " + query);
            else
                logger.warn("Error occurred on inserting data to the database, an stream element dropped due to: " + e.getMessage() + ". (Stream element: " + streamElement.toString() + ")+ Query: " + query);
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
     * @param tableName The table which the generated sql will pointing to.
     * @param fields    The stream element for which the sql statement is generated.
     * @return A sql statement which can be used for inserting the provided
     *         stream element into the specified table.
     */
    public static StringBuilder getStatementInsert(CharSequence tableName, DataField fields[]) {
        StringBuilder toReturn = new StringBuilder("insert into ").append(tableName).append(" ( ");
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
        toReturn.deleteCharAt(toReturn.length() - 1);
        toReturn.append(")");
        return toReturn;
    }

    public static String getStatementRenameTable(String oldName, String newName) {
        return new StringBuilder("alter table ").append(oldName).append(
                " rename to ").append(newName).toString();
    }

    public static StringBuilder getStatementDropTable(CharSequence tableName, Connection conn) throws SQLException {
        StringBuilder sb = new StringBuilder("Drop table ").append(" if exists ").append(tableName);
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
                indexName).replace("#TABLE", tableName));
    }

    public static StringBuilder getStatementDropView(CharSequence viewName,
                                                     Connection connection) throws SQLException {
        DATABASE db = getDatabaseForConnection(connection);
        return new StringBuilder(db.getStatementDropView().replace("#NAME",
                viewName));
    }

    public static StringBuilder getStatementCreateIndexOnTimed(
            CharSequence tableName, boolean unique) throws SQLException {
        StringBuilder toReturn = new StringBuilder("CREATE ");
        if (unique)
            toReturn.append(" UNIQUE ");
        toReturn.append(" INDEX ").append(Main.tableNamePostFixAppender(tableName, "_INDEX")).append(" ON ").append(tableName).append(" (timed DESC)");
        return toReturn;
    }

    public static StringBuilder getStatementCreateTable(CharSequence tableName, DataField[] structure, Connection connection) throws SQLException {
        StringBuilder result = new StringBuilder("CREATE TABLE ").append(tableName);
        DATABASE db = getDatabaseForConnection(connection);
        result.append(" (PK BIGINT NOT NULL IDENTITY, timed BIGINT NOT NULL, ");

        for (DataField field : structure) {
            if (field.getName().equalsIgnoreCase("pk") || field.getName().equalsIgnoreCase("timed")) continue;
            result.append(field.getName().toUpperCase()).append(' ');
            result.append(DataTypes.convertGSNTypeNameToSQLTypeString(field.getType()));
            result.append(" ,");
        }
        result.delete(result.length() - 2, result.length());
        result.append(")");
        return result;
    }

    public static StringBuilder getStatementCreateView(CharSequence viewName,
                                                       CharSequence selectQuery) {
        return new StringBuilder("create view ").append(viewName).append(
                " AS ( ").append(selectQuery).append(" ) ");
    }

    /**
     * ************************************************************************
     * INITIALIZATION PARAMETERS. SET IN THE FIRST TIME THIS CLASS USED.
     * ************************************************************************
     */

    public static enum DATABASE {
        H2("jdbc:h2:", "org.h2.Driver") {
            public int getTableNotExistsErrNo() {
                return 42102;
            }


            public String getStatementDropIndex() {
                return "DROP INDEX #NAME";
            }

            public String getStatementDropView() {
                return "DROP VIEW #NAME IF EXISTS";
            }

            public String addLimit(String query, int limit, int offset) {
                return query + " LIMIT " + limit + " OFFSET " + offset;
            }

            public byte convertLocalTypeToGSN(int colTypeInJDBCFormat, int colScale) {
                switch (colTypeInJDBCFormat) {
                    case Types.BIGINT:
                        return DataTypes.NUMERIC;
                    case Types.INTEGER:
                        return DataTypes.NUMERIC;
                    case Types.SMALLINT:
                        return DataTypes.NUMERIC;
                    case Types.TINYINT:
                        return DataTypes.NUMERIC;
                    case Types.VARCHAR:
                        return DataTypes.STRING;
                    case Types.CHAR:
                        return DataTypes.STRING;
                    case Types.DOUBLE:
                    case Types.DECIMAL:    // This is needed for doing aggregates in datadownload servlet.
                        return DataTypes.NUMERIC;
                    case Types.BINARY:
                    case Types.BLOB:
                    case Types.VARBINARY:
                    case Types.LONGVARBINARY:
                        return DataTypes.BINARY;
                    default:
                        logger.error("The type can't be converted to GSN form : " + colTypeInJDBCFormat);
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


        public byte convertLocalTypeToGSN(int jdbcType) {
            return convertLocalTypeToGSN(jdbcType, 0);
        }

        public abstract byte convertLocalTypeToGSN(int jdbcType, int precision);

        public abstract String getStatementDropIndex();

        public abstract String getStatementDropView();

        public abstract int getTableNotExistsErrNo();

        public abstract String addLimit(String query, int limit, int offset);

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

    private ComboPooledDataSource pool;


    public void init(String databaseURL) {
        this.databaseURL = databaseURL;

        dbConnectionProperties.put("user", "sa");
        dbConnectionProperties.put("password", "");

        dbConnectionProperties.put("username", "sa");
        dbConnectionProperties.put("ignorecase", "true");
        dbConnectionProperties.put("autocommit", "true");
        dbConnectionProperties.put("useUnicode", "true");
        dbConnectionProperties.put("autoReconnect", "true");
        dbConnectionProperties.put("autoReconnectForPools", "true");
        dbConnectionProperties.put("cacheCallableStmts", "true");
        dbConnectionProperties.put("cachePrepStmts", "true");
        dbConnectionProperties.put("cacheResultSetMetadata", "true");
        dbConnectionProperties.put("cacheResultSetMetadata", "true");
        dbConnectionProperties.put("defaultFetchSize", "5");
        dbConnectionProperties.put("useLocalSessionState", "true");
        dbConnectionProperties.put("useLocalSessionState", "true");
        dbConnectionProperties.put("useServerPrepStmts", "false");
        dbConnectionProperties.put("prepStmtCacheSize", "512");
        pool = new ComboPooledDataSource();

        if (logger.isDebugEnabled()) {
            pool.setDebugUnreturnedConnectionStackTraces(true);
            pool.setUnreturnedConnectionTimeout(10);//10 seconds
        }

        try {
            pool.setDriverClass("org.h2.Driver");
        } catch (PropertyVetoException e) {
            logger.fatal(e.getMessage(), e);
            System.exit(1);
        }

        pool.setJdbcUrl(databaseURL);
        pool.setProperties(dbConnectionProperties);
        pool.setMaxPoolSize(50);
        pool.setInitialPoolSize(50);

        logger.info("Initializing the access to the database server ...");

        Connection con = null;

        try {
            con = getConnection();
            Statement stmt = con.createStatement();
            stmt.execute("SET REFERENTIAL_INTEGRITY FALSE");
            stmt.execute("CREATE ALIAS IF NOT EXISTS NOW_MILLIS FOR \"java.lang.System.currentTimeMillis\";");
        } catch (SQLException e) {
            logger.error(new StringBuilder().append("Connecting to the database with the following properties failed :")
                    .append("\n\t Database URL : ").append(databaseURL).toString());
            logger.error(new StringBuilder().append(e.getMessage()).append(", Please refer to the logs for more detailed information.").toString());
            logger.error("Make sure in the : " + getContainerConfig().getContainerFileName() + " file, the <storage ...> element is correct.");
            e.printStackTrace();
            if (logger.isInfoEnabled())
                logger.info(e.getMessage(), e);
        } finally {
            close(con);
        }
    }

    /**
     * Obtains the default database connection.
     * The conneciton comes from the data source which is configured through gsn.xml file.
     *
     * @return
     * @throws SQLException
     */
    public Connection getConnection() throws SQLException {
//        if (this.connection == null || this.connection.isClosed())
//            this.connection = DriverManager.getConnection(databaseURL,
//                    dbConnectionProperties);
//        return connection;
        logger.debug("Asking for connections to default DB=> busy: " + pool.getNumBusyConnections() + ", max-size:" + pool.getMaxPoolSize() + ", idle:" + pool.getNumIdleConnections());
        // try{ // used for tracking the calls to this method.
        //     throw new RuntimeException("Trackeeer");
        // }   catch (Exception e){
        //     logger.fatal(e.getMessage(),e);
        // }
        return pool.getConnection();
    }


    /**
     * Retruns an approximation of the difference between the current time of the DB and that of the local system
     *
     * @return
     */
    public long getTimeDifferenceInMillis() {
        StringBuilder query = new StringBuilder();
        query.append("call NOW_MILLIS()");

        PreparedStatement prepareStatement = null;
        try {
            prepareStatement = getConnection().prepareStatement(query.toString());
            long time1 = System.currentTimeMillis();
            ResultSet resultSet;
            resultSet = prepareStatement.executeQuery();
            resultSet.next();
            long time2 = System.currentTimeMillis();
            return resultSet.getLong(1) - time2 + (time2 - time1) / 2;
        } catch (SQLException error) {
            logger.error(error.getMessage(), error);
        } finally {
            close(prepareStatement);
        }
        return 0;
    }

    public ArrayList<String> getInternalTables() throws SQLException {
        ArrayList<String> toReturn = new ArrayList<String>();
        Connection c = getConnection();
        ResultSet rs = null;
        if (rs != null)
            while (rs.next())
                if (rs.getString(1).startsWith("_"))
                    toReturn.add(rs.getString(1));
        close(rs);
        return toReturn;
    }
}
