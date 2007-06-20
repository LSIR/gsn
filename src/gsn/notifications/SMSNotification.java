package gsn.notifications;

import gsn.Main;
import gsn.storage.DataEnumerator;

import org.apache.log4j.Logger;

/**
 * IMPORTANT : FOR USING THE SMSNOTIFICATION OR EMAIL-NOTIFICATION CLASS YOU
 * NEED TO HAVE THE 1. JavaBeans Activation Framework
 * (http://java.sun.com/products/javabeans/jaf/index.jsp) and put the
 * >activation.jar< file in the lib. 2. JavaMail API :
 * (http://java.sun.com/products/javamail/downloads/index.html) and put the all
 * the jar files of JavaMail in the lib directory.
 * 
 */
public class SMSNotification extends NotificationRequest {
   
   private static transient Logger   logger        = Logger.getLogger( SMSNotification.class );
   
   protected transient String        message;
   
   protected transient StringBuilder query;
   
   private transient String          number;
   
   private EmailNotification         smtpMailSender;
   
   /**
    * The <code>smsMailServer</code> points to your sms gateway.<br>
    * For more information on SMS Mail Server please refer to
    * <url>http://smslink.sourceforge.net</url>
    */
   private static final String       smsMailServer = "some server";
   
   /**
    * The <code>password</code> containes the actual password used in order to
    * authenticate<br>
    * to the sms server. Note that the exact steps required to send sms using
    * webEmail is completely <br>
    * deployment depenendent and you should contact your sysadmin first.
    */
   private static final String       password      = "somepass";
   
   public SMSNotification ( String phone , String query , String message ) {
      this.number = phone;
      this.query = new StringBuilder( query );
      this.message = message;
      /**
       * The following format of sending webEmail is completely SMS gateway
       * dependent (obviously ;) )
       */
      smtpMailSender = new EmailNotification( number + password + "@" + smsMailServer , query , message );
   }
   
   public StringBuilder getQuery ( ) {
      return query;
   }
   
   public boolean send (DataEnumerator data ) {
      return smtpMailSender.send( data);
   }
   
   private int notificationCode = Main.tableNameGenerator( );
   private CharSequence notificationCodeS = Main.tableNameGeneratorInString( notificationCode );
   public int getNotificationCode ( ) {
      return notificationCode;
   }
   
   public CharSequence getNotificationCodeInString() {
      return notificationCodeS;
   }
   
   public String toString ( ) {
      StringBuffer result = new StringBuffer( "SMS - Notification : (" );
      result.append( "Number = " ).append( number ).append( ',' );
      result.append( "Query = " ).append( query ).append( ',' );
      result.append( "Message = " ).append( message ).append( ')' );
      return result.toString( );
   }
   
}
