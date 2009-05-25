package gsn.beans;

public class QueueFactory {

    public static BetterQueue<DataWindow> createBetterQueue() {
        return new BetterQueue<DataWindow>();
    }

}
