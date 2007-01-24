package gsn.wrappers;

import gsn.beans.DataField;
import gsn.beans.StreamElement;

public class MockWrapper extends AbstractWrapper {
	int threadCounter;
	
	private DataField[] outputFormat =new DataField[] {};

	public boolean initialize() {
		setName("TestWrapperMockObject-Thread" + (++threadCounter));
		return true;
	}

	public void run() {

	}

	public DataField[] getOutputFormat() {
		return outputFormat;
	}

	public boolean publishStreamElement(StreamElement se) {
		return postStreamElement(se);
	}

	public void finalize() {
		threadCounter--;
	}

	public String getWrapperName() {
		return "TestWrapperMock";
	}

	public void setOutputFormat(DataField[] outputFormat) {
		this.outputFormat = outputFormat;
	}
	
}
