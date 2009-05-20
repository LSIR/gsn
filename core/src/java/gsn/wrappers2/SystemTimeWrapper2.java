package gsn.wrappers2;


import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.model.Parameter;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.List;

public class SystemTimeWrapper2 extends AbstractWrapper2 implements ActionListener {

    private static final Serializable[] EMPTY_DATA_PART = new Serializable[]{};

    private static final Byte[] EMPTY_FIELD_TYPES = new Byte[]{};

    private String[] EMPTY_FIELD_LIST = new String[]{};

    private transient Logger logger = Logger.getLogger(this.getClass());

    private Timer timer;

    private long nextTimestamp;

    private int period;

    public DataField[] getOutputFormat() {
        return new DataField[0];
    }

    public boolean initialize(List<Parameter> parameters) {
        boolean auto = true;
        for (Parameter parameter : parameters) {
            if ("mode".equals(parameter.getModel().getName())) {
                String mode = parameter.getValue();
                if (!"auto".equals(mode)) {
                    auto = false;
                }
            } else if ("period".equals(parameter.getModel().getName())) {
                period = Integer.getInteger(parameter.getValue());
            }
        }

        if (auto) {
            timer = new Timer(period, this);
        }
        return true;
    }

    public void run() {
        if (timer != null) {
            timer.start();
        } else {
            nextTimestamp = System.currentTimeMillis();
        }
    }

    public void produceNext() {
        nextTimestamp += period;
        StreamElement streamElement = new StreamElement(EMPTY_FIELD_LIST, EMPTY_FIELD_TYPES, EMPTY_DATA_PART, nextTimestamp);
        postStreamElement(streamElement);
    }

    public void actionPerformed(ActionEvent actionEvent) {
        StreamElement streamElement = new StreamElement(EMPTY_FIELD_LIST, EMPTY_FIELD_TYPES, EMPTY_DATA_PART, actionEvent.getWhen());
        postStreamElement(streamElement);
    }

    public void finalize() {
        if (timer != null) {
            timer.stop();
        }
    }
}

