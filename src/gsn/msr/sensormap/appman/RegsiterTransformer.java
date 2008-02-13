
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
 *         &lt;element name="userID" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="token" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="TxID" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="TxGeoRSSUrl" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="TxWsdlUrl" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="TxGuiUrl" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
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
    "txID",
    "txGeoRSSUrl",
    "txWsdlUrl",
    "txGuiUrl",
    "description"
})
@XmlRootElement(name = "RegsiterTransformer")
public class RegsiterTransformer {

    protected String userID;
    protected String token;
    @XmlElement(name = "TxID")
    protected String txID;
    @XmlElement(name = "TxGeoRSSUrl")
    protected String txGeoRSSUrl;
    @XmlElement(name = "TxWsdlUrl")
    protected String txWsdlUrl;
    @XmlElement(name = "TxGuiUrl")
    protected String txGuiUrl;
    protected String description;

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
     * Gets the value of the txID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTxID() {
        return txID;
    }

    /**
     * Sets the value of the txID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTxID(String value) {
        this.txID = value;
    }

    /**
     * Gets the value of the txGeoRSSUrl property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTxGeoRSSUrl() {
        return txGeoRSSUrl;
    }

    /**
     * Sets the value of the txGeoRSSUrl property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTxGeoRSSUrl(String value) {
        this.txGeoRSSUrl = value;
    }

    /**
     * Gets the value of the txWsdlUrl property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTxWsdlUrl() {
        return txWsdlUrl;
    }

    /**
     * Sets the value of the txWsdlUrl property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTxWsdlUrl(String value) {
        this.txWsdlUrl = value;
    }

    /**
     * Gets the value of the txGuiUrl property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTxGuiUrl() {
        return txGuiUrl;
    }

    /**
     * Sets the value of the txGuiUrl property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTxGuiUrl(String value) {
        this.txGuiUrl = value;
    }

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

}
