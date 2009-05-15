package gsn.beans.model;

import java.util.List;

public interface DataNodeInterface {
    List<DataNodeInterface> getParents();

    void setParents(List<DataNodeInterface> parents);

    Window getWindow();

    void setWindow(Window window);

    Sliding getSliding();

    void setSliding(Sliding sliding);

    List<DataNodeInterface> getChildren();

    void setChildren(List<DataNodeInterface> children);

    VirtualSensor getVirtualSensor();

    void setVirtualSensor(VirtualSensor virtualSensor);

}
