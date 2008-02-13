
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
 *         &lt;element name="publisherName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="passCode" type="{http://microsoft.com/wsdl/types/}guid"/>
 *         &lt;element name="sensorName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="sensorType" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
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
    "publisherName",
    "passCode",
    "sensorName",
    "sensorType"
})
@XmlRootElement(name = "DeleteVectorSensor")
public class DeleteVectorSensor {

    protected String publisherName;
    @XmlElement(required = true)
    protected String passCode;
    protected String sensorName;
    protected String sensorType;

    /**
     * Gets the value of the publisherName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPublisherName() {
        return publisherName;
    }

    /**
     * Sets the value of the publisherName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPublisherName(String value) {
        this.publisherName = value;
    }

    /**
     * Gets the value of the passCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPassCode() {
        return passCode;
    }

    /**
     * Sets the value of the passCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPassCode(String value) {
        this.passCode = value;
    }

    /**
     * Gets the value of the sensorName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSensorName() {
        return sensorName;
    }

    /**
     * Sets the value of the sensorName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSensorName(String value) {
        this.sensorName = value;
    }

    /**
     * Gets the value of the sensorType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSensorType() {
        return sensorType;
    }

    /**
     * Sets the value of the sensorType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSensorType(String value) {
        this.sensorType = value;
    }

}
