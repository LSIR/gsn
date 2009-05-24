package gsn.sliding;

import java.util.ArrayList;

public class SlidingHandler {

    private ArrayList<SlidingListener> slidingListeners = new ArrayList<SlidingListener>();
    
    public void addSlidingListener(SlidingListener listener){
        if (!slidingListeners.contains(listener))
            slidingListeners.add(listener);
    }

    public void removeSlidingListener(SlidingListener listener){
        slidingListeners.remove(listener);
    }
}
