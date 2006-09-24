package gsn.vsensor ;

import gsn.notifications.NotificationRequest ;
import gsn.wrappers.RemoteDS ;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public interface Container {

   public static final String REQUEST = "REQUEST" ;

   public static final int REGISTER_PACKET = 110 ;

   public static final int DEREGISTER_PACKET = 111 ;

   public static final int DATA_PACKET = 112 ;

   public static final int DATA_STRCTURE_REQUEST = 113 ;

   public static final int ONE_SHOT_QUERY_EXECUTION_REQUEST = 114 ;

   public static final int LIST_VIRTUAL_SENSORS = 115 ;

   public static final String RESPOND = "RESPOND" ;

   public static final String RES_HEADER_DATA_FIELD_DESCRIPTION = "RES_HEADER_DATA_FIELD_DESCRIPTION" ;

   public final String RES_HEADER_DATA_FIELD_TYPE = "RES_HEADER_DATA_FIELD_TYPE" ;

   public final String RES_HEADER_DATA_FIELD_NAME = "RES_HEADER_DATA_FIELD_NAME" ;

   public final String QUERY_VS_NAME = "QUERY_VS_NAME" ;

   public final String NOTIFICATION_CODE = "NOTIFICATION_CODE" ;

   public final String INVALID_REQUEST = "INVALID_NOTIFICATION_CODE_RECEIVED" ;

   public final String RES_STATUS = "RES_STATUS" ;

   public final String REQUEST_HANDLED_SUCCESSFULLY = "VALID_NOTIFICATION_CODE_RECEIVED" ;

   public final String DATA = "DATA" ;

   public final String VS_QUERY = "VS_QUERY" ;

   public final String STREAM_SOURCE_ACTIVE_ADDRESS_BEAN = "STREAM_SOURCE_ACTIVE_ADDRESS_BEAN" ;

   public void publishData ( VirtualSensor sensor ) ;

   public void addRemoteStreamSource ( String alias , RemoteDS remoteDS ) ;

   public void addNotificationRequest ( String localVirtualSensorName , NotificationRequest notificationRequest ) ;

   public void removeRemoteStreamSource ( String alias ) ;

   public void removeAllResourcesAssociatedWithVSName ( String virtualSensorName ) ;

   public NotificationRequest [ ] getAllNotificationRequests ( ) ;

   public void removeNotificationRequest ( String localVirtualSensorName , NotificationRequest notificationRequest ) ;

   public void removeNotificationRequest ( NotificationRequest notificationRequest ) ;
}
