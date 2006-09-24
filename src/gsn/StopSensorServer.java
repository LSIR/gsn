package gsn ;

import java.io.File ;

import org.apache.log4j.Logger ;
import org.apache.log4j.PropertyConfigurator ;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public final class StopSensorServer {

   private static final transient Logger logger = Logger.getLogger ( StopSensorServer.class ) ;

   public static void main ( String args[] ) {
      PropertyConfigurator.configure ( args [ 1 ] ) ;
      File file = new File ( args [ 2 ] + "/" + new File ( args [ 0 ] ).lastModified ( ) + ".pid" ) ;
      if ( ! file.exists ( ) )
         return ;

      if ( file.isFile ( ) )
         file.setLastModified ( System.currentTimeMillis ( ) ) ;
      while ( file.exists ( ) ) {
         try {
            Thread.sleep ( 500 ) ;
         } catch ( InterruptedException e ) {
            logger.error ( e.getMessage ( ) , e ) ;
         }
      }
   }

}
