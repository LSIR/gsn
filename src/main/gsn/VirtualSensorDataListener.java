package gsn;

import gsn.beans.StreamElement;
import gsn.core.OperatorConfig;

public interface VirtualSensorDataListener {
	public void consume(StreamElement se, OperatorConfig config);
}
