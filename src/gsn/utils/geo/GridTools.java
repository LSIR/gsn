/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
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
* File: src/gsn/utils/geo/GridTools.java
*
* @author Sofiane Sarni
* @author Milos Stojanovic
*
*/

package gsn.utils.geo;

import gsn.Main;
import gsn.beans.DataTypes;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.simple.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class GridTools {

    private static transient Logger logger = Logger.getLogger(GridTools.class);

    public static String deSerializeToString(byte[] bytes) {

        StringBuilder sb = new StringBuilder();

        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream in = null;

            in = new ObjectInputStream(bis);

            Double deserial[][] = new Double[0][];

            deserial = (Double[][]) in.readObject();
            in.close();

            logger.debug("deserial.length" + deserial.length);
            logger.debug("deserial[0].length" + deserial[0].length);

            for (int i = 0; i < deserial.length; i++) {

                for (int j = 0; j < deserial[0].length; j++) {
                    sb.append(deserial[i][j]).append(" ");
                }
                sb.append("\n");
            }

        } catch (IOException e) {
            logger.warn(e);
        } catch (ClassNotFoundException e) {
            logger.warn(e);
        }

        return sb.toString();
    }

    public static double deSerializeToCell(byte[] bytes, int xcell, int ycell) {

        StringBuilder sb = new StringBuilder();
        double value = 0;

        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream in = null;

            in = new ObjectInputStream(bis);

            Double deserial[][] = new Double[0][];

            deserial = (Double[][]) in.readObject();
            in.close();

            logger.debug("deserial.length" + deserial.length);
            logger.debug("deserial[0].length" + deserial[0].length);

            value = deserial[ycell][xcell];

        } catch (IOException e) {
            logger.warn(e);
        } catch (ClassNotFoundException e) {
            logger.warn(e);
        }

        return value;
    }

    public static String deSerializeToStringWithBoundaries(byte[] bytes, int xmin, int xmax, int ymin, int ymax) {

        StringBuilder sb = new StringBuilder();

        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream in = null;

            in = new ObjectInputStream(bis);

            Double deserial[][] = new Double[0][];

            deserial = (Double[][]) in.readObject();
            in.close();

            logger.debug("deserial.length" + deserial.length);
            logger.debug("deserial[0].length" + deserial[0].length);

            for (int i = ymin; i <= ymax; i++) {

                for (int j = xmin; j <= xmax; j++) {
                    sb.append(deserial[i][j]).append(" ");
                }
                sb.append("\n");
            }

        } catch (IOException e) {
            logger.warn(e);
        } catch (ClassNotFoundException e) {
            logger.warn(e);
        }

        return sb.toString();
    }

    /*
    * deserialization
    * */
    public static Double[][] deSerialize(byte[] bytes) {

        Double deserial[][] = new Double[0][];

        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream in = null;

            in = new ObjectInputStream(bis);

            deserial = (Double[][]) in.readObject();
            in.close();

            if (logger.isDebugEnabled()) {
                logger.debug("deserial.length" + deserial.length);
                logger.debug("deserial[0].length" + deserial[0].length);
            }

            for (int i = 0; i < deserial.length; i++) {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < deserial[0].length; j++) {
                    sb.append(deserial[i][j]).append(" ");
                }
                logger.debug(sb.toString());
            }

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            logger.error(e.getMessage(), e);
        }

        return deserial;
    }

    public static String executeQueryForGridAsString(String query, String sensor) {

        Connection connection = null;
        StringBuilder sb = new StringBuilder();
        ResultSet results = null;

        try {
            connection = Main.getStorage(sensor).getConnection();
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            results = statement.executeQuery(query);
            ResultSetMetaData metaData;    // Additional information about the results
            int numCols, numRows;          // How many rows and columns in the table
            metaData = results.getMetaData();       // Get metadata on them
            numCols = metaData.getColumnCount();    // How many columns?
            results.last();                         // Move to last row
            numRows = results.getRow();             // How many rows?

            String s;

            // headers
            sb.append("# Query: " + query + "\n");
            sb.append("# ");

            byte typ[] = new byte[numCols];
            String columnLabel[] = new String[numCols];

            for (int col = 0; col < numCols; col++) {
                columnLabel[col] = metaData.getColumnLabel(col + 1);
                typ[col] = Main.getDefaultStorage().convertLocalTypeToGSN(metaData.getColumnType(col + 1));
                if (typ[col] == -100){
                    logger.error("The type can't be converted to GSN form - error description: column label is:"+columnLabel[col]+", query is: " + query);
                }
            }

            for (int row = 0; row < numRows; row++) {
                results.absolute(row + 1);                // Go to the specified row
                for (int col = 0; col < numCols; col++) {
                    Object o = results.getObject(col + 1); // Get value of the column
                    if (o == null)
                        s = "null";
                    else
                        s = o.toString();
                    if (typ[col] == DataTypes.BINARY) {
                        byte[] bin = (byte[]) o;
                        sb.append(GridTools.deSerializeToString(bin));
                    } else {
                        sb.append(columnLabel[col] + " " + s + "\n");
                    }
                }
                sb.append("\n");
            }
        } catch (SQLException e) {
        	logger.warn("SQLException: " + e.getMessage());
            sb.append("ERROR in execution of query: " + e.getMessage());
        } finally {
            if (results != null)
                try {
                    results.close();
                } catch (SQLException e) {
                    logger.warn(e.getMessage(), e);
                }
            Main.getStorage(sensor).close(connection);
        }

        return sb.toString();
    }


    public static Map<Long, Double> executeQueryForCell2TimeSeriesAsListOfDoubles(String query, int xcell, int ycell, String sensor) {

        Map<Long, Double> listOfDoubles = new HashMap<Long, Double>();
        Connection connection = null;
        StringBuilder sb = new StringBuilder();
        ResultSet results = null;

        try {
        	connection = Main.getStorage(sensor).getConnection();
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            results = statement.executeQuery(query);
            ResultSetMetaData metaData;    // Additional information about the results
            int numCols, numRows;          // How many rows and columns in the table
            metaData = results.getMetaData();       // Get metadata on them
            numCols = metaData.getColumnCount();    // How many columns?
            results.last();                         // Move to last row
            numRows = results.getRow();             // How many rows?

            String s;

            byte typ[] = new byte[numCols];
            String columnLabel[] = new String[numCols];

            for (int col = 0; col < numCols; col++) {
                columnLabel[col] = metaData.getColumnLabel(col + 1);
                typ[col] = Main.getDefaultStorage().convertLocalTypeToGSN(metaData.getColumnType(col + 1));
                if (typ[col] == -100){
                    logger.error("The type can't be converted to GSN form - error description: column label is: "+columnLabel[col]+", query is: " + query);
                }
            }

            Long timed = 0L;
            double value = 0.0;

            for (int row = 0; row < numRows; row++) {
                sb = new StringBuilder("");
                results.absolute(row + 1);                // Go to the specified row
                for (int col = 0; col < numCols; col++) {
                    Object o = results.getObject(col + 1); // Get value of the column
                    if (o == null)
                        s = "null";
                    else
                        s = o.toString();
                    if (columnLabel[col].equalsIgnoreCase("timed")) {
                        timed = Long.valueOf(s);
                        continue;
                    }
                    if (typ[col] == DataTypes.BINARY) {
                        byte[] bin = (byte[]) o;
                        value = GridTools.deSerializeToCell(bin, xcell, ycell);
                    }
                }

                listOfDoubles.put(timed, value);
            }

            //.add(sb.toString());
        } catch (SQLException e) {
        	logger.warn("SQLException: " + e.getMessage());
            sb.append("ERROR in execution of query: " + e.getMessage());
        } finally {
            if (results != null)
                try {
                    results.close();
                } catch (SQLException e) {
                    logger.warn(e.getMessage(), e);
                }
            Main.getStorage(sensor).close(connection);
        }

        return listOfDoubles;
    }

    public static Map<Long, String> executeQueryForSubGridAsListOfStrings(String query, int xmin, int xmax, int ymin, int ymax, String sensor) {

        Map<Long, String> listOfStrings = new HashMap<Long, String>();
        Connection connection = null;
        StringBuilder sb = new StringBuilder();
        ResultSet results = null;

        try {
        	connection = Main.getStorage(sensor).getConnection();
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            results = statement.executeQuery(query);
            ResultSetMetaData metaData;    // Additional information about the results
            int numCols, numRows;          // How many rows and columns in the table
            metaData = results.getMetaData();       // Get metadata on them
            numCols = metaData.getColumnCount();    // How many columns?
            results.last();                         // Move to last row
            numRows = results.getRow();             // How many rows?

            String s;




            byte typ[] = new byte[numCols];
            String columnLabel[] = new String[numCols];

            for (int col = 0; col < numCols; col++) {
                columnLabel[col] = metaData.getColumnLabel(col + 1);
                typ[col] = Main.getDefaultStorage().convertLocalTypeToGSN(metaData.getColumnType(col + 1));
                if (typ[col] == -100){
                    logger.error("The type can't be converted to GSN form - error description: column label is:"+columnLabel[col]+", query is: " + query);
                }
            }

            Long timed = 0L;

            for (int row = 0; row < numRows; row++) {
                sb = new StringBuilder("");
                results.absolute(row + 1);                // Go to the specified row
                for (int col = 0; col < numCols; col++) {
                    Object o = results.getObject(col + 1); // Get value of the column
                    if (o == null)
                        s = "null";
                    else
                        s = o.toString();
                    if (columnLabel[col].equalsIgnoreCase("pk"))
                        continue; // skip PK field
                    if (columnLabel[col].equalsIgnoreCase("timed")) {
                        timed = Long.valueOf(s);
                        continue;
                    }
                    if (typ[col] == DataTypes.BINARY) {
                        byte[] bin = (byte[]) o;
                        sb.append(GridTools.deSerializeToStringWithBoundaries(bin, xmin, xmax, ymin, ymax));
                    } else {
                        String fieldName = columnLabel[col];
                        String fieldValue = s;
                        if (fieldName.equalsIgnoreCase("ncols")) {
                            int nCols = xmax - xmin + 1;
                            fieldValue = Integer.toString(nCols);
                        } else if (fieldName.equalsIgnoreCase("nrows")) {
                            int nRows = ymax - ymin + 1;
                            fieldValue = Integer.toString(nRows);
                        }
                        sb.append(fieldName + " " + fieldValue + "\n");
                    }
                }
                sb.append("\n");
                listOfStrings.put(timed, sb.toString());
            }

            //.add(sb.toString());
        } catch (SQLException e) {
        	logger.warn("SQLException: " + e.getMessage());
            sb.append("ERROR in execution of query: " + e.getMessage());
        } finally {
            if (results != null)
                try {
                    results.close();
                } catch (SQLException e) {
                    logger.warn(e.getMessage(), e);
                }
            Main.getStorage(sensor).close(connection);
        }

        return listOfStrings;
    }

    public static Map<Long, String> executeQueryForGridAsListOfStrings(String query, String sensor) {

        Map<Long, String> listOfStrings = new HashMap<Long, String>();
        Connection connection = null;
        StringBuilder sb = new StringBuilder();
        ResultSet results = null;

        try {
            connection = Main.getStorage(sensor).getConnection();
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            results = statement.executeQuery(query);
            ResultSetMetaData metaData;    // Additional information about the results
            int numCols, numRows;          // How many rows and columns in the table
            metaData = results.getMetaData();       // Get metadata on them
            numCols = metaData.getColumnCount();    // How many columns?
            results.last();                         // Move to last row
            numRows = results.getRow();             // How many rows?

            String s;




            byte typ[] = new byte[numCols];
            String columnLabel[] = new String[numCols];

            for (int col = 0; col < numCols; col++) {
                columnLabel[col] = metaData.getColumnLabel(col + 1);
                typ[col] = Main.getDefaultStorage().convertLocalTypeToGSN(metaData.getColumnType(col + 1));
                if (typ[col] == -100){
                    logger.error("The type can't be converted to GSN form - error description:  column label is:"+columnLabel[col]+", query is: " + query);
                }
            }

            Long timed = 0L;

            for (int row = 0; row < numRows; row++) {
                sb = new StringBuilder("");
                results.absolute(row + 1);                // Go to the specified row
                for (int col = 0; col < numCols; col++) {
                    Object o = results.getObject(col + 1); // Get value of the column
                    if (o == null)
                        s = "null";
                    else
                        s = o.toString();
                    if (columnLabel[col].equalsIgnoreCase("pk"))
                        continue; // skip PK field
                    if (columnLabel[col].equalsIgnoreCase("timed")) {
                        timed = Long.valueOf(s);
                        continue;
                    }
                    if (typ[col] == DataTypes.BINARY) {
                        byte[] bin = (byte[]) o;
                        sb.append(GridTools.deSerializeToString(bin));
                    } else {
                        String fieldName = columnLabel[col];
                        String fieldValue = s;

                        sb.append(fieldName + " " + fieldValue + "\n");
                    }
                }
                sb.append("\n");
                listOfStrings.put(timed, sb.toString());
            }

            //.add(sb.toString());
        } catch (SQLException e) {
            sb.append("ERROR in execution of query: " + e.getMessage());
            logger.warn("SQLException: " + e.getMessage());
        } finally {
            if (results != null)
                try {
                    results.close();
                } catch (SQLException e) {
                    logger.warn(e.getMessage(), e);
                }
            Main.getStorage(sensor).close(connection);
        }
   
        return listOfStrings;
    }

    public static String executeQueryForGridAsJSON(String sensor, long timestamp) {

        String query = new StringBuilder("select * from ").append(sensor).append(" where timed=").append(timestamp).toString();
        Connection connection = null;
        StringBuilder sb = new StringBuilder();
        ResultSet results = null;

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("sensor", sensor);
        jsonResponse.put("epoch", timestamp);

        try {
        	connection = Main.getStorage(sensor).getConnection();
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            results = statement.executeQuery(query);
            ResultSetMetaData metaData;    // Additional information about the results
            int numCols, numRows;          // How many rows and columns in the table
            metaData = results.getMetaData();       // Get metadata on them
            numCols = metaData.getColumnCount();    // How many columns?
            results.last();                         // Move to last row
            numRows = results.getRow();             // How many rows?

            byte typ[] = new byte[numCols];
            String columnLabel[] = new String[numCols];

            for (int col = 0; col < numCols; col++) {
                columnLabel[col] = metaData.getColumnLabel(col + 1);
                typ[col] = Main.getDefaultStorage().convertLocalTypeToGSN(metaData.getColumnType(col + 1));
                if (typ[col] == -100){
                    logger.error("The type can't be converted to GSN form - error description: virtual sensor name is: "+sensor+", column label is:"+columnLabel[col]+", query is: " + query);
                }
            }

            for (int row = 0; row < numRows; row++) {
                results.absolute(row + 1);                // Go to the specified row
                for (int col = 0; col < numCols; col++) {
                    Object o = results.getObject(col + 1); // Get value of the column

                    if (typ[col] == DataTypes.BINARY) {
                        byte[] bin = (byte[]) o;
                        Double[][] array = GridTools.deSerialize(bin);
                        JSONArray jsonArray = new JSONArray();
                        for (int i = 0; i < array.length; i++) {
                            JSONArray anArray = new JSONArray();
                            for (int j = 0; j < array[i].length; j++) {
                                anArray.put(array[i][j]);
                            }
                            jsonArray.put(anArray);
                        }
                        jsonResponse.put(columnLabel[col], jsonArray);
                    } else {
                        jsonResponse.put(columnLabel[col], o);
                    }
                }
            }
        } catch (SQLException e) {
        	logger.warn("SQLException: " + e.getMessage());
            sb.append("ERROR in execution of query: " + e.getMessage());
        } finally {
            if (results != null)
                try {
                    results.close();
                } catch (SQLException e) {
                    logger.warn(e.getMessage(), e);
                }
            Main.getStorage(sensor).close(connection);
        }

        return jsonResponse.toJSONString();
    }
}
