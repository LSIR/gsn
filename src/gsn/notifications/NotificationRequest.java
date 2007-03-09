package gsn.notifications;

import gsn.storage.DataEnumerator;
import gsn.storage.SQLUtils;
import java.util.ArrayList;
import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public abstract class NotificationRequest {
   
   public abstract StringBuilder getQuery ( );
   
   private transient ArrayList < CharSequence > cachedPrespectiveVirtualSensors = null;
   
   private static transient Logger        logger                          = Logger.getLogger( NotificationRequest.class );
   
   public ArrayList < CharSequence > getPrespectiveVirtualSensors ( ) {
      if ( cachedPrespectiveVirtualSensors == null ) cachedPrespectiveVirtualSensors = SQLUtils.extractTableNamesUsedInQuery( getQuery( ) );
      return cachedPrespectiveVirtualSensors;
   }
   
   public abstract boolean send (DataEnumerator data );
   
   public abstract int getNotificationCode ( );
   
   public boolean equals ( Object obj ) {
      if ( obj == null || !( obj instanceof NotificationRequest ) ) return false;
      NotificationRequest input = ( NotificationRequest ) obj;
      return getNotificationCode( )== input.getNotificationCode( );
   }
   
   public int hashCode ( ) {
      return getNotificationCode( );
   }
   
   public String toString ( ) {
      return "The notification request with the code of *" + getNotificationCode( ) + "*";
   }
   
}
