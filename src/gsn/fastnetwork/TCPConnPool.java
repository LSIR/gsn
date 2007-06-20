package gsn.fastnetwork;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpRecoverableException;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.log4j.Logger;


public class TCPConnPool {
   
   private static transient MultiThreadedHttpConnectionManager httpConnectionManager;
   
   private static transient HttpClient                         httpClient;
   
   private static boolean                                      initialized = false;
   
   private static Logger                                       logger      = Logger.getLogger( TCPConnPool.class );
   
   private TCPConnPool ( ) {}
   
   /**
    * Tries 3 times in the case of HttpRecoverableException.
    * 
    * @param postMethod
    * @return
    */
   public static int executeMethod ( PostMethod postMethod ) {
      return executeMethod( postMethod , true );
   }
   
   /**
    * @param postMethod
    * @param reportException
    * @return -1 if the operation is unsuccessful otherwise returns the
    * operation code from the server. Typically, the return value of 200 means
    * successful.
    */
   public static int executeMethod ( PostMethod postMethod , boolean reportException ) {
      if ( initialized == false ) {
         HttpConnectionManagerParams connectionManagerParams = new HttpConnectionManagerParams( );
         connectionManagerParams.setDefaultMaxConnectionsPerHost( 10000 );
         connectionManagerParams.setMaxTotalConnections( 10000 );
         connectionManagerParams.setStaleCheckingEnabled( true );
         httpConnectionManager = new MultiThreadedHttpConnectionManager( );
         httpConnectionManager.setParams( connectionManagerParams );
         httpClient = new HttpClient( httpConnectionManager );
         initialized = true;
      }
      int statusCode = -1;
      for ( int attempt = 0 ; statusCode == -1 && attempt < 3 ; attempt++ ) {
         try {
            statusCode = httpClient.executeMethod( postMethod );
         } catch ( HttpRecoverableException e ) {
            if ( reportException ) logger.warn( e.getMessage( ) , e );
            continue;
         } catch ( IOException e ) {
            if ( reportException ) logger.warn( e.getMessage( ) , e );
            else if ( logger.isDebugEnabled( ) ) logger.debug( e.getMessage( ) , e );
            break;
         }
      }
      return statusCode;
   }
}
