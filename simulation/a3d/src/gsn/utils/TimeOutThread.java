package gsn.utils;

import org.apache.log4j.Logger;

/*
* this thread launches a timer and waits until time elapses and then sets a variable to true
* unless it was stopped in between
* */
public class TimeOutThread extends Thread {

    private boolean timeout;
    private static int threadCounter = 0;
    private long count=0;
    private long delay=0;

    private boolean running = true;

    private static final transient Logger logger = Logger.getLogger(TimeOutThread.class);

    public TimeOutThread(long delay) {

        this.setName("TimeOut-Thread" + (++threadCounter));
        this.count = 0;
        this.delay = delay;
        this.timeout = false;
        logger.warn("TimeOut-Thread started");

    }

    public void stopMe() {
        logger.warn("Requested to stop.");
        running = false;
    }

    public boolean isTimedOut() {
        return timeout;
    }

    public void run() {
        while ((count < delay) && (running)) {
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                logger.warn(e.getMessage(), e);
            }
            count++;
        }

        if (count>=delay) { // timeout was exceeded
            timeout = true;
            logger.warn("timeout was exceeded: count="+count+" delay="+delay);
        }
        else { // means that running was set to false
            timeout = false;
            logger.warn("thread was stoppped by setting calling stopMe(). running="+running);
        }
        logger.warn("Thread finished.");
        // stops
    }

}