package gsn.http.rest;

import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.wrappers.AbstractWrapper;

import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;

public class DirectPushRemoteWrapper extends AbstractWrapper implements IPushWrapper {

    private final transient Logger logger = Logger.getLogger(DirectPushRemoteWrapper.class);

    private final XStream XSTREAM = StreamElement4Rest.getXstream();

    private double uid = -1; //statically set from the parameters


    private DataField[] structure;


    public void dispose() {
        NotificationRegistry.getInstance().removeNotification(uid);
    }

    public boolean initialize() {

        try {
            uid = Double.parseDouble(getActiveAddressBean().getPredicateValueWithException(PushDelivery.NOTIFICATION_ID_KEY));
            NotificationRegistry.getInstance().addNotification(uid, this);
            DataField df[] = new DataField[10];
            df[0] = new DataField("ozone1", "int");
            df[1] = new DataField("ozone2", "int");
            df[2] = new DataField("resistance1", "int");
            df[3] = new DataField("resistance2", "int");
            df[4] = new DataField("humidity", "int");
            df[5] = new DataField("temperature", "double");
            df[6] = new DataField("ozoneCalibrated", "double");
            df[7] = new DataField("latitude", "double");
            df[8] = new DataField("longitude", "double");
            df[9] = new DataField("speed", "double");
            structure = df;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            NotificationRegistry.getInstance().removeNotification(uid);
            return false;
        }


        return true;
    }

    public DataField[] getOutputFormat() {
        return structure;
    }

    public String getWrapperName() {
        return "Direct Push-Remote Wrapper";
    }

    public boolean registerAndSetStructure(String struct) { 
    	logger.debug(new StringBuilder().append("Received Stream Structure at the push wrapper."));
    	DataField[] s = null;
    	try{
    		s = (DataField[]) XSTREAM.fromXML(struct);
    	}
        catch (XStreamException e){
            logger.warn(e.getMessage(), e);
            return false;
        }
    	if (s != null){
    		structure = s;
    		return true;
    	}else{
    		return false;
    	}
    }

    public boolean manualDataInsertion(String Xstream4Rest) {
        logger.debug(new StringBuilder().append("Received Stream Element at the push wrapper."));
        if (structure == null) return false;
        
        try {        
            StreamElement4Rest se = (StreamElement4Rest) XSTREAM.fromXML(Xstream4Rest);
            StreamElement streamElement = se.toStreamElement();


            // If the stream element is out of order, we accept the stream element and wait for the next (update the last received time and return true)
            if (isOutOfOrder(streamElement)) {
                return true;
            }
            // Otherwise, we first try to insert the stream element.
            // If the stream element was inserted succesfully, we wait for the next,
            // otherwise, we return false.
            boolean status = postStreamElement(streamElement);
            return status;
        }
        catch (SQLException e) {
            logger.warn(e.getMessage(), e);
            return false;
        }
        catch (XStreamException e){
        	logger.warn(e.getMessage(), e);
        	return false;
        }
    }
}
