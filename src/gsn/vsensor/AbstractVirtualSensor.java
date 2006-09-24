package gsn.vsensor ;

import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public abstract class AbstractVirtualSensor implements VirtualSensor {

    private static final transient Logger logger = Logger.getLogger(AbstractVirtualSensor.class);

    private static final transient boolean isDebugEnabled = logger.isDebugEnabled();

    private static final transient boolean isInfoEnabled = logger.isDebugEnabled();

    /**
     * A reference to the Container implementation. Will be initialized via
     * <code>initialize</code> method.
     */
    protected Container container;

    protected VSensorConfig virtualSensorConfiguration;

    protected ArrayList<StreamElement> producedData = new ArrayList<StreamElement>();

    private long lastVisitiedTime = 0;

    public boolean initialize(HashMap map) {
        container = (Container) map.get(VirtualSensorPool.CONTAINER);
        virtualSensorConfiguration = (VSensorConfig) map.get(VirtualSensorPool.VSENSORCONFIG);
        if (isInfoEnabled)
            logger.info((new StringBuilder("Initialization of :")).append(virtualSensorConfiguration.getVirtualSensorName()).toString());
        return true;
    }

    protected void validateStreamElement(StreamElement streamElement) {
        if (! compatibleStructure(streamElement.getFieldTypes(), virtualSensorConfiguration.getOutputStructure())) {
            StringBuilder exceptionMessage = new StringBuilder().append("The streamElement produced by :").append(virtualSensorConfiguration
                    .getVirtualSensorName()).append(" Virtual Sensor is not compatible with the defined streamElement.\n");
            exceptionMessage
                    .append("The expected stream element structure (specified in ").append(virtualSensorConfiguration.getFileName()).append(" is [");
            for (DataField df : virtualSensorConfiguration.getOutputStructure()) {
                exceptionMessage.append(df.getFieldName()).append(" (").append(DataTypes.TYPE_NAMES[df.getDataTypeID()]).append(") , ");
            }
            exceptionMessage
                    .append("] but the actual stream element received from the " + virtualSensorConfiguration.getVirtualSensorName()).append(" has the [");
            for (int i = 0; i < streamElement.getFieldNames().length; i ++)
                exceptionMessage
                        .append(streamElement.getFieldNames()[i]).append("(").append(DataTypes.TYPE_NAMES[streamElement.getFieldTypes()[i]])
                        .append("),").append(" ] thus the stream element dropped !!!");
            throw new RuntimeException(exceptionMessage.toString());
        }
    }

    protected synchronized void dataProduced(StreamElement streamElement) {
        try {
            validateStreamElement(streamElement);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return;
        }

        if (! streamElement.isTimestampSet())
            streamElement.setTimeStamp(System.currentTimeMillis());

        final int outputStreamRate = virtualSensorConfiguration.getOutputStreamRate();
        final long currentTime = System.currentTimeMillis();
        if ((currentTime - lastVisitiedTime) < outputStreamRate) {
            if (isInfoEnabled)
                logger.info("Called by *discarded* b/c of the rate limit reached.");
            return;
        }
        lastVisitiedTime = currentTime;
        synchronized (producedData) {
            producedData.add(streamElement);
        }
        container.publishData(this);
    }

    private static boolean compatibleStructure(Integer [ ] fieldTypes, ArrayList<DataField> outputStructure) {
        if (outputStructure.size() != fieldTypes.length) {
            logger.warn("Validation problem, the number of field doesn't match the number of output data strcture of the virtual sensor");
            return false;
        }
        for (int i = 0; i < outputStructure.size(); i ++) {
            if (fieldTypes[i] != outputStructure.get(i).getDataTypeID()) {
                logger.warn("Validation problem for output field >" + outputStructure.get(i).getFieldName() + ", The field type declared as >"
                        + DataTypes.TYPE_NAMES[fieldTypes[i]] + "< within the stream element but it is defined as in the VSD as : "
                        + DataTypes.TYPE_NAMES[outputStructure.get(i).getDataTypeID()]);
                return false;
            }
        }
        return true;
    }

    public String getName() {
        return virtualSensorConfiguration.getVirtualSensorName();
    }

    public synchronized StreamElement getData() {
        StreamElement toReturn;
        synchronized (producedData) {
            toReturn = producedData.remove(0);
        }
        return toReturn;
    }

    public void finalize(HashMap map) {
        if (isInfoEnabled)
            logger.info((new StringBuilder("Finalizing the :")).append(virtualSensorConfiguration.getVirtualSensorName()).append(".").toString());
    }

    public void dataFromWeb(String data) {
        return;
    }
}
