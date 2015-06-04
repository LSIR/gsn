package ch.epfl.gsn.metadata.core.model;


import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * Created by kryvych on 10/03/15.
 */
public class RelativePosition {
    private String x;
    private String y;
    private String z;

    public RelativePosition(String x, String y, String z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String getPosition() {
        if (x != null && y != null && z != null) {
            return Joiner.on(",").join(Lists.newArrayList(x, y, z));
        }
        return "";
    }
}
