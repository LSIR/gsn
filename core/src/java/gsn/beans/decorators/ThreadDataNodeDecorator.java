package gsn.beans.decorators;

import gsn.beans.model.*;

import java.util.List;

public class ThreadDataNodeDecorator implements DataNodeInterface, Runnable {

    private QueueDataNodeDecorator node;

    public ThreadDataNodeDecorator(QueueDataNodeDecorator node) {
        this.node = node;
    }

    public boolean initialize(Parameter parameters) {
        return false;
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


    public void run() {
        // There is something to be processed.
        // insert the final output into my window.
        // distribute my window to the parent nodes.

    }

    protected void distribute(Window data) {
        for (DataNodeInterface parent : getParents()) {
            QueueDataNodeDecorator parentDec = (QueueDataNodeDecorator) parent;
            parentDec.getQueue(this).add(data);
        }
    }


}