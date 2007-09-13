package gsn.vsensor;

import gsn.beans.DataTypes;
import gsn.beans.StreamElement;

import org.apache.log4j.Logger;

import java.io.Serializable;

import net.tinyos.packet.*;
import net.tinyos.util.*;
import net.tinyos.message.*;
import net.tinyos.tools.*;

/** 
 * This virtual sensor is used for accessing Sensorscope data with
 * MigMessageWrapper.
 */

public class SensorscopeVS extends AbstractVirtualSensor {
    
    private static final transient Logger logger = Logger.getLogger( SensorscopeVS.class );

    private static final int SAMPLING_TIME = 30000;
    private static final int NO_VALUE      = Short.MIN_VALUE;


    private static final String NTW_SENDER_ID = "NTWSENDERID";
    private static final String NTW_DISTANCE_TO_BTS = "NTWDISTTOBS";
    private static final String TSP_HOP_COUNT = "TSPHOPCOUNT";
    private static final String TSP_PACKET_SN = "TSPPACKETSN";
    private static final String REPORTER_ID = "REPORTERID";
    private static final String TIMESTAMP = "TIMESTAMP";
    private static final String RAIN_METER = "RAINMETER";
    private static final String WIND_SPEED = "WINDSPEED";
    private static final String WATERMARK = "WATERMARK";
    private static final String SOLAR_RADIATION = "SOLARRADIATION";
    private static final String AIR_TEMPERATURE = "AIRTEMPERATURE";
    private static final String AIR_HUMIDITY = "AIRHUMIDITY";
    private static final String SKIN_TEMPERATURE = "SKINTEMPERATURE";
    private static final String SOIL_MOISTURE = "SOILMOISTURE";
    private static final String WIND_DIRECTION = "WINDDIRECTION";
    private static final String FOO = "FOO";
    
    private static final String [ ] FIELD_NAMES = new String [ ] { 
            NTW_SENDER_ID, NTW_DISTANCE_TO_BTS, TSP_HOP_COUNT, TSP_PACKET_SN,
            REPORTER_ID, TIMESTAMP, RAIN_METER, WIND_SPEED, WATERMARK,
            SOLAR_RADIATION, AIR_TEMPERATURE, AIR_HUMIDITY, SKIN_TEMPERATURE,
            SOIL_MOISTURE, WIND_DIRECTION };

    public boolean initialize ( ) {
        return true;
    }

    /**
     * This method is called whenever there is new data coming to
     * virtual sensor.
     * In this case the data structure should apply the one used
     * in sensorscope.
     * If some data can't be read, then a value of smallest short
     * -32768 is returned for ints, shorts and doubles.
     */
    
    public void dataAvailable ( String inputStreamName , StreamElement data ) {
        Serializable[] dataFields = data.getData();
        short ntwSenderId = NO_VALUE;
        short ntwDistToBts = NO_VALUE;
        short tspHopCount = NO_VALUE;
        short tspPacketSn = NO_VALUE;
        short reporterId = NO_VALUE;
        long timestamp = NO_VALUE;
        double rainMeter = NO_VALUE;
        double windSpeed = NO_VALUE;
        double watermark = NO_VALUE;
        double solarRadiation = NO_VALUE;
        double airTemperature = NO_VALUE;
        double airHumidity = NO_VALUE;
        double skinTemperature = NO_VALUE;
        double soilMoisture = NO_VALUE;
        double windDirection = NO_VALUE;
        short foo = 0;
        
        // Air temperature is needed afterwards by watermark and humidity,
        // so it has to be calculated first
        int i = 0;
        for(String fieldName : data.getFieldNames()) {
            fieldName = fieldName.toUpperCase();
            if(fieldName.equals(AIR_TEMPERATURE)) {
                airTemperature = getTemperature((Integer) dataFields[i]);
            }
            i++;
        }
        i=0;
        for(String fieldName : data.getFieldNames()) {
            fieldName = fieldName.toUpperCase();
            if(fieldName.equals(NTW_SENDER_ID)) {
                ntwSenderId = (Short) dataFields[i];
            } else if(fieldName.equals(NTW_DISTANCE_TO_BTS)) {
                ntwDistToBts = (Short) dataFields[i];
            } else if(fieldName.equals(TSP_HOP_COUNT)) {
                tspHopCount = (Short) dataFields[i];
            } else if(fieldName.equals(TSP_PACKET_SN)) {
                tspPacketSn = (Short) dataFields[i];
            } else if(fieldName.equals(REPORTER_ID)) {
                reporterId = (Short) dataFields[i];
            } else if(fieldName.equals(TIMESTAMP)) {
                timestamp = (Long) dataFields[i];
            } else if(fieldName.equals(RAIN_METER)) {
                rainMeter = getRainMeter((Short) dataFields[i]);
            } else if(fieldName.equals(WIND_SPEED)) {
                windSpeed = getWindSpeed((Short) dataFields[i]);
            } else if(fieldName.equals(WATERMARK)) {
                watermark = getWatermark((Integer) dataFields[i], airTemperature);
            } else if(fieldName.equals(SOLAR_RADIATION)) {
                solarRadiation = getSolarRadiation((Integer) dataFields[i]);
            } else if(fieldName.equals(AIR_HUMIDITY)) {
                airHumidity = getHumidity((Integer) dataFields[i], airTemperature);
            } else if(fieldName.equals(SKIN_TEMPERATURE)) {
                skinTemperature = getTemperature((Integer) dataFields[i]);
            } else if(fieldName.equals(SOIL_MOISTURE)) {
                soilMoisture = getSoilMoisture((Integer) dataFields[i]);
            } else if(fieldName.equals(WIND_DIRECTION)) {
                windDirection = getWindDirection((Integer) dataFields[i]);
            } else if(fieldName.equals(FOO)) {
                foo = (Short) dataFields[i];
            }
            i++;
        }
        
        
        // foo is left out from the output
        StreamElement out = new StreamElement( FIELD_NAMES , 
            new Byte[ ] { 
                DataTypes.SMALLINT, DataTypes.SMALLINT, DataTypes.SMALLINT, DataTypes.SMALLINT,
                DataTypes.SMALLINT, DataTypes.BIGINT, DataTypes.DOUBLE, DataTypes.DOUBLE,
                DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.DOUBLE,
                DataTypes.DOUBLE, DataTypes.DOUBLE, DataTypes.DOUBLE } , 
            new Serializable [ ] { 
                ntwSenderId, ntwDistToBts, tspHopCount, tspPacketSn,
                reporterId, timestamp, rainMeter, windSpeed, watermark,
                solarRadiation, airTemperature, airHumidity, skinTemperature, 
                soilMoisture, windDirection } , 
            System.currentTimeMillis() );
        dataProduced( out );

    }
    
