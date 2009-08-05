package gsn.tests;

import gsn.beans.DataField;
import gsn.beans.WrapperConfig;
import gsn.channels.DataChannel;
import gsn.wrappers.Wrapper;

public class MockWrapper2 implements Wrapper {
	
	private final WrapperConfig conf;

	private final DataChannel dataChannel;
	
	public MockWrapper2(WrapperConfig config, DataChannel channel) {
		this.conf = config;
		this.dataChannel= channel;
	}

	public void start() {
		System.out.println("Wrapper2 Start");
		System.out.println(toString());
	}

	public void stop() {
		System.out.println("Wrapper2 Stopped");
	}

	public void dispose() {
		System.out.println("Wrapper2 Disposed");
	}

	public DataField[] getOutputFormat() {
		return new DataField[] {};
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
	    
	    retValue = "MockWrapper2 ( "
	        + super.toString() + TAB
	        + "conf = " + this.conf + TAB
	        + "dataChannel = " + this.dataChannel + TAB
	        + " )";
	
	    return retValue;
	}
	

}

