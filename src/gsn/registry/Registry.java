package gsn.registry;

import java.io.IOException;

import gsn.utils.ValidityTools;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.xmlrpc.webserver.XmlRpcServletServer;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * TODO, we need to keep the whole addressing stucture b/c we want to use google
 * maps to show the outcome to the users.
 */
public class Registry {
   
   public static final String            _GUID                            = "_guid";
   
   public static final int               REFERESH_INTERVAL                = 5 * 60 * 1000;
   
   public static final String            DEFAULT_DIR_LOG4J_PROPERTIES     = "conf/log4j.directory.properties";
   
   public static final String            DEFAULT_DIRECTORY_SERVER_WEB_APP = "dswebapp";
   
   private static final transient Logger logger                           = Logger.getLogger( Registry.class );
   
   public static final String            REQUEST                          = "REQUEST";
   
   public static final int               REGISTER                         = 100;
   
   public static final int               QUERY                            = 102;
   
   public static final String            VS_NAME                          = "NAME";
   
   public static final String            VS_PORT                          = "PORT";
   
   public static final String            VS_PREDICATES_VALUES             = "VSensorPredicatesValues";
   
   public static final String            VS_PREDICATES_KEYS               = "VSensorPredicatesKeys";
   
   public static final String            VS_HOST                          = "HOST";
   
   public final transient static String  SPACE_CHARACTER                  = " ";
   
   public static final String            COL                              = ":";
   
   public static void main ( String [ ] args ) throws Exception {
      if ( args.length < 2 ) {
         System.out.println( "You must specify the port on which the directory service will listen (e.g. 1882)" );
         System.out.println( "You must specify the interface IP on which the directory service will listen.(e.g. localhost)" );
         System.out.println( "You can optionally specify the path to the directory into which lucene will store its data, otherwise we'll use Ram for storage." );
         System.exit( 1 );
      }
      System.out.println( "Loading logging details from : " + DEFAULT_DIR_LOG4J_PROPERTIES );
      PropertyConfigurator.configure( DEFAULT_DIR_LOG4J_PROPERTIES );
      int port = -1;
      try {
         port = Integer.parseInt( args[ 0 ] );
      } catch ( Exception e ) {
         logger.error( "Can't parse the port no. from the input (" + args[ 0 ] + ")" , e );
         return;
      }
      
      if ( logger.isInfoEnabled( ) ) logger.info( "GSN-Registry-Server startup " );
      System.getProperties( ).put( "org.mortbay.level" , "error" );
      String computerIP = args[ 1 ];
      if ( !ValidityTools.isLocalhost( computerIP ) ) {
         logger.fatal( "The specified IP address (" + args[ 1 ] + ") is not pointing to the local machine." );
         return;
      }
      if ( ValidityTools.isAccessibleSocket( "localhost" , port , 500 ) ) {
         logger.fatal( "The specified port :" + port + " is busy. Start failed." );
         return;
      }
      final Server server = new Server( );
      Connector connector = new SelectChannelConnector( );
      connector.setPort( port );
      server.setConnectors( new Connector [ ] { connector } );
      
      WebAppContext wac = new WebAppContext( );
      wac.setContextPath( "/" );
      wac.setResourceBase( DEFAULT_DIRECTORY_SERVER_WEB_APP );
      
      ServletHandler servletHandler = new ServletHandler( );
      servletHandler.addServletWithMapping( "gsn.registry.MyXmlRPCServlet" , "/registry" );
      wac.setServletHandler( servletHandler );
      server.setHandler( wac );
      server.setStopAtShutdown( true );
      server.setSendServerVersion( false );
      
      singleton = new RegistryReferesh( args.length == 3 ? args[ 2 ] : null );
      thread = new Thread( singleton );
      thread.start( );
      server.start( );
      
      if ( logger.isInfoEnabled( ) ) logger.info( "[ok]" );
   }
   
   private static RegistryReferesh singleton;
   
   private static Thread           thread;
   
}
