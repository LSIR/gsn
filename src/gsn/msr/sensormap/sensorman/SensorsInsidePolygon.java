
package gsn.msr.sensormap.sensorman;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
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
 *         &lt;element name="polygon" type="{http://tempuri.org/}ArrayOfPointF" minOccurs="0"/>
 *         &lt;element name="viewport" type="{http://tempuri.org/}ArrayOfPointF" minOccurs="0"/>
 *         &lt;element name="searchStr" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="sensorTypes" type="{http://tempuri.org/}ArrayOfSensorTypeEnum" minOccurs="0"/>
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
    "polygon",
    "viewport",
    "searchStr",
    "sensorTypes"
})
@XmlRootElement(name = "SensorsInsidePolygon")
public class SensorsInsidePolygon {

    protected ArrayOfPointF polygon;
    protected ArrayOfPointF viewport;
    protected String searchStr;
    protected ArrayOfSensorTypeEnum sensorTypes;

    /**
     * Gets the value of the polygon property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfPointF }
     *     
     */
    public ArrayOfPointF getPolygon() {
        return polygon;
    }

    /**
     * Sets the value of the polygon property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfPointF }
     *     
     */
    public void setPolygon(ArrayOfPointF value) {
        this.polygon = value;
    }

    /**
     * Gets the value of the viewport property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfPointF }
     *     
     */
    public ArrayOfPointF getViewport() {
        return viewport;
    }

    /**
     * Sets the value of the viewport property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfPointF }
     *     
     */
    public void setViewport(ArrayOfPointF value) {
        this.viewport = value;
    }

    /**
     * Gets the value of the searchStr property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSearchStr() {
        return searchStr;
    }

    /**
     * Sets the value of the searchStr property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSearchStr(String value) {
        this.searchStr = value;
    }

    /**
     * Gets the value of the sensorTypes property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfSensorTypeEnum }
     *     
     */
    public ArrayOfSensorTypeEnum getSensorTypes() {
        return sensorTypes;
    }

    /**
     * Sets the value of the sensorTypes property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfSensorTypeEnum }
     *     
     */
    public void setSensorTypes(ArrayOfSensorTypeEnum value) {
        this.sensorTypes = value;
    }

}
