package gsn.notifications ;

import gsn.beans.StreamElement ;
import gsn.storage.DataPacket;
import gsn.storage.SQLUtils ;
import gsn.vsensor.ContainerImpl;

import java.util.ArrayList ;
import java.util.Collection ;
import java.util.Enumeration ;

import org.apache.log4j.Logger;

import sun.util.logging.resources.logging;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public abstract class NotificationRequest {

   public abstract StringBuilder getQuery ( ) ;

   private transient ArrayList < String > cachedPrespectiveVirtualSensors = null ;
   private static transient Logger logger = Logger.getLogger(NotificationRequest.class);

   private Enumeration < StreamElement > data ;

   public ArrayList < String > getPrespectiveVirtualSensors ( ) {
      if ( cachedPrespectiveVirtualSensors == null )
         cachedPrespectiveVirtualSensors = SQLUtils.extractTableNamesUsedInQuery ( getQuery ( ) ) ;
      return cachedPrespectiveVirtualSensors ;
   }

   public abstract boolean send ( ) ;

   public abstract String getNotificationCode ( ) ;

   public boolean equals ( Object obj ) {
      if ( obj == null || ! ( obj instanceof NotificationRequest ) )
         return false ;
      NotificationRequest input = ( NotificationRequest ) obj ;
      return getNotificationCode ( ).equals ( input.getNotificationCode ( ) ) ;
   }

   public int hashCode ( ) {
      return getNotificationCode ( ).hashCode ( ) ;
   }

   public String toString ( ) {
      return "The notification request with the code of *" + getNotificationCode ( ) + "*" ;
   }

   public void setData ( Enumeration < StreamElement > data ) {
      this.data = data ;
   }

   public Enumeration < StreamElement > getData ( ) {
      return data ;
   }

   public boolean needNotification ( ) {
	   return data != DataPacket.EMPTY_ENUM ;
   }

}
