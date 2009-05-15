package gsn.beans.model;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("window")
public class WindowModel extends BasicModel {

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof WindowModel)) return false;

        return super.equals(other);
    }
}
