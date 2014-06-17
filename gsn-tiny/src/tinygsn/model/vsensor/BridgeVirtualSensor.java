package tinygsn.model.vsensor;

import tinygsn.beans.StreamElement;



public class BridgeVirtualSensor extends AbstractVirtualSensor {

	@Override
	public boolean initialize() {
		return false;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void dataAvailable(String inputStreamName, StreamElement streamElement) {
		dataProduced(streamElement);
	}

}
