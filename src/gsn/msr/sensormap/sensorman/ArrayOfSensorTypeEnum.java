
package gsn.msr.sensormap.sensorman;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ArrayOfSensorTypeEnum complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArrayOfSensorTypeEnum">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="SensorTypeEnum" type="{http://tempuri.org/}SensorTypeEnum" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfSensorTypeEnum", propOrder = {
    "sensorTypeEnum"
})
public class ArrayOfSensorTypeEnum {

    @XmlElement(name = "SensorTypeEnum")
    protected List<SensorTypeEnum> sensorTypeEnum;

    /**
     * Gets the value of the sensorTypeEnum property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the sensorTypeEnum property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSensorTypeEnum().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SensorTypeEnum }
     * 
     * 
     */
    public List<SensorTypeEnum> getSensorTypeEnum() {
        if (sensorTypeEnum == null) {
            sensorTypeEnum = new ArrayList<SensorTypeEnum>();
        }
        return this.sensorTypeEnum;
    }

}
