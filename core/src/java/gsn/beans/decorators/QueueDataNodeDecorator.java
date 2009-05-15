package gsn.beans.decorators;

import gsn.beans.model.*;
import gsn.beans.BetterQueue;

import java.util.List;
import java.util.Queue;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

public class QueueDataNodeDecorator  implements DataNodeInterface{

    private DataNodeInterface node;

    private HashMap<DataNodeInterface, BetterQueue> childQueues = new HashMap<DataNodeInterface, BetterQueue>();

    public QueueDataNodeDecorator(DataNodeInterface node){
        this.node = node;
        for(DataNodeInterface child : node.getChildren()){
            BetterQueue<Window> queue = QueueFactory.createBetterQueue();
            childQueues.put(child,queue);
        }
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

    public HashMap<DataNodeInterface, BetterQueue> getChildrenQueues(){
        return childQueues;
    }

    public BetterQueue<Window> getQueue(DataNodeInterface childNode){
        return childQueues.get(childNode);
    }

    ArrayBlockingQueue q;
}
