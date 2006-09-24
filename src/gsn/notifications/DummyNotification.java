package gsn.notifications ;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class DummyNotification extends NotificationRequest {

   private String notificationCode ;

   public DummyNotification ( String notificationCode ) {
      this.notificationCode = notificationCode ;
   }

   public StringBuilder getQuery ( ) {
      throw new UnsupportedOperationException ( "This method shouldn't be called by others." ) ;
   }

   public boolean send ( ) {
      throw new UnsupportedOperationException ( "This method shouldn't be called by others." ) ;
   }

   public String getNotificationCode ( ) {
      return notificationCode ;
   }

}
