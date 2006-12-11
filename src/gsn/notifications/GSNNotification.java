package gsn.notifications;

import gsn.beans.StreamElement;
import gsn.storage.DataEnumerator;
import gsn.storage.SQLUtils;
import gsn.utils.CaseInsensitiveComparator;
import java.sql.PreparedStatement;
import java.util.TreeMap;
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
   private CharSequence                      query;
   
   private String                      prespectiveVirtualSensor;
   
   private transient PreparedStatement preparedStatement;
   
   private transient long              latestVisitTime;
   
   private int                         notificationCode;
   
  public GSNNotification ( int port , String remoteHost , String virtualSensorName , String query , int notificationCode ) {
      this.remotePort = port;
      this.remoteAddress = remoteHost;
      this.prespectiveVirtualSensor = virtualSensorName;
      TreeMap rewritingInfo = new TreeMap(new CaseInsensitiveComparator());
      rewritingInfo.put("wrapper", virtualSensorName);
      this.query = SQLUtils.newRewrite(query, rewritingInfo) ;
      this.notificationCode = notificationCode;
   }
   
   /**
    * @return Returns the query.
    */
   
   public CharSequence getQuery ( ) {
      return query;
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
   public int getNotificationCode ( ) {
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
      return input.notificationCode == notificationCode;
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
   
   private boolean notifyPeerAboutData ( StreamElement data ) {
      if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( ).append( "Wants to send message to : " ).append( remoteAddress ).toString( ) );
      logger.fatal( "Not implemented" );
      return true;
   }
   
   public boolean send ( DataEnumerator data ) {
      StreamElement se;
      while ( data.hasMoreElements( ) ) {
         se = data.nextElement( );
         boolean result = notifyPeerAboutData( se );
         if ( result == false ) {
            logger.warn( new StringBuilder( ).append( "The result of delivering data was false, the remote client is not interested anymore, query dropped." ).toString( ) );
            return false;
         }
      }
      return true;
   }
}
