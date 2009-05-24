package gsn.sliding;

import gsn.utils.EasyParamWrapper;
import gsn.beans.StreamElement;

public interface SlidingInterface {

    public boolean initialize(EasyParamWrapper easyParamWrapper,SlidingListener notify);

    public void postData(StreamElement se);

    public void reset(); //Sliding manager should reinitialize itself after this call.

}
