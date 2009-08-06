package gsn2.conf;

import gsn.beans.WrapperConfig;

import java.io.Serializable;

public class ChannelConfig implements Serializable {
	
	private String windowValue;
	private String slidingValue;
	private String name;
	private WrapperConfig sourceConfig;
	
	public ChannelConfig(String name,String slidingValue,String windowValue) {
		this.slidingValue = slidingValue;
		this.windowValue = windowValue;
		this.name = name;
	}

	public String getWindowValue() {
		return windowValue;
	}

	public String getSlidingValue() {
		return slidingValue;
	}

	public String getName() {
		return name;
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
	    
	    retValue = "ChannelConfig ( "
	        + super.toString() + TAB
	        + "windowValue = " + this.windowValue + TAB
	        + "slidingValue = " + this.slidingValue + TAB
	        + "name = " + this.name + TAB
	        + " )";
	
	    return retValue;
	}

	
	
}
