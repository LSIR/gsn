package gsn.beans.windowing;

import gsn.beans.model.Sliding;
import gsn.beans.decorators.ThreadDataNodeDecorator;

public abstract class SlidingHandler2 {
    private Sliding sliding;
    private ThreadDataNodeDecorator threadDataNodeDec;

    public SlidingHandler2(ThreadDataNodeDecorator threadDataNodeDec, Sliding sliding) {
        this.threadDataNodeDec = threadDataNodeDec;
        this.sliding = sliding;
    }

    public abstract void addNewElement(long timestamp);

    public void notify(long timestamp){
        threadDataNodeDec.slide(timestamp);
    }
}
