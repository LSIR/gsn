package ch.epfl.gsn.vsensor;

import java.io.Serializable;
import javax.naming.OperationNotSupportedException;

public class OpensenseBridgeVirtualSensor extends BridgeVirtualSensor {
	
	@Override
	public boolean dataFromWeb ( String action,String[] paramNames, Serializable[] paramValues ) {
			try {
				return getVirtualSensorConfiguration().getInputStream("connector").getSource("connector").getWrapper().sendToWrapper(action, paramNames, paramValues);
			} catch (OperationNotSupportedException e) {
				return false;
			}
	}
}
