package gsn.wrappers.tinyos2x.sensorscope;

import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.vsensor.Container;
import gsn.wrappers.AbstractStreamProducer;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;

import net.tinyos.message.Message;
import net.tinyos.message.MessageListener;
import net.tinyos.message.MoteIF;
import net.tinyos.packet.BuildSource;
import net.tinyos.packet.PhoenixSource;

import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS)<br>
 */
public class SensorScopeWrapper extends AbstractStreamProducer implements MessageListener {
   
   private final transient Logger logger        = Logger.getLogger( SensorScopeWrapper.class );
   
   private MoteIF                 moteif;
   
   /**
    * The varialbe <code>threadCounter</code> is just used for debugging
    * purposes.
    */
   private static int             threadCounter = 0;
   
   public boolean initialize ( TreeMap initialContext ) {
      AddressBean addressBean = ( AddressBean ) initialContext.get( Container.STREAM_SOURCE_ACTIVE_ADDRESS_BEAN );
      String host = addressBean.getPredicateValue( "host" );
      int port;
      if ( host == null || host.trim( ).length( ) == 0 ) {
         logger.warn( "The >host< parameter is missing from the RemoteDS wrapper." );
         return false;
      }
      String portRaw = addressBean.getPredicateValue( "port" );
      if ( portRaw == null || portRaw.trim( ).length( ) == 0 ) {
         logger.warn( "The >port< parameter is missing from the RemoteDS wrapper." );
         return false;
      }
      try {
         port = Integer.parseInt( portRaw );
         if ( port > 65000 || port <= 0 ) throw new Exception( "Bad port No" + port );
      } catch ( Exception e ) {
         logger.warn( "The >port< parameter is not a valid integer for the RemoteDS wrapper." );
         return false;
      }
      if ( logger.isDebugEnabled( ) ) {
         logger.debug( "The SFWrapperDS connects to the Serial Forwarder interface at *" + host + ":" + port + "*" );
      }
      setName( "TinyOS2-SensorScope-Thread:" + ( ++threadCounter ) );
      try {
         PhoenixSource reader = BuildSource.makePhoenix( BuildSource.makeSF( host , port ) , null );
         reader.start( );
         moteif = new MoteIF( reader );
      } catch ( Exception e ) {
         logger.error( e.getMessage( ) , e );
         return false;
      }
      moteif.registerListener( new SensorScopeDataMsg( ) , this );
      return true;
   }
   
   public synchronized void finalize ( HashMap context ) {
      super.finalize( context );
      moteif.deregisterListener( new SensorScopeDataMsg( ) , this );
      threadCounter--;
   }
   
   public String toString ( ) {
      return "Serial Forwarder Wrapper for Sensor Scope Project at EPFL.";
   }
   
   public void messageReceived ( int toAddr , Message message ) {
      SensorScopeDataMsgWrapper parsed = new SensorScopeDataMsgWrapper( ( SensorScopeDataMsg ) message );
      if ( isActive( ) ) if ( !listeners.isEmpty( ) ) postStreamElement( parsed.getStreamElement( ) );
   }
   
   public Collection < DataField > getOutputFormat ( ) {
      return SensorScopeDataMsgWrapper.getStructure( );
   }
}

class SensorScopeDataMsgWrapper {
   
   private static final int               SAMPLING_TIME = 30000;
   
   private static final int               NO_VALUE      = Integer.MAX_VALUE;
   
   private static ArrayList < DataField > structure     = null;
   
   private SensorScopeDataMsg             msg;
   
   public SensorScopeDataMsgWrapper ( byte [ ] packet , int offset ) {
      msg = new SensorScopeDataMsg( packet , offset );
   }
   
   public SensorScopeDataMsgWrapper ( SensorScopeDataMsg message ) {
      msg = message;
   }
   
   public static final ArrayList < DataField > getStructure ( ) {
      if ( structure == null ) {
         structure = new ArrayList < DataField >( );
         structure.add( new DataField( "nodeId" , DataTypes.INTEGER_NAME , "Node ID" ) );
         structure.add( new DataField( "sequenceNumber" , DataTypes.INTEGER_NAME , "Sequence Number" ) );
         structure.add( new DataField( "temperature" , DataTypes.DOUBLE_NAME , "Ambient Temperature (°C)" ) );
         structure.add( new DataField( "surfaceTemperature" , DataTypes.DOUBLE_NAME , "Surface Temperature (°C)" ) );
         structure.add( new DataField( "solarRadiation" , DataTypes.DOUBLE_NAME , "Solar Radiation (W/m²)" ) );
         structure.add( new DataField( "relativeHumidity" , DataTypes.DOUBLE_NAME , "Relative Humidity (%)" ) );
         structure.add( new DataField( "soilMoisture" , DataTypes.DOUBLE_NAME , "Soil Moisture (%)" ) );
         structure.add( new DataField( "watermark" , DataTypes.DOUBLE_NAME , "Watermark (kPa)" ) );
         structure.add( new DataField( "rainMeter" , DataTypes.DOUBLE_NAME , "Rain Meter (mm)" ) );
         structure.add( new DataField( "windSpeed" , DataTypes.DOUBLE_NAME , "Wind Speed (m/s)" ) );
         structure.add( new DataField( "windDirection" , DataTypes.DOUBLE_NAME , "Wind Direction (°)" ) );
      }
      return structure;
   }
   
