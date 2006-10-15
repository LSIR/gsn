package gsn.control;

import gsn.beans.VSensorConfig;
import gsn.storage.StorageManager;
import gsn.vsensor.VirtualSensorPool;

import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */

public class VSensorInstance {
   
   private long                   lastModified;
   
   private VSensorConfig          config;
   
   private String                 filename;
   
   private VirtualSensorPool      pool;
   
   private TableSizeEnforceThread sizeEnforce;
   
   private static int             TABLE_SIZE_ENFORCING_THREAD_COUNTER = 0;
   
   /**
    * @param fileName
    * @param modified
    * @param config
    */
   public VSensorInstance ( String fileName , long modified , VSensorConfig config ) {
      this.filename = fileName;
      this.lastModified = modified;
      this.config = config;
      this.sizeEnforce = new TableSizeEnforceThread( config );
   }
   
   /**
    * @return Returns the pool.
    */
   public VirtualSensorPool getPool ( ) {
      return this.pool;
   }
   
   /**
    * @param pool The pool to set.
    */
   public void setPool ( VirtualSensorPool pool ) {
      this.pool = pool;
   }
   
   /**
    * @param config The config to set.
    */
   public void setConfig ( VSensorConfig config ) {
      this.config = config;
   }
   
   /**
    * @param filename The filename to set.
    */
   public void setFilename ( String filename ) {
      this.filename = filename;
   }
   
   /**
    * @param lastModified The lastModified to set.
    */
   public void setLastModified ( long lastModified ) {
      this.lastModified = lastModified;
   }
   
   /**
    * @return Returns the config.
    */
   public VSensorConfig getConfig ( ) {
      return this.config;
   }
   
   /**
    * @return Returns the lastModified.
    */
   public long getLastModified ( ) {
      return this.lastModified;
   }
   
   /**
    * @return Returns the name.
    */
   public String getFilename ( ) {
      return this.filename;
   }
   
   public void shutdown ( ) {
      this.sizeEnforce.stopPlease( );
      this.getPool( ).closePool( );
      
   }
   
   public void start ( ) {
      this.sizeEnforce.start( );
   }
   
   class TableSizeEnforceThread extends Thread {
      
      private final transient Logger logger            = Logger.getLogger( TableSizeEnforceThread.class );
      
      private final int              RUNNING_INTERVALS = 10 * 1000;
      
      private VSensorConfig          virtualSensorConfiguration;
      
      private boolean                canRun            = true;
      
      public TableSizeEnforceThread ( VSensorConfig virtualSensor ) {
         this.virtualSensorConfiguration = virtualSensor;
         this.setName( "TableSizeEnforceing-VSensor-Thread" + TABLE_SIZE_ENFORCING_THREAD_COUNTER++ );
         
      }
      
      public void run ( ) {
         if ( this.virtualSensorConfiguration.getParsedStorageSize( ) == VSensorConfig.STORAGE_SIZE_NOT_SET ) return;
         String virtualSensorName = this.virtualSensorConfiguration.getVirtualSensorName( );
         StringBuilder query = null;
         if ( StorageManager.isHsql( ) ) {
            if ( this.virtualSensorConfiguration.isStorageCountBased( ) ) query = new StringBuilder( ).append( "delete from " ).append( virtualSensorName ).append( " where " ).append(
               virtualSensorName ).append( ".TIMED not in ( select " ).append( virtualSensorName ).append( ".TIMED from " ).append( virtualSensorName ).append( " order by " ).append(
               virtualSensorName ).append( ".TIMED DESC  LIMIT  " ).append( this.virtualSensorConfiguration.getParsedStorageSize( ) ).append( " offset 0 )" );
            else
               query = new StringBuilder( ).append( "delete from " ).append( virtualSensorName ).append( " where " ).append( virtualSensorName ).append( ".TIMED < (NOW_MILLIS() -" ).append(
                  this.virtualSensorConfiguration.getParsedStorageSize( ) ).append( ")" );
         } else if ( StorageManager.isMysqlDB( ) ) {
            if ( this.virtualSensorConfiguration.isStorageCountBased( ) ) query = new StringBuilder( ).append( "delete from " ).append( virtualSensorName ).append( " where " ).append(
               virtualSensorName ).append( ".TIMED <= ( SELECT * FROM ( SELECT TIMED FROM " ).append( virtualSensorName ).append( " group by " ).append( virtualSensorName )
                  .append( ".TIMED ORDER BY " ).append( virtualSensorName ).append( ".TIMED DESC LIMIT 1 offset " ).append( this.virtualSensorConfiguration.getParsedStorageSize( ) ).append(
                     "  ) AS TMP_" ).append( ( int ) ( Math.random( ) * 100000000 ) ).append( " )" );
            else
               query = new StringBuilder( ).append( "delete from " ).append( virtualSensorName ).append( " where " ).append( virtualSensorName ).append( ".TIMED < (UNIX_TIMESTAMP() -" ).append(
                  this.virtualSensorConfiguration.getParsedStorageSize( ) ).append( ")" );
         }
         if ( query == null ) return;
         try {
            /**
             * Initial delay. Very important, dont remove it. The VSensorLoader
             * when reloads a sensor (touching the configuration file), it
             * creates the data strcture of the table in the last step thus this
             * method should be executed after some initial delay (therefore
             * making sure the structure is created by the loader).
             */
            Thread.sleep( this.RUNNING_INTERVALS );
         } catch ( InterruptedException e ) {
            if ( this.canRun == false ) return;
            this.logger.error( e.getMessage( ) , e );
         }
         
         if ( this.logger.isDebugEnabled( ) ) this.logger.debug( new StringBuilder( ).append( "Enforcing the limit size on the table by : " ).append( query ).toString( ) );
         while ( this.canRun ) {
            int effected = StorageManager.getInstance( ).executeUpdate( query );
            if ( this.logger.isDebugEnabled( ) ) this.logger.debug( new StringBuilder( ).append( effected ).append( " old rows dropped from " ).append(
               this.virtualSensorConfiguration.getVirtualSensorName( ) ).toString( ) );
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
}
