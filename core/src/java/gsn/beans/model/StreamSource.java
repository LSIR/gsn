package gsn.beans.model;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToOne;

@Entity
@DiscriminatorValue("SS")
public class StreamSource extends DataNode {

    @OneToOne(optional = false, mappedBy = "streamSource")
    private Wrapper wrapper;

    public Wrapper getWrapper() {
        return wrapper;
    }

    public void setWrapper(Wrapper wrapper) {
        this.wrapper = wrapper;
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof StreamSource)) return false;

        StreamSource that = (StreamSource) other;

        if (!super.equals(other)) return false;
        if (getWrapper() != null ? !getWrapper().equals(that.getWrapper()) : that.getWrapper() != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getWrapper() != null ? getWrapper().hashCode() : 0);
        return result;
    }
}
