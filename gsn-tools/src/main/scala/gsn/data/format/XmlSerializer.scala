package gsn.data.format

import gsn.data._

import gsn.data.format.TimeFormats._
import scala.xml.XML
import scala.xml.Text
import scala.xml.Null
import scala.xml.Elem
import scala.xml.Attribute
import org.joda.time.format.ISODateTimeFormat
import gsn.data.discovery.PropertiesManager

object XmlSerializer extends DataSerializer{
  val dateFormat=ISODateTimeFormat.dateTimeNoMillis
  
  override def ser(data:Seq[SensorData],props:Seq[String],withVals:Boolean)={
    val xml=
      <gsn name="gsnlocal" author="gsn" email="emailo" description="desc">{
      data.map(d=>toXml(d,withVals))
      }        
      </gsn>;
    xml.toString
  }
    
  override def ser(data:SensorData,props:Seq[String],withVals:Boolean)=
    toXml(data,withVals).toString
  
  private def toXml(data:SensorData, withVals:Boolean):xml.Elem={    
    val s= data.sensor 
    val desc=s.properties.getOrElse("description","")
    val acces=s.properties.getOrElse("accessProtected","false").toBoolean
    val protect=if (acces) "(protected)" else " "
    def xmlFields= 
      s.fields.map{f=>
         var tmp = <field name={f.fieldName}
            type={f.dataType.name} unit={f.unit.code} >
         </field>
         addAttributes(tmp, f.mapping)
      }
    def xmlFieldValues=
      data.stats.latestValues.map{ts=>
         var tmp = <field name={fieldName(ts.output.fieldName) }
            type={ts.output.dataType.name} unit={ts.output.unit.code}>{
              if (ts.output.fieldName=="timestamp") 
                dateFormat.print(ts.series.head.toString.toLong)
              else
                ts.series.headOption.getOrElse(null)
            }</field>
        addAttributes(tmp, ts.output.mapping)
      }
    def fieldName(name:String)=
      if (name=="timestamp") "time" else name
    
    val vs= 
      <virtual-sensor name={s.name.toLowerCase()} 
        protected={protect} 
        description={desc}>
        {         
          val predicates=s.properties.map{case (k,v)=>
            <field name={k} category="predicate" >{v}</field>
          }
          if (withVals)
            xmlFieldValues++predicates
          else 
            xmlFields++predicates
        }
      </virtual-sensor>;
    vs
  }
  private def addAttributes(e:Elem, attributes:Option[List[(String, String)]]):Elem = {
    var acc = e
    attributes match {
      case Some(m) => m.foreach(x => acc = acc % Attribute(None, x._1, Text(x._2), Null))
      case None =>
    }
    acc
  }
}