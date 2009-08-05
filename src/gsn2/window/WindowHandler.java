package gsn2.window;

import gsn.beans.StreamElement;

import java.util.List;

public interface WindowHandler {
	/**
	 * 
	 * @return returns all the un processes data items.
	 */
	public List<StreamElement> getTotalContent(); 
	/**
	 * Add data to the bottom of the window
	 * @param se
	 */
	public void postData(StreamElement se); 
	/**
	 * removes everything from the current window and resets the internal state.
	 */
	public void reset(); 
	/**
	 * consumes the window and moves next.
	 * @param timestamp
	 * @return
	 */
	public List<StreamElement> nextWindow(long timestamp); 
	/**
	 * doesn't consume
	 * @param timestamp
	 * @return
	 */
	public List<StreamElement> checkNextWindow(long timestamp); 

}
