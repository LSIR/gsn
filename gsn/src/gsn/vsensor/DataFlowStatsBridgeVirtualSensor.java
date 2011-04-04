package gsn.vsensor;

import gsn.beans.StreamElement;

public class DataFlowStatsBridgeVirtualSensor extends AbstractVirtualSensor {

	@Override
	public boolean initialize() {
		return true;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void dataAvailable(String inputStreamName, StreamElement streamElement) { }
	
	@Override
	public void dataAvail ( String inputStreamName , StreamElement streamElement ) {
		streamElement.doNotProduceStatistics();
		dataProduced( streamElement );
	}
}
