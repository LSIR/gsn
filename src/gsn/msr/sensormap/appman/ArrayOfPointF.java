
package gsn.msr.sensormap.appman;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ArrayOfPointF complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArrayOfPointF">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="PointF" type="{http://nec.research.microsoft.com/}PointF" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfPointF", propOrder = {
    "pointF"
})
public class ArrayOfPointF {

    @XmlElement(name = "PointF", nillable = true)
    protected List<PointF> pointF;

    /**
     * Gets the value of the pointF property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the pointF property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPointF().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PointF }
     * 
     * 
     */
    public List<PointF> getPointF() {
        if (pointF == null) {
            pointF = new ArrayList<PointF>();
        }
        return this.pointF;
    }

}