   public StreamElement getStreamElement ( ) {
      return new StreamElement( getStructure( ) , new Serializable [ ] { getNodeID( ) , getSequenceNumber( ) , getTemperature( ) , getInfrared( ) , getSolarRadiation( ) , getHumidity( ) ,
            getSoilMoisture( ) , getWatermark( ) , getRainMeter( ) , getWindSpeed( ) , getWindDirection( ) } , System.currentTimeMillis( ) );
   }
   
   public int getSequenceNumber ( ) {
      return msg.get_counter( );
   }
   
   public double getRainMeter ( ) {
      double rawValue = msg.get_rainmeter( );
      return rawValue * 0.254;
   }
   
   public double getWatermark ( ) {
      double rawValue = ( double ) msg.get_watermark( ) * 1500.0 / 4095.0;
      double temperature = getTemperature( );
      
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
   
   public double getSoilMoisture ( ) {
      double rawValue = msg.get_soilmoisture( );
      if ( rawValue >= 400 ) {
         return ( ( ( rawValue * 2.5 * 1.7 ) / 4095.0 ) - 0.4 ) * 100.0;
      } else {
         return NO_VALUE;
      }
   }
   
   public double getSolarRadiation ( ) {
      double rawValue = msg.get_solarradiation( );
      return ( ( rawValue * 2.5 * 1.4545 ) / 4095.0 ) * 1000.0 / 1.67;
   }
   
   public double getWindDirection ( ) {
      double rawValue = msg.get_winddirection( );
      return ( ( rawValue * 2.5 * 1.4545 ) / 4095.0 ) * 360.0 / 3.3;
   }
   
   public double getWindSpeed ( ) {
      double rawValue = msg.get_windspeed( );
      return ( rawValue * 2250.0 / SAMPLING_TIME ) * 1.609 * 1000.0 / 3600.0;
   }
   
   public double getTemperature ( ) {
      double rawValue = msg.get_temperature( );
      if ( rawValue != 0 ) {
         return ( rawValue * 0.01 ) - 39.6;
      } else {
         return NO_VALUE;
      }
   }
   
   public double getHumidity ( ) {
      double rawValue = msg.get_humidity( );
      double temperature = getTemperature( );
      if ( rawValue != 0 && temperature != NO_VALUE ) {
         return ( ( rawValue * 0.0405 ) - 4.0 - ( 0.0000028 * rawValue * rawValue ) ) + ( ( ( rawValue * 0.00008 ) + 0.01 ) * ( temperature - 25.0 ) );
      } else {
         return NO_VALUE;
      }
   }
   
   public int getNodeID ( ) {
      return msg.get_nodeid( );
   }
   
   public double getInfrared ( ) {
      double rawValue = msg.get_infraredtmp( );
      if ( rawValue != 0 ) {
         return ( rawValue / 16.0 ) - 273.15;
      } else {
         return NO_VALUE;
      }
   }
   
   public void printMsg ( ) {
      DecimalFormat df = new DecimalFormat( "0.00" );
      double temperature;
      double tmp;
      
      System.out.println( "<<<<< New DATA message received from NODE " + msg.get_nodeid( ) + " >>>>>" );
      System.out.println( "Arrival Timee:\t\t\t" + new java.util.Date( ) );
      System.out.println( "Sequence Number:\t\t" + getSequenceNumber( ) );
      
      System.out.print( "Ambient Temperature (°C):\t" );
      if ( ( temperature = getTemperature( ) ) != NO_VALUE ) {
         System.out.println( df.format( temperature ) );
      } else {
         System.out.println( "n/a" );
      }
      
      System.out.print( "Surface Temperature (°C):\t" );
      if ( ( tmp = getInfrared( ) ) != NO_VALUE ) {
         System.out.println( df.format( tmp ) );
      } else {
         System.out.println( "n/a" );
      }
      
      System.out.println( "Solar Radiation (W/m²):\t\t" + df.format( getSolarRadiation( ) ) );
      
      System.out.print( "Relative Humidity (%):\t\t" );
      if ( ( tmp = getHumidity( ) ) != NO_VALUE ) {
         System.out.println( df.format( tmp ) );
      } else {
         System.out.println( "n/a" );
      }
      
      System.out.print( "Soil Moisture (%):\t\t" );
      if ( ( tmp = getSoilMoisture( ) ) != NO_VALUE ) {
         System.out.println( df.format( tmp ) );
      } else {
         System.out.println( "n/a" );
      }
      
      System.out.print( "Watermark (kPa):\t\t" );
      if ( ( tmp = getWatermark( ) ) != NO_VALUE ) {
         System.out.println( df.format( tmp ) );
      } else {
         System.out.println( "n/a" );
      }
      
      System.out.println( "Rain Meter (mm):\t\t" + getRainMeter( ) );
      System.out.println( "Wind Speed (m/s):\t\t" + df.format( getWindSpeed( ) ) );
      System.out.println( "Wind Direction (°):\t\t" + df.format( getWindDirection( ) ) );
      
      System.out.println( );
      System.out.println( );
   }
}
