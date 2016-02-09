package gsn.vsensor;

import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;

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
