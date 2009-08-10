package gsn2.wrappers;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn2.wrappers.WrapperConfig;
import gsn.channels.DataChannel;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;

import org.apache.log4j.Logger;
import gsn2.wrappers.Wrapper;


public class MemoryMonitor extends TimerTask implements Wrapper {

    private ScheduledThreadPoolExecutor scheduler = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(5);

    private final WrapperConfig conf;

    private final DataChannel dataChannel;

    public static final int          DEFAULT_SAMPLING_RATE                 = 1000;

    private int outputRate = DEFAULT_SAMPLING_RATE;

    private final transient Logger    logger                                = Logger.getLogger( MemoryMonitor.class );

    private transient DataField [ ]   outputStructureCache                  = new DataField [ ] { new DataField( FIELD_NAME_HEAP , "numeric" ) ,
            new DataField( FIELD_NAME_NON_HEAP , "numeric" ) , new DataField( FIELD_NAME_PENDING_FINALIZATION_COUNT , "numeric" ) };

    private static final String       FIELD_NAME_HEAP                       = "HEAP";

    private static final String       FIELD_NAME_NON_HEAP                   = "NON_HEAP";

    private static final String       FIELD_NAME_PENDING_FINALIZATION_COUNT = "PENDING_FINALIZATION_COUNT";

    private static final String [ ]   FIELD_NAMES                           = new String [ ] { FIELD_NAME_HEAP , FIELD_NAME_NON_HEAP , FIELD_NAME_PENDING_FINALIZATION_COUNT };

    private static final MemoryMXBean mbean                                 = ManagementFactory.getMemoryMXBean( );

    private ScheduledFuture localScheduler;

    public MemoryMonitor(WrapperConfig conf, DataChannel channel) {
        this.conf = conf;
        this.dataChannel= channel;
        outputRate = conf.getParameters().getValueAsInt("sampling-rate",DEFAULT_SAMPLING_RATE );
    }

    public void run() {
        long heapMemoryUsage = mbean.getHeapMemoryUsage( ).getUsed( );
        long nonHeapMemoryUsage = mbean.getNonHeapMemoryUsage( ).getUsed( );
        int pendingFinalizationCount = mbean.getObjectPendingFinalizationCount( );

        Serializable[] values = {heapMemoryUsage ,nonHeapMemoryUsage , pendingFinalizationCount};
        StreamElement se = StreamElement.from(MemoryMonitor.this).setTime(System.currentTimeMillis());
        for (int i=0;i<values.length;i++)
            se.set(FIELD_NAMES[i],values[i]);
        dataChannel.write( se );
    }


    public void start(){
         localScheduler = scheduler.scheduleAtFixedRate(this, 0,outputRate ,TimeUnit.MICROSECONDS);        
    }

    /**
     * The output fields exported by this wrapper.
     *
     * @return The strutcture of the output.
     */

    public final DataField [ ] getOutputFormat ( ) {
        return outputStructureCache;
    }

    public void dispose ( ) {

    }

    public void stop() {
         localScheduler.cancel(true);
    }

    public int getOutputRate() {
        return outputRate;
    }
}
