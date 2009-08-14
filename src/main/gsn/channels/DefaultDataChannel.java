package gsn.channels;

import gsn.beans.Operator;
import gsn.beans.StreamElement;
import gsn.channels.ChannelConfig;
import gsn2.sliding.SlidingHandler;
import gsn2.sliding.SlidingListener;
import gsn2.window.SlidingWindowFactory;
import gsn2.window.WindowHandler;

public class DefaultDataChannel implements DataChannel, SlidingListener {
	
	private String name;
	
	private Operator operator;
	
	private SlidingHandler sliding;
	
	private WindowHandler window;
	
	public DefaultDataChannel(Operator operator,ChannelConfig config) {
		sliding = SlidingWindowFactory.getSliding(config.getSlidingValue());
		window = SlidingWindowFactory.getWindow(config.getWindowValue());
		this.name = config.getName();
	}

	public void write(StreamElement se) {
		window.postData(se);
		sliding.postData(se);
	}

	public void slide(long timestamp) {
		operator.process(name, window.nextWindow(timestamp));
	}

	/**
	 * Constructs a <code>String</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString()
	{
	    final String TAB = "    ";
	    
	    String retValue = "";
	    
	    retValue = "DefaultDataChannel ( "
	        + super.toString() + TAB
	        + "name = " + this.name + TAB
	        + "operator = " + this.operator + TAB
	        + "sliding = " + this.sliding + TAB
	        + "window = " + this.window + TAB
	        + " )";
	
	    return retValue;
	}
	
	

}
