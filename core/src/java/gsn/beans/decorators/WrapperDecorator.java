package gsn.beans.decorators;

import gsn.beans.BetterQueue;
import gsn.beans.StreamElement;
import gsn.beans.model.Parameter;

import java.util.ArrayList;

public class WrapperDecorator extends QueryDecorator {

    ArrayList<StreamElement> totalWindow = new ArrayList<StreamElement>();
    BetterQueue slidingQueue = new BetterQueue(); // get Queue from Slider.

    public WrapperDecorator(QueueDataNodeDecorator node) {
        super(node);
    }

    public boolean initialize(Parameter parameters){
        return false;
    }

    public void post(StreamElement se){
        totalWindow.add(se);
        slidingQueue.add(se.getTimeStamp());
    }

}
