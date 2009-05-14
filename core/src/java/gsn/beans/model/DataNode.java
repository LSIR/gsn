package gsn.beans.model;

import javax.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "childType", discriminatorType = DiscriminatorType.STRING)
public abstract class DataNode extends NameDescriptionClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private DataNode parent;

    @Column(length = (1024 * 8), nullable = false)
    private String query;

    @OneToOne(optional = false, mappedBy = "dataNode")
    private WindowPredicate windowPredicate;

    private float loadShedding = 0.0F;

    public Long getId() {
        return id;
    }

    public DataNode getParent() {
        return parent;
    }

    public void setParent(DataNode parent) {
        this.parent = parent;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public float getLoadShedding() {
        return loadShedding;
    }

    public void setLoadShedding(float loadShedding) {
        this.loadShedding = loadShedding;
    }

    public WindowPredicate getWindowPredicate() {
        return windowPredicate;
    }

    public void setWindowPredicate(WindowPredicate windowPredicate) {
        this.windowPredicate = windowPredicate;
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof DataNode)) return false;

        DataNode that = (DataNode) other;

        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) return false;
        if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) return false;
        if (getQuery() != null ? !getQuery().equals(that.getQuery()) : that.getQuery() != null) return false;
        if (getWindowPredicate() != null ? !getWindowPredicate().equals(that.getWindowPredicate()) : that.getWindowPredicate() != null)
            return false;
        if (getParent() != null ? !getParent().equals(that.getParent()) : that.getParent() != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        result = 31 * result + (getQuery() != null ? getQuery().hashCode() : 0);
        result = 31 * result + (getWindowPredicate() != null ? getWindowPredicate().hashCode() : 0);
        result = 31 * result + (getParent() != null ? getParent().hashCode() : 0);
        return result;
    }
}
