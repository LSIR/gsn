package gsn2.sliding;

import gsn.beans.StreamElement;

import java.util.ArrayList;

public class CountBasedSliding implements SlidingHandler{
	private int size;
	private int tempCounter = 0;

	public CountBasedSliding(int size) {
		this.size = size;
	}

	public void postData(StreamElement se) {
		tempCounter++;
		if (tempCounter % size == 0) 
			for (SlidingListener listener:listeners)
				listener.slide(se.getTimeInMillis());
	}

	public void reset() {
		tempCounter = 0;
	}

	private ArrayList<SlidingListener> listeners = new ArrayList<SlidingListener>();

	public void addListener(SlidingListener listener) {
		if (listeners.contains(listener))
			return;
		listeners.add(listener);
	}

	public void removeListener(SlidingListener listener) {
		listeners.remove(listener);
	}
	

}
