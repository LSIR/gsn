package gsn.beans.model;

import java.util.List;

public interface DataNodeInterface {

    VirtualSensor getVirtualSensor();

    void setVirtualSensor(VirtualSensor virtualSensor);

    List<DataChannel> getInChannels();

    void setInChannels(List<DataChannel> inChannels);

    List<DataChannel> getOutChannels();

    void setOutChannels(List<DataChannel> outChannels);

}
