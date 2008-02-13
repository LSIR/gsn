
package gsn.msr.sensormap.appman;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;


/**
 * <p>Java class for DataType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="DataType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Unknown"/>
 *     &lt;enumeration value="Scalar"/>
 *     &lt;enumeration value="BMP"/>
 *     &lt;enumeration value="JPG"/>
 *     &lt;enumeration value="GIF"/>
 *     &lt;enumeration value="VECTOR"/>
 *     &lt;enumeration value="HTML"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlEnum
public enum DataType {

    @XmlEnumValue("Unknown")
    UNKNOWN("Unknown"),
    @XmlEnumValue("Scalar")
    SCALAR("Scalar"),
    BMP("BMP"),
    JPG("JPG"),
    GIF("GIF"),
    VECTOR("VECTOR"),
    HTML("HTML");
    private final String value;

    DataType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static DataType fromValue(String v) {
        for (DataType c: DataType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v.toString());
    }

}
