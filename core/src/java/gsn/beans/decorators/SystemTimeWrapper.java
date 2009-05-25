package gsn.beans.decorators;

import gsn.beans.BetterQueue;
import gsn.beans.StreamElement;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.HashMap;

public class SystemTimeWrapper implements ActionListener {
    private Timer timer;
    private BetterQueue queue;
    private Logger logger = Logger.getLogger(this.getClass());

    public boolean initialize(HashMap<String, String> parameters, BetterQueue input) {
        timer = new Timer(0, this);
        this.queue = input;
        return true;
    }

    public void destroy() {
        timer.stop();
    }

    public void run() {
        timer.start();
    }

    public void actionPerformed(ActionEvent actionEvent) {
        final Serializable[] EMPTY_DATA_PART = new Serializable[]{};
        final Byte[] EMPTY_FIELD_TYPES = new Byte[]{};
        String[] EMPTY_FIELD_LIST = new String[]{};
        StreamElement streamElement = new StreamElement(EMPTY_FIELD_LIST, EMPTY_FIELD_TYPES, EMPTY_DATA_PART, actionEvent.getWhen());
        queue.add(streamElement);
    }
}
