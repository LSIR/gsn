
package gsn.msr.sensormap.appman;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ResponseOfArrayOfSensorInfo complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ResponseOfArrayOfSensorInfo">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="message" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="returnData" type="{http://nec.research.microsoft.com/}ArrayOfSensorInfo" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ResponseOfArrayOfSensorInfo", propOrder = {
    "message",
    "returnData"
})
public class ResponseOfArrayOfSensorInfo {

    protected String message;
    protected ArrayOfSensorInfo returnData;

    /**
     * Gets the value of the message property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the value of the message property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMessage(String value) {
        this.message = value;
    }

    /**
     * Gets the value of the returnData property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfSensorInfo }
     *     
     */
    public ArrayOfSensorInfo getReturnData() {
        return returnData;
    }

    /**
     * Sets the value of the returnData property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfSensorInfo }
     *     
     */
    public void setReturnData(ArrayOfSensorInfo value) {
        this.returnData = value;
    }

}
