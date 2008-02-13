
package gsn.msr.sensormap.appman;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for SensorData complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SensorData">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Timestamp" type="{http://www.w3.org/2001/XMLSchema}dateTime"/>
 *         &lt;element name="Timeseries" type="{http://tempuri.org/}ArrayOfDateTime" minOccurs="0"/>
 *         &lt;element name="Data" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
 *         &lt;element name="SensorType" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="SensorTypeString" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="DataType" type="{http://tempuri.org/}DataType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SensorData", namespace = "http://tempuri.org/", propOrder = {
    "timestamp",
    "timeseries",
    "data",
    "sensorType",
    "sensorTypeString",
    "dataType"
})
public class SensorData {

    @XmlElement(name = "Timestamp", namespace = "http://tempuri.org/", required = true)
    protected XMLGregorianCalendar timestamp;
    @XmlElement(name = "Timeseries", namespace = "http://tempuri.org/")
    protected ArrayOfDateTime timeseries;
    @XmlElement(name = "Data", namespace = "http://tempuri.org/")
    protected Object data;
    @XmlElement(name = "SensorType", namespace = "http://tempuri.org/")
    protected int sensorType;
    @XmlElement(name = "SensorTypeString", namespace = "http://tempuri.org/")
    protected String sensorTypeString;
    @XmlElement(name = "DataType", namespace = "http://tempuri.org/", required = true)
    protected DataType dataType;

    /**
     * Gets the value of the timestamp property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the value of the timestamp property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setTimestamp(XMLGregorianCalendar value) {
        this.timestamp = value;
    }

    /**
     * Gets the value of the timeseries property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfDateTime }
     *     
     */
    public ArrayOfDateTime getTimeseries() {
        return timeseries;
    }

    /**
     * Sets the value of the timeseries property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfDateTime }
     *     
     */
    public void setTimeseries(ArrayOfDateTime value) {
        this.timeseries = value;
    }

    /**
     * Gets the value of the data property.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getData() {
        return data;
    }

    /**
     * Sets the value of the data property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setData(Object value) {
        this.data = value;
    }

    /**
     * Gets the value of the sensorType property.
     * 
     */
    public int getSensorType() {
        return sensorType;
    }

    /**
     * Sets the value of the sensorType property.
     * 
     */
    public void setSensorType(int value) {
        this.sensorType = value;
    }

    /**
     * Gets the value of the sensorTypeString property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSensorTypeString() {
        return sensorTypeString;
    }

    /**
     * Sets the value of the sensorTypeString property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSensorTypeString(String value) {
        this.sensorTypeString = value;
    }

    /**
     * Gets the value of the dataType property.
     * 
     * @return
     *     possible object is
     *     {@link DataType }
     *     
     */
    public DataType getDataType() {
        return dataType;
    }

    /**
     * Sets the value of the dataType property.
     * 
     * @param value
     *     allowed object is
     *     {@link DataType }
     *     
     */
    public void setDataType(DataType value) {
        this.dataType = value;
    }

}
