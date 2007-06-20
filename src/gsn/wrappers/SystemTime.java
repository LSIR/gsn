package gsn.wrappers;

import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.beans.StreamElement;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import javax.swing.Timer;
import org.apache.log4j.Logger;

/**
 * This wrapper presents the system current clock.
 */

public class SystemTime extends AbstractWrapper implements ActionListener {
  
  public static final String CLOCK_PERIOD_KEY = "clock-period";
  
  private static final Serializable [ ] EMPTY_DATA_PART   = new Serializable [ ] {};
  
  private static final Byte [ ]      EMPTY_FIELD_TYPES = new Byte [ ] {};
  
  private static final int              DEFAULT_CLOCK_PERIODS     = 1 * 1000;
  
  private String [ ]                    EMPTY_FIELD_LIST  = new String [ ] {};
  
  private  DataField   []    collection        = new DataField[] {};
  
  private static int                    threadCounter     = 0;
  
  private  transient Logger        logger            = Logger.getLogger( this.getClass() );
  
  private Timer                         timer;
  
  public boolean initialize (  ) {
    setName( "LocalTimeWrapper-Thread" + ( ++threadCounter ) );
    AddressBean addressBean =getActiveAddressBean ( );
    timer = new Timer(  addressBean.getPredicateValueAsInt( CLOCK_PERIOD_KEY ,DEFAULT_CLOCK_PERIODS) , this );
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
  public int getTimerClockPeriod() {
    return timer.getDelay();
  }
  
}
