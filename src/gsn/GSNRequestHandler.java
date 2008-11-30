package gsn;

import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.notifications.GSNNotification;
import gsn.registry.MyConfig;
import gsn.registry.RequestInitializableRequestProcessor;
import gsn.wrappers.AbstractWrapper;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;

public class GSNRequestHandler implements RequestInitializableRequestProcessor {

  private static transient Logger logger = Logger.getLogger( GSNRequestHandler.class );

  private String                  remoteIP;

  public void init ( MyConfig pConfig ) {
    this.remoteIP = pConfig.getRemoteAddress( );
  }
  public void init ( String remoteAddress) {
    this.remoteIP = remoteAddress;
  }

  public boolean deliverData ( int notificationCode ,  Object [] fieldNamesInStreamElement ,  Object [] fieldValues , String timeStampStr ) {
    long timeStamp = Long.parseLong( timeStampStr );
    AbstractWrapper remoteWrapper = Mappings.getContainer( ).findNotificationCodeListener( notificationCode );
    if ( remoteWrapper == null ) { // This client is no more interested
      // in this notificationCode.
      if ( logger.isInfoEnabled( ) ) logger.info( "Invalid notification code recieved, query droped." );
      return false;
    } else {
      // /**
      // * If the received stream element from remote stream producer
      // * doesn't have the timed field inside, the<br>
      // * container will automatically insert the receive time to the
      // * stream element.<br>
      // */
      if ( timeStamp <= 0 ) timeStamp = System.currentTimeMillis( );
      StreamElement se = StreamElement.createElementFromXMLRPC(remoteWrapper.getOutputFormat( ), fieldNamesInStreamElement , fieldValues , timeStamp );
      if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( ).append( "data received for notification code *" ).append( notificationCode ).toString( ) );
      return remoteWrapper.manualDataInsertion(se );
    }
  }
  /**
   * If GSN wants to register a request to a virtual sensor on this machine.
   * @param port
   * @param virtualSensorName, the name of the virtual sensor for which this gsn received a query.
   * @param query, the query from the remote machine.
   * @param notificationCode.
   * @return
   */
  public boolean registerQuery ( String url, String virtualSensorName , String query , int notificationCode ) {
    if ( virtualSensorName == null || ( virtualSensorName = virtualSensorName.trim( ).toLowerCase( ) ).length( ) == 0 ) {
      logger.warn( "Bad request received for Data_strctutes" );
      return false;
    }
    VSensorConfig sensorConfig = Mappings.getVSensorConfig( virtualSensorName );
    if ( sensorConfig == null ) {
      logger.info( "Request received for unknown v-sensor : " + virtualSensorName );
      return false;
    }

    GSNNotification interest = new GSNNotification( url , virtualSensorName , query , notificationCode );
    if ( logger.isInfoEnabled( ) )
      logger.info( new StringBuilder( ).append( "REGISTER REQUEST FOR " ).append( virtualSensorName ).append( " received from :" ).append( interest.getRemoteAddress( ) ).append( ":" ).append(
          interest.getRemotePort( ) ).append( " under the code " ).append( notificationCode ).append( " With the query of *" ).append( query ).append( "*" )
          .toString( ) );
    Mappings.getContainer( ).addNotificationRequest( virtualSensorName , interest );
    if ( logger.isDebugEnabled( ) ) logger.debug( "Respond sent to the requestee." );
    return true;
  }

  /**
   * I want to drop this method and use deliverData returning false for
   * removing the query.
   * 
   * @param notificationCode
   * @return
   */
  public boolean removeQuery ( int notificationCode ) {
    // if ( logger.isInfoEnabled( ) ) logger.info( new StringBuilder(
    // ).append( "DEREGISTER REQUEST WITH CODE :" ).append( notificationCode
    // ).append( " received" ).toString( ) );
    // Mappings.getContainer( ).removeNotificationRequest(notificationCode );
    // if ( logger.isDebugEnabled( ) ) logger.debug( "Respond sent to the
    // requestee." );


    return true;
  }

  /**
   * Empty structure means that the virtualSensorName is not valid.
   * 
   * @param virtualSensorName
   * @return
   */
  public String [ ][ ] getOutputStructure ( String virtualSensorName ) {
    if ( virtualSensorName == null || ( virtualSensorName = virtualSensorName.trim( ).toLowerCase( ) ).length( ) == 0 ) {
      logger.warn( "Bad request received for Data_strctutes" );
      return new String [ ] [ ] {};
    }
    VSensorConfig sensorConfig = Mappings.getVSensorConfig( virtualSensorName );
    if ( sensorConfig == null ) {
      logger.warn( "Requested virtual sensor doesn't exist >" + virtualSensorName + "<." );
      return new String [ ] [ ] {};
    }
    if ( logger.isInfoEnabled( ) ) logger.info( new StringBuilder( ).append( "Structure request for *" ).append( virtualSensorName ).append( "* received." ).toString( ) );
    return sensorConfig.getRPCFriendlyOutputStructure( );
  }

  /*** NEW METHODS FOR NEW JRUBY ***/
  /**
   * If GSN wants to register a request to a virtual sensor on this machine.
   * @param port
   * @param virtualSensorName, the name of the virtual sensor for which this gsn received a query.
   * @param query, the query from the remote machine.
   * @param notificationCode.
   * @return
   */
  public void consumeRegisterQueryRequest ( String url , String virtualSensorName , String query , int notificationCode ) {
	  url = url.replace("/127.0.0.1:", "/"+remoteIP+":");
    GSNNotification interest = new GSNNotification( url, virtualSensorName , query , notificationCode );
    if ( logger.isInfoEnabled( ) )
      logger.info( new StringBuilder( ).append( "REGISTER REQUEST FOR " ).append( virtualSensorName ).append( " received from :" ).append( interest.getRemoteAddress( ) ).append( ":" ).append(
          interest.getRemotePort( ) ).append( " under the code " ).append( notificationCode ).append( " With the query of *" ).append( query ).append( "*" )
          .toString( ) );
    Mappings.getContainer( ).addNotificationRequest( virtualSensorName , interest );
    if ( logger.isDebugEnabled( ) ) logger.debug( "Respond sent to the requestee." );
  }

  public static String[][] parseOutputStructureFromREST(String output) {
    StringTokenizer st = new StringTokenizer(output,",",false);
    String[][] toReturn = new String[st.countTokens()][2] ;
    int i=0;
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      toReturn[i++] = token.split("=>");
    }
    return toReturn;
  }

  public static boolean sendRegisterQueryRequest(String query,String remoteVSName, String windowSize, String slideValue, String remoteHost,int remotePort,String localHost, int  localPort,int notificationCode) {
    String url = "http://"+remoteHost+":"+remotePort+"/gsn/register/"+remoteVSName+"/"+windowSize+"/"+slideValue+"/notify/"+localHost+"/"+localPort+"/"+notificationCode;
    PostMethod post = new PostMethod(url);
    HttpClient client = new HttpClient();
    post.setRequestBody(new org.apache.commons.httpclient.NameValuePair[] {new NameValuePair("query",query)});
    int status = -1;
    try {
      status = client.executeMethod(post);
      System.out.println(post.getResponseBodyAsString());
    } catch (HttpException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return status == 201;

  }

  public static boolean deliverDataFromRest( int notificationCode ,  String [] fieldNamesInStreamElement ,  Object [] fieldValues ) {
//    long timeStamp = NumberUtils.toLong(timeStampStr ,0);
    
    AbstractWrapper remoteWrapper = Mappings.getContainer( ).findNotificationCodeListener( notificationCode );
    if ( remoteWrapper == null ) { // This client is no more interested in this notificationCode.
      if ( logger.isInfoEnabled( ) ) logger.info( "Invalid notification code >"+notificationCode+"<recieved, query droped." );
      return false;
    } else {
      // /**
      // * If the received stream element from remote stream producer
      // * doesn't have the timed field inside, the<br>
      // * container will automatically insert the receive time to the
      // * stream element.<br>
      // */
      StreamElement se = StreamElement.createElementFromREST(remoteWrapper.getOutputFormat( ), fieldNamesInStreamElement , fieldValues );
      if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( ).append( "data received for notification code *" ).append( notificationCode ).toString( ) );
      return remoteWrapper.manualDataInsertion(se);
    }
  }

}
