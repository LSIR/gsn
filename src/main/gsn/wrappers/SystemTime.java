package gsn.wrappers;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.WrapperConfig;
import gsn.channels.DataChannel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.swing.Timer;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.buffer.SynchronizedBuffer;
import org.apache.commons.collections.buffer.UnboundedFifoBuffer;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.log4j.Logger;

/**
 * This wrapper presents the system current clock. 
 */

public class SystemTime implements Wrapper , ActionListener {

	private final WrapperConfig conf;

	private final DataChannel dataChannel;

	public static final String CLOCK_PERIOD_KEY = "clock-period";

	public static final String MAX_DELAY_KEY = "max-delay";

	private static final Serializable [ ] EMPTY_DATA_PART   = new Serializable [ ] {};

	private static final Byte [ ]      EMPTY_FIELD_TYPES = new Byte [ ] {};

	private static final int              DEFAULT_CLOCK_PERIODS     = 1 * 1000;

	private static final int DEFAULT_MAX_DELAY = -1 ;//5 seconds;

	private String [ ]                    EMPTY_FIELD_LIST  = new String [ ] {};

	private  DataField   []    collection        = new DataField[] {};

	private  transient Logger        logger            = Logger.getLogger( this.getClass() );

	private Timer                         timer;

	private boolean                      delayPostingElements = false;

	private int maximumDelay = DEFAULT_MAX_DELAY;

	private Buffer streamElementBuffer; 

	private Object objectLock = new Object();

	public SystemTime(WrapperConfig conf, DataChannel channel) {
		this.conf = conf;
		this.dataChannel= channel;
		
		//  TODO: negative values?
		timer = new Timer(  conf.getParameters().getPredicateValueAsInt( CLOCK_PERIOD_KEY ,DEFAULT_CLOCK_PERIODS) , this );
		maximumDelay = conf.getParameters().getPredicateValueAsInt(MAX_DELAY_KEY, DEFAULT_MAX_DELAY);
		
		if(maximumDelay > 0){
			streamElementBuffer = SynchronizedBuffer.decorate(new UnboundedFifoBuffer());
			delayPostingElements = true;
			if(timer.getDelay() < maximumDelay)
				logger.warn("Maximum delay is greater than element production interval. Running for a long time may lead to an OutOfMemoryException" );
		}
	}

	public void start() {
		timer.start( );
		if(delayPostingElements){
			if(logger.isDebugEnabled())
				logger.warn("Starting <" + getClass().getCanonicalName() + "> with delayed elements.");
			while(isActive){
				synchronized(objectLock){
					while(streamElementBuffer.isEmpty()){
						try {
							objectLock.wait();
						} catch (InterruptedException e) {
							logger.error( e.getMessage( ) , e );
						}
					}
				}
				try{
					int nextInt = RandomUtils.nextInt(maximumDelay);
					Thread.sleep(nextInt);
					//				System.out.println("next delay : " + nextInt + "   -->    buffer size : " + streamElementBuffer.size());
				}catch(InterruptedException e){
					logger.error( e.getMessage( ) , e );
				}

				if(!streamElementBuffer.isEmpty()){
					StreamElement nextStreamElement = (StreamElement)streamElementBuffer.remove();
					dataChannel.write(nextStreamElement);
				}
			}
		}
	}

	public  DataField [] getOutputFormat ( ) {
		return collection;
	}

	public void actionPerformed ( ActionEvent actionEvent ) {
		StreamElement streamElement = StreamElement.from(this).setTime( actionEvent.getWhen( ) );
		if(delayPostingElements){
			streamElementBuffer.add(streamElement);
			synchronized(objectLock){
				objectLock.notifyAll();
			}
		}
		else
			dataChannel.write( streamElement );
	}


	private boolean isActive=true;

	public void dispose ( ) {
	
	}


	public int getTimerClockPeriod() {
		return timer.getDelay();
	}

	public void stop() {
		isActive = false;
		timer.stop( );
		
	}


}
