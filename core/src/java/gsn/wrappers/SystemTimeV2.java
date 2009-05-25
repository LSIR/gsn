package gsn.wrappers;

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

public class SystemTimeV2 implements Wrapper, ActionListener {

    private Timer timer;
    private transient Logger logger = Logger.getLogger(this.getClass());
    private int period;
    private boolean isEnabled = false;
    private long nextTimestamp = System.currentTimeMillis();
    private WrapperListener dispatcher;

    public boolean initialize(EasyParamWrapper parameters, WrapperListener listener) {
        this.dispatcher = listener;
        boolean auto = parameters.getPredicateValueAsBoolean("mode", false);
        period = parameters.getPredicateValueAsInt("period", 10); //defaults to 10msc

        if (auto) {
            isEnabled = true;
            timer = new Timer(period, this);
        }

        return true;
    }

    public boolean sendToWrapper(String action, String[] paramNames, Object[] paramValues) throws OperationNotSupportedException {
        throw new OperationNotSupportedException("This wrapper doesn't support sending data back to the source.");
    }

    public void releaseResources() {
        timer.stop();
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


    public void actionPerformed(ActionEvent e) {
        Serializable[] EMPTY_DATA_PART = new Serializable[]{};
        Byte[] EMPTY_FIELD_TYPES = new Byte[]{};
        String[] EMPTY_FIELD_LIST = new String[]{};

        nextTimestamp += period;
        StreamElement streamElement = new StreamElement(EMPTY_FIELD_LIST, EMPTY_FIELD_TYPES, EMPTY_DATA_PART, nextTimestamp);
        dispatcher.dataProduced(streamElement);
    }
}
