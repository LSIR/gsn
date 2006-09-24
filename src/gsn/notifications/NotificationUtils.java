package gsn.notifications ;

import gsn.beans.StreamElement ;

import org.antlr.stringtemplate.StringTemplate ;
import org.apache.log4j.Logger ;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 *         Create : May 26, 2005 <br>
 *         Created for : GSN-PD project. <br>
 */
public class NotificationUtils {

   private static transient Logger logger = Logger.getLogger ( NotificationUtils.class ) ;

   public static final int EMAIL = 2 ;

   public static final String EMAIL_PROTOCOL = "EMAIL://" ;

   public static final int FAX = 6 ;

   public static final String FAX_PROTOCOL = "FAX://" ;

   public static final int PAGER = 5 ;

   public static final String PAGER_PROTOCOL = "PAGER://" ;

   public static final int PDA = 4 ;

   public static final String PDA_PROTOCOL = "PDA://" ;

   public static final int PHONE = 3 ;

   public static final String PHONE_PROTOCOL = "PHONE://" ;

   public static final int SMS = 1 ;

   public static final String SMS_PROTOCOL = "SMS://" ;

   public static final int GSN = 7 ;

   public static final String GSN_PROTOCOL = "GSN://" ;

   /**
    * Extracts the prefix of the address url of the notification. If it doesn't
    * defined, it returns zero.
    * 
    * @param url
    *           of the notification address.
    */
   public static int getProtocol ( String url ) {
      if ( url == null )
         return 0 ;
      String toUpper = url.trim ( ).replace ( " " , "" ).toUpperCase ( ) ;
      if ( toUpper.startsWith ( SMS_PROTOCOL ) )
         return SMS ;
      else if ( toUpper.startsWith ( PDA_PROTOCOL ) )
         return PDA ;
      else if ( toUpper.startsWith ( EMAIL_PROTOCOL ) )
         return EMAIL ;
      else if ( toUpper.startsWith ( PHONE_PROTOCOL ) )
         return PHONE ;
      else if ( toUpper.startsWith ( GSN_PROTOCOL ) )
         return GSN ;
      else if ( toUpper.startsWith ( PAGER_PROTOCOL ) )
         return PAGER ;
      else if ( toUpper.startsWith ( FAX_PROTOCOL ) )
         return FAX ;
      return 0 ;
   }

   /**
    * Removes the protocol:// from the beginning of the input.
    * 
    * @param url :
    *           Complete URL
    * 
    * @return The url without <code>protocol://</code> part.
    */
   public static String extractAddress ( String url ) {
      String pattern = "://" ;
      int index = url.indexOf ( pattern ) ;
      return url.substring ( index + pattern.length ( ) ) ;
   }

   /**
    * @param address
    *           The full address (incl. the protocol prefix).
    * @param query
    * @param message
    */
   public static NotificationRequest getProcessedQuery ( String address , String query , String message ) {
      int protocol = NotificationUtils.getProtocol ( address ) ;
      if ( protocol == 0 ) {
         if ( logger.isInfoEnabled ( ) )
            logger.info ( "BAD QUERY RECEIVED" ) ;
         if ( logger.isInfoEnabled ( ) )
            logger.info ( "The address path of the queries doesn't contain recognized protocol" ) ;
      }
      NotificationRequest notificationRequest = null ;
      switch ( protocol ) {
      case NotificationUtils.SMS :
         notificationRequest = new SMSNotification ( NotificationUtils.extractAddress ( address ).trim ( ) , query , message ) ;
         break ;
      case NotificationUtils.EMAIL :
         notificationRequest = new EmailNotification ( NotificationUtils.extractAddress ( address ) , query , message ) ;
         break ;
      case NotificationUtils.PDA :
         break ;
      case NotificationUtils.PHONE :
         break ;
      case NotificationUtils.FAX :
         break ;
      case NotificationUtils.PAGER :
         break ;
      }
      return notificationRequest ;
   }

   public static String prepareMessage ( StreamElement streamElement , String message ) {
      StringTemplate template = new StringTemplate ( message ) ;
      /**
       * @todo : Do checks for attributes with more than one value ?
       */
      String [ ] fieldNames = streamElement.getFieldNames ( ) ;
      for ( int i = 0 ; i < fieldNames.length ; i ++ )
         template.setAttribute ( streamElement.getFieldNames ( ) [ i ] , streamElement.getData ( ) [ i ] ) ;
      String resultMessage = template.toString ( ) ;
      return resultMessage ;
   }
}
