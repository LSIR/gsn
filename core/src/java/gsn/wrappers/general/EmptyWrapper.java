package gsn.wrappers.general;

import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.wrappers.AbstractWrapper;
import org.apache.log4j.Logger;

public class EmptyWrapper extends AbstractWrapper {

    private final transient Logger logger = Logger.getLogger(EmptyWrapper.class);

    private int threadCounter = 0;

    private static DataField[] dataField;

    public boolean initialize() {
        AddressBean addressBean = getActiveAddressBean();
        dataField = new DataField[]{new DataField("DATA", "int", "incremental int")};
        return true;
    }

    public void run() {

    }

    public DataField[] getOutputFormat() {
        return dataField;
    }

    public String getWrapperName() {
        return "empty template";
    }

    public void finalize() {
        threadCounter--;
    }

}
