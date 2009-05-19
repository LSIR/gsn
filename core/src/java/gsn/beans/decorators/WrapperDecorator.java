package gsn.beans.decorators;

import gsn.beans.BetterQueue;
import gsn.beans.StreamElement;
import gsn.beans.model.Parameter;
import gsn.beans.model.WrapperNode;
import gsn.beans.model.WrapperModel;
import gsn.wrappers2.WrapperListener;
import gsn.wrappers2.AbstractWrapper2;

import java.util.ArrayList;

import org.apache.log4j.Logger;

public class WrapperDecorator extends ThreadDataNodeDecorator implements WrapperListener{

    private final static transient Logger logger = Logger.getLogger(WrapperDecorator.class);

    ArrayList<StreamElement> totalWindow = new ArrayList<StreamElement>();
    BetterQueue slidingQueue = new BetterQueue(); // get Queue from Slider.

    public WrapperDecorator(QueueDataNodeDecorator node) {
        super(node);
        WrapperNode wrapperNode;
        if (!(node.getDecoratedNode() instanceof WrapperNode)) {
            throw new IllegalArgumentException("WrapperDecorator only accepts a decorated WrapperNode parameter. " +
                    "You provided an instance of \"" + node.getDecoratedNode().getClass().getName() + "\"");
        }

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
        totalWindow.add(se);
        slidingQueue.add(se.getTimeStamp());
    }

    public void dataProduced(StreamElement se) {
        post(se);       
    }
}
