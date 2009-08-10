package gsn2.wrappers;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn2.wrappers.WrapperConfig;
import gsn.channels.DataChannel;
import gsn2.wrappers.Wrapper;

import org.apache.log4j.Logger;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SystemTime extends TimerTask implements Wrapper {

    private ScheduledThreadPoolExecutor scheduler = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(5);

    private ScheduledFuture localScheduler;

    private static final int              DEFAULT_CLOCK_PERIODS     = 1 * 1000;
    
    public static final String CLOCK_PERIOD_KEY = "clock-period";

    private final WrapperConfig conf;

	private final DataChannel dataChannel;

	private final transient Logger               logger        = Logger.getLogger( SystemTime.class );


	private static   DataField [] dataField  ;

    private int rate;

    public SystemTime(WrapperConfig conf, DataChannel channel) {
		this.conf = conf;
		this.dataChannel= channel;
		dataField = new DataField[] { };
        rate = conf.getParameters().getValueAsInt(CLOCK_PERIOD_KEY,DEFAULT_CLOCK_PERIODS);
	}

    public void start(){
	    localScheduler = scheduler.scheduleAtFixedRate(this, 0,rate , TimeUnit.MICROSECONDS);
	}

	public  DataField[] getOutputFormat ( ) {
		return dataField;
	}


	public void dispose ( ) {
		
	}

	public void stop() {
         localScheduler.cancel(true);
	}

    public void run() {
        dataChannel.write(StreamElement.from(this).setTime(System.currentTimeMillis()));
    }
}
