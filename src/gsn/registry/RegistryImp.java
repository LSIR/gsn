package gsn.registry;

import gsn.pid.PIDUtils;
import gsn.shared.VirtualSensorIdentityBean;
import gsn.utils.KeyValueImp;
import gsn.utils.ValidityTools;
import gsn.vsensor.EPuckVS;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.collections.KeyValue;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * TODO, we need to keep the whole addressing stucture b/c we want to use google maps
 * to show the outcome to the users.
 */
public class RegistryImp extends HttpServlet  {
   
   /**
    * 
    */
   private static final String _GUID = "_guid";

   private static final int                       REFERESH_INTERVAL                = 5 * 60 * 1000;
   
   private Directory                       directory                        = new RAMDirectory( );
   
   public static final String              DEFAULT_DIR_LOG4J_PROPERTIES     = "conf/log4j.directory.properties";
   
   public static final String              DEFAULT_DIRECTORY_SERVER_WEB_APP = "dswebapp";
   
   private static final transient Logger   logger                           = Logger.getLogger( RegistryImp.class );
   
   private static RegistryImp              singleton;
   
   private static final List < VSAddress > list                             = Collections.synchronizedList( new ArrayList < VSAddress >( ) );
   
   private RemoveOutdatedEntries           removeOutdatedEntriesThread      = new RemoveOutdatedEntries( );
   
   private Analyzer analyzer = new StandardAnalyzer();

   private QueryParser queryParser;
   
   public static final String REQUEST              = "REQUEST";
   
   public static final int    REGISTER             = 100;
   
   public static final int    DEREGISTER           = 101;
   
   public static final int    QUERY                = 102;
   
   public static final String VS_NAME              = "NAME";
   
   public static final String VS_PORT              = "PORT";
   
   public static final String VS_PREDICATES_VALUES = "VSensorPredicatesValues";
   
   public static final String VS_PREDICATES_KEYS   = "VSensorPredicatesKeys";
   
   public static final String VS_HOST              = "HOST";

   private static final String DEFAULT_FIELD = "address";
    
   public RegistryImp ( ) throws IOException {
      this.singleton = this;
      removeOutdatedEntriesThread.start( );
      queryParser = new QueryParser(DEFAULT_FIELD,analyzer);
   }
   
   public synchronized static RegistryImp getRegistry ( ) {
      if ( singleton == null ) try {
         singleton = new RegistryImp( );
      } catch ( IOException e ) {
         logger.fatal( e.getMessage( ) , e );
         return null;
      }
      return singleton;
   }
   
   private final transient static String SPACE_CHARACTER = " ";
   
   private static long                   counter         = 0;
   
   public synchronized void addVirtualSensor ( VirtualSensorIdentityBean newVS ) {
      VSAddress vsAddr = new VSAddress( newVS );
      removeVirtualSensor( newVS );
      /**
       * TODO, calling the remove method only when there is a change. To do so,
       * I can add a LRU hashmap in which the key is GUID and the value is the
       * hash code of the addressing and output predicates (concat). If there is
       * no value in the map or the value is the same as what I computed for the
       * new incoming virtual sensor, implies that there is no need for calling
       * the remove.
       */
      list.remove( vsAddr );
      try {
         IndexWriter writer = new IndexWriter( directory , analyzer, true );
         Document document = new Document( );
         document.add( new Field( _GUID , newVS.getGUID( ) , Field.Store.NO , Field.Index.UN_TOKENIZED ) );
         StringBuffer addresses = new StringBuffer( );
         StringBuffer addressKeys = new StringBuffer( );
         StringBuffer addressValues = new StringBuffer( );
         addresses.append( newVS.getVSName( ) ).append( SPACE_CHARACTER );
         for ( KeyValue predicate : newVS.getPredicates( ) ) {
            addresses.append( predicate.getKey( ) ).append( SPACE_CHARACTER ).append( predicate.getValue( ) ).append( SPACE_CHARACTER );
            addressKeys.append( predicate.getKey( ) ).append( SPACE_CHARACTER );
            addressValues.append( predicate.getValue( ) ).append( SPACE_CHARACTER );
         }
         document.add( new Field( DEFAULT_FIELD , addresses.toString( ) , Field.Store.NO , Field.Index.TOKENIZED ) );
         document.add( new Field( "key" , addressKeys.toString( ) , Field.Store.NO , Field.Index.TOKENIZED ) );
         document.add( new Field( "value" , addressValues.toString( ) , Field.Store.NO , Field.Index.TOKENIZED ) );
         document.add( new Field( "name" , newVS.getVSName( ) , Field.Store.NO , Field.Index.TOKENIZED ) );
         document.add( new Field( "host" , newVS.getRemoteAddress( ) , Field.Store.NO , Field.Index.UN_TOKENIZED ) );
         document.add( new Field( "port" , Integer.toString( newVS.getRemotePort( ) ) , Field.Store.NO , Field.Index.UN_TOKENIZED ) );
         writer.addDocument( document );
         counter++;
         if ( counter % 100 == 0 ) writer.optimize( );
         writer.close( );
         list.add( vsAddr );
      } catch ( IOException e ) {
         logger.fatal( e.getMessage( ) , e );
      }
   }
   
