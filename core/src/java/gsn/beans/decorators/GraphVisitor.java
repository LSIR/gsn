package gsn.beans.decorators;

import gsn.beans.model.DataNode;
import gsn.beans.model.DataNodeInterface;
import gsn.beans.model.DataChannel;

import java.util.ArrayList;

public class GraphVisitor {
    private Object visit(DataNodeInterface node, ArrayList<DataNodeInterface> visitedNodes, DecoratorFactory factory) {
        visitedNodes.add(node);
        node = factory.createDecorator(node);
        for (DataChannel child : node.getInChannels()) {
            if (!visitedNodes.contains(child.getProducer())) {
                visitedNodes.add(child.getProducer());
                visit(child.getProducer(), visitedNodes, factory);
            }
        }
        return node;
    }

    public void visitGraph(DataNode node) {
//        visit(node,new ArrayList<DataNode>());
    }

    public ThreadDataNodeDecorator addThreadDecorator(DataNode node) {
        return (ThreadDataNodeDecorator) visit(node, new ArrayList<DataNodeInterface>(), new ThreadDataNodeFactory());
    }

    public QueueDataNodeDecorator addQueueDecorator(DataNode node) {
        return (QueueDataNodeDecorator) visit(node, new ArrayList<DataNodeInterface>(), new QueueDataNodeFactory());
    }
}

class ThreadDataNodeFactory implements DecoratorFactory {
    public DataNodeInterface createDecorator(DataNodeInterface node) {
        return new ThreadDataNodeDecorator((QueueDataNodeDecorator) node);
    }
}

class QueueDataNodeFactory implements DecoratorFactory {
    public DataNodeInterface createDecorator(DataNodeInterface node) {
        return new QueueDataNodeDecorator(node);
    }

}