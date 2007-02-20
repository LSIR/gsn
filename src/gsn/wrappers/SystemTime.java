package gsn.wrappers;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import javax.swing.Timer;
import org.apache.log4j.Logger;

/**
 * This wrapper presents the system current clock.
 * 
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */

public class SystemTime extends AbstractWrapper implements ActionListener {
   
   private static final Serializable [ ] EMPTY_DATA_PART   = new Serializable [ ] {};
   
   private static final Byte [ ]      EMPTY_FIELD_TYPES = new Byte [ ] {};
   
   private static final int              INITIAL_DELAY     = 5 * 1000;
   
   private static final int              CLOCK_PERIODS     = 1 * 1000;
   
   private String [ ]                    EMPTY_FIELD_LIST  = new String [ ] {};
   
   private  DataField   []    collection        = new DataField[] {};
   
   private static int                    threadCounter     = 0;
   
   private final transient Logger        logger            = Logger.getLogger( SystemTime.class );
   
   private Timer                         timer;
   
   public boolean initialize (  ) {
      setName( "LocalTimeWrapper-Thread" + ( ++threadCounter ) );
      timer = new Timer( CLOCK_PERIODS , this );
      timer.setInitialDelay( INITIAL_DELAY );
      return true;
   }
   
   public void run ( ) {
      timer.start( );
   }
   
   public  DataField [] getOutputFormat ( ) {
      return collection;
   }
   
   public void actionPerformed ( ActionEvent actionEvent ) {
      StreamElement streamElement = new StreamElement( EMPTY_FIELD_LIST , EMPTY_FIELD_TYPES , EMPTY_DATA_PART , actionEvent.getWhen( ) );
      postStreamElement( streamElement );
   }
   
   public void finalize ( ) {
      timer.stop( );
      threadCounter--;
  }
   
   public String getWrapperName() {
       return "System Time";
   }
   
}
