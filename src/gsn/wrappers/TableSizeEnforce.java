/**
 * 
 */
package gsn.wrappers;
import org.apache.log4j.Logger;


/**
 * @author alisalehi
 */
public class TableSizeEnforce implements Runnable {
   
   private AbstractWrapper     wrapper;
     
   public TableSizeEnforce ( AbstractWrapper wrapper ) {
      this.wrapper = wrapper;
   }
   
   /**
    * This thread executes a query which in turn droppes the stream elements
    * which are not interested by any of the existing listeners. The query which
    * is used for dropping the unused stream elements is going to be generated
    * whenever there is a change in the set of registered DataListeners.
    */
   
   private final transient Logger     logger             = Logger.getLogger( TableSizeEnforce.class );
   
   private final int                  RUNNING_INTERVALS  = 1000 * 5;
   
   public void run ( ) {
      while ( true ) {
         try {
            Thread.sleep( RUNNING_INTERVALS );
         } catch ( InterruptedException e ) {
            logger.error( e.getMessage( ) , e );
         }
         if (wrapper.isActive()&&wrapper.getListeners().size()==0)
        	 continue;
         /**
          * Garbage collector's where clause. The garbage collector is in fact,
          * the actual worker for size enforcement.
          */
         int deletedRows = wrapper.removeUselessValues();
         if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( ).append( deletedRows ).append( " old rows dropped from " ).append( wrapper.getDBAliasInStr() ).toString( ) );
      }
   }
}
