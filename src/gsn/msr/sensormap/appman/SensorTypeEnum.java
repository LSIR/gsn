
package gsn.msr.sensormap.appman;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;


/**
 * <p>Java class for SensorTypeEnum.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="SensorTypeEnum">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Unknown"/>
 *     &lt;enumeration value="Generic"/>
 *     &lt;enumeration value="Temperature"/>
 *     &lt;enumeration value="Video"/>
 *     &lt;enumeration value="Traffic"/>
 *     &lt;enumeration value="Parking"/>
 *     &lt;enumeration value="Pressure"/>
 *     &lt;enumeration value="Humidity"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlEnum
public enum SensorTypeEnum {

    @XmlEnumValue("Unknown")
    UNKNOWN("Unknown"),
    @XmlEnumValue("Generic")
    GENERIC("Generic"),
    @XmlEnumValue("Temperature")
    TEMPERATURE("Temperature"),
    @XmlEnumValue("Video")
    VIDEO("Video"),
    @XmlEnumValue("Traffic")
    TRAFFIC("Traffic"),
    @XmlEnumValue("Parking")
    PARKING("Parking"),
    @XmlEnumValue("Pressure")
    PRESSURE("Pressure"),
    @XmlEnumValue("Humidity")
    HUMIDITY("Humidity");
    private final String value;

    SensorTypeEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static SensorTypeEnum fromValue(String v) {
        for (SensorTypeEnum c: SensorTypeEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v.toString());
    }

}
