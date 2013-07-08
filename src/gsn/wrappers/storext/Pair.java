package gsn.wrappers.storext;

/**
 * Created with IntelliJ IDEA.
 * User: ivo
 * Date: 4/18/13
 * Time: 1:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class Pair {
    private String fieldName;
    private String fieldValue;

    public Pair(String fieldName, String fieldValue) {
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getFieldName() {
        return fieldName;
    }
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
    public String getFieldValue() {
        return fieldValue;
    }
    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }
}
