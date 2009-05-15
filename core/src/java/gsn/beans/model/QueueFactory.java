package gsn.beans.model;

import gsn.beans.BetterQueue;

import java.util.LinkedList;
import java.util.Queue;

public class QueueFactory {

    public static BetterQueue<Window> createBetterQueue(){
        return new BetterQueue();
    }

}
