package gsn.beans.decorators;

import gsn.beans.BetterQueue;
import gsn.beans.QueueFactory;
import gsn.beans.DataWindow;
import gsn.beans.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public class QueueDataNodeDecorator implements DataNodeInterface {

    protected DataNodeInterface node;

    private HashMap<DataNodeInterface, BetterQueue<DataWindow>> childQueues = new HashMap<DataNodeInterface, BetterQueue<DataWindow>>();

    public QueueDataNodeDecorator(DataNodeInterface node) {
        this.node = node;
        for (DataNodeInterface child : node.getChildren()) {
            BetterQueue<DataWindow> queue = QueueFactory.createBetterQueue();
            childQueues.put(child, queue);
        }
    }

    public DataNodeInterface getDecoratedNode(){
        return node;
    }

    public List<DataNodeInterface> getParents() {
        return node.getParents();
    }

    public void setParents(List<DataNodeInterface> parents) {
        node.setParents(parents);
    }

    public Window getWindow() {
        return node.getWindow();
    }

    public void setWindow(Window window) {
        node.setWindow(window);
    }

    public Sliding getSliding() {
        return node.getSliding();
    }

    public void setSliding(Sliding sliding) {
        node.setSliding(sliding);
    }

    public List<DataNodeInterface> getChildren() {
        return node.getChildren();
    }

    public void setChildren(List<DataNodeInterface> children) {
        node.setChildren(children);
    }

    public VirtualSensor getVirtualSensor() {
        return node.getVirtualSensor();
    }

    public void setVirtualSensor(VirtualSensor virtualSensor) {
        node.setVirtualSensor(virtualSensor);
    }

    public HashMap<DataNodeInterface, BetterQueue<DataWindow>> getChildrenQueues() {
        return childQueues;
    }

    public BetterQueue<DataWindow> getQueue(DataNodeInterface childNode) {
        return childQueues.get(childNode);
    }

    private ArrayBlockingQueue q;
}
