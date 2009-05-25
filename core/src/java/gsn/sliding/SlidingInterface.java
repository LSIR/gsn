package gsn.sliding;

import gsn.beans.StreamElement;
import gsn.utils.EasyParamWrapper;

public interface SlidingInterface {

    public boolean initialize(EasyParamWrapper easyParamWrapper, SlidingListener listener);

    public void postData(StreamElement se);

    public void reset(); //Sliding manager should reinitialize itself after this call.

}
