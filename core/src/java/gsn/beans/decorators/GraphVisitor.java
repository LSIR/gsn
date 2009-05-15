package gsn.beans.decorators;

import gsn.beans.model.DataNode;
import gsn.beans.model.DataNodeInterface;

import java.util.ArrayList;

public class GraphVisitor {
    private Object visit(DataNodeInterface node, ArrayList visitedNodes,  DecoratorFactory factory){
        visitedNodes.add(node);
        node = factory.createDecorator(node);
        for (DataNodeInterface child : node.getChildren()){
            if (!visitedNodes.contains(child)){
                visitedNodes.add(child);
                visit(child,visitedNodes,factory);
            }
        }
        return node;
    }

    public void visitGraph(DataNode node){
//        visit(node,new ArrayList<DataNode>());
    }

    public ThreadDataNodeDecorator addThreadDecorator(DataNode node){
        return (ThreadDataNodeDecorator) visit(node, new ArrayList(), new ThreadDataNodeFactory());
    }

    public QueueDataNodeDecorator addQueueDecorator(DataNode node){
        return (QueueDataNodeDecorator) visit(node, new ArrayList(), new QueueDataNodeFactory());
    }
}

class ThreadDataNodeFactory implements DecoratorFactory{
     public DataNodeInterface createDecorator(DataNodeInterface node) {
        return new ThreadDataNodeDecorator((QueueDataNodeDecorator)node);
    }
}

class QueueDataNodeFactory implements DecoratorFactory{
    public DataNodeInterface createDecorator(DataNodeInterface node) {
        return new QueueDataNodeDecorator(node);
    }

}