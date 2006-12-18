package gsn;

import java.util.Date;
import java.util.Vector;
import org.apache.log4j.Logger;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.notifications.GSNNotification;
import gsn.registry.MyConfig;
import gsn.registry.RequestInitializableRequestProcessor;
import gsn.storage.StorageManager;
import gsn.wrappers.RemoteWrapper;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * Creation time : Dec 8, 2006@11:15:01 AM<br> *
 */
public class GSNRequestHandler implements RequestInitializableRequestProcessor {
   
   private static transient Logger logger = Logger.getLogger( GSNRequestHandler.class );
   
   private String                  remoteAddress;
   
   public void init ( MyConfig pConfig ) {
      this.remoteAddress = pConfig.getRemoteAddress( );
   }
   
   public boolean deliverData ( int notificationCode ,  Object [] fieldNamesInStreamElement ,  Object [] fieldValues , String timeStampStr ) {
      long timeStamp = Long.parseLong( timeStampStr );
      RemoteWrapper remoteWrapper = Mappings.getContainer( ).getRemoteDSForANotificationCode( notificationCode );
      if ( remoteWrapper == null ) { // This client is no more interested
         // in this notificationCode.
         if ( logger.isInfoEnabled( ) ) logger.info( "Invalid notification code recieved, query droped." );
         return false;
      } else {
         // /**
         // * If the received stream element from remote stream producer
         // * doesn't have the timed field inside, the<br>
         // * container will automatically insert the receive time to the
         // * stream element.<br>
         // */
         if ( timeStamp <= 0 ) timeStamp = System.currentTimeMillis( );
         StorageManager.getInstance( ).insertData(Main.tableNameGeneratorInString( notificationCode ).toString( ) ,
            StreamElement.createElementFromXMLRPC(remoteWrapper.getOutputFormat( ), fieldNamesInStreamElement , fieldValues , timeStamp ) );
         if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( ).append( "data received for notification code *" ).append( notificationCode ).toString( ) );
         remoteWrapper.remoteDataReceived( );
         return true;
      }
   }
   
   public boolean registerQuery ( int port , String virtualSensorName , String query , int notificationCode ) {
      if ( virtualSensorName == null || ( virtualSensorName = virtualSensorName.trim( ).toLowerCase( ) ).length( ) == 0 ) {
         logger.warn( "Bad request received for Data_strctutes" );
         return false;
      }
      VSensorConfig sensorConfig = Mappings.getVSensorConfig( virtualSensorName );
      if ( sensorConfig == null ) {
         logger.info( "Request received for unknown v-sensor : " + virtualSensorName );
         return false;
      }
      GSNNotification interest = new GSNNotification( port , remoteAddress , virtualSensorName , query , notificationCode );
      if ( logger.isInfoEnabled( ) )
         logger.info( new StringBuilder( ).append( "REGISTER REQUEST FOR " ).append( virtualSensorName ).append( " received from :" ).append( interest.getRemoteAddress( ) ).append( ":" ).append(
                  interest.getRemotePort( ) ).append( " under the code " ).append( notificationCode ).append( " With the query of *" ).append( query ).append( "*" )
                     .toString( ) );
      Mappings.getContainer( ).addNotificationRequest( virtualSensorName , interest );
      if ( logger.isDebugEnabled( ) ) logger.debug( "Respond sent to the requestee." );
      return true;
   }
   
   /**
    * I want to drop this method and use deliverData returning false for
    * removing the query.
    * 
    * @param notificationCode
    * @return
    */
   public boolean removeQuery ( int notificationCode ) {
      // if ( logger.isInfoEnabled( ) ) logger.info( new StringBuilder(
      // ).append( "DEREGISTER REQUEST WITH CODE :" ).append( notificationCode
      // ).append( " received" ).toString( ) );
      // Mappings.getContainer( ).removeNotificationRequest(notificationCode );
      // if ( logger.isDebugEnabled( ) ) logger.debug( "Respond sent to the
      // requestee." );
      
      return true;
   }
   
   /**
    * Empty structure means that the virtualSensorName is not valid.
    * 
    * @param virtualSensorName
    * @return
    */
   public String [ ][ ] getOutputStructure ( String virtualSensorName ) {
      if ( virtualSensorName == null || ( virtualSensorName = virtualSensorName.trim( ).toLowerCase( ) ).length( ) == 0 ) {
         logger.warn( "Bad request received for Data_strctutes" );
         return new String [ ] [ ] {};
      }
      VSensorConfig sensorConfig = Mappings.getVSensorConfig( virtualSensorName );
      if ( sensorConfig == null ) {
         logger.warn( "Requested virtual sensor doesn't exist >" + virtualSensorName + "<." );
         return new String [ ] [ ] {};
      }
      if ( logger.isInfoEnabled( ) ) logger.info( new StringBuilder( ).append( "Structure request for *" ).append( virtualSensorName ).append( "* received." ).toString( ) );
      return sensorConfig.getRPCFriendlyOutputStructure( );
   }
}
