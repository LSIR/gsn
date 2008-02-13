
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
 *         &lt;element name="InsertVectorSensorResult" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
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
    "insertVectorSensorResult"
})
@XmlRootElement(name = "InsertVectorSensorResponse")
public class InsertVectorSensorResponse {

    @XmlElement(name = "InsertVectorSensorResult")
    protected String insertVectorSensorResult;

    /**
     * Gets the value of the insertVectorSensorResult property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInsertVectorSensorResult() {
        return insertVectorSensorResult;
    }

    /**
     * Sets the value of the insertVectorSensorResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInsertVectorSensorResult(String value) {
        this.insertVectorSensorResult = value;
    }

}
