package gsn.tests;

import gsn.beans.Operator;
import gsn.beans.StreamElement;
import gsn.beans.DataField;
import gsn.channels.DataChannel;
import gsn2.conf.OperatorConfig;

import java.util.List;

public class MockProcessingClass implements Operator {
	private OperatorConfig conf;
	private DataChannel outputChannel;

	public MockProcessingClass(OperatorConfig conf,DataChannel outputChannel ) {
		this.outputChannel = outputChannel;
		this.conf = conf;
	}

	public void process(String inputStreamName,			StreamElement streamElement) {
		process(inputStreamName, streamElement);
	}


	public void dispose() {
		
	}

	public void process(String name, List<StreamElement> window) {
		
	}

  public DataField[] getStructure() {
    return new DataField[0];  
  }

  public void start() {
		
	}

	public void stop() {
		
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
	    
	    retValue = "MockProcessingClass ( "
	        + super.toString() + TAB
	        + "conf = " + this.conf + TAB
	        + "outputChannel = " + this.outputChannel + TAB
	        + " )";
	
	    return retValue;
	}
	
}
