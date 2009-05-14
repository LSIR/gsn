package gsn.beans.model;

import javax.persistence.*;

@Entity
public class Parameter extends NameDescriptionClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private ParameterModel model;

    @Column(name = "_value")
    private String value;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ParameterModel getModel() {
        return model;
    }

    public void setModel(ParameterModel model) {
        this.model = model;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof Parameter)) return false;

        Parameter that = (Parameter) other;

        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) return false;
        if (getModel() != null ? !getModel().equals(that.getModel()) : that.getModel() != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getModel() != null ? getModel().hashCode() : 0);
        return result;
    }
}
