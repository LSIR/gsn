package gsn.beans.model;

import org.hibernate.annotations.Cascade;
import static org.hibernate.annotations.CascadeType.ALL;
import static org.hibernate.annotations.CascadeType.DELETE_ORPHAN;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.List;

@Entity
public class ModelNode extends DataNode {
    @ManyToOne(optional = false)
    private DataModel model;

    @OneToMany
    @Cascade({ALL, DELETE_ORPHAN})
    private List<Parameter> parameters;

    public DataModel getModel() {
        return model;
    }

    public void setModel(DataModel model) {
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
        if (other == null || !(other instanceof ModelNode)) return false;

        ModelNode that = (ModelNode) other;

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