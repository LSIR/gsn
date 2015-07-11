package gsn.monitoring;

import gsn.Main;
import gsn.storage.StorageManager;
import gsn.vsensor.AbstractVirtualSensor;
import gsn.beans.VSensorConfig;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.monitoring.Anomaly;

import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Iterator;
import java.sql.*;

import org.apache.log4j.Logger;

/*
 *  Each AbstractVirtualSensor will have a reference to its instance of AnomalyDetector
 *  
 */
public class AnomalyDetector implements Monitorable { 
    
    private static final transient Logger logger = Logger.getLogger (AnomalyDetector.class);
       
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
     * Valid Key Format : anomaly.function.field
     *  Where function could be iqr, positive_outlier etc.
     * Valid Value format : value,interval
     *  Where value is the threshold and interval is the recent time window over which 
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
            
            
            String key = entry.getKey().trim();        // key should be of the form anomaly.function.field
            String value = entry.getValue().trim();    // Value should be of the form value,interval
            
            if (!key.startsWith("anomaly."))           // For a parameter defined for anamaly, key (name) has to start with string "anomaly."             
                continue;
                
            String parts [] = key.split("\\.");
            String function = null, fieldName=null;    
            
            try {
                    
                function = parts[1];
                if (!supportedFunctions.contains(function)) {
                    logger.info ("ANOMALY-PARSING-ERROR ["+key+"="+value+"] "
                           + "Function: " + function + " not supported");
                    continue;
                }

                fieldName = parts[2]; 
                
                int i = 0;
                // If field matches, we parse the value and add an Anomaly object to functions
                for (i = 0; i < fields.length; i++) { 
                    if (fields[i].getName().equals(fieldName)) {

                        String valParts [] = value.split(",");
                        Anomaly anomaly;
                        
                        if (valParts.length == 1) // Interval is not provided, anomalies would be detected over the entire period
                            anomaly = new Anomaly (function,fields[i],value);

                        else if (valParts.length == 2) // valParts[0] is value and valParts[1] is interval 
                            anomaly = new Anomaly (function,fields[i],valParts[0], valParts[1]);
                        
                        else {
                            logger.info ("ANOMALY-PARSING-ERROR ["+ key+"="+value+"] "
                                    + "Value: " + value + " should be of the form value,interval");        
                            break;
                        }
                        
                        if (!functions.containsKey(function)) { 
                            functions.put(function, new ArrayList <Anomaly>());
                        }                            
                        functions.get(function).add(anomaly);
                        break;
                    }
                }
                // If field is not found in the output structure of the VS, logging the parsing error
                if ( i == fields.length )
                    logger.info ("ANOMALY-PARSING-ERROR ["+ key+"="+value+"] "
                        + "Field: " + fieldName + " is invalid");
            } catch (ArrayIndexOutOfBoundsException e) {
                    logger.info ("ANOMALY-PARSING-ERROR ["+ key+"="+value+"] "
                            + "Key: " + key + " should be of the form anomaly.function.field");
                continue;
            }

                    
        }   
    }
    
    /*
     *  Retrieve each anomaly against outlier (positive OR negative) function
     *  Create query for the anomaly and retrieve the no of anomalies
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
            
            String query = getOutlierQuery (sensor.getVirtualSensorConfiguration().getName().toLowerCase(), field, direction);
            try {
                ps = con.prepareStatement(query);
                
                switch (field.getDataTypeID()) {
            
                    case DataTypes.DOUBLE:
                        ps.setDouble (1, Double.parseDouble(anomaly.getValue()));
                        ps.setDouble(2, this.getTimeStamp(anomaly.getTime()));

                        break;

                    default:
                        logger.info ("ANOMALY-EXECUTION-ERROR [anomaly."+ anomaly.getFunction()+"."+
                                field.getName()+"="+anomaly.getValue()+","+anomaly.getTimeStr()+"] "+ "Field: " + 
                                field.getName() + " datatype " + field.getDataTypeID() + " not stuppored");
                        iterator.remove();
                        continue;
            
                }   
                
                rs = ps.executeQuery();

                if (rs.next()) {    // Putting monitoring result for this anomaly in the HashMap stat
                    stat.put ("anomaly.vs." + sensor.getVirtualSensorConfiguration().getName().replaceAll("\\,","_") + 
                            "." + anomaly.getFunction()+"." + field.getName() + "." + anomaly.getValue().trim() + ".count" , rs.getLong(1)); 
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
    
        StringBuilder toReturn = new StringBuilder ("SELECT COUNT (*) FROM (");
        toReturn.append("SELECT * FROM ").append(tableName).append( " WHERE " + field.getName());
        
        // Outier could be positive or negative
        if (direction == POSITIVE)
            toReturn.append(" > ");
        else
            toReturn.append(" < ");

        toReturn.append("? AND timed > ?) AS maxTabel");
        
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

            String query = getIQRQuery(sensor.getVirtualSensorConfiguration().getName().toLowerCase(), field);
            try {
                ps = con.prepareStatement(query);
                long timestamp = 0;
                switch (field.getDataTypeID()) {
                
                    case DataTypes.DOUBLE:
                        //TODO: Improve IQR SQL Query
                        timestamp = this.getTimeStamp(anomaly.getTime());
                        ps.setDouble(1, timestamp);
                        ps.setDouble(2, timestamp);
                        ps.setDouble(3, timestamp);
                        ps.setDouble(4,timestamp);
                        break;    
                    
                    default: 
                        logger.info ("ANOMALY-EXECUTION-ERROR [anomaly."+ anomaly.getFunction()+"."+
                            field.getName()+"="+anomaly.getValue()+","+anomaly.getTimeStr()+"] "+ "Field: " +
                            field.getName() + " datatype " + field.getDataTypeID() + " not stuppored");
                        iterator.remove();
                        continue;
                
                }
            
                rs = ps.executeQuery();
                
                if (rs.next()) {
                    stat.put ("anomaly.vs." +sensor.getVirtualSensorConfiguration().getName().replaceAll("\\,","_")  
                        +"."+anomaly.getFunction()+"."+ field.getName() +"." + anomaly.getValue().trim() +".iqr" , rs.getLong(1));
                }
            } catch (NumberFormatException e) {
                //TODO: For other datatpyes, there could be different exception for formatting
                logger.info ("ANOMALY-EXECUTION-ERROR [anomaly."+ anomaly.getFunction()+"."+field.getName()+
                        "="+anomaly.getValue()+","+anomaly.getTimeStr()+"] "+ "Value: " + anomaly.getValue() + " Invalid number format");
                iterator.remove();
                continue;
            }
            
            finally {
                if (rs!=null)
                    rs.close();
                if (ps!=null)
                    ps.close();
            } 
        }
        
        con.close();
    }
    
    //TODO: A complex IQR Query. It should be improved. 
    //TODO : Comment the query
    private String getIQRQuery (String tableName, DataField field) {
    
        StringBuilder toReturn = new StringBuilder ("SELECT ");
        toReturn.append("(SELECT " + field.getName() + " FROM ( ");
        toReturn.append("SELECT ROW_NUMBER() OVER (ORDER BY " + field.getName() + " ASC) AS ROWNUMBER, " + field.getName());
        toReturn.append(" FROM (SELECT * FROM " + tableName + " WHERE timed > ?) AS window_interval");
        toReturn.append(" ) AS q3");
        toReturn.append(" WHERE ROWNUMBER = (SELECT COUNT(*) FROM (SELECT * FROM " + tableName + 
                " WHERE timed > ?) AS window_interval)*75/100)");
        
        toReturn.append(" - ");
        
        toReturn.append("(SELECT " + field.getName() + " FROM ( ");
        toReturn.append("SELECT ROW_NUMBER() OVER (ORDER BY " + field.getName() + " ASC) AS ROWNUMBER, " + field.getName());
        toReturn.append(" FROM (SELECT * FROM " + tableName + " WHERE timed > ?) AS window_interval");
        toReturn.append(" ) AS q3");
        toReturn.append(" WHERE ROWNUMBER = (SELECT COUNT(*) FROM (SELECT * FROM " + tableName + 
                " WHERE timed > ?) AS window_interval)*25/100) AS IQR");
        
        return toReturn.toString();
    }
     
    // Returns the time stamp by subtractive the windowSize from the current time
    private long getTimeStamp (long windowSize) {
        
        // Counter intuitively, zero represents we will look into all the stored data for anomalies
        if (windowSize ==0)
            return 0;

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

    public Hashtable <String, Object> getStatistics () {
    
        Hashtable <String, Object> stat = new Hashtable <String, Object>();

        try {
            countOutliers (stat, POSITIVE);
            countOutliers (stat, NEGATIVE);
            interQuartileRange(stat);

        } catch (SQLException e) { e.printStackTrace(); } 
    
        return stat; 
    
    }
}

