package gsn.notifications;

import gsn.beans.StreamElement;
import gsn.storage.DataEnumerator;
import gsn.storage.SQLUtils;
import gsn.utils.CaseInsensitiveComparator;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
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
   
   private XmlRpcClient                    client             = new XmlRpcClient ( );
   
   private  XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl ( );
   
  public GSNNotification ( int port , String remoteHost , String virtualSensorName , String query , int notificationCode ) {
      this.remotePort = port;
      this.remoteAddress = remoteHost;
      this.prespectiveVirtualSensor = virtualSensorName;
      TreeMap rewritingInfo = new TreeMap(new CaseInsensitiveComparator());
      rewritingInfo.put("wrapper", virtualSensorName);
      this.query = SQLUtils.newRewrite(query, rewritingInfo) ;
      this.notificationCode = notificationCode;
      try {
         config.setServerURL ( new URL ( "http://" + remoteHost +":"+remotePort+ "/gsn-handler" ) );
         client.setConfig ( config );
      } catch ( MalformedURLException e1 ) {
         logger.warn ( "GSNNotification initialization failed! : "+e1.getMessage ( ) , e1 );
      }
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
      Object [ ] params = new Object [ ] {notificationCode,data.getFieldNames( ),data.getDataInRPCFriendly(),Long.toString( data.getTimeStamp( ) )};
      boolean result = false;
      try {
         result =  (Boolean) client.execute ("gsn.deliverData", params);
      } catch ( XmlRpcException e ) {
         if (logger.isInfoEnabled( ))
         logger.info("Couldn't notify the remote host : "+config.getServerURL( )+e.getMessage( ),e);
         return false;
      }
      if (result==false) {
         logger.fatal( "The remote is not interested anymore, the notification should be removed (not implemented)");
         return false;
      }
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
