package gsn.windows;

import gsn.utils.EasyParamWrapper;
import gsn.beans.StreamElement;
import java.util.List;

public interface WindowInterface {
    
    public boolean initialize(EasyParamWrapper easyParamWrapper) ;

    public List<StreamElement> getTotalContent(); // returns all the un processes data items.

    public void postData(StreamElement se); // add data to the buttom of the window

    public void reset(); //removes everything from the current window

    public List<StreamElement> nextWindow(); //consumes the window and moves next.
    
    public List<StreamElement> checkNextWindow(); // doesn't consume
}
