package gsn.beans.model;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class DataNode extends NameDescriptionClass implements DataNodeInterface {

    @OneToMany(mappedBy = "consumer")
    @Cascade({org.hibernate.annotations.CascadeType.ALL, CascadeType.DELETE_ORPHAN})
    private List<DataChannel> inChannels = new ArrayList<DataChannel>();

    @OneToMany(mappedBy = "producer")
    @Cascade({org.hibernate.annotations.CascadeType.ALL, CascadeType.DELETE_ORPHAN})
    private List<DataChannel> outChannels = new ArrayList<DataChannel>();

    @ManyToOne(optional = true)
    private VirtualSensor virtualSensor;

    public List<DataChannel> getInChannels() {
        return inChannels;
    }

    public void setInChannels(List<DataChannel> inChannels) {
        this.inChannels = inChannels;
    }

    public List<DataChannel> getOutChannels() {
        return outChannels;
    }

    public void setOutChannels(List<DataChannel> outChannels) {
        this.outChannels = outChannels;
    }

    public VirtualSensor getVirtualSensor() {
        return virtualSensor;
    }

    public void setVirtualSensor(VirtualSensor virtualSensor) {
        this.virtualSensor = virtualSensor;
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof DataNodeInterface)) return false;

        DataNode that = (DataNode) other;

        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) return false;
        if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) return false;
        if (getInChannels() != null ? !getInChannels().equals(that.getInChannels()) : that.getInChannels() != null)
            return false;
        if (getOutChannels() != null ? !getOutChannels().equals(that.getOutChannels()) : that.getOutChannels() != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        result = 31 * result + (getInChannels() != null ? getInChannels().hashCode() : 0);
        result = 31 * result + (getOutChannels() != null ? getOutChannels().hashCode() : 0);
        return result;
    }

}

