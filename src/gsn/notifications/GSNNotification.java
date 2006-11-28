package gsn.notifications;

import gsn.Container;
import gsn.beans.StreamElement;
import gsn.registry.RegistryImp;
import gsn.utils.TCPConnPool;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * Note that : 1. Set the REQUEST HEADER PARAMETER DESCRIBING THE BODY OF THE
 * REQUEST. 2. ONCE YOU SET THE BODY AS STREAM THE REQUEST PARAMETER DOESN'T
 * WORK HENCE USE THE REQUEST-HEADER-PARAMETER.
 */
public class GSNNotification extends NotificationRequest {
   
   private final transient Logger      logger = Logger.getLogger( GSNNotification.class );
   
   private int                         remotePort;
   
   private String                      remoteAddress;
   
   /**
    * The <code>query</code> contains all the data in
    * <code>queryWithoutDoubleQuots</code><br>
    * except without using any double quotation.
    */
   private String                      query;
   
   private String                      prespectiveVirtualSensor;
   
   private transient PreparedStatement preparedStatement;
   
   private transient long              latestVisitTime;
   
   private String                      notificationCode;
   
   private StringBuilder               queryWithoutDoubleQuots;
   
   public GSNNotification ( HttpServletRequest req ) {
      this.remoteAddress = req.getRemoteAddr( );
      this.notificationCode = req.getHeader( Container.NOTIFICATION_CODE );
      this.prespectiveVirtualSensor = req.getHeader( Container.QUERY_VS_NAME );
      this.remotePort = Integer.parseInt( req.getHeader( RegistryImp.VS_PORT ) );
      this.query = req.getHeader( Container.VS_QUERY );
      this.queryWithoutDoubleQuots = new StringBuilder( req.getHeader( Container.VS_QUERY ).trim( ).replace( "\"" , "" ) );
   }
   
   /**
    * @return Returns the query.
    */
   
   public StringBuilder getQuery ( ) {
      return queryWithoutDoubleQuots;
   }
   
   public String getRawQuery ( ) {
      return query;
   }
   
   public boolean send ( ) {
      Enumeration < StreamElement > data = getData( );
      StreamElement se;
      while ( data.hasMoreElements( ) ) {
         se = data.nextElement( );
         int result = notifyPeerAboutData( getRemoteAddress( ) , getRemotePort( ) , getNotificationCode( ) , se );
         if ( result != 0 && result != 200 ) {
            logger.warn( new StringBuilder( ).append( "The result of HTTP response was :" ).append( result ).append( " ,Thus notification failed" ).toString( ) );
            return false;
         }
      }
      return true;
   }
   
   /**
    * @return Returns the address.
    */
   public String getRemoteAddress ( ) {
      return remoteAddress;
   }
   
   /**
    * @return Returns the prespectiveVirtualSensor.
    */
   public String getPrespectiveVirtualSensor ( ) {
      return prespectiveVirtualSensor;
   }
   
   /**
    * @return Returns the port.
    */
   public int getRemotePort ( ) {
      return remotePort;
   }
   
   /**
    * @return Returns the notificationCode.
    */
   public String getNotificationCode ( ) {
      return notificationCode;
   }
   
   /**
    * @return Returns the preparedStatement.
    */
   public PreparedStatement getPreparedStatement ( ) {
      return preparedStatement;
   }
   
   /**
    * @param preparedStatement The preparedStatement to set.
    */
   public void setPreparedStatement ( PreparedStatement preparedStatement ) {
      this.preparedStatement = preparedStatement;
   }
   
   /**
    * @return Returns the lastRespondTime.
    */
   public long getLatestVisitTime ( ) {
      return latestVisitTime;
   }
   
   /**
    * @param lastRespondTime The lastRespondTime to set.
    */
   public void setLatestVisitTime ( long lastRespondTime ) {
      this.latestVisitTime = lastRespondTime;
   }
   
   public boolean equals ( Object obj ) {
      if ( obj == null || !( obj instanceof GSNNotification ) ) return false;
      GSNNotification input = ( GSNNotification ) obj;
      return input.notificationCode.equals( notificationCode );
   }
   
   public int hashCode ( ) {
      return toString( ).hashCode( );
   }
   
   public String toString ( ) {
      StringBuffer result = new StringBuffer( "GSN Notification Request : [ " );
      result.append( "Address = " ).append( remoteAddress ).append( ", " );
      result.append( "Port = " ).append( remotePort ).append( ", " );
      // result.append ( "InputVariableName = " ).append (
      // remoteInputVariableName ).append ( ", " ) ;
      result.append( "Query = " ).append( query ).append( ", " );
      result.append( "PrespectiveVS = " ).append( prespectiveVirtualSensor ).append( ", " );
      result.append( "NotificationCode = " ).append( notificationCode );
      result.append( "]" );
      return result.toString( );
   }
   
   private int notifyPeerAboutData ( String host , int port , String notificationCode , Serializable data ) {
      if ( host.indexOf( "http://" ) < 0 ) host = "http://" + host;
      String destination = host + ":" + port + "/gsn";
      if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( ).append( "Wants to send message to : " ).append( destination ).toString( ) );
      
      PostMethod postMethod = new PostMethod( destination );
      postMethod.addRequestHeader( Container.NOTIFICATION_CODE , notificationCode );
      postMethod.addRequestHeader( Container.REQUEST , Integer.toString( Container.DATA_PACKET ) );
      int statusCode = -1;
      try {
         postMethod.setRequestEntity( new InputStreamRequestEntity( InputStreamAdapter( data ) ) );
         statusCode = TCPConnPool.executeMethod( postMethod , true );
      } catch ( Exception e ) {
         e.printStackTrace( );
      }
      try {
         if ( statusCode != -1 ) if ( postMethod.getResponseHeader( Container.RESPONSE_STATUS ).getValue( ).equals( Container.INVALID_REQUEST ) ) statusCode = -10;
      } catch ( NullPointerException e ) {
         e.printStackTrace( );
      }
      postMethod.releaseConnection( );
      return statusCode;
   }
   
   public static ByteArrayInputStream InputStreamAdapter ( Serializable so ) throws IOException {
      ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream( );
      ObjectOutputStream oos = new ObjectOutputStream( arrayOutputStream );
      oos.writeObject( so );
      oos.flush( );
      oos.close( );
      return new ByteArrayInputStream( arrayOutputStream.toByteArray( ) );
   }
}
