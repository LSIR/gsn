
package gsn.msr.sensormap.appman;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ArrayOfSensorData complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArrayOfSensorData">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="SensorData" type="{http://tempuri.org/}SensorData" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfSensorData", propOrder = {
    "sensorData"
})
public class ArrayOfSensorData {

    @XmlElement(name = "SensorData", nillable = true)
    protected List<SensorData> sensorData;

    /**
     * Gets the value of the sensorData property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the sensorData property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSensorData().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SensorData }
     * 
     * 
     */
    public List<SensorData> getSensorData() {
        if (sensorData == null) {
            sensorData = new ArrayList<SensorData>();
        }
        return this.sensorData;
    }

}
