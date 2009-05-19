package gsn.wrappers2;

import gsn.Main;
import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.model.Parameter;
import gsn.utils.GSNRuntimeException;
import org.apache.log4j.Logger;

import javax.naming.OperationNotSupportedException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractWrapper2 implements Runnable {

    private final static transient Logger logger = Logger.getLogger(AbstractWrapper2.class);

    protected final List<WrapperListener> listeners = Collections.synchronizedList(new ArrayList<WrapperListener>());

    private boolean isActive = true;


    public void addListener(WrapperListener listener) {
        listeners.add(listener);
        if (logger.isDebugEnabled())
            logger.debug("Adding listeners: " + listener.toString());
    }

    public void removeListener(WrapperListener listener){
        listeners.remove(listener);
        if (listeners.size() == 0)
            releaseResources();
    }

    /**
     * @return the listeners
     */
    public List<WrapperListener> getListeners() {
        return listeners;
    }

    /**
     * This method is called whenever the wrapper wants to send a data item back
     * to the source where the data is coming from. For example, If the data is
     * coming from a wireless sensor network (WSN), This method sends a data item
     * to the sink node of the virtual sensor. So this method is the
     * communication between the System and actual source of data. The data sent
     * back to the WSN could be a command message or a configuration message.
     *
     * @return True if the send operation is successful.
     * @throws javax.naming.OperationNotSupportedException If the wrapper doesn't support
     *                                        sending the data back to the source. Note that by default this method
     *                                        throws this exception unless the wrapper overrides it.
     */

    public boolean sendToWrapper(String action, String[] paramNames, Object[] paramValues) throws OperationNotSupportedException {
        throw new OperationNotSupportedException("This wrapper doesn't support sending data back to the source.");
    }

    private final transient int aliasCode = Main.tableNameGenerator();
    private final CharSequence aliasCodeS = Main.tableNameGeneratorInString(aliasCode);

    public abstract DataField[] getOutputFormat();

    public boolean isActive() {
        return isActive;
    }

    protected void postStreamElement(Serializable... values) {
        StreamElement se = new StreamElement(getOutputFormat(), values, System.currentTimeMillis());
        postStreamElement(se);
    }

    protected void postStreamElement(long timestamp, Serializable[] values) {
        StreamElement se = new StreamElement(getOutputFormat(), values, timestamp);
        postStreamElement(se);
    }

    /**
     * This method gets the generated stream element and notifies the input streams if needed.
     * The return value specifies if the newly provided stream element generated
     * at least one input stream notification or not.
     *
     * @param streamElement
     * @return If the method returns false, it means the insertion doesn't effected any input stream.
     */

    protected void postStreamElement(StreamElement streamElement) {
        for (WrapperListener listener : listeners) {
            listener.dataProduced(streamElement);
        }
    }


    /**
     * This method is called whenever the wrapper wants to send a data item back
     * to the source where the data is coming from. For example, If the data is
     * coming from a wireless sensor network (WSN), This method sends a data item
     * to the sink node of the virtual sensor. So this method is the
     * communication between the System and actual source of data. The data sent
     * back to the WSN could be a command message or a configuration message.
     *
     * @param dataItem : The data which is going to be send to the source of the
     *                 data for this wrapper.
     * @return True if the send operation is successful.
     * @throws javax.naming.OperationNotSupportedException If the wrapper doesn't support
     *                                        sending the data back to the source. Note that by default this method
     *                                        throws this exception unless the wrapper overrides it.
     */

    public boolean sendToWrapper(Object dataItem) throws OperationNotSupportedException {
        if (isActive == false)
            throw new GSNRuntimeException("Sending to an inactive/disabled wrapper is not allowed !");
        throw new OperationNotSupportedException("This wrapper doesn't support sending data back to the source.");
    }

    public void releaseResources() {
        isActive = false;
        finalize();
        if (logger.isInfoEnabled()) logger.info("Finalized called");
        listeners.clear();
    }

    public static final String TIME_FIELD = "timed";

    public abstract boolean initialize(List<Parameter> parameters);

    public abstract void finalize();

}