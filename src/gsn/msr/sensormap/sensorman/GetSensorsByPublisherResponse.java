
package gsn.msr.sensormap.sensorman;

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
 *         &lt;element name="GetSensorsByPublisherResult" type="{http://tempuri.org/}ArrayOfSensorInfo" minOccurs="0"/>
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
    "getSensorsByPublisherResult"
})
@XmlRootElement(name = "GetSensorsByPublisherResponse")
public class GetSensorsByPublisherResponse {

    @XmlElement(name = "GetSensorsByPublisherResult")
    protected ArrayOfSensorInfo getSensorsByPublisherResult;

    /**
     * Gets the value of the getSensorsByPublisherResult property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfSensorInfo }
     *     
     */
    public ArrayOfSensorInfo getGetSensorsByPublisherResult() {
        return getSensorsByPublisherResult;
    }

    /**
     * Sets the value of the getSensorsByPublisherResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfSensorInfo }
     *     
     */
    public void setGetSensorsByPublisherResult(ArrayOfSensorInfo value) {
        this.getSensorsByPublisherResult = value;
    }

}
