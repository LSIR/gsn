package gsn.windows;

import gsn.beans.StreamElement;
import gsn.utils.EasyParamWrapper;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class CountBasedWindow implements WindowInterface {

    private int size;
    private LinkedList<StreamElement> items = new LinkedList<StreamElement>();
    private long minimumRequiredTimestamp;

    public boolean initialize(EasyParamWrapper easyParamWrapper) {
        size = easyParamWrapper.getPredicateValueAsIntWithException("size");
        return true;
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
        Iterator<StreamElement> it = toReturn.iterator();

        for (ListIterator<StreamElement> iter = items.listIterator(); iter.hasNext();) {
            if (iter.next().getTimeStamp() < minimumRequiredTimestamp) {
                iter.remove();
            }
        }
        return toReturn;
    }

    public List<StreamElement> checkNextWindow(long timestamp) {
        LinkedList<StreamElement> toReturn = new LinkedList<StreamElement>();

        for (int i = 0; i < items.size(); i++) {
            StreamElement se = items.get(i);
            if (se.getTimeStamp() <= timestamp) {
                toReturn.add(se);
                minimumRequiredTimestamp = se.getTimeStamp();
            }
            if (toReturn.size() == size) break;
        }

        int emptySlots = size - toReturn.size();
        for (int i = 0; i < emptySlots; i++) {
            toReturn.add(null);
        }
        return toReturn;
    }
}