   public synchronized void removeListOfVirtualSensors ( ArrayList < VSAddress > list ) {
      if ( list == null || list.isEmpty( ) ) return;
      IndexReader indexReader;
      try {
         indexReader = IndexReader.open( directory );
         for ( VSAddress address : list )
            indexReader.deleteDocuments( new Term( _GUID , address.getGUID( ) ) );
         indexReader.close( );
         counter++;
      } catch ( IOException e ) {
         logger.error( e.getMessage( ) , e );
      }
   }
   
   public synchronized void removeVirtualSensor ( VirtualSensorIdentityBean vsensor ) {
      IndexReader indexReader;
      try {
         indexReader = IndexReader.open( directory );
         indexReader.deleteDocuments( new Term( _GUID , vsensor.getGUID( ) ) );
         indexReader.close( );
         counter++;
      } catch ( IOException e ) {
         logger.error( e.getMessage( ) , e );
      }
   }
   
   public ArrayList < VirtualSensorIdentityBean > findVSensor ( String query ) {
      return null;
   }
   
   private static File pidFile;
   
   public static void main ( String [ ] args ) throws Exception {
      if ( args.length < 2 ) {
         System.out.println( "You must specify the port on which the directory service will listen (e.g. 1882)" );
         System.out.println( "You must specify the interface IP on which the directory service will listen.(e.g. localhost)" );
         System.exit( 1 );
      }
      System.out.println( "Loading logging details from : " + DEFAULT_DIR_LOG4J_PROPERTIES );
      PropertyConfigurator.configure( DEFAULT_DIR_LOG4J_PROPERTIES );
      if ( PIDUtils.isPIDExist( PIDUtils.DIRECTORY_SERVICE_PID ) ) {
         System.out.println( "Error : Another GSN Directory Service is running." );
         System.exit( 1 );
      } else
         pidFile = PIDUtils.createPID( PIDUtils.DIRECTORY_SERVICE_PID );
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
      final Server server = new Server( );
      Connector connector = new SelectChannelConnector( );
      connector.setPort( port );
      server.setConnectors( new Connector [ ] { connector } );
      
      WebAppContext wac = new WebAppContext( );
      wac.setContextPath( "/" );
      wac.setResourceBase( DEFAULT_DIRECTORY_SERVER_WEB_APP );
      
      ServletHandler servletHandler = new ServletHandler( );
      servletHandler.addServletWithMapping( "gsn.registry.RegistryImp" , "/registry" );
      wac.setServletHandler( servletHandler );
      
      server.setHandler( wac );
      server.setStopAtShutdown( true );
      server.setSendServerVersion( false );
      server.start( );
      
      if ( logger.isInfoEnabled( ) ) logger.info( "[ok]" );
      Thread shutdownObserver = new Thread( new Runnable( ) {
         
         public void run ( ) {
            try {
               while ( true ) {
                  int value = PIDUtils.getFirstIntFrom( pidFile );
                  if ( value != '0' )
                     Thread.sleep( 2500 );
                  else
                     break;
               }
               server.stop( );
               RegistryImp.getRegistry( ).removeOutdatedEntriesThread.stopPlease( );
            } catch ( Exception e ) {
               logger.warn( "Shutdowning the webserver failed." , e );
               System.exit( 1 );
            }
            logger.warn( "GSN Directory server is stopped." );
            System.exit( 0 );
         }
      } );
      shutdownObserver.start( );
      
   }
   public Hits doQuery(String queryString) throws ParseException {
      try {
      IndexSearcher searcher = new IndexSearcher(directory);
      Query query = queryParser.parse(queryString);
      return searcher.search(query);
      }catch (IOException e) {
         logger.error( e.getMessage( ),e );
         return null;
      }    
   }
   
