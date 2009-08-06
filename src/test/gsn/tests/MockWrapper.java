package gsn.tests;

import gsn.beans.DataField;
import gsn.beans.WrapperConfig;
import gsn.channels.DataChannel;
import gsn.wrappers.Wrapper;

public class MockWrapper implements Wrapper {
	
	private final WrapperConfig conf;

	private final DataChannel dataChannel;

  private DataField[] fields = new DataField[0];
	public MockWrapper(WrapperConfig config, DataChannel channel) {
		this.conf = config;
		this.dataChannel= channel;
	}

	public void start() {
		System.out.println("Wrapper1 Started");
		System.out.println(toString());
	}

	public void stop() {
		System.out.println("Wrapper1 Stopped");
	}

	public void dispose() {
		System.out.println("Wrapper1 Disposed");
	}


	public DataField[] getOutputFormat() {
		return fields;
	}

  public void setFields(DataField[] fields) {
    this.fields = fields;
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
	    
	    retValue = "MockWrapper ( "
	        + super.toString() + TAB
	        + "conf = " + this.conf + TAB
	        + "dataChannel = " + this.dataChannel + TAB
	        + " )";
	
	    return retValue;
	}

}
