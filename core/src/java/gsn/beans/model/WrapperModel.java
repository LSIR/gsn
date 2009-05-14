package gsn.beans.model;

import javax.persistence.*;
import java.util.Set;

@Entity
public class WrapperModel extends NameDescriptionClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String className;


    @OneToMany
    private Set<ParameterModel> parameters;

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

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof WrapperModel)) return false;

        WrapperModel that = (WrapperModel) other;

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
