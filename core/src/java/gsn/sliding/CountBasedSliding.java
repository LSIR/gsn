package gsn.sliding;

import gsn.utils.EasyParamWrapper;
import gsn.beans.StreamElement;

public class CountBasedSliding implements SlidingInterface{
    private int size ;

    private SlidingListener notify;

    private int tempCounter = 0;

    public boolean initialize(EasyParamWrapper easyParamWrapper, SlidingListener notify) {
        this.size = easyParamWrapper.getPredicateValueAsIntWithException("size");
        this.notify = notify;
        return true;
    }

    public void postData(StreamElement se) {
        tempCounter++;
        if (tempCounter%size == 0)
            notify.slide();
    }

    public void reset() {
        tempCounter=0;
    }
}
