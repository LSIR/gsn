
package gsn.msr.sensormap.appman;

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
 *         &lt;element name="userID" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="token" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="polygon" type="{http://nec.research.microsoft.com/}ArrayOfPointF" minOccurs="0"/>
 *         &lt;element name="searchTerms" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="sensorTypes" type="{http://nec.research.microsoft.com/}ArrayOfSensorTypeEnum" minOccurs="0"/>
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
    "userID",
    "token",
    "polygon",
    "searchTerms",
    "sensorTypes"
})
@XmlRootElement(name = "FindSensorsByLocation")
public class FindSensorsByLocation {

    protected String userID;
    protected String token;
    protected ArrayOfPointF polygon;
    protected String searchTerms;
    protected ArrayOfSensorTypeEnum sensorTypes;

    /**
     * Gets the value of the userID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUserID() {
        return userID;
    }

    /**
     * Sets the value of the userID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUserID(String value) {
        this.userID = value;
    }

    /**
     * Gets the value of the token property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getToken() {
        return token;
    }

    /**
     * Sets the value of the token property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setToken(String value) {
        this.token = value;
    }

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
     * Gets the value of the searchTerms property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSearchTerms() {
        return searchTerms;
    }

    /**
     * Sets the value of the searchTerms property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSearchTerms(String value) {
        this.searchTerms = value;
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
