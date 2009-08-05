package gsn;

import gsn.beans.StreamElement;
import gsn.beans.VSFile;

public interface VirtualSensorDataListener {
	public void consume(StreamElement se,VSFile config);
}
