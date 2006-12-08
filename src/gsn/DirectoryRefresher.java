/**
 * This code is protected under the GNU General Public License Version 2 which implies that <br>
 * all derivative works MUST be freely available and open sourced under GPL licence. <br>
 * If you are wishing to combine/use the GSN with proprietary code and distribute your code under non-open source<br>
 * licence please contact the authors to get specialized license.<br>
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * Creation time : Dec 7, 2006@9:53:35 PM<br> *  
 */
package gsn;

import gsn.beans.VSensorConfig;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Timer;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

/**
 * @author alisalehi
 */
public class DirectoryRefresher {
   
   private static transient Logger logger             = Logger.getLogger( DirectoryRefresher.class );
   
   // int INTERVAL = Registry.REFERESH_INTERVAL/2;
   int                             INTERVAL           = 500;                                          // for
                                                                                                      // testing
   
   int                             CONNECTION_TIMEOUT = 5000;
   
   int                             INITIAL_DELAY      = 5000;
   
   Timer                           timer              = new Timer( "Directory Refreshing Timer" );
   
   int                             errorCounter       = 0;
   
   XmlRpcClient                    client             = new XmlRpcClient( );
   
   public DirectoryRefresher ( ) {
      XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl( );
      try {
         config.setServerURL( new URL( "http://" + Main.getContainerConfig( ).getRegistryBootstrapAddr( ) + "/registry" ) );
         client.setConfig( config );
      } catch ( MalformedURLException e1 ) {
         logger.error( e1.getMessage( ) , e1 );
      }
      timer.scheduleAtFixedRate( new java.util.TimerTask( ) {
         public void run ( ) {
            if ( DirectoryRefresher.this.errorCounter == 3 ) {
               logger.warn( "After 3 unsuccessful tries, GSN stopped contacting the directory." );
               this.cancel( );
               return;
            }
            Iterator < VSensorConfig > keys = Mappings.getAllVSensorConfigs( );
            try {
               while ( keys.hasNext( ) ) {
                 VSensorConfig configuration = keys.next( );
                  if ( logger.isDebugEnabled( ) )
                     logger.debug( new StringBuilder( "Wants to connect to directory service at " ).append( Main.getContainerConfig( ).extractDirectoryServiceHost( ) ).append( ":" ).append(
                        Main.getContainerConfig( ).extractDirectoryServicePort( ) ) );
                  Object [ ] params = new Object [ ] {Main.getContainerConfig( ).getContainerPort( ),configuration.getVirtualSensorName( ),configuration.getDescription( ),configuration.getRPCFriendlyAddressing( ),configuration.getUsedSources( )};
                  Boolean result = ( Boolean ) client.execute( "Registry.addVirtualSensor" , params );
                  if ( result == false ) {
                     logger.debug( new StringBuilder( "Registering the " ).append( configuration.getVirtualSensorName( ) ).append( " failed !" ) );
                  }
                  DirectoryRefresher.this.errorCounter = 0;
               }
            } catch ( XmlRpcException e ) {
               logger.error( "Can't register the existing virtual sensors with the specified directory. (try " + ( ++DirectoryRefresher.this.errorCounter ) + ") err : "+e.getMessage( ) );
               logger.error( e.getMessage( ) , e );            }
         }
      } , INITIAL_DELAY , INTERVAL );
   } 
}
