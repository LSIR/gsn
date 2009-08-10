package gsn2.window;

import gsn.beans.StreamElement;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class TimeBasedWindow implements WindowHandler {
	private int size;
    private LinkedList<StreamElement> items = new LinkedList<StreamElement>();

    public TimeBasedWindow(int timePeriodInMSec) {
        this.size = timePeriodInMSec;
    }

    public List<StreamElement> getTotalContent() {
        return items;
    }

    public void postData(StreamElement se) {
        items.addFirst(se);
    }

    public void reset() {
        items.clear();
    }

    public List<StreamElement> nextWindow(long timestamp) {
        List<StreamElement> toReturn = checkNextWindow(timestamp);

        for (ListIterator<StreamElement> iter = items.listIterator(); iter.hasNext();) {
            if (iter.next().getTimeInMillis() <= timestamp - size) {
                iter.remove();
            }
        }
        
        return toReturn;
    }

    public List<StreamElement> checkNextWindow(long timestamp) {
        LinkedList<StreamElement> toReturn = new LinkedList<StreamElement>();
        for (StreamElement se : items) {
            if (se.getTimeInMillis() <= timestamp && se.getTimeInMillis() > timestamp - size) {
                toReturn.add(se);
            }
        }
        return toReturn;
    }
}
