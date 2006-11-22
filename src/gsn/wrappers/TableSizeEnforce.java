/**
 * 
 */
package gsn.wrappers;

import gsn.beans.StreamSource;
import gsn.storage.StorageManager;
import java.util.ArrayList;
import java.util.Date;
import org.apache.log4j.Logger;

/**
 * @author alisalehi
 */
public class TableSizeEnforce implements Runnable {
   
   private AbstractStreamProducer     abstractStreamProducer;
   
   private ArrayList < DataListener > listeners;

   private String tableName;
   
   public TableSizeEnforce ( AbstractStreamProducer abstractStreamProducer ) {
      this.abstractStreamProducer = abstractStreamProducer;
      this.listeners=abstractStreamProducer.getListeners( );
      this.tableName = abstractStreamProducer.getDBAlias( );
   }
   
   /**
    * This thread executes a query which in turn droppes the stream elements
    * which are not interested by any of the existing listeners. The query which
    * is used for dropping the unused stream elements is going to be generated
    * whever there is a change in the set of registered DataListeners. If there
    * is a change the <code>isCacheNeedsUpdate</code> will be true which
    * triggers the thread to generate a new query for removing the unused stream
    * elements.
    */
   private boolean                    isCacheNeedsUpdate = true;
   
   private final transient Logger     logger             = Logger.getLogger( TableSizeEnforce.class );
   
   private final int                  RUNNING_INTERVALS  = 1000 * 5;
   
   
   public void updateInternalCaches ( ) {
      isCacheNeedsUpdate = true;
   }
   
   public void run ( ) {
      StringBuilder deleteStatement = new StringBuilder( );
      while ( abstractStreamProducer.isActive( ) ) {
         try {
            Thread.sleep( RUNNING_INTERVALS );
         } catch ( InterruptedException e ) {
            logger.error( e.getMessage( ) , e );
         }
         /**
          * Garbage collector's where clause. The garbage collector is in fact,
          * the actual worker for size enforcement.
          */
         
         synchronized ( abstractStreamProducer.getListeners( ) ) {
            if ( listeners.size( ) == 0 ) continue;
            if ( isCacheNeedsUpdate ) {
               deleteStatement = new StringBuilder( );
               for ( DataListener listener : listeners ) {
                  if ( listener.getStreamSource( ).getParsedStorageSize( ) != StreamSource.STORAGE_SIZE_NOT_SET )
                     deleteStatement.append(
                        extractConditions( listener.isCountBased( ) , listener.getHistorySize( ) , listener.getStreamSource( ).getStartDate( ) , listener.getStreamSource( ).getEndDate( ) ) ).append(
                        "  AND " );
               }
               if ( deleteStatement.length( ) == 0 ) {
                  continue;
               }
               deleteStatement.replace( deleteStatement.length( ) - 4 , deleteStatement.length( ) , "" );
               deleteStatement.insert( 0 , " where " ).insert( 0 , tableName ).insert( 0 , "delete from " );
               if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( ).append( "RESULTING QUERY FOR Table Size Enforce " ).append( deleteStatement.toString( ) ).toString( ) );
               isCacheNeedsUpdate = false;
            }
         }
         int resultOfUpdate = StorageManager.getInstance( ).executeUpdate( deleteStatement );
         if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( ).append( resultOfUpdate ).append( " old rows dropped from " ).append( tableName ).toString( ) );
      }
   }
   
   private StringBuilder extractConditions ( boolean countBased , int historySize , Date startDate , Date endDate ) {
      StringBuilder result = new StringBuilder( );
      if ( StorageManager.isHsql( ) ) {
         if ( countBased )
            // result.append ( "( ( " ).append ( tableName ).append
            // ( ".TIMED
            // not in ( select " ).append ( tableName ).append (
            // ".TIMED from
            // " ).append (
            // tableName ).append ( " order by " ).append (
            // tableName
            // ).append ( ".TIMED DESC LIMIT " ).append (
            // historySize
            // ).append (
            // " offset 0 )) AND (" + tableName + ".TIMED >=" +
            // startDate.getTime () + ") AND (" + tableName +
            // ".TIMED<=" +
            // endDate.getTime () + ") )" );
            result.append( "( ( " ).append( tableName ).append( ".TIMED <= ( SELECT TIMED from " ).append( tableName ).append( " group by TIMED order by TIMED desc limit 1 offset " ).append(
               historySize ).append( "  ) ) AND (" + tableName + ".TIMED >=" + startDate.getTime( ) + ") AND (" + tableName + ".TIMED<=" + endDate.getTime( ) + ") )" );
         else
            result.append( "( ( " ).append( tableName ).append( ".TIMED < (NOW_MILLIS() -" ).append( historySize ).append(
               ") ) AND (" + tableName + ".TIMED >=" + startDate.getTime( ) + ") AND (" + tableName + ".TIMED<=" + endDate.getTime( ) + ") )" );
      } else if ( StorageManager.isMysqlDB( ) ) {
         if ( countBased )
            result.append( "( ( " ).append( tableName ).append( ".TIMED <= ( SELECT * from ( SELECT TIMED from " ).append( tableName ).append( " group by TIMED order by TIMED desc limit 1 offset " )
                  .append( historySize ).append( "  ) AS TMP_" ).append( ( int ) ( Math.random( ) * 10000000 ) ).append(
                     " ) ) AND (" + tableName + ".TIMED >=" + startDate.getTime( ) + ") AND (" + tableName + ".TIMED<=" + endDate.getTime( ) + ") )" );
         else
            result.append( "( ( " ).append( tableName ).append( ".TIMED < (UNIX_TIMESTAMP() -" ).append( historySize ).append(
               ") ) AND (" + tableName + ".TIMED >=" + startDate.getTime( ) + ") AND (" + tableName + ".TIMED<=" + endDate.getTime( ) + ") )" );
      }
      
      return result;
   }

}
