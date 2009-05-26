package gsn.wrappers2;


import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.interfaces.Wrapper;
import gsn.beans.interfaces.WrapperListener;
import gsn.utils.EasyParamWrapper;
import org.apache.log4j.Logger;

import javax.naming.OperationNotSupportedException;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

public class SystemTimeWrapper2 implements Wrapper, ActionListener {

    private transient Logger logger = Logger.getLogger(this.getClass());
    private static final Serializable[] EMPTY_DATA_PART = new Serializable[]{};
    private static final Byte[] EMPTY_FIELD_TYPES = new Byte[]{};
    private String[] EMPTY_FIELD_LIST = new String[]{};
    private Timer timer;
    private long nextTimestamp = -1;
    private int period;
    private boolean isEnabled;
    private WrapperListener dataReceiver;

    public DataField[] getOutputFormat() {
        return new DataField[0];
    }

    public boolean initialize(EasyParamWrapper parameters, WrapperListener wrapperListener) {
        this.dataReceiver = wrapperListener;
        boolean auto = parameters.getPredicateValueAsBoolean("mode", false);
        period = parameters.getPredicateValueAsInt("period", 10); //defaults to 10msc

        if (auto) {
            timer = new Timer(period, this);
        }
        return true;
    }

    public void produceNext() {
        if (nextTimestamp == -1) {
            nextTimestamp = System.currentTimeMillis();
        } else {
            nextTimestamp += period;
        }
        StreamElement streamElement = new StreamElement(EMPTY_FIELD_LIST, EMPTY_FIELD_TYPES, EMPTY_DATA_PART, nextTimestamp);
        dataReceiver.dataProduced(streamElement);
    }

    public void actionPerformed(ActionEvent actionEvent) {
        produceNext();
    }

    public boolean sendToWrapper(String action, String[] paramNames, Object[] paramValues) throws OperationNotSupportedException {
        throw new OperationNotSupportedException("This wrapper doesn't support sending data back to the source.");
    }

    public void releaseResources() {
        if (timer != null) {
            timer.stop();
        }
    }

    public synchronized void setEnabled(boolean newEnabled) {
        if (newEnabled == this.isEnabled)
            return;
        if (newEnabled == false)
            timer.stop();
        if (newEnabled == true)
            timer.restart();
        this.isEnabled = newEnabled;
    }

    public synchronized boolean getEnabled() {
        return isEnabled;
    }

    public void run() {

    }
}

