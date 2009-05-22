package gsn.beans.decorators;

import gsn.beans.DataWindow;
import gsn.beans.StreamElement;
import gsn.beans.BetterQueue;
import gsn.beans.QueueChangeListener;
import gsn.beans.interfaces.Wrapper;
import gsn.beans.model.Parameter;
import gsn.beans.model.WrapperModel;
import gsn.beans.model.WrapperNode;
import gsn.beans.model.DataNodeInterface;
import gsn.beans.windowing.CountBasedSlidingHandler;
import gsn.wrappers2.AbstractWrapper2;
import gsn.wrappers2.WrapperListener;
import gsn.utils.EasyParamWrapper;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.ListIterator;

public class WrapperDecorator extends ThreadDataNodeDecorator implements WrapperListener {

    private final static transient Logger logger = Logger.getLogger(WrapperDecorator.class);

    private  ArrayList<StreamElement> totalWindow = new ArrayList<StreamElement>();

    public WrapperDecorator(QueueDataNodeDecorator node) {
        super(node);
        WrapperNode wrapperNode;
        if (!(node.getDecoratedNode() instanceof WrapperNode)) {
            throw new IllegalArgumentException("WrapperDecorator only accepts a decorated WrapperNode parameter. " +
                    "You provided an instance of \"" + node.getDecoratedNode().getClass().getName() + "\"");
        }

        slidingHandler = new CountBasedSlidingHandler(this, getSliding());
        wrapperNode = (WrapperNode) node.getDecoratedNode();
        initializeWrapper(wrapperNode);
    }

    private void initializeWrapper(WrapperNode wrapperNode) {
        WrapperModel wrapperModel = wrapperNode.getModel();
        Wrapper wrapper = null;
        try {
            wrapper = (Wrapper) Class.forName(wrapperModel.getClassName()).newInstance();
        } catch (Exception e) {
            logger.error("Error in initializing wrapper class of type: [" + wrapperModel.getClassName() + "]", e);
            //todo ?
        }
        BetterQueue distributerQueue = new BetterQueue();
        distributerQueue.addListener(new QueueChangeListener(){
            public void itemAdded(Object obj) {
                for (DataNodeInterface parent : getParents()){
                    QueueDataNodeDecorator p = (QueueDataNodeDecorator) parent;
                    // TODO: data dissemination, how !
                }
            }

            public void itemRemove(Object obj) {

            }

            public void queueEmpty() {

            }

            public void queueNotEmpty() {

            }
        });

        wrapper.initialize(new EasyParamWrapper(wrapperNode.getParameters()),distributerQueue);

        // start the wrapper in a thread
    }



    public boolean initialize(Parameter parameters) {
        return false;
    }

    public void post(StreamElement se) {
        synchronized (this) {
            totalWindow.add(se);
        }
        slidingHandler.addNewElement(se.getTimeStamp());
    }

    public void dataProduced(StreamElement se) {
        post(se);
    }

    public void slide(long timestamp) {
        //update window if necessary
        //distrubute data
        DataWindow dataWindow = new DataWindow(getWindow());
        synchronized (this) {
            for (ListIterator<StreamElement> iter = totalWindow.listIterator(); iter.hasNext(); ) {
                StreamElement streamElement = iter.next();
                if(streamElement.getTimeStamp() <= timestamp && streamElement.getTimeStamp() >= timestamp - dataWindow.getSize()){
                    dataWindow.addElement(streamElement);
                    iter.remove();
                }
            }
        }
        distribute(dataWindow);
    }
}
