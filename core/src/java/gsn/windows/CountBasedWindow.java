package gsn.windows;

import gsn.utils.EasyParamWrapper;
import gsn.beans.StreamElement;

import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;

public class CountBasedWindow implements  WindowInterface{

    private int size;
    private LinkedList<StreamElement> items = new LinkedList<StreamElement>();
    public boolean initialize(EasyParamWrapper easyParamWrapper) {
        size = easyParamWrapper.getPredicateValueAsIntWithException("size");
        return true;
    }

    public List<StreamElement> getTotalContent() {
        return items;
    }

    public void postData(StreamElement se) {
        items.add(se);

    }

    public void reset() {
        items.clear();
    }

    public List<StreamElement> nextWindow() {
        List<StreamElement> toReturn =checkNextWindow();
        Iterator<StreamElement> it = toReturn.iterator();
        while (it.hasNext() && it.next()!=null)
            items.poll(); //retirives and removes the first element of the items.
        return toReturn;
    }

    public List<StreamElement> checkNextWindow() {
        LinkedList<StreamElement> toReturn = new LinkedList();
        Iterator<StreamElement> it = items.iterator();
        for (int i=0;i<size;i++){
            if (it.hasNext())
                toReturn.add(it.next());
            else
                toReturn.add(null);
        }
        return toReturn;
    }
}
