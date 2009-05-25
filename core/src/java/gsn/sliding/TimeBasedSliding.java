package gsn.sliding;

import gsn.beans.StreamElement;
import gsn.utils.EasyParamWrapper;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TimeBasedSliding implements SlidingInterface, ActionListener {
    private int size;
    private SlidingListener listener;
    private Timer timer;

    public boolean initialize(EasyParamWrapper easyParamWrapper, SlidingListener listener) {
        this.size = easyParamWrapper.getPredicateValueAsIntWithException("size");
        this.listener = listener;
        timer = new Timer(size, this);
        timer.start();
        return true;
    }

    public void postData(StreamElement se) {

    }

    public void reset() {
        timer.stop();
        timer = new Timer(size, this);
        timer.start();
    }

    public void actionPerformed(ActionEvent e) {
        listener.slide(System.currentTimeMillis());
    }
}
