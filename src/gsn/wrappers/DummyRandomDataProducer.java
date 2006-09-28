package gsn.wrappers;

import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.vsensor.Container;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

/**
 * This is a dummy data generator which is highly configurable through the
 * wrappers configuration in the virtual sensor's configuration file.
 * 
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class DummyRandomDataProducer extends AbstractStreamProducer {

    /**
         * When generating an stream element, the size of the stream element is
         * specified in the following varialbe. The default value is one byte.
         */
    private int streamElementSize = 1;

    /**
         * The stream element could be produced on a regular bases such as an
         * stream element each 10 minutes or it can be produced randomly (e.g.,
         * when a sensor see a special rfid tag). The default value is
         * <code>-1</code> meaning the stream elements will be produced
         * randomly with no regular bases. If one wants to produces the stream
         * on regular bases, one should set the following variable reflecting
         * the interval between two stream production.
         */
    private int streamOutputRate = -1;

    private byte[] testBinaryData;

    /**
         * The data part of the output stream element is an integer in the range
         * of zero to the value specified in <code>data_range</code>. The
         * default <code>data_range</code> is specified below. The code
         */
    private int data_range = 100;

    private int value;

    private double burstProbability = -1;

    private int burstMaxSize = -1;

    private int count = -1;

    /**
         * Specifies the interval between to subsequent stream elements produced
         * by the wrapper. Default is 500msec.
         */
    private int countOutputPeriodInMSec = 500;

    private final transient Logger logger = Logger
	    .getLogger(DummyRandomDataProducer.class);

    /**
         * Initializes the DataSource object. This method should be called
         * before any real task to be done.
         */
    private static int threadCounter = 0;

    /**
         * Configurable option through the xml file are :<br>
         * rate : the output rate in milli seconds <br>
         * size : the output stream element size in bytes, used for constructing
         * binary DATA_BIN <br>
         * range : the produced number will be from zero up to the number
         * specified in range <br>
         * burst : a float value between zero and less than 1. The higher the
         * value, the high probability of burst.<br>
         */
    boolean rateBased = false;

    public boolean initialize(TreeMap context) {
	super.initialize(context);
	setName("DummyRandomDataProducer-Thread" + (++threadCounter));

	try {
	    getStorageManager().createTable(getDBAlias(),
		    getProducedStreamStructure());
	} catch (SQLException e) {
	    logger.error(e.getMessage(), e);
	    return false;
	}
	// Iterator<String> iterator = context.keySet ().iterator () ;
	// while ( iterator.hasNext () ) {
	// String name = iterator.next () ;
	// System.out.println ("*"+ name + "*==*" + context.get ( name )+"*" ) ;
	// }

	AddressBean addressBean = (AddressBean) context
		.get(Container.STREAM_SOURCE_ACTIVE_ADDRESS_BEAN);
	/**
         * Reading from the XML Configurations provided.
         */
	if (addressBean.getPredicateValue("size") != null) {
	    streamElementSize = Integer.parseInt((String) addressBean
		    .getPredicateValue("size"));
	}
	if (addressBean.getPredicateValue("rate") != null) {
	    streamOutputRate = Integer.parseInt((String) addressBean
		    .getPredicateValue("rate"));
	    rateBased = true;
	}

	if (addressBean.getPredicateValue("burst-probability") != null) {
	    burstProbability = Double.parseDouble((String) addressBean
		    .getPredicateValue("burst-probability"));
	}
	if (addressBean.getPredicateValue("burst-max-size") != null) {
	    burstMaxSize = Integer.parseInt((String) addressBean
		    .getPredicateValue("burst-max-size"));
	}

	if (addressBean.getPredicateValue("count") != null) {
	    count = Integer.parseInt((String) addressBean
		    .getPredicateValue("count"));
	}

	if (addressBean.getPredicateValue("count-period") != null) {
	    countOutputPeriodInMSec = Integer.parseInt((String) addressBean
		    .getPredicateValue("count-period"));
	}

	if (addressBean.getPredicateValue("range") != null) {
	    data_range = Integer.parseInt((String) addressBean
		    .getPredicateValue("range"));
	}
	if (addressBean.getPredicateValue("log") != null) {
	    // if ( Integer.parseInt ( ( String )
	    // addressBean.getPredicateValue (
	    // "log" ) ) == 1 )
	    // SimulationResult.initialize ( streamElementSize,
	    // streamOutputRate,
	    // burstProbability, burstMaxSize, countOutputPeriodInMSec,
	    // count );
	}

	/**
         * Finish of reading the configurations
         */
	testBinaryData = new byte[streamElementSize];
	for (int i = 0; i < streamElementSize; i++)
	    testBinaryData[i] = (byte) ((Math.random() * 255));

	this.start();
	return true;
    }

    public void run() {
	boolean actualBurstProbability;
	int actualBurstCount;
	long totalCount = 0;
	long actualCountOutputPeriodInMSec = countOutputPeriodInMSec;
	long actualCount = count;
	long sleepPeriod;
	try {
	    Thread.sleep(2000);
	} catch (InterruptedException e1) {
	    e1.printStackTrace();
	}
	while (isActive()) {
	    actualBurstProbability = (Math.random() < burstProbability);
	    actualBurstCount = (int) (Math.random() * burstMaxSize) + 1;
	    if (!actualBurstProbability)
		actualBurstCount = 1;
	    for (int burstCounter = 0; burstCounter < actualBurstCount; burstCounter++) {
		value = (int) (Math.random() * data_range);
		totalCount++;
		StreamElement streamElement = new StreamElement(new String[] {
			"DATA", "DATA_BIN" }, new Integer[] {
			DataTypes.INTEGER, DataTypes.BINARY },
			new Serializable[] { value, testBinaryData }, System
				.currentTimeMillis());
		// SimulationResult.addJustProducedFromDummyDataSource () ;
		publishData(streamElement);
	    }
	    if (rateBased)
		sleepPeriod = streamOutputRate;
	    else {
		if (actualCount > 0 && actualCountOutputPeriodInMSec > 0) {
		    sleepPeriod = (int) (Math.random() * actualCountOutputPeriodInMSec) + 1;
		} else if (actualCount <= 0
			&& actualCountOutputPeriodInMSec > 0) {
		    sleepPeriod = actualCountOutputPeriodInMSec
			    + (int) (Math.random() * countOutputPeriodInMSec);
		} else {// if ( actualCountOutputPeriodInMSec <= 0 )
		    actualCount = count;
		    actualCountOutputPeriodInMSec = countOutputPeriodInMSec;
		    sleepPeriod = (int) (Math.random() * actualCountOutputPeriodInMSec) + 1;
		}
		actualCount--;
		actualCountOutputPeriodInMSec -= sleepPeriod;

	    }
	    try {
		Thread.sleep(sleepPeriod);
	    } catch (InterruptedException e) {
		logger.error(e.getMessage(), e);
	    }
	}
    }

    public void finalize(HashMap context) {
	super.finalize(context);
	threadCounter--;
    }

    public Collection<DataField> getProducedStreamStructure() {
	ArrayList<DataField> dataField = new ArrayList<DataField>();
	dataField.add(new DataField("DATA", "int", "incremental int"));
	dataField.add(new DataField("DATA_BIN", "BINARY", "Binary-data"));
	return dataField;
    }
}
