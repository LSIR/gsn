package gsn.beans.decorators;

import gsn.beans.StreamElement;
import gsn.beans.interfaces.Wrapper;
import gsn.beans.interfaces.WrapperListener;
import gsn.beans.model.WrapperModel;
import gsn.beans.model.WrapperNode;
import gsn.sliding.SlidingListener;
import gsn.utils.EasyParamWrapper;
import org.apache.log4j.Logger;

import java.util.ArrayList;

public class WrapperDecorator extends ThreadDataNodeDecorator implements WrapperListener, SlidingListener {

    private final static transient Logger logger = Logger.getLogger(WrapperDecorator.class);

    private ArrayList<StreamElement> totalWindow = new ArrayList<StreamElement>();
    private Wrapper wrapper;

    public WrapperDecorator(QueueDataNodeDecorator node) {
        super(node);
        // start the wrapper in a thread
    }

    public boolean initialize() {
        boolean result = super.initialize();
        if (result) {
            WrapperNode wrapperNode;
            if (!(getDecoratedNode() instanceof WrapperNode)) {
                throw new IllegalArgumentException("WrapperDecorator only accepts a decorated WrapperNode parameter. " +
                        "You provided an instance of \"" + getDecoratedNode().getClass().getName() + "\"");
            }

            wrapperNode = (WrapperNode) getDecoratedNode();

            WrapperModel wrapperModel = wrapperNode.getModel();
            try {
                wrapper = (Wrapper) Class.forName(wrapperModel.getClassName()).newInstance();
                result = wrapper.initialize(new EasyParamWrapper(wrapperNode.getParameters()), this);
            } catch (Exception e) {
                logger.error("Error in initializing wrapper class of type: [" + wrapperModel.getClassName() + "]", e);
                //todo ?
            }
//            result = slidingHandler.initialize(new EasyParamWrapper(getSliding().getParameters()), this);
//            result = windowHandler.initialize(new EasyParamWrapper(getWindow().getParameters()));
        }
        return result;
    }

    public void post(StreamElement se) {
        synchronized (this) {
            totalWindow.add(se);
        }
        windowHandler.postData(se);
        slidingHandler.postData(se);
    }

    public void dataProduced(StreamElement se) {
        post(se);
    }

    public Wrapper getWrapper() {
        return wrapper;
    }
}
