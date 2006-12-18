package gsn.wrappers;

import gsn.Mappings;
import gsn.VirtualSensorInitializationFailedException;
import gsn.beans.DataField;
import gsn.beans.VSensorConfig;
import gsn.notifications.InGSNNotification;
import gsn.notifications.NotificationRequest;
import gsn.storage.PoolIsFullException;
import gsn.vsensor.AbstractVirtualSensor;
import gsn.vsensor.http.OneShotQueryHandler;

import java.io.Serializable;
import java.util.Iterator;
import javax.naming.OperationNotSupportedException;
import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * Creation time : Dec 18, 2006@4:03:24 PM<br> *
 */
public class InVMPipeWrapper extends AbstractWrapper {
   private static transient Logger                                      logger                             = Logger.getLogger( InVMPipeWrapper.class );

   public void finalize ( ) {

   }
   
   public DataField [ ] getOutputFormat ( ) {
      return config.getOutputStructure( );
   }
   
   public String getWrapperName ( ) {
      return "In VM Pipe Wrapper";
   }
   
   private VSensorConfig          config;
   
   private NotificationRequest    notificationRequest;
   
   public boolean initialize ( ) {
      String remoteVSName = getActiveAddressBean( ).getPredicateValue( "name" );
      if ( remoteVSName == null ) {
         logger.warn( "The \"NAME\" paramter of the AddressBean which corresponds to the local Virtual Sensor is missing, Initialization failed." );
         return false;
      }
      this.config = Mappings.getVSensorConfig( remoteVSName.toLowerCase( ).trim( ) );
      if ( this.config == null ) {
         logger.warn( "The Requested virtual sensor is not available (or not loaded yet), initialization failed !" );
         return false;
      }
      return true;
   }
   
   public CharSequence addListener ( DataListener dataListener ) {
      /**
       * First we create a view over the main source
       * (config.getVirtualSensorName). We encode all the conditions and
       * filtering in the view's definition. We perform select * from the view.
       */
      CharSequence toReturn = super.addListener( dataListener );
      notificationRequest = new InGSNNotification( this , config.getVirtualSensorName( ) );
      Mappings.getContainer( ).addNotificationRequest( config.getVirtualSensorName( ) , notificationRequest );
      return toReturn;
   }
   
   public boolean sendToWrapper ( String action,String[] paramNames, Serializable[] paramValues ) throws OperationNotSupportedException {
      AbstractVirtualSensor vs;
      try {
         vs = Mappings.getVSensorInstanceByVSName( config.getVirtualSensorName( ) ).borrowVS( );
      } catch ( PoolIsFullException e ) {
         logger.warn( "Sending data back to the source virtual sensor failed !: "+e.getMessage( ),e);
         return false;
      } catch ( VirtualSensorInitializationFailedException e ) {
         logger.warn("Sending data back to the source virtual sensor failed !: "+e.getMessage( ),e);
         return false;
      }
      boolean toReturn = vs.dataFromWeb( action , paramNames , paramValues );
      Mappings.getVSensorInstanceByVSName( config.getVirtualSensorName( ) ).returnVS( vs );
      return toReturn;
   }
   
   public void remoteDataReceived ( ) {
      if ( logger.isDebugEnabled( ) ) logger.debug( "There are results, is there any listeners ?" );
      for ( Iterator < DataListener > iterator = listeners.iterator( ) ; iterator.hasNext( ) ; ) {
         DataListener dataListener = iterator.next( );
         boolean results = getStorageManager( ).isThereAnyResult( dataListener.getViewQuery( ) );
         if ( results ) {
            if ( logger.isDebugEnabled( ) )
               logger.debug( new StringBuilder( ).append( "There are listeners, notify the " ).append( dataListener.getInputStream( ).getInputStreamName( ) ).append( " inputStream" ).toString( ) );
            // TODO :DECIDE WHETHER TO INFORM THE CLIENT OR NOT (TIME
            // TRIGGERED. DATA TRIGGERED)
            dataListener.dataAvailable( );
         }
      }
   }
   
   public String toString ( ) {
      StringBuilder sb = new StringBuilder( "InVMPipeWrapper, " );
      sb.append( " RemoteVS : " ).append( config.getVirtualSensorName( ) );
      return sb.toString( );
   }
   
}
