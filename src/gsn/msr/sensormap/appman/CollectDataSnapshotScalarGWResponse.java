
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
 *         &lt;element name="CollectDataSnapshotScalarGWResult" type="{http://nec.research.microsoft.com/}ResponseOfArrayOfSensorData" minOccurs="0"/>
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
    "collectDataSnapshotScalarGWResult"
})
@XmlRootElement(name = "CollectDataSnapshotScalarGWResponse")
public class CollectDataSnapshotScalarGWResponse {

    @XmlElement(name = "CollectDataSnapshotScalarGWResult")
    protected ResponseOfArrayOfSensorData collectDataSnapshotScalarGWResult;

    /**
     * Gets the value of the collectDataSnapshotScalarGWResult property.
     * 
     * @return
     *     possible object is
     *     {@link ResponseOfArrayOfSensorData }
     *     
     */
    public ResponseOfArrayOfSensorData getCollectDataSnapshotScalarGWResult() {
        return collectDataSnapshotScalarGWResult;
    }

    /**
     * Sets the value of the collectDataSnapshotScalarGWResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link ResponseOfArrayOfSensorData }
     *     
     */
    public void setCollectDataSnapshotScalarGWResult(ResponseOfArrayOfSensorData value) {
        this.collectDataSnapshotScalarGWResult = value;
    }

}
