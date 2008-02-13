
package gsn.msr.sensormap.appman;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="FindSensorsByLocationResult" type="{http://nec.research.microsoft.com/}ResponseOfArrayOfSensorInfo" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "findSensorsByLocationResult"
})
@XmlRootElement(name = "FindSensorsByLocationResponse")
public class FindSensorsByLocationResponse {

    @XmlElement(name = "FindSensorsByLocationResult")
    protected ResponseOfArrayOfSensorInfo findSensorsByLocationResult;

    /**
     * Gets the value of the findSensorsByLocationResult property.
     * 
     * @return
     *     possible object is
     *     {@link ResponseOfArrayOfSensorInfo }
     *     
     */
    public ResponseOfArrayOfSensorInfo getFindSensorsByLocationResult() {
        return findSensorsByLocationResult;
    }

    /**
     * Sets the value of the findSensorsByLocationResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link ResponseOfArrayOfSensorInfo }
     *     
     */
    public void setFindSensorsByLocationResult(ResponseOfArrayOfSensorInfo value) {
        this.findSensorsByLocationResult = value;
    }

}
