package gsn.beans.model;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.List;

@Entity
@DiscriminatorValue("IS")
public class InputStream extends DataNode {

    @OneToMany(mappedBy = "parent")
    private List<DataNode> children;

    @ManyToOne(optional = true)
    private VirtualSensor virtualSensor;

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
        if (other == null || !(other instanceof InputStream)) return false;

        InputStream that = (InputStream) other;

        if (!super.equals(other)) return false;
        if (getChildren() != null ? !getChildren().equals(that.getChildren()) : that.getChildren() != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getChildren() != null ? getChildren().hashCode() : 0);
        return result;
    }
}