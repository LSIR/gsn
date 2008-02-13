
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
 *         &lt;element name="CollectDataSnapshotScalarResult" type="{http://nec.research.microsoft.com/}ResponseOfArrayOfSensorData" minOccurs="0"/>
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
    "collectDataSnapshotScalarResult"
})
@XmlRootElement(name = "CollectDataSnapshotScalarResponse")
public class CollectDataSnapshotScalarResponse {

    @XmlElement(name = "CollectDataSnapshotScalarResult")
    protected ResponseOfArrayOfSensorData collectDataSnapshotScalarResult;

    /**
     * Gets the value of the collectDataSnapshotScalarResult property.
     * 
     * @return
     *     possible object is
     *     {@link ResponseOfArrayOfSensorData }
     *     
     */
    public ResponseOfArrayOfSensorData getCollectDataSnapshotScalarResult() {
        return collectDataSnapshotScalarResult;
    }

    /**
     * Sets the value of the collectDataSnapshotScalarResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link ResponseOfArrayOfSensorData }
     *     
     */
    public void setCollectDataSnapshotScalarResult(ResponseOfArrayOfSensorData value) {
        this.collectDataSnapshotScalarResult = value;
    }

}
