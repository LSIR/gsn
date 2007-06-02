package gsn;

import gsn.beans.StreamElement;
import gsn.notifications.InGSNNotification;
import gsn.notifications.NotificationRequest;
import gsn.storage.DataEnumerator;
import gsn.storage.StorageManager;
import gsn.vsensor.AbstractVirtualSensor;
import gsn.wrappers.RemoteWrapper;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;


public class ContainerImpl implements Container {
   
   private static transient Logger                                      logger                             = Logger.getLogger( ContainerImpl.class );
   
   /**
    * The <code> waitingVirtualSensors</code> contains the virtual sensors that
    * recently produced data. This variable is useful for batch processing timed
    * couple virtual sensor produce data.
    */
   /*
    * In the <code>registeredQueries</code> the key is the local virtual
    * sensor name.
    */

   private static ConcurrentHashMap<String , ArrayList < NotificationRequest >> notificationRequests               = new ConcurrentHashMap<String, ArrayList<NotificationRequest>>();

   private static final Class < ContainerImpl >                         notificationRequestsLock           = ContainerImpl.class;
   
   private static final HashMap < Integer , RemoteWrapper >             notificationCodeToRemoteDataSource = new HashMap < Integer , RemoteWrapper >( );
   
   private static final Object                                          psLock                             = new Object( );
   
   public ContainerImpl ( ) {
   }

   public void publishData ( AbstractVirtualSensor sensor ) throws SQLException {
      StreamElement data = sensor.getData( );
      String name = sensor.getVirtualSensorConfiguration( ).getName( ).toLowerCase();
      StorageManager storageMan = StorageManager.getInstance( );
      synchronized ( psLock ) {
       	storageMan.executeInsert( name ,sensor.getVirtualSensorConfiguration().getOutputStructure(), data );
      }
      // SimulationResult.addJustBeforeStartingToEvaluateQueries ();
      ArrayList < NotificationRequest > registered;
      synchronized ( notificationRequestsLock ) {
         registered = notificationRequests.get( name );
      }
      if ( registered == null ) {
         if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( ).append( "No Query registered for >" ).append( name ).append( "<" ).toString( ) );
         // SimulationResult.addJustQueryEvaluationFinished ( 0 );
         return;
      }
      if ( logger.isDebugEnabled( ) )
         logger.debug( new StringBuilder( ).append( "There are queries " ).append( registered.size( ) ).append( " registered for >" ).append( name ).append( "<" ).toString( ) );
      ArrayList < NotificationRequest > notificationCandidates = new ArrayList < NotificationRequest >( );
      ArrayList < DataEnumerator > notificationData = new ArrayList < DataEnumerator >( );
      
      synchronized ( registered ) {
         Iterator < NotificationRequest > registeredIterator = registered.iterator( );
         while ( registeredIterator.hasNext( ) ) {
            NotificationRequest interestedClient = registeredIterator.next( );
            DataEnumerator de = storageMan.executeQuery( interestedClient.getQuery( ) , false );
            if ( logger.isDebugEnabled( ) )
               logger.debug( new StringBuilder( "Evaluating the query : " ).append( interestedClient ).append( ", needs notification > " ).append( de.hasMoreElements( ) ) );
            if ( de.hasMoreElements( ) ) {
               notificationCandidates.add( interestedClient );
               notificationData.add( de );
            }else
            	de.close();
         }
      }
      // IMPROVE : The Asynchronous notification System.
      for ( int i = 0 ; i < notificationCandidates.size( ) ; i++ ) {
    		  boolean notificationResult = notificationCandidates.get( i ).send( notificationData.get( i ) );
         if ( notificationResult == false ) {
            logger.info( "Query notification fail, query removed " + notificationCandidates.get( i ).toString( ) );
            removeNotificationRequest( notificationCandidates.get( i ) );
         }
      }
      
   }
   
   public synchronized void addNotificationRequest ( String localVirtualSensorName , NotificationRequest notificationRequest ) {
      localVirtualSensorName = localVirtualSensorName.toLowerCase( );
      ArrayList < NotificationRequest > contents;
      if ( notificationRequests.get( localVirtualSensorName ) == null ) {
         contents = new ArrayList < NotificationRequest >( );
         notificationRequests.put( localVirtualSensorName , contents );
      } else
         contents = notificationRequests.get( localVirtualSensorName );
      if ( logger.isDebugEnabled( ) ) {
         logger.debug( new StringBuilder( "Notification request added to " ).append( localVirtualSensorName ).toString( ) );
      }
      synchronized ( contents ) {
         /**
          * We want to handle InGSNNotification's faster.
          */
         if ( notificationRequest instanceof InGSNNotification )
            contents.add( 0 , notificationRequest );
         else
            contents.add( notificationRequest );
      }
   }
   
   public synchronized void removeNotificationRequest ( String localVirtualSensorName , NotificationRequest notificationRequest ) {
      localVirtualSensorName = localVirtualSensorName.toLowerCase( );
      ArrayList < NotificationRequest > contents = notificationRequests.get( localVirtualSensorName );
      if ( contents == null ) {// when an invalid remove request recevied for
         // a
         // virtual sensor which doesn't have any query
         // registered to it.
         return;
      }
      synchronized ( contents ) {
         boolean changed = contents.remove( notificationRequest );
      }
   }
   
   public synchronized void removeNotificationRequest ( NotificationRequest notificationRequest ) {
      Iterator < String > virtualSensorNames = notificationRequests.keySet( ).iterator( );
      while ( virtualSensorNames.hasNext( ) ) {
         String virtualSensorName = virtualSensorNames.next( );
         ArrayList < NotificationRequest > contents = notificationRequests.get( virtualSensorName );
         if ( contents == null || contents.size( ) == 0 ) {// when an
            // invalid
            // remove request
            // recevied for a
            // virtual sensor which
            // doesn't have any
            // query registered to
            // it.
            return;
         }
         synchronized ( contents ) {
            boolean changed = contents.remove( notificationRequest );
         }
      }
   }
   
   public synchronized NotificationRequest [ ] getAllNotificationRequests ( ) {
      Vector < NotificationRequest > results = new Vector < NotificationRequest >( );
      for ( ArrayList < NotificationRequest > notifications : notificationRequests.values( ) )
         results.addAll( notifications );
      return results.toArray( new NotificationRequest [ ] {} );
   }
   
   public void addRemoteStreamSource ( int notificationCode , RemoteWrapper remoteWrapper ) {
      notificationCodeToRemoteDataSource.put( notificationCode , remoteWrapper );
      if ( logger.isDebugEnabled( ) )
         logger.debug( new StringBuilder( ).append( "Remote DataSource DBALIAS *" ).append( remoteWrapper.getDBAlias( ) ).append( "* with the code : *" ).append( notificationCode )
               .append( "* added." ).toString( ) );
   }
   
   public void removeAllResourcesAssociatedWithVSName ( String vsensorName ) {
	  vsensorName = vsensorName.toLowerCase();
      ArrayList < NotificationRequest > effected = notificationRequests.remove( vsensorName );
      // FIXME : The used prepare statements should be released from the
      // stroagemanager using a timeout mechanism.
      // PreparedStatement ps;
      // synchronized (psLock) {
      // ps = preparedStatements.remove(vsensorName);
      // }
      // StorageManager.getInstance().returnPrepaedStatement(ps);
   }
   
   public void removeRemoteStreamSource ( int notificationCode ) {
      notificationCodeToRemoteDataSource.remove( notificationCode );
   }
   
   public RemoteWrapper getRemoteDSForANotificationCode ( int code ) {
      return notificationCodeToRemoteDataSource.get( code );
   }
   
}
