package gsn.beans;

import gsn.beans.BetterQueue;
import gsn.beans.model.Window;

public class QueueFactory {

    public static BetterQueue<Window> createBetterQueue() {
        return new BetterQueue<Window>();
    }

}
