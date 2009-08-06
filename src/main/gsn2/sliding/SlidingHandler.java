package gsn2.sliding;

import gsn.beans.StreamElement;

public interface SlidingHandler {

	public void postData(StreamElement se);
	/**
	 * Sliding manager should reinitialize itself after this call.
	 */
	public void reset();
	public void addListener(SlidingListener listener);
	public void removeListener(SlidingListener listener); 

}
