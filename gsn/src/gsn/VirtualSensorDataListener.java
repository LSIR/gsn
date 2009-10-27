package gsn;

import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;

public interface VirtualSensorDataListener {
	public void consume(StreamElement se,VSensorConfig config);
}