   public double getRainMeter ( short rawValue ) {
      return rawValue * 0.254;
   }
   
   public double getWatermark (int rawGot, double temperature) {
      double rawValue = rawGot * 1500.0 / 4095.0;
      
      if ( rawValue >= 200 && temperature != NO_VALUE ) {
         
         double p1 = -3.171e-23;
         double p2 = 1.868e-19;
         double p3 = -4.779e-16;
         double p4 = 6.957e-13;
         double p5 = -6.337e-10;
         double p6 = 3.735e-7;
         double p7 = -0.0001421;
         double p8 = 0.03357;
         double p9 = -4.463;
         double p10 = 258.4;
         
         double T = p1 * Math.pow( rawValue , 9 ) + p2 * Math.pow( rawValue , 8 ) + p3 * Math.pow( rawValue , 7 ) + p4 * Math.pow( rawValue , 6 ) + p5 * Math.pow( rawValue , 5 ) + p6
            * Math.pow( rawValue , 4 ) + p7 * Math.pow( rawValue , 3 ) + p8 * Math.pow( rawValue , 2 ) + p9 * rawValue + p10;
         
         T = Math.pow( 10 , T ) / 1000.0;
         
         if ( T <= 1 ) {
            T = -20.0 * ( T * ( 1.0 + 0.018 * ( temperature - 24.0 ) ) - 0.55 );
         } else if ( T <= 8 ) {
            T = ( -3.213 * T - 4.093 ) / ( 1.0 - 0.009733 * T - 0.01205 * temperature );
         } else {
            T = -2.246 - 5.239 * T * ( 1.0 + 0.018 * ( temperature - 24.0 ) ) - 0.06756 * T * T * Math.pow( 1.0 + 0.018 * ( temperature - 24.0 ) , 2 );
         }
         return T;
      } else {
         return NO_VALUE;
      }
   }

    public double getSoilMoisture (int rawValue) {
      if ( rawValue >= 400 ) {
         return ( ( ( rawValue * 2.5 * 1.7 ) / 4095.0 ) - 0.4 ) * 100.0;
      } else {
         return NO_VALUE;
      }
    }

    public double getSolarRadiation (int rawValue) {
      return ( ( rawValue * 2.5 * 1.4545 ) / 4095.0 ) * 1000.0 / 1.67;
    }

    public double getWindDirection (int rawValue) {
      return ( ( rawValue * 2.5 * 1.4545 ) / 4095.0 ) * 360.0 / 3.3;
    }

    public double getWindSpeed (short rawValue) {
      return ( rawValue * 2250.0 / SAMPLING_TIME ) * 1.609 * 1000.0 / 3600.0;
    }

    public double getTemperature (int rawValue) {
      if ( rawValue != 0 ) {
         return ( rawValue * 0.01 ) - 39.6;
      } else {
         return NO_VALUE;
      }
    }

    public double getHumidity (int rawValue, double temperature) {
      if ( rawValue != 0 && temperature != NO_VALUE ) {
         return ( ( rawValue * 0.0405 ) - 4.0 - ( 0.0000028 * rawValue * rawValue ) ) + ( ( ( rawValue * 0.00008 ) + 0.01 ) * ( temperature - 25.0 ) );
      } else {
         return NO_VALUE;
      }
    }
   
    public void finalize ( ) {

    }
    
}