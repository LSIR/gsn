package gsn.wrappers;

import gsn.Container;
import gsn.Main;
import gsn.Mappings;
import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.shared.Registry;
import gsn.utils.TCPConnPool;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class RemoteDS extends Wrapper {
   
   private final transient Logger     logger                 = Logger.getLogger( RemoteDS.class );
   
   private ArrayList < DataField >    strcture               = null;
   
   private String                     remoteVSName;
   
   private ArrayList < StringBuffer > registeredWhereClauses = new ArrayList < StringBuffer >( );
   
   private String                     host;
   
   private int                        port;
   
   public boolean initialize (  ) {
      AddressBean addressBean =getActiveAddressBean( );
      host = addressBean.getPredicateValue( "host" );
      if ( host == null || host.trim( ).length( ) == 0 ) {
         logger.warn( "The >host< parameter is missing from the RemoteDS wrapper." );
         return false;
      }
      String portRaw = addressBean.getPredicateValue( "port" );
      if ( portRaw == null || portRaw.trim( ).length( ) == 0 ) {
         logger.warn( "The >port< parameter is missing from the RemoteDS wrapper." );
         return false;
      }
      try {
         port = Integer.parseInt( portRaw );
         if ( port > 65000 || port <= 0 ) throw new Exception( "Bad port No" + port );
      } catch ( Exception e ) {
         logger.warn( "The >port< parameter is not a valid integer for the RemoteDS wrapper." );
         return false;
      }
      this.remoteVSName = addressBean.getPredicateValue( "name" );
      if ( this.remoteVSName == null ) {
         logger.warn( "The \"NAME\" paramter of the AddressBean which corresponds to the remote Virtual Sensor is missing" );
         return false;
      }
      this.strcture = askForStrcture( );
      if ( this.strcture == null ) {
         logger.warn( "The initialization of the ** virtual sensor failed due to *askForStrcture* failure." );
         return false;
      }
      Mappings.getContainer( ).addRemoteStreamSource( getDBAlias( ) , this );
      return true;
   }
   
   /**
    * @return Null if the RemoteDS can't obtain the data strcture from the
    */
   private ArrayList < DataField > askForStrcture ( ) {
      
      if ( host.indexOf( "http://" ) < 0 ) host = "http://" + host;
      String destination = new StringBuilder( ).append( host ).append( ":" ).append( port ).append( "/gsn" ).toString( );
      if ( logger.isInfoEnabled( ) ) logger.info( new StringBuilder( ).append( "Wants to ask for structure from : " ).append( destination ).toString( ) );
      PostMethod postMethod = new PostMethod( destination );
      postMethod.setRequestHeader( Container.REQUEST , Integer.toString( Container.REQUEST_OUTPUT_FORMAT ) );
      postMethod.setRequestHeader( Container.QUERY_VS_NAME , remoteVSName );
      if ( logger.isDebugEnabled( ) ) {
         logger.debug( new StringBuilder( "Post request contains : " ).append( "QUERY_VS_NAME = " ).append( remoteVSName ).append( ";" ).append( "Request Type : " ).append(
            Container.REQUEST_OUTPUT_FORMAT ).toString( ) );
      }
      int statusCode = TCPConnPool.executeMethod( postMethod );
      if ( statusCode == -1 ) {
         logger.warn( new StringBuilder( ).append( "Message couldn't be sent to :" ).append( postMethod.getHostConfiguration( ).getHostURL( ) ).toString( ) );
         return null;
      }
      
      if ( postMethod.getResponseHeader( Container.RESPONSE_STATUS ) == null || postMethod.getResponseHeader( Container.RESPONSE_STATUS ).getValue( ).equals( Container.INVALID_REQUEST ) ) {
         logger.debug( "The respond from server : " + postMethod.getResponseHeader( Container.RESPONSE_STATUS ) );
         return null;
      }
      ArrayList < DataField > outputStreamStruecture = null;
      ObjectInputStream ois = null;
      try {
         ois = new ObjectInputStream( postMethod.getResponseBodyAsStream( ) );
         outputStreamStruecture = ( ArrayList < DataField > ) ois.readObject( );
      } catch ( IOException e ) {
         logger.warn( e.getMessage( ) , e );
      } catch ( ClassNotFoundException e ) {
         logger.warn( e.getMessage( ) , e );
      } finally {
         if ( ois != null ) try {
            ois.close( );
         } catch ( IOException e ) {
            logger.debug( e.getMessage( ) , e );
         }
      }
      
      return outputStreamStruecture;
   }
   
   /**
    * Sends in fact two requests, one <code>Deregister</code> request for
    * removing the previously resgistered <br>
    * query and afterwards it will send a <code>register</code> request for
    * adding the new version of the query.<br>
    */
   private void refreshRemotelyRegisteredQuery ( ) {
      String notificationCode = getDBAlias( );
      String query = new StringBuffer( "SELECT * FROM " ).append( remoteVSName ).append( " WHERE " ).append( getWhereClausesAllTogher( ) ).append( " ORDER BY " ).append( remoteVSName ).append(
         ".TIMED DESC LIMIT 1 OFFSET 0" ).toString( ).replace( "\"" , "" );
      if ( host.indexOf( "http://" ) < 0 ) host = "http://" + host;
      String destination = host + ":" + port + "/gsn";
      if ( logger.isDebugEnabled( ) )
         logger.debug( new StringBuilder( ).append( "Wants to send message to : " ).append( destination ).append( "  for DEREGISTERING the previous query" ).toString( ) );
      PostMethod postMethod = new PostMethod( destination );
      postMethod.addRequestHeader( Container.REQUEST , Integer.toString( Container.DEREGISTER_PACKET ) );
      postMethod.addRequestHeader( Registry.VS_PORT , Integer.toString( Main.getContainerConfig( ).getContainerPort( ) ) );
      postMethod.addRequestHeader( Container.VS_QUERY , query );
      postMethod.addRequestHeader( Container.QUERY_VS_NAME , remoteVSName );
      postMethod.addRequestHeader( Container.NOTIFICATION_CODE , notificationCode );
      
      int statusCode = TCPConnPool.executeMethod( postMethod );
      if ( statusCode == -1 ) {
         logger.warn( "Message couldn't be sent to :" + postMethod.getHostConfiguration( ).getHostURL( ) );
      }
      
      if ( logger.isDebugEnabled( ) )
         logger.debug( new StringBuilder( ).append( "Wants to send message to : " ).append( destination ).append( " with the query ->" ).append( query ).append( "<-" ).toString( ) );
      postMethod.setRequestHeader( Container.REQUEST , Integer.toString( Container.REGISTER_PACKET ) );
      
      statusCode = TCPConnPool.executeMethod( postMethod );
      if ( statusCode == -1 ) {
         logger.warn( "Message couldn't be sent to :" + postMethod.getHostConfiguration( ).getHostURL( ) );
      }
   }
   
   public String addListener ( DataListener dataListener ) {
      StringBuffer completeMergedWhereClause = dataListener.getCompleteMergedWhereClause( remoteVSName );
      registeredWhereClauses.add( completeMergedWhereClause );
      refreshRemotelyRegisteredQuery( );
      return super.addListener( dataListener );
   }
   
   public void removeListener ( DataListener dataListener ) {
      registeredWhereClauses.remove( dataListener.getCompleteMergedWhereClause( remoteVSName ) );
      if ( registeredWhereClauses.size( ) > 0 ) refreshRemotelyRegisteredQuery( );
      super.removeListener( dataListener );
   }
   
   /**
    * The container is going to notify the <code>RemoteDS</code> about arrival
    * of a new data by calling this method. (note that, container will first
    * insert the data into the appropriate database and then calls the following
    * method).
    */
   public void remoteDataReceived ( ) {
      if ( logger.isDebugEnabled( ) ) logger.debug( "There are results, is there any listeners ?" );
      for ( Iterator < DataListener > iterator = listeners.iterator( ) ; iterator.hasNext( ) ; ) {
         DataListener dataListener = iterator.next( );
         boolean results = getStorageManager( ).isThereAnyResult( dataListener.getViewQuery( ) );
         if ( results ) {
            if ( logger.isDebugEnabled( ) )
               logger.debug( new StringBuilder( ).append( "There are listeners, notify the " ).append( dataListener.getInputStream( ).getInputStreamName( ) ).append( " inputStream" ).toString( ) );
            // TODO :DECIDE WHETHER TO INFORM THE CLIENT OR NOT (TIME
            // TRIGGERED. DATA TRIGGERED)
            dataListener.dataAvailable( );
         }
      }
   }
   
   public Collection < DataField > getOutputFormat ( ) {
      return strcture;
   }
   
   private String getWhereClausesAllTogher ( ) {
      StringBuffer result = new StringBuffer( );
      for ( StringBuffer whereClause : registeredWhereClauses ) {
         result.append( "(" ).append( whereClause ).append( ")" ).append( " OR " );
      }
      return result.delete( result.length( ) - " OR ".length( ) , result.length( ) ).toString( );
   }

   public void finalize ( ) {
   //TODO   
   }
}