   public void doPost ( HttpServletRequest req , HttpServletResponse res ) throws ServletException , IOException {
      int requestType = Integer.parseInt( req.getHeader( RegistryImp.REQUEST ) );
      VirtualSensorIdentityBean sensorIdentityBean;
      switch ( requestType ) {
         case RegistryImp.REGISTER :
            sensorIdentityBean = new VirtualSensorIdentityBean( req );
            if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( ).append( "Register request received for VSName : " ).append( sensorIdentityBean.getVSName( ) ).toString( ) );
            addVirtualSensor( sensorIdentityBean );
            break;
         case  RegistryImp.DEREGISTER :
            sensorIdentityBean = new VirtualSensorIdentityBean( req );
            if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( ).append( "Deregister request received for VSName : " ).append( sensorIdentityBean.getVSName( ) ).toString( ) );
            removeVirtualSensor( sensorIdentityBean );
            break;
            /**
             * Query received from other gsn instances to identify a source.
             */
         case RegistryImp.QUERY :
            logger.error( "Not Implemeneted !!!" );
//            if ( logger.isDebugEnabled( ) ) logger.debug( "Query request received containg the following predicates : " );
//            Enumeration keys = req.getHeaders( RegistryImp.VS_PREDICATES_KEYS );
//            Enumeration values = req.getHeaders( RegistryImp.VS_PREDICATES_VALUES );
//            ArrayList < KeyValue > predicates = new ArrayList < KeyValue >( );
//            while ( keys.hasMoreElements( ) ) {
//               String key = ( String ) keys.nextElement( );
//               String value = ( String ) values.nextElement( );
//               if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( ).append( "[key=" ).append( key ).append( ",value=" ).append( value ).append( "]" ).toString( ) );
//               predicates.add( new KeyValueImp( key , value ) );
//            }
//            ArrayList < VirtualSensorIdentityBean > vsQueryResult = findVSensor( predicates );
//            if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( ).append( "The query resulted in " ).append( vsQueryResult.size( ) ).append( " results." ).toString( ) );
//            fillQueryRespond( res , vsQueryResult );
            break;
         default :
            if ( logger.isInfoEnabled( ) ) logger.info( "Request received at the register with unknow request type !!!" );
      }
   }
   
   private void fillQueryRespond ( HttpServletResponse res , ArrayList < VirtualSensorIdentityBean > vsensors ) {
      for ( VirtualSensorIdentityBean vsensor : vsensors ) {
         res.addHeader( RegistryImp.VS_NAME , vsensor.getVSName( ) );
         res.addHeader( RegistryImp.VS_PORT , Integer.toString( vsensor.getRemotePort( ) ) );
         res.addHeader( RegistryImp.VS_HOST , vsensor.getRemoteAddress( ) );
      }
   }
   
   class RemoveOutdatedEntries extends Thread {
      
      private final transient Logger logger   = Logger.getLogger( RemoveOutdatedEntries.class );
      
      private boolean                isActive = true;
      
      public void run ( ) {
         ArrayList < VSAddress > removeList = new ArrayList < VSAddress >( );
         while ( isActive ) {
            long currentTime = System.currentTimeMillis( );
            removeList.clear( );
            while ( true ) {
               VSAddress bean = list.get( 0 );
               if ( ( currentTime - REFERESH_INTERVAL ) < bean.getTime( ) ) break;
               removeList.add( list.remove( 0 ) );
            }
            RegistryImp.this.removeListOfVirtualSensors( removeList );
            try {
               sleep( REFERESH_INTERVAL / 2 );
            } catch ( InterruptedException e ) {
               logger.error( e.getMessage( ) , e );
            }
         }
         
      }
      
      public void stopPlease ( ) {
         isActive = false;
         interrupt( );
      }
   }
   
}

class VSAddress {
   
   private long   time = System.currentTimeMillis( );
   
   private String guid;
   
   /**
    * @return the key
    */
   public String getGUID ( ) {
      return guid;
   }
   
   /**
    * @return the Creation time of this object;
    */
   public long getTime ( ) {
      return time;
   }
   
   public VSAddress ( VirtualSensorIdentityBean key ) {
      this.guid = key.getGUID( );
   }
   
   public int hashCode ( ) {
      return guid.hashCode( );
   }
   
   public boolean equals ( Object obj ) {
      if ( this == obj ) return true;
      if ( obj == null ) return false;
      if ( getClass( ) != obj.getClass( ) ) return false;
      final VSAddress other = ( VSAddress ) obj;
      if ( guid == null ) {
         if ( other.guid != null ) return false;
      } else if ( !guid.equals( other.guid ) ) return false;
      return true;
   }
   
}