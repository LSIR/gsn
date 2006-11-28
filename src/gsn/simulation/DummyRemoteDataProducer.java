package gsn.simulation;

import gsn.Container;
import gsn.Main;
import gsn.beans.DataField;
import gsn.registry.RegistryImp;
import gsn.utils.TCPConnPool;
import gsn.wrappers.AbstractWrapper;
import gsn.wrappers.DataListener;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class DummyRemoteDataProducer extends AbstractWrapper {
   
   private static final transient Logger logger = Logger.getLogger( DummyDataListener.class );
   
   private String                        host;
   
   private int                           port;
   
   private String                        remoteVSName;
   
   public boolean initialize () {
      host = getActiveAddressBean( ).getPredicateValue( "host" );
      if ( host == null ) {
         logger.error( "The host for stream source can't be Null." );
         return false;
      }
      port = Integer.parseInt( getActiveAddressBean( ).getPredicateValue( "port" ) );
      if ( port <= 0 ) {
         logger.error( "The port " + port + " is invalid for a stream source." );
         return false;
      }
      remoteVSName =getActiveAddressBean( ).getPredicateValue( "name" );
      if ( remoteVSName == null ) {
         logger.warn( "The \"NAME\" parameter of the AddressBean which corresponds to the remote Virtual Sensor is missing" );
         return false;
      }
      if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( ).append( "The DummyRemoteDataProducer is initialized with :[host=" ).append( host ).append( ":" ).append( port ).append(
         ", VSName=" ).append( remoteVSName ).append( "]" ).toString( ) );
      return true;
   }
   
   public String addListener ( DataListener input ) {
      DummyDataListener dataListener = ( DummyDataListener ) input;
      String viewName = dataListener.getViewName( );
      HttpQueryForAllValuesAt( dataListener.getContainerPort( ) );
      return viewName;
   }
   
   private void HttpQueryForAllValuesAt ( int containerPort ) {
      String notificationCode = Main.tableNameGenerator( );
      if ( host.indexOf( "http://" ) < 0 ) host = "http://" + host;
      String destination = host + ":" + port + "/gsn";
      if ( logger.isInfoEnabled( ) ) logger.info( new StringBuilder( ).append( "Wants to send message to : " ).append( destination ).toString( ) );
      
      PostMethod postMethod = new PostMethod( destination );
      postMethod.addRequestHeader( Container.REQUEST , Integer.toString( Container.REGISTER_PACKET ) );
      postMethod.addRequestHeader( RegistryImp.VS_PORT , Integer.toString( containerPort ) );
      ArrayList < String > tables = new ArrayList < String >( );
      tables.add( remoteVSName );
      StringBuffer generateQuery = QueryGenerator.generateQuery( "\"" + remoteVSName + ".DATA" + "\"" , tables , 2 , tables.size( ) , 10000000 );
      
      postMethod.addRequestHeader( Container.VS_QUERY , generateQuery.toString( ) );
      postMethod.addRequestHeader( Container.QUERY_VS_NAME , remoteVSName );
      postMethod.addRequestHeader( Container.NOTIFICATION_CODE , notificationCode );
      
      int statusCode = TCPConnPool.executeMethod( postMethod );
      if ( statusCode == -1 ) {
         logger.warn( "Message couldn't be sent to :" + postMethod.getHostConfiguration( ).getHostURL( ) );
      }
   }
   
   public Collection < DataField > getOutputFormat ( ) {
      return null;
   }

  
   public void finalize ( ) {
   }
}
