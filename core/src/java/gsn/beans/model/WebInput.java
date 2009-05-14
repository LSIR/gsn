package gsn.beans.model;

import javax.persistence.*;
import java.util.List;

@Entity
public class WebInput extends NameDescriptionClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "webInput")
    private List<WebCommand> commands;

    @ManyToOne(optional = false)
    private VirtualSensorProcessorModel virtualSensorProcessorModel;

    public Long getId() {
        return id;
    }

    public List<WebCommand> getCommands() {
        return commands;
    }

    public void setCommands(List<WebCommand> commands) {
        this.commands = commands;
    }

    public VirtualSensorProcessorModel getVirtualSensorProcessorModel() {
        return virtualSensorProcessorModel;
    }

    public void setVirtualSensorProcessorModel(VirtualSensorProcessorModel virtualSensorProcessorModel) {
        this.virtualSensorProcessorModel = virtualSensorProcessorModel;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof WebInput)) return false;

        WebInput that = (WebInput) other;

        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) return false;
        if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) return false;
        if (getVirtualSensorProcessorModel() != null ? !getVirtualSensorProcessorModel().equals(that.getVirtualSensorProcessorModel()) : that.getVirtualSensorProcessorModel() != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        return result;
    }

}