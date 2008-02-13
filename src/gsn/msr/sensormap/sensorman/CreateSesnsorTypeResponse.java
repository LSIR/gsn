
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
 *         &lt;element name="CreateSesnsorTypeResult" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
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
    "createSesnsorTypeResult"
})
@XmlRootElement(name = "CreateSesnsorTypeResponse")
public class CreateSesnsorTypeResponse {

    @XmlElement(name = "CreateSesnsorTypeResult")
    protected String createSesnsorTypeResult;

    /**
     * Gets the value of the createSesnsorTypeResult property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCreateSesnsorTypeResult() {
        return createSesnsorTypeResult;
    }

    /**
     * Sets the value of the createSesnsorTypeResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCreateSesnsorTypeResult(String value) {
        this.createSesnsorTypeResult = value;
    }

}
