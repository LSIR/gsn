package gsn;

import gsn.beans.VSensorConfig;
import gsn.storage.PoolIsFullException;
import gsn.storage.StorageManager;
import gsn.vsensor.AbstractVirtualSensor;

import java.io.File;
import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class VirtualSensorPool {
   
   public static final String                    VSENSORCONFIG   = "VSENSORCONFIG";
   
   public static final String                    CONTAINER       = "CONTAINER";
   
   private static final transient Logger         logger          = Logger.getLogger( VirtualSensorPool.class );
   
   private static final transient boolean        isDebugEnabled  = logger.isDebugEnabled( );
   
   private int                                   maxPoolSize;
   
   private int                                   currentPoolSize = 0;
   
   private String                                processingClassName;
   
   private ArrayList < AbstractVirtualSensor > allInstances    = new ArrayList < AbstractVirtualSensor >( );
   
   private ArrayList < AbstractVirtualSensor > idleInstances   = new ArrayList < AbstractVirtualSensor >( );
   
   private VSensorConfig                         config;
   
   private TableSizeEnforceThread                sizeEnforce;
   
   private long                                  lastModified    = -1;
   
   public VirtualSensorPool ( VSensorConfig config ) {
      if ( logger.isInfoEnabled( ) )
         logger.info( ( new StringBuilder( "Preparing the pool for: " ) ).append( config.getVirtualSensorName( ) ).append( " with the max size of:" ).append( maxPoolSize ).toString( ) );
      this.config = config;
      this.processingClassName = config.getProcessingClass( );
      this.maxPoolSize = config.getLifeCyclePoolSize( );
      this.sizeEnforce = new TableSizeEnforceThread( config );
      this.lastModified = new File( config.getFileName( ) ).lastModified( );
   }
   
   public synchronized AbstractVirtualSensor borrowVS ( ) throws PoolIsFullException , VirtualSensorInitializationFailedException {
      if ( currentPoolSize == maxPoolSize ) throw new PoolIsFullException( config.getVirtualSensorName( ) );
      currentPoolSize++;
      AbstractVirtualSensor newInstance = null;
      if ( idleInstances.size( ) > 0 )
         newInstance = idleInstances.remove( 0 );
      else
         try {
            newInstance = ( AbstractVirtualSensor ) Class.forName( processingClassName ).newInstance( );
            newInstance.setVirtualSensorConfiguration( config );
            allInstances.add( newInstance );
            if ( newInstance.initialize( ) == false ) {
               returnVS( newInstance );
               throw new VirtualSensorInitializationFailedException( );
            }
         } catch ( InstantiationException e ) {
            logger.error( e.getMessage( ) , e );
         } catch ( IllegalAccessException e ) {
            logger.error( e.getMessage( ) , e );
         } catch ( ClassNotFoundException e ) {
            logger.error( e.getMessage( ) , e );
         }
      if ( isDebugEnabled )
         logger.debug( new StringBuilder( ).append( "VSPool Of " ).append( config.getVirtualSensorName( ) ).append( " current busy instances : " ).append( currentPoolSize ).toString( ) );
      return newInstance;
   }
   
   public synchronized void returnVS ( AbstractVirtualSensor o ) {
      if ( o == null ) return;
      idleInstances.add( o );
      currentPoolSize--;
      if ( isDebugEnabled )
         logger.debug( new StringBuilder( ).append( "VSPool Of " ).append( config.getVirtualSensorName( ) ).append( " current busy instances : " ).append( currentPoolSize ).toString( ) );
   }
   
   public synchronized void closePool ( ) {
      sizeEnforce.stopPlease( );
      for ( AbstractVirtualSensor o : allInstances )
         o.finalize( );
      if ( isDebugEnabled ) logger.debug( new StringBuilder( ).append( "The VSPool Of " ).append( config.getVirtualSensorName( ) ).append( " is now closed." ).toString( ) );
   }
   
   public void start ( ) throws PoolIsFullException , VirtualSensorInitializationFailedException {
      returnVS( borrowVS( ) ); // To initialize the first VS.
      sizeEnforce.start( );
   }
   
   /**
    * @return the config
    */
   public VSensorConfig getConfig ( ) {
      return config;
   }
   
   /**
    * @return the lastModified
    */
   public long getLastModified ( ) {
      return lastModified;
   }
   
   public void finalize ( ) {

   }
}

class TableSizeEnforceThread extends Thread {
   
