package ch.epfl.gsn.monitoring;

import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Iterator;
import java.sql.*;

import org.slf4j.LoggerFactory;

import ch.epfl.gsn.Main;
import ch.epfl.gsn.beans.DataField;
import ch.epfl.gsn.beans.DataTypes;
import ch.epfl.gsn.beans.VSensorConfig;
import ch.epfl.gsn.monitoring.Anomaly;
import ch.epfl.gsn.storage.StorageManager;
import ch.epfl.gsn.vsensor.AbstractVirtualSensor;

import org.slf4j.Logger;

/*
 *  Each AbstractVirtualSensor will have a reference to its instance of AnomalyDetector
 */

public class AnomalyDetector implements Monitorable { 
    
    private static final transient Logger logger = LoggerFactory.getLogger (AnomalyDetector.class);
       
    /* Reference to the VirtualSensor this instance belongs to */
    private AbstractVirtualSensor sensor;
    
    /* OutputStructure of the VirtualSensor */
    private DataField [] fields;

    /* HashMap functions 
     *  Key : Function  
     *  Value : ArrayList of Anomaly on which the function has to be applied
     */
    private HashMap <String, ArrayList<Anomaly>> functions = new HashMap<String, ArrayList<Anomaly>>();   
    
    /* A list of supported functions*/
    private ArrayList <String> supportedFunctions = new ArrayList <String> () {{
        add("positive_outlier");
        add("negative_outlier");
        add("iqr");
        add("unique");
    }};
    
    // Constant flag for specifiying driection of outlier | Helps in code readability 
    private static final boolean POSITIVE = true;
    private static final boolean NEGATIVE = false;
    
    public AnomalyDetector (AbstractVirtualSensor sensor)  { 
        this.sensor = sensor;
        this.fields = sensor.getVirtualSensorConfiguration().getOutputStructure();
        initAnomalyParams(); 
    }    
    
    /* Parsing anomaly parameters from the VSP file configuration provided 
     *
     * Valid Key Format : anomaly.function.field[.groupByField] 
     *              WHERE groupByField is optional
     *  Function could be iqr, positive_outlier etc.
     * Valid Value format : interval[,value] 
     *              WHERE value is optional (needed for outliers)
     *                    interval = -1 if all the historical data is to be used
     *
     *  Value is the threshold and interval is the recent time window over which 
     *  we look for anomalies. Interval should be specified as for e.g. 5h (last 5 hours)
     *  or 3m (last 3 minutes)
     * */
    
    /*  TODO: ALL input should be parsed and validated here
    *   Parsing should not be left for runtime
    */
    private void initAnomalyParams() {
        
        TreeMap <String, String> params = sensor.getVirtualSensorConfiguration().
            getMainClassInitialParams();
        
        if (params == null)
            return;

        for (Map.Entry<String,String> entry : params.entrySet()) {
            
            
            String key = entry.getKey().trim();        // key should be of the form anomaly.function.field[.groupByField]
            String value = entry.getValue().trim();    // Value should be of the form interval[,value]
            
            if (!key.startsWith("anomaly."))                // For a parameter defined for anamaly, key (name) has to start with string "anomaly."             
                continue;
            
            String parts [] = key.split("\\.");             // Tokenizing String with '.'
            String function = null, fieldName=null, groupBy = null;
    
            if (parts.length < 3 || parts.length > 4) {
                logger.info("ANOMALY-PARSING-ERROR ["+key+"="+value+"] "
                    + "Key: " + key + " should be of the form anomaly.function.field[.groupByField]");
                continue; 
            }
            
            try {
                    
                function = parts[1];
                if (!supportedFunctions.contains(function)) {
                    logger.info ("ANOMALY-PARSING-ERROR ["+key+"="+value+"] "
                           + "Function: " + function + " not supported");
                    continue;
                }

                fieldName = parts[2]; 
                
                DataField field = getDataField(fieldName);
                
                if (field == null) {
                    logger.info ("ANOMALY-PARSING-ERROR ["+ key+"="+value+"] "
                        + "Field: " + fieldName + " is invalid"); 
                    continue;   // Move to next anomaly
                }
                 
                DataField groupByField = null;
                if (parts.length == 4) {
                    groupBy = parts[3];
                    groupByField = getDataField(groupBy);
                    
                    if (groupByField == null) {
                        logger.info ("ANOMALY-PARSING-ERROR ["+ key+"="+value+"] "
                            + "GroupBy: " + groupByField + " is invalid");                        
                        continue;
                    }                 
                }  
                String valParts [] = value.split(",");
                Anomaly anomaly;
                if (valParts.length == 1) // Threshold is not provided
                    anomaly = new Anomaly (function,field,valParts[0],groupByField);  //valParts[0] is the interval
                else if (valParts.length == 2) // valParts[0] is interval and valParts[1] is threshold value 
                    anomaly = new Anomaly (function,field,valParts[0], groupByField,valParts[1]);
                        
                else {
                    logger.info ("ANOMALY-PARSING-ERROR ["+ key+"="+value+"] "
                        + "Value: " + value + " should be of the form value,interval");        
                    break;
                }
                        
                if (!functions.containsKey(function)) { 
                    functions.put(function, new ArrayList <Anomaly>());
                }                            
                functions.get(function).add(anomaly);
                
                logger.info (anomaly.toString());
                  
            } catch (Exception e) { 
                
                logger.info ("ANOMALY-PARSING-ERROR ["+key+" : " +value+"]");
                logger.info (e.getMessage());
                continue;
            }

                    
        }   
    }
    
