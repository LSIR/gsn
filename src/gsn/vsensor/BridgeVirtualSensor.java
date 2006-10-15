package gsn.vsensor;

import gsn.beans.StreamElement;

import java.util.HashMap;

import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class BridgeVirtualSensor extends AbstractVirtualSensor {

    private static final transient Logger logger = Logger
	    .getLogger(BridgeVirtualSensor.class);

    public boolean initialize(HashMap map) {
	return super.initialize(map);
    }

    public void dataAvailable(String inputStreamName, StreamElement data) {
	dataProduced(data);
	if (logger.isDebugEnabled())
	    logger.debug("Data received under the name: " + inputStreamName);
    }

}
