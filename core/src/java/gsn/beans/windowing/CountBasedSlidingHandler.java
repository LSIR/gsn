package gsn.beans.windowing;

import gsn.beans.BetterQueue;
import gsn.beans.interfaces.SlideListener;
import gsn.beans.model.Sliding;
import gsn.utils.EasyParamWrapper;

public class CountBasedSlidingHandler extends SlidingHandler2 {
    private BetterQueue<Long> slidingQueue;
    private int counter;
    private int slideCount;

    public CountBasedSlidingHandler(SlideListener listener, Sliding sliding) {
        super(listener, sliding);
        EasyParamWrapper easyParamWrapper = new EasyParamWrapper(sliding.getParameters());
        slideCount = easyParamWrapper.getPredicateValueAsInt("slide", 1);
        slidingQueue = new BetterQueue<Long>();
        counter = 0;
    }

    public void addNewElement(long timestamp) {
        synchronized (this) {
            slidingQueue.add(timestamp);
            notifyAll();
        }
    }

    public void run() {
        while (true) {
            long nextTimestamp;
            synchronized (this) {
                while (slidingQueue.isEmpty()) {
                    try {
                        wait();
                    } catch (InterruptedException e) {

                    }
                }
                System.out.println("QueueSize: " + slidingQueue.size());
                nextTimestamp = slidingQueue.dequeue();
            }

            if (++counter == slideCount) {
                notify(nextTimestamp);
                counter = 0;
            }
        }
    }
}
