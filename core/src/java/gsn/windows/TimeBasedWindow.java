package gsn.windows;

import gsn.utils.EasyParamWrapper;
import gsn.beans.StreamElement;

import java.util.ArrayList;

public class TimeBasedWindow implements WindowInterface{
    public boolean initialize(EasyParamWrapper easyParamWrapper) {
        return false;
    }

    public ArrayList<StreamElement> getTotalContent() {
        return null; 
    }

    public void postData(StreamElement se) {

    }

    public void reset() {

    }

    public ArrayList<StreamElement> nextWindow() {
        return null;
    }

    public ArrayList<StreamElement> checkNextWindow() {
        return null;
    }
}
