package gsn.beans;

import java.util.Queue;

public class BetterQueue {
    private Queue queue;
    private int counter;
    private long lastInsert = -1;
    private long firstInsert = -1;

    public boolean insert(Object o) {
        counter++;
        lastInsert = System.currentTimeMillis();
        if (firstInsert == -1)
            firstInsert = lastInsert;
        return queue.add(o);
    }

    public void resetCounter() {
        counter = 0;
    }

    public Queue getQueue() {
        return queue;
    }

    public long getLastInsert() {
        return lastInsert;
    }

    public long getFirstInsert() {
        return firstInsert;
    }
}
