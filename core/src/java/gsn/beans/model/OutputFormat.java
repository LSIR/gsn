package gsn.beans.model;

import gsn.beans.DataType;

import javax.persistence.*;

@Entity
public class OutputFormat extends NameDescriptionClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private DataType dataType;

    @ManyToOne(optional = false)
    private VirtualSensorProcessorModel virtualSensorProcessorModel;

    public Long getId() {
        return id;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
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
        if (other == null || !(other instanceof OutputFormat)) return false;

        OutputFormat that = (OutputFormat) other;
        //id, then VSensorProcessorModel, then name
        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) return false;
        if (getVirtualSensorProcessorModel() != null ? !getVirtualSensorProcessorModel().equals(that.getVirtualSensorProcessorModel()) : that.getVirtualSensorProcessorModel() != null)
            return false;
        if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getVirtualSensorProcessorModel() != null ? getVirtualSensorProcessorModel().hashCode() : 0);
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        return result;
    }
}