    private DataField getDataField (String fieldName) {
        
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getName().equals(fieldName))
                return fields[i];
        }
        
        return null;
    }

    /*
     *  Executes query for the positive or negative outlier and retrieves the no of anomalies
     */
    private void countOutliers (Hashtable <String, Object> stat, boolean direction) throws SQLException {
             
        if (direction == POSITIVE && !functions.containsKey("positive_outlier"))
            return;
        
        if (direction == NEGATIVE && !functions.containsKey("negative_outlier"))
            return;
        
        ArrayList <Anomaly> anomalies = null;

        if (direction == POSITIVE)
            anomalies = functions.get("positive_outlier");
        else
            anomalies = functions.get("negative_outlier");
        
        if (anomalies == null || anomalies.size() == 0) 
            return;
         
        StorageManager storageMan = Main.getStorage (sensor.getVirtualSensorConfiguration().getName()); 
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        // Get connection, process anomalies of this function, close connection
        // Any SQLException thrown is propogated up the stack 
        con = storageMan.getConnection();   
        
        Iterator <Anomaly> iterator = anomalies.iterator();
        while ( iterator.hasNext() ) {
            Anomaly anomaly = iterator.next();
            DataField field = anomaly.getField();
            String query;
            
            if (anomaly.isGroupBy())
                query = getOutlierQueryByGroup (sensor.getVirtualSensorConfiguration().getName().toLowerCase(), field,
                                                        anomaly.getGroupByField(), direction);
            else
                query = getOutlierQuery (sensor.getVirtualSensorConfiguration().getName().toLowerCase(), field,
                                                        direction);
            try {
                ps = con.prepareStatement(query);
                
                switch (field.getDataTypeID()) {
            
                    case DataTypes.DOUBLE:
                        ps.setDouble (1, Double.parseDouble(anomaly.getValue()));
                        break;
                    case DataTypes.SMALLINT:
                        ps.setShort(1, Short.parseShort (anomaly.getValue()));
                        break;
                    case DataTypes.INTEGER:
                        ps.setInt(1, Integer.parseInt (anomaly.getValue()));
                        break;
                    case DataTypes.FLOAT:
                        ps.setFloat(1, Float.parseFloat (anomaly.getValue()));
                        break;
                    default:
                        logger.info ("ANOMALY-EXECUTION-ERROR [anomaly."+ anomaly.getFunction()+"."+
                                field.getName()+"="+anomaly.getValue()+","+anomaly.getTimeStr()+"] "+ "Field: " + 
                                field.getName() + " datatype " + field.getDataTypeID() + " not stuppored");
                        iterator.remove();
                        continue;
            
                }    
                long timeStamp = this.getTimeStamp(anomaly.getTime());
                ps.setDouble(2, timeStamp); 
                rs = ps.executeQuery();
                
                if (anomaly.isGroupBy()) {
                    while (rs.next()) {
                        stat.put(this.getMetricName (anomaly) +"."+ rs.getString(1)+".gauge", rs.getString(2));
                    }
                    
                }
                else {
                    if (rs.next()) {    // Putting monitoring result for this anomaly in the HashMap stat
                        stat.put( this.getMetricName(anomaly)+".gauge", rs.getString(1));
                    }
                }



        
            } catch (NumberFormatException e) { // In case of number format exception, anomaly specification is erroneous, hence removing it
                //TODO: For other datatpyes, there could be different exception for formatting
                logger.info ("ANOMALY-EXECUTION-ERROR [anomaly."+ anomaly.getFunction()+"."+field.getName()+
                        "="+anomaly.getValue()+","+anomaly.getTimeStr()+"] "+ "Value: " + anomaly.getValue() + " Invalid number format");        
                iterator.remove();
                continue;
            }    
        
        
            finally {
                if (rs!= null)
                    rs.close();
                if (ps!=null)
                    ps.close();
            }

        }   
        
        con.close();
        
    }
     
    // Returns SQL query for outliers
    private String getOutlierQuery ( String tableName, DataField field, boolean direction ) {
         
        StringBuilder toReturn = new StringBuilder ();
        toReturn.append("SELECT COUNT(*) FROM ").append(tableName).append( " WHERE " + field.getName());
        
        // Outier could be positive or negative
        if (direction == POSITIVE)
            toReturn.append(" > ");
        else
            toReturn.append(" < ");

        toReturn.append("? AND timed > ?"); // ) as maxTable"
        
        return toReturn.toString();
    }

    private String getOutlierQueryByGroup (String tableName, DataField field, DataField groupBy, boolean direction) {
        
        StringBuilder toReturn = new StringBuilder ();
        toReturn.append("SELECT " + groupBy.getName() + ", COUNT(*) FROM ").append(tableName).append(" WHERE " + field.getName());
        
        if (direction == POSITIVE)
            toReturn.append(" > ");
        else
            toReturn.append(" < ");

        toReturn.append("? AND timed > ?");

        toReturn.append(" GROUP BY " + groupBy.getName());

        return toReturn.toString();
    
    }
    
    private void interQuartileRange (Hashtable <String, Object> stat) throws SQLException {
    
        if (!functions.containsKey("iqr"))
            return;

        ArrayList <Anomaly> anomalies = functions.get("iqr");
    
        // TODO: remove key iqr
        if (anomalies == null || anomalies.size() == 0)
            return;

        StorageManager storageMan = Main.getStorage(sensor.getVirtualSensorConfiguration().getName());
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        con = storageMan.getConnection();

        Iterator <Anomaly> iterator = anomalies.iterator();
        while (iterator.hasNext()) {
            Anomaly anomaly = iterator.next();
            DataField field = anomaly.getField();
            String query;

            if (anomaly.isGroupBy())
                query = getIQRQueryByGroup (sensor.getVirtualSensorConfiguration().getName().toLowerCase(),field,anomaly.getGroupByField());
            else
                query = getIQRQuery(sensor.getVirtualSensorConfiguration().getName().toLowerCase(), field);
            
            ps = con.prepareStatement(query);
            long timestamp = 0;
            //TODO: Improve IQR SQL Query
            timestamp = this.getTimeStamp(anomaly.getTime());
            ps.setDouble(1, timestamp);
            ps.setDouble(2, timestamp);
            ps.setDouble(3, timestamp);
            ps.setDouble(4, timestamp);   
                
            
            rs = ps.executeQuery();
            if (anomaly.isGroupBy()) {
                while (rs.next()) {
                    stat.put (this.getMetricName (anomaly) +"."+ rs.getString(1)+".gauge", rs.getString (2));
                }
            }
            else {
                if (rs.next()) {
                    stat.put(this.getMetricName(anomaly)+".gauge", rs.getString(1));
                }
            }
            
            if (rs!=null)
                rs.close();
            if (ps!=null)
                ps.close();
        }
        
        con.close();
    }
    
    //TODO: A complex IQR Query. It should be improved. 
    //TODO : Comment the query
    private String getIQRQuery (String tableName, DataField field) {
    
        StringBuilder toReturn = new StringBuilder ("SELECT ");
        toReturn.append("(SELECT " + field.getName() + " FROM ( ");
        toReturn.append("SELECT ROW_NUMBER() OVER (ORDER BY " + field.getName() + " ASC) AS ROWNUMBER, " + field.getName());
        toReturn.append(" FROM " + tableName + " WHERE timed > ?");
        toReturn.append(" ) AS q3");
        toReturn.append(" WHERE ROWNUMBER = (SELECT COUNT(*) FROM " + tableName + 
                " WHERE timed > ?)*75/100)");
        
        toReturn.append(" - ");
        
        toReturn.append("(SELECT " + field.getName() + " FROM ( ");
        toReturn.append("SELECT ROW_NUMBER() OVER (ORDER BY " + field.getName() + " ASC) AS ROWNUMBER, " + field.getName());
        toReturn.append(" FROM " + tableName + " WHERE timed > ?");
        toReturn.append(" ) AS q1");
        toReturn.append(" WHERE ROWNUMBER = (SELECT COUNT(*) FROM " + tableName + 
                " WHERE timed > ?) *25/100) AS IQR");
        
        return toReturn.toString();
    }
    
    private String getIQRQueryByGroup (String tableName, DataField field, DataField groupBy) {
        String fieldName = field.getName();
        String groupByName = groupBy.getName();

        StringBuilder toReturn = new StringBuilder ("SELECT q1." + groupByName + ", (q3." + fieldName +" - q1." + fieldName +") IQR ");
        toReturn.append ("FROM ");
        toReturn.append ("(SELECT doo." + fieldName + ", doo." + groupByName + ", doo.row ");
        toReturn.append ("FROM ");
        toReturn.append ("(SELECT ROW_NUMBER() OVER (PARTITION BY " + groupByName + " ORDER BY " + groupByName + "," 
                                                           + fieldName + ") as row, " + groupByName + ", " + fieldName);
        toReturn.append (" FROM " + tableName + " WHERE timed > ?) doo, ");
        toReturn.append ("(SELECT max(foo.row)*25/100 AS max, " + groupByName);
        toReturn.append (" FROM (SELECT ROW_NUMBER() OVER (PARTITION BY " + groupByName + " ORDER BY " + groupByName + "," 
                                                            + fieldName + ") as row, " + groupByName + ", " + fieldName);
        toReturn.append (" FROM " + tableName + " WHERE timed > ?) foo ");
        toReturn.append (" GROUP BY " + groupByName + ") boo ");
        toReturn.append ("WHERE doo." + groupByName + "=" + "boo." + groupByName);
        toReturn.append (" AND doo.row = boo.max) q1, ");
        
        toReturn.append ("(SELECT doo." + fieldName + ", doo." + groupByName + ", doo.row ");
        toReturn.append ("FROM ");
        toReturn.append ("(SELECT ROW_NUMBER() OVER (PARTITION BY " + groupByName + " ORDER BY " + groupByName + "," 
                                                           + fieldName + ") as row, " + groupByName + ", " + fieldName);
        toReturn.append (" FROM " + tableName + " WHERE timed > ?) doo, ");
        toReturn.append ("(SELECT max(foo.row)*75/100 AS max, " + groupByName);
        toReturn.append (" FROM (SELECT ROW_NUMBER() OVER (PARTITION BY " + groupByName + " ORDER BY " + groupByName + "," 
                                                            + fieldName + ") as row, " + groupByName + ", " + fieldName);
        toReturn.append (" FROM " + tableName + " WHERE timed > ?) foo ");
        toReturn.append (" GROUP BY " + groupByName + ") boo ");
        toReturn.append ("WHERE doo." + groupByName + "=" + "boo." + groupByName);
        toReturn.append (" AND doo.row = boo.max) q3 ");
        
        toReturn.append("WHERE q1." + groupByName + " = q3." +groupByName +";");
        
        return toReturn.toString();
    }


    // Count unique values of a field over a specified timed-interval
    private void countUnique (Hashtable <String, Object> stat) throws SQLException {

        if (!functions.containsKey("unique"))
            return;

        ArrayList <Anomaly> anomalies = functions.get("unique");

        // TODO: remove key iqr
        if (anomalies == null || anomalies.size() == 0) {
            functions.remove("unique");
            return;
        }

        StorageManager storageMan = Main.getStorage(sensor.getVirtualSensorConfiguration().getName());
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        con = storageMan.getConnection();

        Iterator <Anomaly> iterator = anomalies.iterator();
        while (iterator.hasNext()) {
            Anomaly anomaly = iterator.next();
            DataField field = anomaly.getField();
            String query;
            
            if (anomaly.isGroupBy())
                query = getUniqueQueryByGroup(sensor.getVirtualSensorConfiguration().getName().toLowerCase(), field, anomaly.getGroupByField());
            else
                query = getUniqueQuery(sensor.getVirtualSensorConfiguration().getName().toLowerCase(), field);
            
            ps = con.prepareStatement(query);
            long timeStamp = this.getTimeStamp(anomaly.getTime());
            //logger.info ("COUNT UNIQUE timestamp : " + timeStamp);
            ps.setLong(1,timeStamp);

            rs = ps.executeQuery();
            if (anomaly.isGroupBy()) {
                while (rs.next()) {
                    stat.put (this.getMetricName(anomaly) +"."+ rs.getString(1)+".gauge",rs.getString(2));
                }
            } 
            else {
                if(rs.next()) {
                    stat.put(this.getMetricName(anomaly)+".gauge", rs.getString(1));
                }   
            }

            if (rs!=null)
                rs.close();
            if (ps!=null)
                ps.close();
        }

        con.close();
    }
    
    // Generates SQL query for the number of unique values of a field
    private String getUniqueQuery ( String tableName, DataField field) {

        StringBuilder toReturn = new StringBuilder ("SELECT COUNT (*) FROM (");
        toReturn.append("SELECT DISTINCT " + field.getName() + " FROM ").append(tableName);
        toReturn.append(" WHERE timed > ?) AS maxTabel");

        return toReturn.toString();
    }
    
     
    private String getUniqueQueryByGroup ( String tableName, DataField field, DataField groupBy) {
    
        StringBuilder toReturn = new StringBuilder ("SELECT " + groupBy.getName()+", COUNT(*) ");
        toReturn.append("FROM ( SELECT DISTINCT " + field.getName() + ", station FROM ").append(tableName);
        toReturn.append(" WHERE timed > ?) AS temp ");
        toReturn.append("GROUP BY " + groupBy.getName());
        
        return toReturn.toString();
    } 
    
    // Returns the time stamp by subtractive the windowSize from the current time
    private long getTimeStamp (long windowSize) {
        
        // Counter intuitively, zero represents we will look into all the stored data for anomalies
        if (windowSize ==-1)
            return -1;

        return System.currentTimeMillis() - windowSize;
    }
    
    public String toString () {
    
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (Map.Entry <String, ArrayList <Anomaly>> entry : functions.entrySet ()) {
        
            String function = entry.getKey();
            ArrayList <Anomaly> anomalies = entry.getValue();
            
            sb.append(function);
            sb.append("\n");
            for (Anomaly anomaly : anomalies ) {
                sb.append("\t"); 
                sb.append(anomaly.toString());
                sb.append("\n"); 
            }
        }

        
        return sb.toString();
    
    }
    
    public String getMetricName (Anomaly anomaly) {
        
        StringBuilder sb = new StringBuilder ("vs."+sensor.getVirtualSensorConfiguration().getName().replaceAll("\\,","_" )+".output");
        sb.append(".anomaly." + anomaly.getFunction() + "." + anomaly.getField().getName());
        
        if (anomaly.getFunction().equals ("positive_outlier") || anomaly.getFunction().equals ("negative_outlier"))
            sb.append("." + anomaly.getValue().trim());
        
        if (anomaly.isGroupBy())
            sb.append("." + anomaly.getGroupByField().getName());

       
        return sb.toString();
        
    }

    public Hashtable <String, Object> getStatistics () {
    
        Hashtable <String, Object> stat = new Hashtable <String, Object>();
        try {
            countOutliers (stat, POSITIVE);
            countOutliers (stat, NEGATIVE);
            interQuartileRange(stat);
            countUnique(stat);
        } catch (SQLException e) { logger.error(e.getMessage(), e); } 
    
        return stat; 
    
    }
}

