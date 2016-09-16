package ch.epfl.gsn.monitoring;

import org.slf4j.LoggerFactory;

import ch.epfl.gsn.beans.DataField;

import org.slf4j.Logger;

/*  Each virtual sensor could have multiple anomalies
 *  
 *  Each anomaly object consists of:
 *      function to be applied
 *      field on which it is applied 
 *      groupBy clause (Optional)      
 *      time interval window over which this anomaly has to be detected
 *      (anomalies would be calculated over entire historical data if an erroneous value is provided)
 *      threshold value (Optional)
 */

public class Anomaly {
    
    private static final transient Logger logger = LoggerFactory.getLogger (Anomaly.class);    
    
    // Field which this anomaly represents 
    private DataField field;
    
    // Threshold value
    private String value;

    // Function to be applied (for e.g. positive_outlier, negative_outlier, iqr, unique  etc.)
    private String function;
    
    // Time interval window
    private long time;
    
    private String timeStr;         // Storing original timeStr to provide feedback in case of error
    
    private boolean groupBy;        // If anomaly specs have a groupBy field specified        
    
    private DataField groupByField; // groupByField


    public Anomaly() {}
    
    public Anomaly (String function, DataField field, String timeStr, DataField groupByField) {
        this.function = function;
        this.field = field;
        this.timeStr = timeStr;
        this.time = this.parseTime(timeStr);
        this.groupByField = groupByField;
        
        if (groupByField != null)
            this.groupBy = true;
    }

    public Anomaly ( String function, DataField field, String timeStr, DataField groupByField ,String value) {
        
        this (function, field, timeStr, groupByField);
        this.value= value;
        

        this.time = this.parseTime(timeStr);
    }

    /*  Parses time. Accepted formal : value.timespecifer
    *    timespecifier belongs to { m, h, d}
    */
    private long parseTime (String timeStr) {
    
        
        timeStr = timeStr.trim();
        
        if (timeStr.equals("-1"))    //timeStr == -1 means anomaly has to be detected over all the historical data
            return -1;
        
        long toReturn = 0;
        
        // timeSpecifer could be m (minutes) , h (hours) or d (days)
        Character timeSpecifier = timeStr.toLowerCase().charAt(timeStr.length()-1);

        double rawTime = 0;

        switch (timeSpecifier) {
        
            case 'm':
                //Converting minutes to milliseconds
                rawTime = Double.parseDouble(timeStr.substring(0,timeStr.length()-1)); 
                toReturn = (long)(rawTime * 60 * 1000);
                break;
            case 'h':
                // Converting hours ti milliseconds
                rawTime = Double.parseDouble(timeStr.substring(0,timeStr.length()-1));
                toReturn = (long)(rawTime * 60* 60 * 1000);
                break;
            case 'd':
                // Converting days to milliseconds
                rawTime = Double.parseDouble(timeStr.substring(0,timeStr.length()-1));                
                toReturn = (long)(rawTime * 24 * 60 * 60 * 1000); 
                break;
            default:
                toReturn = -1;
                logger.info ("Invalid time specifer in anomaly: " + this.toString() 
                        + ". Anomaly would be detected over entire data\nValid time specifiers include m, d and h preceeded by a number");
        }
    
        return toReturn; 
    }
    
    // Getters
    public DataField getField () { return field; }
    public String getValue () { return value; }
    public String getFunction () { return function; }
    public long getTime () { return time;} 
    public String getTimeStr () { return timeStr;}   
    public boolean isGroupBy () { return groupBy; } 
    public DataField getGroupByField () { return this.groupByField; } 

    public String toString () {
    
        StringBuilder sb = new StringBuilder();
        
        sb.append("anomaly." + this.function + "." + this.field.getName() + "=");

        if (this.timeStr != null)
            sb.append(this.timeStr);
        if (this.value!= null)
            sb.append("," + this.value);

        if (this.isGroupBy())
           sb.append(",["+ this.groupByField.getName() + "]"); 
        return sb.toString();
    }
}
