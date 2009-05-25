package gsn.beans.model;

import org.hibernate.annotations.Cascade;
import static org.hibernate.annotations.CascadeType.ALL;
import static org.hibernate.annotations.CascadeType.DELETE_ORPHAN;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.List;

@Entity
public class WrapperNode extends DataNode {
    @ManyToOne(optional = false)
    private WrapperModel model;

    @OneToMany
    @Cascade({ALL, DELETE_ORPHAN})
    private List<Parameter> parameters;

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

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof WrapperNode)) return false;

        WrapperNode that = (WrapperNode) other;

        if (!super.equals(other)) return false;
        if (getModel() != null ? !getModel().equals(that.getModel()) : that.getModel() != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getModel() != null ? getModel().hashCode() : 0);
        return result;
    }
}
