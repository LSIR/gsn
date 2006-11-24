package gsn.wrappers.tinyos1x;

import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.wrappers.Wrapper;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.StringTokenizer;
import net.tinyos1x.message.Message;
import net.tinyos1x.message.MessageListener;
import net.tinyos1x.message.MoteIF;
import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS)<br>
 */
public class GenericTinyOS1xWrapper extends Wrapper implements MessageListener {
   
   private final transient Logger  logger                = Logger.getLogger( GenericTinyOS1xWrapper.class );
   
   /**
    * A flag showing whether there exist a new data or not. This flag is used
    * internally.
    */
   private boolean                 isConsumed            = true;
   
   private Message                 latestReceivedMessage = null;
   
   private MoteIF                  mote;
   
   /**
    * The varialbe <code>threadCounter</code> is just used for debugging
    * purposes.
    */
   private static int              threadCounter         = 0;
   
   private ArrayList < DataField > outputDataFields      = new ArrayList < DataField >( );
   
   private ArrayList < String >    getterMethodNames     = new ArrayList < String >( );
   
   public boolean initialize ( ) {
      AddressBean addressBean = getActiveAddressBean( );
      String host = addressBean.getPredicateValue( "host" );
      int port =-1;
      if (host ==null || host.trim( ).length( )==0) {
         logger.warn( "The >host< parameter is missing from the RemoteDS wrapper." );
         return false;
      }
      String portRaw = addressBean.getPredicateValue( "port" );
      if (portRaw ==null || portRaw.trim( ).length( )==0) {
         logger.warn( "The >port< parameter is missing from the RemoteDS wrapper." );
         return false;
      }
      try {
         port= Integer.parseInt( portRaw );
         if (port>65000 || port<=0)
            throw new Exception("Bad port No"+port);
      }catch (Exception e) {
         logger.warn( "The >port< parameter is not a valid integer for the RemoteDS wrapper." );
         return false;
      }
     
      int indexCounter = 1;
      logger.warn( "configuring the SerialForwader Wrapper using the fields described in the virtual sensor file : " );
      while ( true ) {
         StringBuilder builder = new StringBuilder( "field." ).append( indexCounter++ );
         String value = addressBean.getPredicateValue( builder.toString( ) );
         if ( value == null ) break;
         StringTokenizer stringTokenizer = new StringTokenizer( value , "|" );
         if ( stringTokenizer.countTokens( ) != 4 ) {
            logger.warn( "field dropped, the parameter : *" + value + "* need to have four parts each separated with | mark." );
            continue;
         }
         DataField dataField = new DataField( stringTokenizer.nextToken( ) , stringTokenizer.nextToken( ) , stringTokenizer.nextToken( ) );
         getterMethodNames.add( stringTokenizer.nextToken( ).trim( ) );
         logger.info( "Output field identified successfully in the serial forwarder wrapper with the following information" + dataField );
         outputDataFields.add( dataField );
      }
      
      if ( logger.isDebugEnabled( ) ) logger.debug( "The SFWrapperDS connects to the Serial Forwarder interface at *" + host + ":" + port + "*" );
      setName( "TinyOS-SerialForwarder-Thread:" + ( ++threadCounter ) );
      try {
         mote = new MoteIF( host , port );
         if ( mote == null ) { throw new Exception( "MoteIF initialization failed" ); }
      } catch ( Exception e ) {
         logger.error( e.getMessage( ) , e );
         return false;
      }
      mote.registerListener(  new GSNMessage( ) , this );
      return true;
   }
   
   public synchronized void messageReceived ( int to , Message m ) {
      latestReceivedMessage = m;
      isConsumed = false;
   }
   
   public ArrayList < DataField > getOutputFormat ( ) {
      return outputDataFields;
   }
   
   public void run ( ) {
      mote.start( );
      try {
         while ( isActive( ) ) {
            if ( listeners.isEmpty( ) || isConsumed ) continue;
            StreamElement streamElement = new StreamElement( getOutputFormat( ) , extractDataUsingFieldNames( latestReceivedMessage , getterMethodNames , getOutputFormat( ) ) ,
               System.currentTimeMillis( ) );
            postStreamElement( streamElement );
            isConsumed = true;
         }
      } catch ( Exception e ) {
         e.printStackTrace( );
         logger.error( e );
         logger.error( getName( ) + " Died." );
      }
   }
   
   private Serializable [ ] extractDataUsingFieldNames ( Message inputMessage , ArrayList < String > getterMethods , ArrayList < DataField > expectedDataFields ) throws IllegalArgumentException ,
      IllegalAccessException , InvocationTargetException {
      ArrayList < Serializable > toReturn = new ArrayList < Serializable >( );
      Method [ ] actualMethods = inputMessage.getClass( ).getMethods( );
      int indexCounter = 0;
      for ( String getterName : getterMethods )
         for ( Method method : actualMethods )
            if ( method.getName( ).equals( getterName ) ) {
               Serializable dataValue = ( Serializable ) method.invoke( inputMessage , new Object [ ] {} );
               DataField definedField = expectedDataFields.get( indexCounter++ );
               Number number;
               switch ( definedField.getDataTypeID( ) ) {
                  case DataTypes.SMALLINT :
                     number = ( Number ) dataValue;
                     toReturn.add( number.shortValue( ) );
                     break;
                  case DataTypes.INTEGER :
                     number = ( Number ) dataValue;
                     toReturn.add( number.intValue( ) );
                     break;
                  case DataTypes.DOUBLE :
                     number = ( Number ) dataValue;
                     toReturn.add( number.doubleValue( ) );
                     break;
                  case DataTypes.VARCHAR :
                  case DataTypes.CHAR :
                     toReturn.add( dataValue.toString( ) );
                     break;
                  case DataTypes.BIGINT :
                     number = ( Number ) dataValue;
                     toReturn.add( number.longValue( ) );
                     break;
                  case DataTypes.BINARY :
                     toReturn.add( ( byte [ ] ) dataValue );
                     break;
                  default :
                     logger.warn( "The return value of " + getterName + " is not convertable to the GSN compatible type !!." );
                     break;
               }
               continue;
            }
      return toReturn.toArray( new Serializable [ ] {} );
   }
   
   public synchronized void finalize ( ) {
      threadCounter--;
   }
   
   public String toString ( ) {
      return "SFWrapper Service Provider";
   }
}
