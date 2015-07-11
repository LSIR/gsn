package gsn.monitoring;

import gsn.beans.DataField;

import org.apache.log4j.Logger;

/* Each virtual sensor might have multiple anomalies specified 
 *  
 *  Each anomaly object consists of:
 *      field on which it is applied, 
 *      the threshold value,
 *      the time interval window over which this anomaly has to be detected
 *      and the function
 */

public class Anomaly {
    
    private static final transient Logger logger = Logger.getLogger (Anomaly.class);    
    
    // Field which this anomaly represents 
    private DataField field;
    
    // Threshold value
    private String value;

    // Function to be applied (for e.g. positive_outlier, negative_outlier etc.)
    private String function;
    
    // Time interval window
    private long time;
    
    private String timeStr;         // Storing original timeStr to provide feedback in case of error
    
    public Anomaly() {}
    
    public Anomaly (String function, DataField field, String value) {
        this.function = function;
        this.field = field;
        this.value = value;
        this.time = 0;              // Counter intuitively, Time = 0 means anomaly would be detected over the entire data history
    }


    public Anomaly ( String function, DataField field, String value, String timeStr) {
        
        this (function, field, value);
        this.timeStr = timeStr;
        

        this.time = this.parseTime(timeStr);
    }

    /*  Parses time. Accepted formal : value.timespecifer
    *    timespecifier belongs to { m, h, d}
    */
    private long parseTime (String timeStr) {
    
        
        timeStr = timeStr.trim();
        long toReturn = 0;
        
        // timeSpecifer could be m (minutes) , h (hours) or d (days)
        Character timeSpecifier = timeStr.toLowerCase().charAt(timeStr.length()-1);
        String timeValue = timeStr.substring(0,timeStr.length()-1);

        double rawTime = Double.parseDouble(timeValue);

        switch (timeSpecifier) {
        
            case 'm':
                //Converting minutes to milliseconds
                toReturn = (long)(rawTime * 60 * 1000);
                break;
            case 'h':
                // Converting hours ti milliseconds
                toReturn = (long)(rawTime * 60* 60 * 1000);
                break;
            case 'd':
                // Converting days to milliseconds
                toReturn = (long)(rawTime * 24 * 60 * 60 * 1000); 
                break;
            default:
                toReturn = 0;
                logger.info ("Invalid time specifer in anomaly: " + this.toString() 
                        + ". Setting time to 0\nValid time format include m, d and h");
        
        
        }
    
        return toReturn; 
    }


    
    
    // Getters

    public DataField getField () { return field; }
    public String getValue () { return value; }
    public String getFunction () { return function; }
    public long getTime () { return time;} 
    public String getTimeStr () { return timeStr;}   
     
    public String toString () {
    
        StringBuilder sb = new StringBuilder();
        
        sb.append("anomaly." + this.function + "." + this.field.getName() + "=" + this.value);

        if (this.timeStr != null)
            sb.append(","+ this.timeStr);
    
        return sb.toString();
    }
}
