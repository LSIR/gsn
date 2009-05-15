package gsn.beans.model;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("model")
public class DataModel extends BasicModel {

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof DataModel)) return false;

        return super.equals(other);
    }
}