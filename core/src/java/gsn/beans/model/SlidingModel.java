package gsn.beans.model;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("sliding")
public class SlidingModel extends BasicModel {

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof SlidingModel)) return false;

        return super.equals(other);
    }
}