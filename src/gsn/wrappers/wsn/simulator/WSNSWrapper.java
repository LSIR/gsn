package gsn.wrappers.wsn.simulator;

import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.storage.StorageManager;
import gsn.utils.ParamParser;
import gsn.vsensor.Container;
import gsn.wrappers.AbstractStreamProducer;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;

import javax.naming.OperationNotSupportedException;

import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class WSNSWrapper extends AbstractStreamProducer implements DataListener {

    private final transient Logger logger = Logger.getLogger(WSNSWrapper.class);

    private static int threadCounter = 0;

    /**
         * The rate, set the rate in which the network is re-revaluated. If the
         * rate is 1000, then the network is reevaluated every seconds. If the
         * rate is negative, the network will stop evaluating it self after
         * specified number of cycles.
         */
    private static String RATE_KEY = "rate";

    /**
         * The Rate is specified in msec.
         */
    private static int RATE_DEFAULT_VALUE = 2000;

    private int rate = RATE_DEFAULT_VALUE;

    private static int NODE_COUNT_DEFAULT_VALUE = 10;

    private static String NODE_COUNT_KEY = "node_count";

    private int node_count = NODE_COUNT_DEFAULT_VALUE;

    private WirelessNode[] nodes;

    private ArrayList<DataPacket> dataBuffer = new ArrayList<DataPacket>();

    private String STEP_COUNTER = "steps";

    private static final int STEP_COUNTER_DEFAULT_VALUE = -1;

    private int step_counter = STEP_COUNTER_DEFAULT_VALUE;

    public boolean initialize(TreeMap context) {
	boolean toReturn = super.initialize(context);
	if (!toReturn)
	    return false;
	setName("WirelessSensorNetworkSimulatorWrapper-Thread"
		+ (++threadCounter));
	AddressBean addressBean = (AddressBean) context
		.get(Container.STREAM_SOURCE_ACTIVE_ADDRESS_BEAN);
	/**
         * Reading the initialization paramteters from the XML Configurations
         * provided.
         */
	if (addressBean.getPredicateValue(NODE_COUNT_KEY) != null) {
	    node_count = ParamParser.getInteger((String) addressBean
		    .getPredicateValue(NODE_COUNT_KEY),
		    NODE_COUNT_DEFAULT_VALUE);
	    if (node_count <= 0) {
		logger
			.warn("The specified >node_count< parameter for the >WSNWrapper< shouldn't be a negative number.\nGSN uses the default node_count ("
				+ NODE_COUNT_DEFAULT_VALUE + ").");
		node_count = NODE_COUNT_DEFAULT_VALUE;
	    }
	}

	if (addressBean.getPredicateValue(STEP_COUNTER) != null) {
	    step_counter = ParamParser.getInteger((String) addressBean
		    .getPredicateValue(STEP_COUNTER),
		    STEP_COUNTER_DEFAULT_VALUE);
	    if (step_counter <= 0) {
		logger
			.warn("The specified >step_counter< parameter for the >WSNWrapper< shouldn't be a negative number.\nGSN disables the step_counter (-1).");
		step_counter = -1;
	    }
	}

	if (addressBean.getPredicateValue(NODE_COUNT_KEY) != null)
	    node_count = ParamParser.getInteger((String) addressBean
		    .getPredicateValue(NODE_COUNT_KEY),
		    NODE_COUNT_DEFAULT_VALUE);

	if (addressBean.getPredicateValue(RATE_KEY) != null) {
	    rate = ParamParser.getInteger((String) addressBean
		    .getPredicateValue(RATE_KEY), RATE_DEFAULT_VALUE);
	    if (rate <= 0) {
		logger
			.warn("The specified rate parameter for the >WSNWrapper< shouldn't be a negative number.\nGSN uses the default rate ("
				+ RATE_DEFAULT_VALUE + ").");
		rate = RATE_DEFAULT_VALUE;
	    }
	}
	try {
	    StorageManager.getInstance().createTable(getDBAlias(),
		    getProducedStreamStructure());
	} catch (SQLException e) {
	    logger.error(e.getMessage(), e);
	    return false;
	}
	nodes = initializeNodes(node_count);
	for (int i = 0; i < node_count; i++)
	    nodes[i].addDataListener(this);
	this.start();
	return true;
    }

    public void run() {
	long tempStepCounter = 0;
	while (isActive()) {
	    if (tempStepCounter <= step_counter || step_counter == -1) {
		tempStepCounter++;
		if (!listeners.isEmpty() && dataBuffer.size() > 0) {
		    DataPacket dataPacket;
		    synchronized (dataBuffer) {
			dataPacket = dataBuffer.remove(0);
		    }
		    StreamElement streamElement = new StreamElement(
			    new String[] { "NODE_ID", "PARENT_ID",
				    "TEMPREATURE" }, new Integer[] {
				    DataTypes.INTEGER, DataTypes.INTEGER,
				    DataTypes.INTEGER }, new Serializable[] {
				    dataPacket.getIdentifier(),
				    dataPacket.getParent(),
				    dataPacket.getValue() }, System
				    .currentTimeMillis());
		    publishData(streamElement);
		    if (dataBuffer.size() > 0)
			continue;
		}
	    }
	    try {
		Thread.sleep(rate);

	    } catch (InterruptedException e) {
		logger.error(e.getMessage(), e);
	    }

	}
	for (WirelessNode node : nodes)
	    node.stopNode();
    }

    public Collection<DataField> getProducedStreamStructure() {
	ArrayList<DataField> dataField = new ArrayList<DataField>();
	dataField.add(new DataField("NODE_ID", DataTypes.INTEGER_NAME,
		"Node's identification."));
	dataField.add(new DataField("PARENT_ID", DataTypes.INTEGER_NAME,
		"Parent Node's identification."));
	dataField.add(new DataField("TEMPREATURE", DataTypes.INTEGER_NAME,
		"incremental int"));
	return dataField;
    }

    public static int randomNumber(int fromNo, int toNo) {
	return (int) ((Math.random() * (toNo - fromNo + 1)) + fromNo);
    }

    public WirelessNode[] initializeNodes(int nodeCount)
	    throws RuntimeException {
	if (nodeCount <= 0)
	    throw new RuntimeException(
		    "Wireless Sensor Network Simulator (WSNS) can't create a network with zero or negative number of nodes : "
			    + nodeCount);
	WirelessNode[] nodes = new WirelessNode[nodeCount];
	for (int i = 0; i < nodeCount; i++) {
	    nodes[i] = new WirelessNode(i);
	    nodes[i].setName("WSNS-Node-" + i);
	}
	for (int i = 1; i < nodeCount; i++)
	    nodes[i].setParent(nodes[randomNumber(i - 1, 0)]);
	for (int i = 1; i < nodeCount; i++)
	    nodes[i].start();
	return nodes;
    }

    public void newDataAvailable(DataPacket dataPacket) {
	synchronized (dataBuffer) {
	    dataBuffer.add(dataPacket);
	}
    }

    public boolean sendToWrapper(Object dataItem)
	    throws OperationNotSupportedException {
	// Integer nodeId = dataItem;
	return super.sendToWrapper(dataItem); // To change body of overridden
	// methods use File | Settings |
	// File Templates.
    }
}
