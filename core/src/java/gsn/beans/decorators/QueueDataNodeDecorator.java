package gsn.beans.decorators;

import gsn.beans.BetterQueue;
import gsn.beans.DataWindow;
import gsn.beans.QueueFactory;
import gsn.beans.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public class QueueDataNodeDecorator implements DataNodeInterface {

    protected DataNodeInterface node;

    private HashMap<DataNodeInterface, BetterQueue<DataWindow>> childQueues = new HashMap<DataNodeInterface, BetterQueue<DataWindow>>();

    public QueueDataNodeDecorator(DataNodeInterface node) {
        this.node = node;
//        for (DataNodeInterface child : node.getChildren()) {
//            BetterQueue<DataWindow> queue = QueueFactory.createBetterQueue();
//            childQueues.put(child, queue);
//        }
    }

    public DataNodeInterface getDecoratedNode() {
        return node;
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

    public HashMap<DataNodeInterface, BetterQueue<DataWindow>> getChildrenQueues() {
        return childQueues;
    }

    public BetterQueue<DataWindow> getQueue(DataNodeInterface childNode) {
        return childQueues.get(childNode);
    }

    private ArrayBlockingQueue q;
}
