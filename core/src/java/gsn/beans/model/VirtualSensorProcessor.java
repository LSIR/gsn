package gsn.beans.model;

import javax.persistence.*;
import java.util.List;

@Entity
public class VirtualSensorProcessor extends NameDescriptionClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private VirtualSensorProcessorModel model;

    @OneToMany
    private List<Parameter> parameters;

    @OneToOne
    private VirtualSensor virtualSensor;

    public Long getId() {
        return id;
    }

    public VirtualSensorProcessorModel getModel() {
        return model;
    }

    public void setModel(VirtualSensorProcessorModel model) {
        this.model = model;
    }

    public VirtualSensor getVirtualSensor() {
        return virtualSensor;
    }

    public void setVirtualSensor(VirtualSensor virtualSensor) {
        this.virtualSensor = virtualSensor;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof VirtualSensorProcessor)) return false;

        VirtualSensorProcessor that = (VirtualSensorProcessor) other;

        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) return false;
        if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) return false;
        if (getModel() != null ? !getModel().equals(that.getModel()) : that.getModel() != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        result = 31 * result + (getModel() != null ? getModel().hashCode() : 0);
        return result;
    }
}
