package gsn.beans.model;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
public class QueryNode extends DataNode {

    @Column(length = (1024 * 8), nullable = false)
    private String query;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }


    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof QueryNode)) return false;

        QueryNode that = (QueryNode) other;

        if (!super.equals(other)) return false;
        if (getQuery() != null ? !getQuery().equals(that.getQuery()) : that.getQuery() != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getQuery() != null ? getQuery().hashCode() : 0);
        return result;
    }
}
