package gsn;

import gsn.sliding.SlidingListener;

/**
 * This class is used to test the sliding algorithms. To make sure the sliding occurs. This
 * class also counts how many times sliding has occured.
 */
public class MockSlidingListener implements SlidingListener {
    private int slidingCount = 0;

    private boolean slided;

    public void slide(long timestamp) {
        slidingCount++;
        slided = true;
    }

    public int getSlidingCount() {
        return slidingCount;
    }

    public boolean isSlided() {
        return slided;
    }

    public void resetSlided() {
        slided = false;
    }
}
