/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2016, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/ch/epfl/gsn/storage/db/MySQLStorageManager.java
*
* @author Timotee Maret
* @author Mehdi Riahi
* @author Milos Stojanovic
*
*/

package ch.epfl.gsn.storage.db;

import org.slf4j.LoggerFactory;

import ch.epfl.gsn.beans.DataField;
import ch.epfl.gsn.beans.DataTypes;
import ch.epfl.gsn.storage.DataEnumerator;
import ch.epfl.gsn.storage.StorageManager;

import org.slf4j.Logger;

import java.sql.*;
import java.util.ArrayList;

public class MySQLStorageManager extends StorageManager {

    private static final transient Logger logger = LoggerFactory.getLogger(MySQLStorageManager.class);

    public MySQLStorageManager() {
        super();
        this.isMysql = true;
    }

    @Override
    public String getJDBCPrefix() {
        return "jdbc:mysql:";
    }

    @Override
    public String convertGSNTypeToLocalType(DataField gsnType) {
        String convertedType;
        switch (gsnType.getDataTypeID()) {
            case DataTypes.CHAR:
            case DataTypes.VARCHAR:
                // Because the parameter for the varchar is not
                // optional.
                if (gsnType.getType().trim().equalsIgnoreCase("string"))
                    convertedType = "TEXT";
                else
                    convertedType = gsnType.getType();
                break;
            case DataTypes.BINARY:
                convertedType = "LONGBLOB";
                break;
            case DataTypes.DOUBLE:
                convertedType = "double precision";
                break;
            case DataTypes.FLOAT:
            	convertedType = "FLOAT(23)"; //just to be sure it doesn't map to double
                break;
            default:
                convertedType = DataTypes.TYPE_NAMES[gsnType.getDataTypeID()];
                break;
        }
        return convertedType;
    }

    @Override
    public byte convertLocalTypeToGSN(int jdbcType, int precision) {
        switch (jdbcType) {
            case Types.BIGINT:
                return DataTypes.BIGINT;
            case Types.INTEGER:
                return DataTypes.INTEGER;
            case Types.SMALLINT:
                return DataTypes.SMALLINT;
            case Types.TINYINT:
                return DataTypes.TINYINT;
            case Types.VARCHAR:
            case Types.LONGVARCHAR: // This is needed because of the string type in CSV wrapper. 	
                return DataTypes.VARCHAR;
            case Types.CHAR:
                return DataTypes.CHAR;
            case Types.FLOAT:      
            case Types.REAL:      
            	return DataTypes.FLOAT;
            case Types.DOUBLE:	
            case Types.DECIMAL:    // This is needed for doing aggregates in datadownload servlet.
                return DataTypes.DOUBLE;
            case Types.BINARY:
            case Types.BLOB:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                return DataTypes.BINARY;
            default:
                if (jdbcType == Types.NULL){
                    logger.error("The type can't be converted to GSN form : 0. (Found  type in JDBC format is \"Null\")");
                }
                else {
                    logger.error("The type can't be converted to GSN form : " + jdbcType);
                }
                break;
        }
        return -100;
    }

    @Override
    public String getStatementDropIndex() {
        //if (AbstractStorageManager.isMysqlDB()) return "DROP INDEX #NAME IF EXISTS";
        return "DROP TABLE IF EXISTS #NAME";
    }

    @Override
    public String getStatementDropView() {
        return "DROP VIEW IF EXISTS #NAME";
    }

    @Override
    public int getTableNotExistsErrNo() {
        return 1146;
    }

    @Override
    public String addLimit(String query, int limit, int offset) {
        return query + " LIMIT " + limit + " OFFSET " + offset;
    }

    @Override
    public void initDatabaseAccess(Connection con) throws Exception {
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("select version();");
        rs.next();
        String versionInfo = rs.getString(1);
        if (!versionInfo.trim().startsWith("5.")) {
            logger.error(new StringBuilder().append("You are using MySQL version : ").append(versionInfo).toString());
            logger.error("To run GSN using MySQL, you need version 5.0 or later.");
            System.exit(1);
        }
        super.initDatabaseAccess(con);
    }

    @Override
    public String getStatementDifferenceTimeInMillis() {
        return "select  UNIX_TIMESTAMP()*1000";
    }

    @Override
    public StringBuilder getStatementDropTable(CharSequence tableName, Connection conn) throws SQLException {
        StringBuilder sb = new StringBuilder("Drop table if exists ");
        sb.append(tableName);
        return sb;
    }

    @Override
    public StringBuilder getStatementCreateTable(String tableName, DataField[] structure) {
        StringBuilder result = new StringBuilder("CREATE TABLE ").append(tableName);
        result.append(" (PK BIGINT PRIMARY KEY NOT NULL AUTO_INCREMENT, timed BIGINT NOT NULL, ");
        for (DataField field : structure) {
            if (field.getName().equalsIgnoreCase("pk") || field.getName().equalsIgnoreCase("timed")) continue;
            result.append(field.getName().toUpperCase()).append(' ');
            result.append(convertGSNTypeToLocalType(field));
            result.append(" ,");
        }
        result.delete(result.length() - 2, result.length());
        result.append(")");
        if (tableName.contains("_")) logger.warn(result.toString());
        return result;
    }

    @Override
    public StringBuilder getStatementUselessDataRemoval(String virtualSensorName, long storageSize) {
        return new StringBuilder()
                .append("delete from ")
                .append(virtualSensorName)
                .append(" where ")
                .append(virtualSensorName)
                .append(".timed <= ( SELECT * FROM ( SELECT timed FROM ")
                .append(virtualSensorName)
                .append(" group by ")
                .append(virtualSensorName)
                .append(".timed ORDER BY ")
                .append(virtualSensorName)
                .append(".timed DESC LIMIT 1 offset ")
                .append(storageSize)
                .append("  ) AS TMP)");
    }

    @Override
    public StringBuilder getStatementRemoveUselessDataCountBased(String virtualSensorName, long storageSize) {
        return new StringBuilder()
                .append("delete from ")
                .append(virtualSensorName)
                .append(" where ")
                .append(virtualSensorName)
                .append(".timed <= ( SELECT * FROM ( SELECT timed FROM ")
                .append(virtualSensorName)
                .append(" group by ")
                .append(virtualSensorName)
                .append(".timed ORDER BY ")
                .append(virtualSensorName)
                .append(".timed DESC LIMIT 1 offset ")
                .append(storageSize).append("  ) AS TMP)");
    }

    //

    @Override
    public ArrayList<String> getInternalTables() throws SQLException {
        ArrayList<String> toReturn = new ArrayList<String>();
        Connection c = null;
        try {
            c = getConnection();
            ResultSet rs = executeQueryWithResultSet(new StringBuilder("show tables"), c);
            if (rs != null)
                while (rs.next())
                    if (rs.getString(1).startsWith("_"))
                        toReturn.add(rs.getString(1));
        } finally {
            close(c);
        }
        return toReturn;
    }

    @Override
    public DataEnumerator streamedExecuteQuery(String query, boolean binaryFieldsLinked, Connection conn) throws SQLException {
        PreparedStatement ps = null;
        // Support streamed queries for MySQL -- see MySQL Implementation notes:
        // http://dev.mysql.com/doc/refman/5.0/en/connector-j-reference-implementation-notes.html
        ps = conn.prepareStatement(query, java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
        ps.setFetchSize(Integer.MIN_VALUE);
        return new DataEnumerator(this, ps, binaryFieldsLinked);
    }

}
