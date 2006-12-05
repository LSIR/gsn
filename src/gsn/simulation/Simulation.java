package gsn.simulation;

import gsn.VSensorLoader;
import gsn.beans.AddressBean;
import gsn.beans.InputStream;
import gsn.beans.StreamSource;
import gsn.beans.VSensorConfig;
import gsn.registry.Registry;
import gsn.utils.CaseInsensitiveComparator;
import gsn.wrappers.DataListener;
import java.io.FileInputStream;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;
import org.mortbay.jetty.Server;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class Simulation {
   
   private DummyRemoteDataProducer       dummyRemoteDataSource;
   
   public static int                     START_PORT_INDEX;
   
   private static transient final Logger logger                                    = Logger.getLogger( Simulation.class );
   
   private static final int              DELAY_BETWEEN_EACH_REGISTERATION_REQUESTS = 30000;
   
   public Simulation ( VSensorConfig configuration , int numOfThreads ) throws Exception {
      HashMap < String , Object > context1 = new HashMap <  String , Object >(  );
      int portCounter = 0;
      for ( int i = 0 ; i < numOfThreads ; i++ ) {
         // TODO: CHANGING FROM JETTY TO A LIGHTER SERVLET ENGINE.
         // try {
         // servers [ i ] = new Server () ;
         // HttpContext context = servers [ i ].getContext ( "/" ) ;
         // servers [ i ].addListener ( ":" + ( START_PORT_INDEX +
         // ++portCounter
         // ) ) ;
         // ServletHandler servletHandler = new ServletHandler () ;
         // servletHandler.addServlet ( "GSN" , "/gsn" ,
         // "gsn.simulation.SimHttpListener" ) ;
         // context.addHandler ( servletHandler ) ;
         // servers [ i ].start () ;
         // }
         // catch ( Exception e ) {
         // System.out.println ( e.getMessage () ) ;
         // // logger.warn ( "The Port :" + ( portounter ) + " is used,
         // the port
         // is skipped" ) ;
         // i-- ;
         // continue ;
         // }
         if ( logger.isInfoEnabled( ) ) logger.info( new StringBuilder( ).append( "The port " ).append( portCounter ).append( " used successfully." ).toString( ) );
         if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( ).append( "The Http Server of the " ).append( i ).append( "th container initialized" ).toString( ) );
         for ( InputStream inputStream : configuration.getInputStreams( ) ) {
            for ( StreamSource streamSource : inputStream.getSources( ) ) {
               for ( AddressBean addressBean : streamSource.getAddressing( ) ) {
                  if ( dummyRemoteDataSource == null ) {
                     dummyRemoteDataSource = new DummyRemoteDataProducer( );
                     if ( addressBean.isAbsoluteAddressSpecified( ) ) {// Absolute
                        context1.put( VSensorLoader.STREAM_SOURCE , streamSource );
                        context1.put( VSensorLoader.INPUT_STREAM , inputStream );
                        context1.put( Registry.VS_HOST , addressBean.getPredicateValue( Registry.VS_HOST ) );
                        context1.put( Registry.VS_PORT , addressBean.getPredicateValue( Registry.VS_PORT ) );
                        context1.put( Registry.VS_NAME , addressBean.getPredicateValue( Registry.VS_NAME ) );
                        context1.put( DummyDataListener.CONTAINER_PORT , ( START_PORT_INDEX + i ) );
                        boolean output = dummyRemoteDataSource.initialize(  );
                        if ( !output ) {
                           logger.error( "dummyRemoteDataSource's initialization failed." );
                           System.exit( 1 );
                        }
                     }
                  }
                  DataListener dbDataListener = new DummyDataListener(inputStream, streamSource);
                  dummyRemoteDataSource.addListener( dbDataListener );
               }
            }
         }
         dummyRemoteDataSource = null;
         if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( ).append( "The input stream of the " ).append( i ).append( "th container initialized" ).toString( ) );
         // Thread.sleep ( ( int ) ( Math.random () *
         // DELAY_BETWEEN_EACH_REGISTERATION_REQUESTS ) + 1 ) ;
         Thread.sleep( DELAY_BETWEEN_EACH_REGISTERATION_REQUESTS );
      }
      
   }
   
   /**
    * The args[0]; the first parameter; is the configuration file address. The
    * args[1]; the second parameter; is the log4J param. The args[2]; the third
    * parameter; is the number of dummy clients to be simulated. The args[3];
    * the forth parameter; is the start index of the port (the program will use
    * portStart+1 to portIndex+args[2])
    * 
    * @throws Exception
    */
   public static void main ( String [ ] args ) throws Exception {
      PropertyConfigurator.configure( args[ 1 ] );
      int THREAD_COUNT = Integer.parseInt( args[ 2 ] );
      START_PORT_INDEX = Integer.parseInt( args[ 3 ] );
      IBindingFactory bfact;
      IUnmarshallingContext uctx;
      bfact = BindingDirectory.getFactory( VSensorConfig.class );
      uctx = bfact.createUnmarshallingContext( );
      VSensorConfig configuration = ( VSensorConfig ) uctx.unmarshalDocument( new FileInputStream( args[ 0 ] ) , null );
      Simulation simulation = new Simulation( configuration , THREAD_COUNT );
   }
}
