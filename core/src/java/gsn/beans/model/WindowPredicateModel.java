package gsn.beans.model;

import javax.persistence.*;
import java.util.List;

@Entity
public class WindowPredicateModel extends NameDescriptionClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany
    private List<ParameterModel> parameters;

    @OneToMany
    private List<WindowPredicate> windowPredicates;

    public Long getId() {
        return id;
    }

    public List<ParameterModel> getParameters() {
        return parameters;
    }

    public void setParameters(List<ParameterModel> parameters) {
        this.parameters = parameters;
    }

    public List<WindowPredicate> getWindowPredicates() {
        return windowPredicates;
    }

    public void setWindowPredicates(List<WindowPredicate> windowPredicates) {
        this.windowPredicates = windowPredicates;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof WindowPredicateModel)) return false;

        WindowPredicateModel that = (WindowPredicateModel) other;

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
