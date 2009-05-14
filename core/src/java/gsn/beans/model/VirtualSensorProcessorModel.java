package gsn.beans.model;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

@Entity
public class VirtualSensorProcessorModel extends NameDescriptionClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String className;

    @OneToMany
    private Set<ParameterModel> parameters;

    @OneToMany(mappedBy = "virtualSensorProcessorModel")
    private List<OutputFormat> outputFormats;

    @OneToMany(mappedBy = "virtualSensorProcessorModel")
    private List<WebInput> webInputs;


    public Long getId() {
        return id;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Set<ParameterModel> getParameters() {
        return parameters;
    }

    public void setParameters(Set<ParameterModel> parameters) {
        this.parameters = parameters;
    }

    public List<OutputFormat> getOutputFormats() {
        return outputFormats;
    }

    public void setOutputFormats(List<OutputFormat> outputFormats) {
        this.outputFormats = outputFormats;
    }

    public List<WebInput> getWebInputs() {
        return webInputs;
    }

    public void setWebInputs(List<WebInput> webInputs) {
        this.webInputs = webInputs;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof VirtualSensorProcessorModel)) return false;

        VirtualSensorProcessorModel that = (VirtualSensorProcessorModel) other;

        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) return false;
        if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        return result;
    }

}
