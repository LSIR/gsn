package gsn.beans.windowing;

import gsn.beans.BetterQueue;
import gsn.beans.decorators.ThreadDataNodeDecorator;
import gsn.beans.model.Sliding;
import gsn.beans.model.Parameter;

import java.util.List;

public class CountBasedSlidingHandler extends SlidingHandler2 implements Runnable {
    private BetterQueue<Long> slidingQueue;
    private long nextTimestamp;
    private int counter;
    private int slideCount;

    public CountBasedSlidingHandler(ThreadDataNodeDecorator dec, Sliding sliding) {
        super(dec, sliding);
        List<Parameter> parameters = sliding.getParameters();
        for (Parameter parameter : parameters) {
            if("slide".equals(parameter.getModel().getName())){
                slideCount = Integer.parseInt(parameter.getValue());
                break;
            }
        }
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

            if(counter++ == slideCount){
                notify(nextTimestamp);
                counter = 0;
            }
        }
    }
}
