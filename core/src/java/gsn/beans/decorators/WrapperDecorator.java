package gsn.beans.decorators;

import gsn.beans.DataWindow;
import gsn.beans.StreamElement;
import gsn.beans.model.Parameter;
import gsn.beans.model.WrapperModel;
import gsn.beans.model.WrapperNode;
import gsn.beans.windowing.CountBasedSlidingHandler;
import gsn.wrappers2.AbstractWrapper2;
import gsn.wrappers2.WrapperListener;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.ListIterator;

public class WrapperDecorator extends ThreadDataNodeDecorator implements WrapperListener {

    private final static transient Logger logger = Logger.getLogger(WrapperDecorator.class);

    ArrayList<StreamElement> totalWindow = new ArrayList<StreamElement>();

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
        AbstractWrapper2 wrapper = null;
        try {
            wrapper = (AbstractWrapper2) Class.forName(wrapperModel.getClassName()).newInstance();
        } catch (Exception e) {
            logger.error("Error in initializing wrapper class of type: [" + wrapperModel.getClassName() + "]", e);
            //todo ?
        }

        wrapper.initialize(wrapperNode.getParameters());
        wrapper.addListener(this);
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
