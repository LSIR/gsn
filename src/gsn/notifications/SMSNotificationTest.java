package gsn.notifications ;

import gsn.Main ;
import gsn.beans.ContainerConfig ;
import gsn.vsensor.Container ;
import junit.framework.TestCase ;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 *         Create : May 26, 2005 <br>
 *         Created for : GSN-PD project. <br>
 */
public class SMSNotificationTest extends TestCase {

   protected void setUp ( ) throws Exception {
      super.setUp ( ) ;
   }

   protected void tearDown ( ) throws Exception {
      super.tearDown ( ) ;
   }

   public void testDoNotify ( ) {
      /**
       * @TODO : DON'T FORGET TO EMPHESIS ON THE WEB THAT THE VARIABLE NAME MUST
       *       BE IN UPPER CASE.
       */
      ContainerConfig config = new ContainerConfig ( ) ;
      config.setEmail ( "ali.salehi@epfl.ch" ) ;
      config.setMailServer ( "mail.epfl.ch" ) ;
      config.setSmsServer ( "sms.epfl.ch" ) ;
      config.setSmsPassword ( ".extelukat66" ) ;
      Main.setContainerConfig ( config ) ;
      String message = "Hello $NAME$" ;
      String parsedPhoneNo = "0786810931" ;
      SMSNotification notification = new SMSNotification ( parsedPhoneNo , "x" , message ) ;
      // notification.setData (p );
      assertTrue ( notification.send ( ) ) ;
   }

   public void testSMSNotification ( ) {
   }

   public void testGetQuery ( ) {
   }

   public void testGetUrl ( ) {
   }

   public void testPrepareMessage ( ) {
   }

}
