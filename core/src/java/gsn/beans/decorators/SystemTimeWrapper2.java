package gsn.beans.decorators;


import gsn.beans.StreamElement;
import gsn.beans.model.Parameter;

import java.io.Serializable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import org.apache.log4j.Logger;

import javax.swing.*;

public class SystemTimeWrapper2 extends WrapperDecorator implements ActionListener{

    private static final Serializable[] EMPTY_DATA_PART = new Serializable[]{};

    private static final Byte[] EMPTY_FIELD_TYPES = new Byte[]{};

    private static final int DEFAULT_MAX_DELAY = 5000;//5 seconds;

    private String[] EMPTY_FIELD_LIST = new String[]{};

    private transient Logger logger = Logger.getLogger(this.getClass());

    private Timer timer;

    private boolean delayPostingElements = false;

    private int maximumDelay = DEFAULT_MAX_DELAY;

    public SystemTimeWrapper2(QueueDataNodeDecorator node) {
        super(node);
    }

    public boolean initialize(List<Parameter> parameters ) {
        timer = new Timer(0, this);
        return true;
    }

    public void run() {
        timer.start();
    }

    public void actionPerformed(ActionEvent actionEvent) {
        StreamElement streamElement = new StreamElement(EMPTY_FIELD_LIST, EMPTY_FIELD_TYPES, EMPTY_DATA_PART, actionEvent.getWhen());
        post(streamElement);
    }

    public void finalize() {
        timer.stop();

    }
}

