package gsn.wrappers ;

import gsn.beans.AddressBean ;
import gsn.beans.DataField ;
import gsn.beans.DataTypes ;
import gsn.beans.StreamElement ;
import gsn.storage.StorageManager ;
import gsn.vsensor.Container ;
import gsn.utils.ParamParser;

import java.io.Serializable ;
import java.sql.SQLException ;
import java.util.ArrayList ;
import java.util.Collection ;
import java.util.HashMap ;
import java.util.TreeMap ;

import org.apache.log4j.Logger ;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class DummyDataProducer extends AbstractStreamProducer {


    /**
     * The rate specified when the wrapper is going to produce data (if the
     * underlying<br>
     * sensor network is time driven).<br>
     * If the underlying sensor network is data-driven (e.g., the sensor network
     * doesn't have a specific rate)<br>
     * you can check the <code>gsn.wrappers.SFWrapperDS</code> which is the
     * wrapper implemented for the serial forward software.<br>
     * The <code>gsn.wrappers.SFWrapperDS</code> is a data driven wrapper.<br>
     */

    private final int RATE_DEFAULT_VALUE = 2000;

    private int rate = RATE_DEFAULT_VALUE;

    private byte [ ] binaryPortionOfEachStreamElement;

    private final transient Logger logger = Logger.getLogger(DummyDataProducer.class);

    /**
     * Initializes the DataSource object. This method should be called before any
     * real task to be done. <p/> The initalContext which can be used to extract
     * required information. The storageManager useful for issuing the queries.
     */
    private static int threadCounter = 0;

    private static final int DATA_SIZE_IN_BYTES_DEFAULT_VALUE = 0;

    private int dataSizeInBytes = DATA_SIZE_IN_BYTES_DEFAULT_VALUE;


    /**
     * Configurable option through the xml file are :<br>
     * rate : the output rate in milli seconds <br>
     * size : the output stream element size in bytes <br>
     */
    public boolean initialize(TreeMap context) {
        boolean toReturn = super.initialize(context);
        if (toReturn == false)
            return false;
        setName("DummyDataProducer-Thread" + (++ threadCounter));

        AddressBean addressBean = (AddressBean) context.get(Container.STREAM_SOURCE_ACTIVE_ADDRESS_BEAN);
        if (addressBean.getPredicateValue("packet-size") != null) {
            dataSizeInBytes = ParamParser.getInteger(addressBean.getPredicateValue("packet-size"), DATA_SIZE_IN_BYTES_DEFAULT_VALUE);
            if (dataSizeInBytes <= 0) {
                logger.warn("The specified >packet-size< parameter for the >DummyDataProducer< shouldn't be a negative number.\nGSN uses the default size(" + DATA_SIZE_IN_BYTES_DEFAULT_VALUE + " bytes).");
                dataSizeInBytes = DATA_SIZE_IN_BYTES_DEFAULT_VALUE;
            }

        }
        if (addressBean.getPredicateValue("rate") != null) {
            rate = ParamParser.getInteger(addressBean.getPredicateValue("rate"), RATE_DEFAULT_VALUE);
            if (rate <= 0) {
                logger.warn("The specified rate parameter for the >DummyDataProducer< should be a positive number.\nGSN uses the default rate (" + RATE_DEFAULT_VALUE + ").");
                rate = RATE_DEFAULT_VALUE;
            }
        }
        if (dataSizeInBytes <= 0) {
            binaryPortionOfEachStreamElement = new byte [ dataSizeInBytes ];
            for (int i = 0; i < dataSizeInBytes; i ++)
                binaryPortionOfEachStreamElement[i] = (byte) ((Math.random() * 255));
        }
        try {
            StorageManager.getInstance().createTable(getDBAlias(), getProducedStreamStructure());
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            return false;
        }

        this.start();
        return true;
    }

    public void run() {
        int counter = 0;
        /**
         * The DummyDataProducer looks for clients.
         * If there is no client register, it sleeps and
         */

        while (isActive()) {
            try {
                Thread.sleep(rate);
                counter++;
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
            if (listeners.isEmpty())
                continue;

            StreamElement streamElement = null;
            if (dataSizeInBytes > 0)
                streamElement = new StreamElement(
                        new String [ ]{"DATA", "DATA_BIN"}, new Integer [ ]{DataTypes.INTEGER, DataTypes.BINARY}, new Serializable [ ]{counter,
                        binaryPortionOfEachStreamElement}, System.currentTimeMillis());
            else
                streamElement = new StreamElement(
                        new String [ ]{"DATA"}, new Integer [ ]{DataTypes.INTEGER}, new Serializable [ ]{counter}, System.currentTimeMillis());
            // SimulationResult.addJustProducedFromDummyDataSource () ;
            publishData(streamElement);
        }
    }

    public void finalize(HashMap context) {
        super.finalize(context);
        threadCounter --;
    }

    /**
     * This Method expresses to the GSN Container the output structure of the
     * wrapper. <br>
     * For instance, the following implementation shows that each stream element
     * produced by<br>
     * the wrapper contains two fields, first one called <code>DATA</code> and
     * the<br>
     * other is <code>DATA_BIN</code>. <br>
     */
    public Collection<DataField> getProducedStreamStructure() {
        ArrayList<DataField> dataField = new ArrayList<DataField>();
        dataField.add(new DataField("DATA", "int", "incremental int"));
        if (dataSizeInBytes > 0)
            dataField.add(new DataField("DATA_BIN", "BINARY", "Binary-data"));
        return dataField;
    }

}
