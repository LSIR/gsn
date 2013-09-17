package gsn.wrappers.ibutton;

import gsn.beans.DataField;
import gsn.beans.InputInfo;
import gsn.wrappers.AbstractWrapper;

import java.io.Serializable;

import javax.naming.OperationNotSupportedException;

import org.apache.log4j.Logger;


/**
 * This wrapper processed iButton data that is uploaded via HTTP POST requests.
 * 
 * @author Matthias Keller
 */
public class IButtonHttpUploadWrapper extends AbstractWrapper {
    
    private static DataField[] dataField = {
            new DataField("TIMESTAMP", "BIGINT"),
            new DataField("PAYLOAD_TYPE", "SMALLINT"),
            new DataField("PAYLOAD", "BINARY")};

    private final transient Logger logger = Logger.getLogger( IButtonHttpUploadWrapper.class );

    @Override
    public DataField[] getOutputFormat() {
        return dataField;
    }
    
    @Override
    public void dispose() { }

    @Override
    public String getWrapperName() {
        return "IButtonHttpUploadWrapper";
    }
    
    @Override
    public boolean initialize() {
        int size = -1;
        for (int i=0; i<getActiveAddressBean().getVirtualSensorConfig().getWebinput().length; i++) {
            if (getActiveAddressBean().getVirtualSensorConfig().getWebinput()[i].getName().equalsIgnoreCase("upload")) {
                size = getActiveAddressBean().getVirtualSensorConfig().getWebinput()[i].getParameters().length;
                break;
            }
        }
        if (size == -1) {
            logger.error("upload name files has to be existing in the web-input section");
            return false;
        }
        
        return true;
    }
    
    @Override
    public InputInfo sendToWrapper (String action, String[] paramNames, Serializable[] paramValues) throws OperationNotSupportedException {
        if(action.compareToIgnoreCase("upload") == 0) {
            try {
                long timestamp = System.currentTimeMillis();
                String payloadTypeStr = null;
                String payload = null;
                
                for( int i=0; i<paramNames.length; i++ ) {
                    String tmp = paramNames[i];
                    if(tmp.endsWith("payload_type")) {
                        payloadTypeStr = (String)paramValues[i];
                    } else if(tmp.endsWith("payload")) {
                        payload = (String)paramValues[i];
                    } else {
                        logger.warn("unknown upload field: " + tmp + " -> skip it");
                    }
                }
             
                if (payloadTypeStr.equals("")) {
                    logger.error("payload_type argument has to be an integer between 0 and 2");
                    return new InputInfo(getActiveAddressBean().toString(), "payload_type argument has to be an integer between 0 and 2", false);
                }
                if (payload.equals("")) {
                    logger.error("payload argument cannot be empty");
                    return new InputInfo(getActiveAddressBean().toString(), "payload argument cannot be empty", false);
                }
                
                Short payloadType;
                try {
                    payloadType = Short.parseShort(payloadTypeStr);
                } catch(Exception e) {
                    logger.error("Could not interpret payload_type argument (" + payloadTypeStr +") as short");
                    return new InputInfo(getActiveAddressBean().toString(), "Could not interprete payload_type argument (" + payloadTypeStr +") as short", false);
                }
                if (payloadType < 0 || payloadType > 2) {
                    logger.error("payload_type argument has to be between 0 and 2");
                    return new InputInfo(getActiveAddressBean().toString(), "payload_type argument has to be between 0 and 2", false);
                }
             
                Serializable[] output = new Serializable[3];
                output[0] = timestamp;
                output[1] = payloadType;
                output[2] = payload.getBytes("UTF-8");
                
                if (postStreamElement(output)) {
                    return new InputInfo(getActiveAddressBean().toString(), "file successfully uploaded", true);
                } else {
                    return new InputInfo(getActiveAddressBean().toString(), "file could not be uploaded", false);
                }
            } catch(Exception e) {
                logger.error(e.getMessage(), e);
                return new InputInfo(getActiveAddressBean().toString(), e.getMessage(), false);
            }
        }
        else {
            logger.warn("action >" + action + "< not supported");
            return new InputInfo(getActiveAddressBean().toString(), "action >" + action + "< not supported", false);
        }
    }

}