   private final transient Logger logger                              = Logger.getLogger( TableSizeEnforceThread.class );
   
   private static int             TABLE_SIZE_ENFORCING_THREAD_COUNTER = 0;
   
   private final int              RUNNING_INTERVALS                   = 10 * 1000;
   
   private VSensorConfig          virtualSensorConfiguration;
   
   private boolean                canRun                              = true;
   
   public TableSizeEnforceThread ( VSensorConfig virtualSensor ) {
      this.virtualSensorConfiguration = virtualSensor;
      this.setName( "TableSizeEnforceing-VSensor-Thread" + TABLE_SIZE_ENFORCING_THREAD_COUNTER++ );
      
   }
   
   public void run ( ) {
      if ( this.virtualSensorConfiguration.getParsedStorageSize( ) == VSensorConfig.STORAGE_SIZE_NOT_SET ) return;
      String virtualSensorName = this.virtualSensorConfiguration.getVirtualSensorName( );
      StringBuilder query = null;
      if ( StorageManager.isHsql( ) ) {
         if ( this.virtualSensorConfiguration.isStorageCountBased( ) )
            query = new StringBuilder( ).append( "delete from " ).append( virtualSensorName ).append( " where " ).append( virtualSensorName ).append( ".TIMED not in ( select " ).append(
               virtualSensorName ).append( ".TIMED from " ).append( virtualSensorName ).append( " order by " ).append( virtualSensorName ).append( ".TIMED DESC  LIMIT  " ).append(
               this.virtualSensorConfiguration.getParsedStorageSize( ) ).append( " offset 0 )" );
         else
            query = new StringBuilder( ).append( "delete from " ).append( virtualSensorName ).append( " where " ).append( virtualSensorName ).append( ".TIMED < (NOW_MILLIS() -" ).append(
               this.virtualSensorConfiguration.getParsedStorageSize( ) ).append( ")" );
      } else if ( StorageManager.isMysqlDB( ) ) {
         if ( this.virtualSensorConfiguration.isStorageCountBased( ) )
            query = new StringBuilder( ).append( "delete from " ).append( virtualSensorName ).append( " where " ).append( virtualSensorName ).append( ".TIMED <= ( SELECT * FROM ( SELECT TIMED FROM " )
                  .append( virtualSensorName ).append( " group by " ).append( virtualSensorName ).append( ".TIMED ORDER BY " ).append( virtualSensorName ).append( ".TIMED DESC LIMIT 1 offset " )
                  .append( this.virtualSensorConfiguration.getParsedStorageSize( ) ).append( "  ) AS TMP_" ).append( ( int ) ( Math.random( ) * 100000000 ) ).append( " )" );
         else
            query = new StringBuilder( ).append( "delete from " ).append( virtualSensorName ).append( " where " ).append( virtualSensorName ).append( ".TIMED < (UNIX_TIMESTAMP() -" ).append(
               this.virtualSensorConfiguration.getParsedStorageSize( ) ).append( ")" );
      }
      if ( query == null ) return;
      try {
         /**
          * Initial delay. Very important, dont remove it. The VSensorLoader
          * when reloads a sensor (touching the configuration file), it creates
          * the data strcture of the table in the last step thus this method
          * should be executed after some initial delay (therefore making sure
          * the structure is created by the loader).
          */
         Thread.sleep( this.RUNNING_INTERVALS );
      } catch ( InterruptedException e ) {
         if ( this.canRun == false ) return;
         this.logger.error( e.getMessage( ) , e );
      }
      
      if ( this.logger.isDebugEnabled( ) ) this.logger.debug( new StringBuilder( ).append( "Enforcing the limit size on the table by : " ).append( query ).toString( ) );
      while ( this.canRun ) {
         int effected = StorageManager.getInstance( ).executeUpdate( query );
         if ( this.logger.isDebugEnabled( ) )
            this.logger.debug( new StringBuilder( ).append( effected ).append( " old rows dropped from " ).append( this.virtualSensorConfiguration.getVirtualSensorName( ) ).toString( ) );
         try {
            Thread.sleep( this.RUNNING_INTERVALS );
         } catch ( InterruptedException e ) {
            if ( this.canRun == false ) break;
            this.logger.error( e.getMessage( ) , e );
         }
      }
   }
   
   public void stopPlease ( ) {
      this.canRun = false;
      this.interrupt( );
   }
}
