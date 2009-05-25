package gsn.beans.windowing;

import gsn.beans.interfaces.SlideListener;
import gsn.beans.model.Sliding;

import java.util.ArrayList;
import java.util.List;

public abstract class SlidingHandler2 implements Runnable {
    private List<SlideListener> slideListeners;

    public SlidingHandler2(SlideListener slideListener, Sliding sliding) {
        slideListeners = new ArrayList<SlideListener>();
        slideListeners.add(slideListener);
    }

    public void addSlideListener(SlideListener listener) {
        slideListeners.add(listener);
    }

    public void removeSlideListener(SlideListener listener) {
        slideListeners.remove(listener);
    }

    public abstract void addNewElement(long timestamp);

    public void notify(long timestamp) {
        for (SlideListener listener : slideListeners) {
            listener.slided(timestamp);
        }
    }
}
