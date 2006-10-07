package gsn.wrappers;

import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.storage.StorageManager;
import gsn.utils.ParamParser;
import gsn.vsensor.Container;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

/**
 * Date: Sep 4, 2006 Time: 4:07:58 PM
 */
public class MemoryMonitoringWrapper extends AbstractStreamProducer {

    private static final int DEFAULT_SAMPLING_RATE = 1000;

    private int samplingRate = DEFAULT_SAMPLING_RATE;

    private final transient Logger logger = Logger
	    .getLogger(MemoryMonitoringWrapper.class);

    private static int threadCounter = 0;

    private static final MemoryMXBean mbean = ManagementFactory
	    .getMemoryMXBean();

    private static final String FIELD_NAME_HEAP = "HEAP";

    private static final String FIELD_NAME_NON_HEAP = "NON_HEAP";

    private static final String FIELD_NAME_PENDING_FINALIZATION_COUNT = "PENDING_FINALIZATION_COUNT";

    private static final String[] FIELD_NAMES = new String[] { FIELD_NAME_HEAP,
	    FIELD_NAME_NON_HEAP, FIELD_NAME_PENDING_FINALIZATION_COUNT };

    public boolean initialize(TreeMap context) {
	boolean toReturn = super.initialize(context);
	if (toReturn == false)
	    return false;
	setName("MemoryMonitoringWrapper-Thread" + (++threadCounter));
	AddressBean addressBean = (AddressBean) context
		.get(Container.STREAM_SOURCE_ACTIVE_ADDRESS_BEAN);
	if (addressBean.getPredicateValue("sampling-rate") != null) {
	    samplingRate = ParamParser.getInteger(addressBean
		    .getPredicateValue("rate"), DEFAULT_SAMPLING_RATE);
	    if (samplingRate <= 0) {
		logger
			.warn("The specified >sampling-rate< parameter for the >MemoryMonitoringWrapper< should be a positive number.\nGSN uses the default rate ("
				+ DEFAULT_SAMPLING_RATE + "ms ).");
		samplingRate = DEFAULT_SAMPLING_RATE;
	    }
	}
	try {
	    StorageManager.getInstance().createTable(getDBAlias(),
		    getProducedStreamStructure());
	} catch (SQLException e) {
	    logger.error(e.getMessage(), e);
	    return false;
	}
	this.start();
	return true;
    }

    public void run() {
	while (isActive()) {
	    try {
		Thread.sleep(samplingRate);
	    } catch (InterruptedException e) {
		logger.error(e.getMessage(), e);
	    }
	    if (listeners.isEmpty())
		continue;

	    long heapMemoryUsage = mbean.getHeapMemoryUsage().getUsed();
	    long nonHeapMemoryUsage = mbean.getNonHeapMemoryUsage().getUsed();
	    int pendingFinalizationCount = mbean
		    .getObjectPendingFinalizationCount();

	    StreamElement streamElement = new StreamElement(FIELD_NAMES,
		    new Integer[] { DataTypes.BIGINT, DataTypes.BIGINT,
			    DataTypes.INTEGER }, new Serializable[] {
			    heapMemoryUsage, nonHeapMemoryUsage,
			    pendingFinalizationCount }, System
			    .currentTimeMillis());
	    publishData(streamElement);
	}
    }

    public void finalize(HashMap context) {
	super.finalize(context);
	threadCounter--;
    }

    private transient boolean isGetStructureInitialized = false;

    private transient ArrayList<DataField> outputStructureCache = null;

    /**
         * The output fields exported by this virtual sensor.
         * 
         * @return The strutcture of the output.
         */

    public final Collection<DataField> getProducedStreamStructure() {
	if (isGetStructureInitialized == false) {
	    outputStructureCache = new ArrayList<DataField>();
	    outputStructureCache.add(new DataField(FIELD_NAME_HEAP, "bigint",
		    "Heap memory usage."));
	    outputStructureCache.add(new DataField(FIELD_NAME_NON_HEAP,
		    "bigint", "Nonheap memory usage."));
	    outputStructureCache.add(new DataField(
		    FIELD_NAME_PENDING_FINALIZATION_COUNT, "int",
		    "The number of objects with pending finalization."));
	}
	return outputStructureCache;
    }

}
