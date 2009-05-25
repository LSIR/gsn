package gsn;

import gsn.beans.DataField;
import gsn.beans.StreamElement;

import java.io.Serializable;

public class TestUtils {
    public static StreamElement createStreamElement(long timed) {
        return new StreamElement(new DataField[]{}, new Serializable[]{}, timed);
    }

    public static void print(String query) {
        System.out.println(query);
    }
}
