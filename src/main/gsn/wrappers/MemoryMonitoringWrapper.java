package gsn.wrappers;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.WrapperConfig;
import gsn.channels.DataChannel;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

import org.apache.log4j.Logger;


public class MemoryMonitoringWrapper implements Wrapper {
	private final WrapperConfig conf;

	private final DataChannel dataChannel;

	private static final int          DEFAULT_SAMPLING_RATE                 = 1000;

	private int                       samplingRate                          = DEFAULT_SAMPLING_RATE;

	private final transient Logger    logger                                = Logger.getLogger( MemoryMonitoringWrapper.class );

	private transient DataField [ ]   outputStructureCache                  = new DataField [ ] { new DataField( FIELD_NAME_HEAP , "bigint" ) ,
			new DataField( FIELD_NAME_NON_HEAP , "bigint" ) , new DataField( FIELD_NAME_PENDING_FINALIZATION_COUNT , "int" ) };

	private static final String       FIELD_NAME_HEAP                       = "HEAP";

	private static final String       FIELD_NAME_NON_HEAP                   = "NON_HEAP";

	private static final String       FIELD_NAME_PENDING_FINALIZATION_COUNT = "PENDING_FINALIZATION_COUNT";

	private static final String [ ]   FIELD_NAMES                           = new String [ ] { FIELD_NAME_HEAP , FIELD_NAME_NON_HEAP , FIELD_NAME_PENDING_FINALIZATION_COUNT };

	private static final MemoryMXBean mbean                                 = ManagementFactory.getMemoryMXBean( );

	public MemoryMonitoringWrapper(WrapperConfig conf, DataChannel channel) {
		this.conf = conf;
		this.dataChannel= channel;
		samplingRate = conf.getParameters().getPredicateValueAsInt("sampling-rate",DEFAULT_SAMPLING_RATE );
	}

	public void start(){
		while ( isActive ) {
			try {
				Thread.sleep( samplingRate );
			} catch ( InterruptedException e ) {
				logger.error( e.getMessage( ) , e );
			}
			long heapMemoryUsage = mbean.getHeapMemoryUsage( ).getUsed( );
			long nonHeapMemoryUsage = mbean.getNonHeapMemoryUsage( ).getUsed( );
			int pendingFinalizationCount = mbean.getObjectPendingFinalizationCount( );

      Serializable[] values = {heapMemoryUsage ,nonHeapMemoryUsage , pendingFinalizationCount};
      StreamElement se = StreamElement.from(this).setTime(System.currentTimeMillis());
      for (int i=0;i<values.length;i++)
        se.set(FIELD_NAMES[i],values[i]);

      dataChannel.write( se );

		}
	}

	/**
	 * The output fields exported by this virtual sensor.
	 * 
	 * @return The strutcture of the output.
	 */

	public final DataField [ ] getOutputFormat ( ) {
		return outputStructureCache;
	}
	private boolean isActive;

	public void dispose ( ) {
	}

	public void stop() {
		isActive = false;		
	}

}
