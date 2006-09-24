package gsn.beans ;

import gsn.control.VSensorLoader ;
import gsn.storage.PoolIsFullException ;
import gsn.storage.SQLUtils ;
import gsn.storage.StorageManager ;
import gsn.utils.CaseInsensitiveComparator ;
import gsn.vsensor.VirtualSensor ;
import gsn.vsensor.VirtualSensorInitializationFailedException ;
import gsn.vsensor.VirtualSensorPool ;

import java.util.ArrayList ;
import java.util.Collection ;
import java.util.Enumeration ;
import java.util.HashMap ;
import java.util.TreeMap ;

import org.apache.log4j.Logger ;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class InputStream {

   public static final int INITIAL_DELAY_5000MSC = 5000;

private transient static final Logger logger = Logger.getLogger ( InputStream.class ) ;

   private transient StorageManager storageMan ;

   private transient boolean needsDelay = true ;

   private String inputStreamName ;

   private long count = Long.MAX_VALUE ;

   private transient long currentCount = 1 ;

   private int rate ;

   private String query ;

   private ArrayList < StreamSource > sources ;

   private TreeMap < String , StreamSource > streamSourceAliasNameToStreamSourceName ;

   private transient long startTime ;

   private transient VirtualSensorPool pool ;

   private transient HashMap < String , String > rewritingData = new HashMap < String , String > ( ) ;

   private transient long lastVisited = 0 ;

   private StringBuilder rewrittenSQL ;

   /**
    * For making one initial delay.
    */

   public void initialize ( HashMap context ) {
      this.pool = ( VirtualSensorPool ) context.get ( "VSENSOR-POOL" ) ;
      this.storageMan = ( StorageManager ) context.get ( VSensorLoader.STORAGE_MANAGER ) ;
      if ( this.pool == null || this.storageMan == null )
         logger.error ( "Initialization failed" ) ;
   }

   public String getQuery ( ) {
      return query ;
   }

   public void setQuery ( String sql ) {
      this.query = sql ;
   }

   public String getInputStreamName ( ) {
      return inputStreamName ;
   }

   public void setInputStreamName ( String inputStreamName ) {
      this.inputStreamName = inputStreamName ;
   }

   public long getCount ( ) {
      if ( this.count == 0 )
         this.count = Long.MAX_VALUE ;
      return count ;
   }

   public void setCount ( long count ) {
      this.count = count ;
   }

   public int getRate ( ) {
      return rate ;
   }

   public Collection < StreamSource > getSources ( ) {
      return streamSourceAliasNameToStreamSourceName.values ( ) ;
   }

   public StreamSource getSource ( String streamSourceName ) {
      return streamSourceAliasNameToStreamSourceName.get ( streamSourceName ) ;
   }

   /**
    * This method is called by the Stream Source TIMED Stream-Source has new
    * results.
    * 
    * @param alias
    *           The alias of the StreamSource which has new data.
    */
   public void dataAvailable ( String alias ) {
      if ( logger.isDebugEnabled ( ) )
         logger.debug ( new StringBuilder ( ).append ( "Notified by StreamSource on the alias: " ).append ( alias ).toString ( ) ) ;
      if ( currentCount > getCount ( ) ) {
         if ( logger.isInfoEnabled ( ) )
            logger.info ( "Maximum count reached, the value *discarded*" ) ;
         return ;
      }

      final long currentTimeMillis = System.currentTimeMillis ( ) ;
      if ( needsDelay && currentTimeMillis - startTime <= INITIAL_DELAY_5000MSC ) {
         if ( logger.isInfoEnabled ( ) )
            logger.info ( "Called but *discarded* b/c of initial delay" ) ;
         // SimulationResult.addJustBeforeStartingToEvaluateQueries ();
         // SimulationResult.addJustQueryEvaluationFinished ( - 1 );
         return ;
      }
      needsDelay = false ;
      if ( rate > 0 && ( currentTimeMillis - lastVisited ) < rate ) {
         if ( logger.isInfoEnabled ( ) )
            logger.info ( "Called by *discarded* b/c of the rate limit reached." ) ;
         // SimulationResult.addJustBeforeStartingToEvaluateQueries ( - 1 );
         // SimulationResult.addJustQueryEvaluationFinished ( - 1 );
         return ;
      }
      lastVisited = currentTimeMillis ;

      if ( rewrittenSQL == null ) {
         rewrittenSQL = new StringBuilder ( SQLUtils.rewriteQuery ( getQuery ( ).trim ( ).toUpperCase ( ) , rewritingData ).replace ( "\"" , "" ) ) ;
         if ( logger.isDebugEnabled ( ) )
            logger.debug ( new StringBuilder ( ).append ( "Rewritten SQL: " ).append ( rewrittenSQL ).append ( "(" ).append ( storageMan
                  .isThereAnyResult ( rewrittenSQL ) ).append ( ")" ).toString ( ) ) ;
      }
      if ( StorageManager.getInstance ( ).isThereAnyResult ( rewrittenSQL ) ) {
         currentCount ++ ;
         VirtualSensor sensor = null ;
         try {
            sensor = pool.borrowObject ( ) ;
            if ( logger.isDebugEnabled ( ) )
               logger.debug ( new StringBuilder ( ).append ( "Executing the main query for InputStream : " ).append ( getInputStreamName ( ) ).toString ( ) ) ;
            final Enumeration < StreamElement > resultOfTheQuery = StorageManager.getInstance ( ).executeQuery (  rewrittenSQL ) ;
            int elementCounterForDebugging = - 1 ;
            while ( resultOfTheQuery.hasMoreElements ( ) ) {
               elementCounterForDebugging ++ ;
               sensor.dataAvailable ( getInputStreamName ( ) , resultOfTheQuery.nextElement ( ) ) ;
            }
            if ( logger.isDebugEnabled ( ) ) {
               logger.debug ( new StringBuilder ( )
                     .append ( "Input Stream's result has *" ).append ( elementCounterForDebugging ).append ( "* stream elements" ).toString ( ) ) ;
            }
         } catch ( PoolIsFullException e ) {
            logger.warn ( "The stream element produced by the virtual sensor is dropped because of the following error : " ) ;
            logger.warn ( e.getMessage ( ) , e ) ;
         } catch ( UnsupportedOperationException e ) {
            logger.warn ( "The stream element produced by the virtual sensor is dropped because of the following error : " ) ;
            logger.warn ( e.getMessage ( ) , e ) ;
         } catch ( VirtualSensorInitializationFailedException e ) {
            logger.error ( "The stream element can't deliver its data to the virtual sensor " + sensor.getName ( )
                  + " because initialization of that virtual sensor failed" ) ;
            e.printStackTrace ( ) ;
         } finally {
            pool.returnInstance ( sensor ) ;
         }
      }
   }

   public void addToRenamingMapping ( String aliasName , String viewName ) {
      rewritingData.put ( aliasName , viewName ) ;
      startTime = System.currentTimeMillis ( ) ;
   }

   public void refreshAlias ( String alias ) {
      if ( logger.isInfoEnabled ( ) )
         logger.info ( "REFERES ALIAS CALEED" ) ;
   }

   public boolean equals ( Object o ) {
      if ( this == o ) {
         return true ;
      }
      if ( ! ( o instanceof InputStream ) ) {
         return false ;
      }

      final InputStream inputStream = ( InputStream ) o ;

      if ( inputStreamName != null ? ! inputStreamName.equals ( inputStream.inputStreamName ) : inputStream.inputStreamName != null ) {
         return false ;
      }
      return true ;
   }

   public int hashCode ( ) {
      return ( inputStreamName != null ? inputStreamName.hashCode ( ) : 0 ) ;
   }

   public void finalize ( ) {
      HashMap map = new HashMap ( ) ;
      finalize ( map ) ;
   }

   public void finalize ( HashMap context ) {
   }

   private transient boolean isValidate = false ;

   boolean cachedValidationResult = false ;

   public boolean validate ( ) {
      if ( isValidate )
         return cachedValidationResult ;
      boolean toReturn = true ;
      TreeMap < String , StreamSource > streamSourceAliasNameToStreamSourceName = new TreeMap < String , StreamSource > ( new CaseInsensitiveComparator ( ) ) ;
      for ( StreamSource ss : sources ) {
         if ( ! ss.validate ( ) ) {
            logger.error ( new StringBuilder ( )
                  .append ( "The Stream Source : " ).append ( ss.getAlias ( ) ).append ( " specified in the Input Stream : " ).append ( getInputStreamName ( ) )
                  .append ( " is not valid." ).toString ( ) ) ;
            toReturn = false ;
            break ;
         }
         streamSourceAliasNameToStreamSourceName.put ( ss.getAlias ( ) , ss ) ;
      }
      this.streamSourceAliasNameToStreamSourceName = streamSourceAliasNameToStreamSourceName ;
      isValidate = true ;
      cachedValidationResult = toReturn ;

      return toReturn ;
   }
}
