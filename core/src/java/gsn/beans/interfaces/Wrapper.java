package gsn.beans.interfaces;

import gsn.utils.EasyParamWrapper;

import javax.naming.OperationNotSupportedException;


public interface Wrapper extends Runnable {
    public boolean initialize(EasyParamWrapper parameters, WrapperListener listener);

    /**
     * This method is called whenever the wrapper wants to send a data item back
     * to the source where the data is coming from. For example, If the data is
     * coming from a wireless sensor network (WSN), This method sends a data item
     * to the sink node of the virtual sensor. So this method is the
     * communication between the System and actual source of data. The data sent
     * back to the WSN could be a command message or a configuration message.
     *
     * @return True if the send operation is successful.
     * @throws javax.naming.OperationNotSupportedException
     *          If the wrapper doesn't support
     *          sending the data back to the source. Note that by default this method
     *          throws this exception unless the wrapper overrides it.
     */
    public boolean sendToWrapper(String action, String[] paramNames, Object[] paramValues) throws OperationNotSupportedException;

    public void releaseResources();

    public void setEnabled(boolean isEnabled);

    public boolean getEnabled();

}
