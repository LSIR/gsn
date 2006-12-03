package gsn.registry;

import gsn.utils.ValidityTools;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * TODO, we need to keep the whole addressing stucture b/c we want to use google maps
 * to show the outcome to the users.
 */
public class Registry  {
   
   public static final String _GUID = "_guid";
   
   public static final int                       REFERESH_INTERVAL                = 5 * 60 * 1000;
   
   public static final String              DEFAULT_DIR_LOG4J_PROPERTIES     = "conf/log4j.directory.properties";
   
   public static final String              DEFAULT_DIRECTORY_SERVER_WEB_APP = "dswebapp";
   
   private static final transient Logger   logger                           = Logger.getLogger ( Registry.class );
   
   private Analyzer analyzer = new StandardAnalyzer ();
   
   private QueryParser queryParser;
   
   public static final String REQUEST              = "REQUEST";
   
   public static final int    REGISTER             = 100;
   
   public static final int    QUERY                = 102;
   
   public static final String VS_NAME              = "NAME";
   
   public static final String VS_PORT              = "PORT";
   
   public static final String VS_PREDICATES_VALUES = "VSensorPredicatesValues";
   
   public static final String VS_PREDICATES_KEYS   = "VSensorPredicatesKeys";
   
   public static final String VS_HOST              = "HOST";
   
   public static final String DEFAULT_FIELD = "address";
   
   public final transient static String SPACE_CHARACTER = " ";
   
   public static final String COL = ":";
   
   private Directory directory;
   
   public Registry ( Directory directory ) throws IOException {
      this.directory = directory;
      queryParser = new QueryParser (DEFAULT_FIELD,analyzer);
   }
   
   public static void main ( String [ ] args ) throws Exception {
      if ( args.length < 2 ) {
         System.out.println ( "You must specify the port on which the directory service will listen (e.g. 1882)" );
         System.out.println ( "You must specify the interface IP on which the directory service will listen.(e.g. localhost)" );
         System.out.println ( "You can optionally specify the path to the directory into which lucene will store its data, otherwise we'll use Ram for storage." );  
         System.exit ( 1 );
      }
      System.out.println ( "Loading logging details from : " + DEFAULT_DIR_LOG4J_PROPERTIES );
      PropertyConfigurator.configure ( DEFAULT_DIR_LOG4J_PROPERTIES );
      int port = -1;
      try {
         port = Integer.parseInt ( args[ 0 ] );
      } catch ( Exception e ) {
         logger.error ( "Can't parse the port no. from the input (" + args[ 0 ] + ")" , e );
         return;
      }
      
      if ( logger.isInfoEnabled ( ) ) logger.info ( "GSN-Registry-Server startup " );
      System.getProperties ( ).put ( "org.mortbay.level" , "error" );
      String computerIP = args[ 1 ];
      if ( !ValidityTools.isLocalhost ( computerIP ) ) {
         logger.fatal ( "The specified IP address (" + args[ 1 ] + ") is not pointing to the local machine." );
         return;
      }
      if ( ValidityTools.isAccessibleSocket ( "localhost",port,500 ) ) {
         logger.fatal ( "The specified port :"+port+" is busy. Start failed." );
         return;
      }
      //            final Server server = new Server( );
      //            Connector connector = new SelectChannelConnector( );
      //            connector.setPort( port );
      //            server.setConnectors( new Connector [ ] { connector } );
      //
      //            WebAppContext wac = new WebAppContext( );
      //            wac.setContextPath( "/" );
      //            wac.setResourceBase( DEFAULT_DIRECTORY_SERVER_WEB_APP );
      //
      //            ServletHandler servletHandler = new ServletHandler( );
      //            servletHandler.addServletWithMapping( "gsn.registry.Registry" , "/registry" );
      //            wac.setServletHandler( servletHandler );
      //
      //            server.setHandler( wac );
      //            server.setStopAtShutdown( true );
      //            server.setSendServerVersion( false );
      //            server.start( );
      Directory  directory;
      if (args.length==3){
         directory = FSDirectory.getDirectory(args[2], false);
         logger.warn("Directory will store its data in "+args[2]);
      }else
         directory= new RAMDirectory ( );
      
      // IoAcceptor acceptor = new DatagramAcceptor ();
      IoAcceptor acceptor = new SocketAcceptor ();
      SocketAcceptorConfig cfg = new SocketAcceptorConfig ();
      cfg.setReuseAddress ( true );
      cfg.getFilterChain().addLast(  "codec", new ProtocolCodecFilter( new ObjectSerializationCodecFactory() ) );
      acceptor.bind (new InetSocketAddress ( port ), new RegistryReferesh (directory),cfg);
      if ( logger.isInfoEnabled ( ) ) logger.info ( "[ok]" );
   }   
   
   /**
    * Returns null if the resultset is empty.
    */
   public Hits doQuery (String queryString) throws ParseException {
      try {
         IndexSearcher searcher = new IndexSearcher (directory);
         Query query = queryParser.parse (queryString);
         return searcher.search (query);
      }catch (IOException e) {
         logger.error ( e.getMessage ( ),e );
         return null;
      }
   }
   
}

