package gsn2.window;

import gsn.beans.StreamElement;

import java.util.LinkedList;
import java.util.List;

public class UnboundedWindow implements WindowHandler {

	private LinkedList<StreamElement> items = new LinkedList<StreamElement>();
	
	public List<StreamElement> checkNextWindow(long timestamp) {
		return items;
	}

	public List<StreamElement> getTotalContent() {
		return items;
	}

	public List<StreamElement> nextWindow(long timestamp) {
		LinkedList<StreamElement> toReturn = items;
		items = new LinkedList<StreamElement>();
		return toReturn;
	}

	public void postData(StreamElement se) {
		items.addFirst(se);
	}

	public void reset() {
		items.clear();
		
	}

}
