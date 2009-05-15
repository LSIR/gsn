package gsn.beans.model;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.List;

@Entity
@DiscriminatorValue("vsprocessor")
public class VirtualSensorProcessorModel extends BasicModel {

    @OneToMany(mappedBy = "virtualSensorProcessorModel")
    private List<OutputFormat> outputFormats;

    @OneToMany(mappedBy = "virtualSensorProcessorModel")
    private List<WebInput> webInputs;

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

        return super.equals(other);
    }

}
