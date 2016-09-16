package ch.epfl.gsn.vsensor;

import ch.epfl.gsn.beans.DataField;
import ch.epfl.gsn.beans.DataTypes;
import ch.epfl.gsn.beans.StreamElement;
import ch.epfl.gsn.vsensor.AbstractVirtualSensor;

public class CounterVS extends AbstractVirtualSensor {
	
	private long count = 0;

	@Override
	public boolean initialize() {
		return true;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void dataAvailable(String inputStreamName,
			StreamElement streamElement) {
		count += 1;
		dataProduced(new StreamElement(new DataField[]{new DataField("Counter",DataTypes.BIGINT)}, new Long[]{count}));

	}

}
