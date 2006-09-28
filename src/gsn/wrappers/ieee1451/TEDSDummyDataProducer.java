package gsn.wrappers.ieee1451;

import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.vsensor.Container;
import gsn.wrappers.AbstractStreamProducer;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.apache.log4j.Logger;

/**
 * This is a dummy data generator which is highly configurable through the
 * wrappers configuration in the virtual sensor's configuration file.
 * 
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */

public class TEDSDummyDataProducer extends AbstractStreamProducer {

    private final Logger logger = Logger.getLogger(TEDSDummyDataProducer.class);

    /**
         * Initializes the DataSource object. This method should be called
         * before any real task to be done.
         */
    private static int threadCounter = 0;

    private ArrayList<TEDSDataField> tedsPredicatesList;

    private int NumOfChannels;

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
	setName("TEDSDummyRandomDataProducer-Thread" + (++threadCounter));

	AddressBean addressBean = (AddressBean) context
		.get(Container.STREAM_SOURCE_ACTIVE_ADDRESS_BEAN);
	/**
         * Reading from the XML Configurations provided.
         */

	tedsPredicatesList = new ArrayList<TEDSDataField>();
	NumOfChannels = Integer.parseInt((String) addressBean
		.getPredicateValue("TotalFields"));
	for (int i = 1; i <= NumOfChannels; i++) {
	    tedsPredicatesList.add(new TEDSDataField(addressBean
		    .getPredicateValue("field." + i)));
	}

	try {
	    getStorageManager().createTable(getDBAlias(),
		    getProducedStreamStructure());
	} catch (SQLException e) {
	    logger.error(e.getMessage(), e);
	    return false;
	}

	this.start();
	return true;
    }

    public void run() {
	int RATE = 3000;
	try {
	    Thread.sleep(RATE);
	} catch (InterruptedException e1) {
	    e1.printStackTrace();
	}
	while (isActive()) {

	    String[] dataFieldNames = new String[NumOfChannels];
	    Integer[] dataFieldTypes = new Integer[NumOfChannels];

	    for (int i = 0; i < NumOfChannels; i++) {
		dataFieldNames[i] = tedsPredicatesList.get(i).name;
		dataFieldTypes[i] = tedsPredicatesList.get(i).dataType;
	    }

	    Serializable[] dataFieldValues = (new TEDSDataField()
		    .RandomData(dataFieldTypes));// new
	    // Serializable[NumOfChannels];

	    // logger.fatal(dataFieldNames);
	    // logger.fatal(dataFieldTypes);
	    // logger.fatal(dataFieldValues);
	    StreamElement streamElement = new StreamElement(
		    getProducedStreamStructure(), dataFieldValues, System
			    .currentTimeMillis());
	    try {
		publishData(streamElement);
	    } catch (Exception e) {
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
	// logger.fatal("Hi...");
	// logger.fatal(tedsPredicatesList);
	for (TEDSDataField field : tedsPredicatesList) {
	    dataField.add(new DataField(field.name, field.type,
		    field.description));
	    // System.out.println("'''''''''''''''''''''''''''''"+field.type);

	}
	return dataField;
    }

    public class TEDSDataField {
	public String name;

	public String type;

	public int dataType;

	public String description;

	public TEDSDataField(String xmlString) {
	    StringTokenizer tokens = new StringTokenizer(xmlString, "|");
	    this.name = tokens.nextToken();
	    this.type = tokens.nextToken();
	    this.description = tokens.nextToken();
	    this.dataType = DataTypes.convertTypeNameToTypeID(type);
	}

	public TEDSDataField() {

	}

	public Serializable[] RandomData(Integer[] dataTypes) {
	    Serializable[] result = new Serializable[dataTypes.length];
	    for (int i = 0; i < dataTypes.length; i++) {
		switch (dataTypes[i]) {
		case DataTypes.BIGINT:
		case DataTypes.INTEGER:
		    result[i] = (int) (Math.random() * 255);
		    break;
		case DataTypes.DOUBLE:
		    result[i] = Math.random() * 255;
		    break;
		case DataTypes.BINARY:
		    result[i] = (byte) Math.random() * 255;
		    break;
		case DataTypes.VARCHAR:
		    byte oneCharacter;
		    StringBuffer resultS = new StringBuffer(10);
		    for (int ii = 0; ii < 10; ii++) {
			oneCharacter = (byte) ((Math.random() * ('z' - 'a' + 1)) + 'a');
			resultS.append((char) oneCharacter);
		    }
		    result[i] = resultS.toString();
		    ;
		    break;
		default:
		    break;

		}

	    }
	    return result;
	}

    }

}
