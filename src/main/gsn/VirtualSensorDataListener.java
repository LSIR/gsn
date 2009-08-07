package gsn;

import gsn.beans.StreamElement;
import gsn2.conf.OperatorConfig;

public interface VirtualSensorDataListener {
	public void consume(StreamElement se, OperatorConfig config);
}
