package gsn.beans.model;

import javax.persistence.*;
import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "childType", discriminatorType = DiscriminatorType.STRING)
public abstract class DataNode extends NameDescriptionClass {

    @OneToMany(mappedBy = "parent")
    private List<DataNode> children;

    @ManyToOne
    private DataNode parent;

    @OneToOne(optional = false, mappedBy = "dataNode")
    private Window window;

    @OneToOne(optional = false, mappedBy = "dataNode")
    private Sliding sliding;

    @ManyToOne(optional = true)
    private VirtualSensor virtualSensor;

    public DataNode getParent() {
        return parent;
    }

    public void setParent(DataNode parent) {
        this.parent = parent;
    }

    public Window getWindow() {
        return window;
    }

    public void setWindow(Window window) {
        this.window = window;
    }

    public Sliding getSliding() {
        return sliding;
    }

    public void setSliding(Sliding sliding) {
        this.sliding = sliding;
    }

    public List<DataNode> getChildren() {
        return children;
    }

    public void setChildren(List<DataNode> children) {
        this.children = children;
    }

    public VirtualSensor getVirtualSensor() {
        return virtualSensor;
    }

    public void setVirtualSensor(VirtualSensor virtualSensor) {
        this.virtualSensor = virtualSensor;
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof DataNode)) return false;

        DataNode that = (DataNode) other;

        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) return false;
        if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) return false;
        if (getWindow() != null ? !getWindow().equals(that.getWindow()) : that.getWindow() != null)
            return false;
        if (getSliding() != null ? !getSliding().equals(that.getSliding()) : that.getSliding() != null)
            return false;
        if (getParent() != null ? !getParent().equals(that.getParent()) : that.getParent() != null) return false;
        if (getChildren() != null ? !getChildren().equals(that.getChildren()) : that.getChildren() != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        result = 31 * result + (getWindow() != null ? getWindow().hashCode() : 0);
        result = 31 * result + (getSliding() != null ? getSliding().hashCode() : 0);
        result = 31 * result + (getParent() != null ? getParent().hashCode() : 0);
        result = 31 * result + (getChildren() != null ? getChildren().hashCode() : 0);
        return result;
    }
}
