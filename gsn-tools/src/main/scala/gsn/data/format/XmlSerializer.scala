package gsn.data.format

import gsn.data._
import gsn.data.format.TimeFormats._
import scala.xml.XML
import org.joda.time.format.ISODateTimeFormat

object XmlSerializer extends DataSerializer{
  val dateFormat=ISODateTimeFormat.dateTimeNoMillis
  
  override def ser(data:Seq[SensorData],props:Seq[String],withVals:Boolean)={
    val xml=
      <gsn name="gsnlocal" author="gssn" email="emailos" description="desc">{
      data.map(d=>toXml(d,withVals))
      }        
      </gsn>;
    xml.toString
  }
    
  override def ser(data:SensorData,props:Seq[String],withVals:Boolean)={
    val xml=
      <gsn name="gsnlocal" >{
        toXml(data,withVals)
      }        
      </gsn>;
    xml.toString
  }
  
  private def toXml(data:SensorData, withVals:Boolean):xml.Elem={    
    val s= data.sensor 
    val desc=s.properties.getOrElse("description","")
    val acces=s.properties.getOrElse("accessProtected","false").toBoolean
    val protect=if (acces) "(protected)" else " "
    def xmlFields= 
      s.fields.map{f=>
         <field name={f.fieldName }
            type={f.dataType.name} unit={f.unit.code} >
         </field>
      }
    def xmlFieldValues=
      data.stats.latestValues.map{ts=>
         <field name={fieldName(ts.output.fieldName) }
            type={ts.output.dataType.name} unit={ts.output.unit.code}>{
              if (ts.output.fieldName=="timestamp") 
                dateFormat.print(ts.series.head.toString.toLong)
              else
                ts.series.headOption.getOrElse(null)
            }</field>
      }
    def fieldName(name:String)=
      if (name=="timestamp") "time" else name
    
    val vs= 
      <virtual-sensor name={s.name} 
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
}