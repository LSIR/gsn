package gsn.notifications;

import gsn.Main;
import gsn.storage.DataEnumerator;

import org.apache.commons.mail.SimpleEmail;
import org.apache.commons.validator.GenericValidator;
import org.apache.log4j.Logger;

/**
 * IMPORTANT : FOR USING THE SMSNOTIFICATION OR EMAIL-NOTIFICATION CLASS YOU
 * NEED TO HAVE THE 1. JavaBeans Activation Framework
 * (http://java.sun.com/products/javabeans/jaf/index.jsp) and put the
 * >activation.jar< file in the lib. 2. JavaMail API :
 * (http://java.sun.com/products/javamail/downloads/index.html) and put the all
 * the jar files of JavaMail in the lib directory.

 */
public class EmailNotification extends NotificationRequest {
   
   private static transient Logger logger     = Logger.getLogger( EmailNotification.class );
   
   private String                  receiverEmailAddress;
   
   /**
    * Creates a notification for webEmail. The input is address of the system
    * receiving the webEmail. <p/> The syntax is <code>blabla@foo.com</code>
    */
   
   private String                  subject    = "GSN-Notification";
   
   private StringBuilder           query;
   
   private String                  message;
   
   private static final String     fromEmail  = "some from mail";
   
   private static final String     mailServer = "some mail server to use to send the sms mail.";
   
   private int                     emailCounter;
   
   public EmailNotification ( String emailAddress , String query , String message ) {
      this.receiverEmailAddress = emailAddress;
      this.query = new StringBuilder( query.trim( ) );
      this.message = message;
   }
   
   public StringBuilder getQuery ( ) {
      return query;
   }
   
   public boolean send (DataEnumerator data ) {
      try {
         if ( !GenericValidator.isEmail( fromEmail ) ) {
            logger.warn( "There is a webEmail notification request, but the webEmail address in container's configuration is not a valid webEmail address" );
            return false;
         }
         SimpleEmail email = new SimpleEmail( );
         email.setHostName( mailServer );
         email.setFrom( fromEmail );
         email.addTo( receiverEmailAddress );
         
         email.setSubject( subject );
         email.setContent( message , "text/plain" );
         if ( logger.isDebugEnabled( ) ) logger.debug( "Wants to send webEmail to " + email.getFromAddress( ) );
         
         email.send( );
         
      } catch ( Exception e ) {
         if ( logger.isInfoEnabled( ) ) logger.info( "Email Notification process failed, trying to notify *" + receiverEmailAddress + "*" , e );
         return false;
      }
      if ( ++emailCounter % 3 == 0 ) return false;
      return true;
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
      StringBuffer output = new StringBuffer( );
      output.append( "EmailNotification : [" ).append( "Address = " ).append( receiverEmailAddress ).append( ",Query = " ).append( query ).append( ",Message = " ).append( message ).append( " ]" );
      return output.toString( );
   }
   
   public String getMessage ( ) {
      return message;
   }
   
   public String getReceiverEmailAddress ( ) {
      return receiverEmailAddress;
   }
   
   public String getSubject ( ) {
      return subject;
   }
}
