package gsn.http.rest;

import gsn.beans.DataField;
import org.apache.log4j.Logger;


public class DirectPushRemoteWrapper extends PushRemoteWrapper {

    private final transient Logger logger = Logger.getLogger(DirectPushRemoteWrapper.class);

    private double uid = -1; //statically set from the parameters

    @Override
    public boolean initialize() {
        try {
            uid = Double.parseDouble(getActiveAddressBean().getPredicateValueWithException(PushDelivery.NOTIFICATION_ID_KEY));
            structure = registerAndGetStructure();
            NotificationRegistry.getInstance().addNotification(uid, this);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            NotificationRegistry.getInstance().removeNotification(uid);
            return false;
        }
        return true;
    }

    @Override
    public String getWrapperName() {
        return "Direct Push-Remote Wrapper";
    }

    @Override
    public DataField[] registerAndGetStructure() throws RuntimeException{ 
    	logger.debug("Received Stream Structure at the push wrapper.");
    	DataField[] s = getActiveAddressBean().getOutputStructure();
    	if (s == null){throw new RuntimeException("Direct Push wrapper has an undefined output structure.");}
    	return s;
    }
    
    /**
     * Passive push. We don't actively query the node for new elements.
     */
    @Override
    public void run() {}

}
