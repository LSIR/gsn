package gsn.beans.decorators;

import gsn.beans.DataWindow;
import gsn.beans.model.*;
import gsn.sliding.SlidingInterface;
import gsn.windows.WindowInterface;
import org.apache.log4j.Logger;

import java.util.List;

public class ThreadDataNodeDecorator implements DataNodeInterface, Runnable {
    private final static transient Logger logger = Logger.getLogger(ThreadDataNodeDecorator.class);
    private QueueDataNodeDecorator node;
    protected SlidingInterface slidingHandler;
    protected WindowInterface windowHandler;

    public ThreadDataNodeDecorator(QueueDataNodeDecorator node) {
        this.node = node;
    }


    public boolean initialize() {
//        if (getSliding() == null) {
//            //TODO
//        }
//
//        try {
//            slidingHandler = (SlidingInterface) Class.forName(getSliding().getModel().getClassName()).newInstance();
//        } catch (Exception e) {
//            logger.error("Error in initializing sliding class of type: [" + getSliding().getModel().getClassName() + "]", e);
//            return false;
//        }
//
//        if (getWindow() == null) {
//            //TODO
//        }
//
//        try {
//            windowHandler = (WindowInterface) Class.forName(getWindow().getModel().getClassName()).newInstance();
//        } catch (Exception e) {
//            logger.error("Error in initializing window class of type: [" + getWindow().getModel().getClassName() + "]", e);
//            return false;
//        }

        return true;
    }


    public VirtualSensor getVirtualSensor() {
        return node.getVirtualSensor();
    }

    public void setVirtualSensor(VirtualSensor virtualSensor) {
        node.setVirtualSensor(virtualSensor);
    }

    public List<DataChannel> getInChannels() {
        return node.getInChannels();
    }

    public void setInChannels(List<DataChannel> inChannels) {
        node.setInChannels(inChannels);
    }

    public List<DataChannel> getOutChannels() {
        return node.getOutChannels();
    }

    public void setOutChannels(List<DataChannel> outChannels) {
        node.setOutChannels(outChannels);
    }


    public void run() {
        // There is something to be processed.
        // insert the final output into my window.
        // distribute my window to the parent nodes.

    }

    protected void distribute(DataWindow data) {
//        for (DataNodeInterface parent : getParents()) {
//            QueueDataNodeDecorator parentDec = (QueueDataNodeDecorator) parent;
//            parentDec.getQueue(this).add(data);
//        }
    }


    public void slide(long timestamp) {
        windowHandler.nextWindow(timestamp);
         //distrubute data
    }

    protected DataNodeInterface getDecoratedNode() {
        return node.getDecoratedNode();
    }
}