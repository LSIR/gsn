package gsn2.window;

import gsn.beans.StreamElement;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class CountBasedWindow implements WindowHandler {
	private int size;
	private LinkedList<StreamElement> items = new LinkedList<StreamElement>();
	private long minimumRequiredTimestamp;

	public CountBasedWindow(int size){
		this.size = size;
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
			if (iter.next().getTimed() < minimumRequiredTimestamp) {
				iter.remove();
			}
		}
		return toReturn;
	}

	public List<StreamElement> checkNextWindow(long timestamp) {
		LinkedList<StreamElement> toReturn = new LinkedList<StreamElement>();

		for (int i = 0; i < items.size(); i++) {
			StreamElement se = items.get(i);
			if (se.getTimed() <= timestamp) {
				toReturn.add(se);
				minimumRequiredTimestamp = se.getTimed();
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
