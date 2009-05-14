package gsn.beans.model;

import javax.persistence.*;
import java.util.List;

@Entity
public class Wrapper extends NameDescriptionClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private WrapperModel model;

    @OneToMany
    private List<Parameter> parameters;

    @OneToOne
    private StreamSource streamSource;

    public Long getId() {
        return id;
    }

    public WrapperModel getModel() {
        return model;
    }

    public void setModel(WrapperModel model) {
        this.model = model;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public StreamSource getStreamSource() {
        return streamSource;
    }

    public void setStreamSource(StreamSource streamSource) {
        this.streamSource = streamSource;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof Wrapper)) return false;

        Wrapper that = (Wrapper) other;

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
