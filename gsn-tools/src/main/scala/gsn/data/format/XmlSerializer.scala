package gsn.data.format

import gsn.data._
import scala.xml.XML

object XmlSerializer extends DataSerializer{
  
  override def ser(data:Seq[SensorData],props:Seq[String],withVals:Boolean)={
    val xml=
      <gsn>{
      data.map(d=>toXml(d))
      }        
      </gsn>;
    xml.toString
  }
    
  override def ser(data:SensorData,props:Seq[String],withVals:Boolean)=
    toXml(data).toString
  
  private def toXml(data:SensorData):xml.Elem={
    val s= data.sensor 
    val desc=s.properties.getOrElse("description","")
    val acces=s.properties.getOrElse("accessProtected","false").toBoolean
    val protect=if (acces) "(protected)" else "" 
    val vs= 
      <virtual-sensor name={s.name} 
        protected={protect} 
        description={desc}>
        {
          val output=s.fields.map{f=>
            <field name={f.fieldName }
              type={f.dataType.name} unit={f.unit.code} >
            </field>
          }
          val predicates=s.properties.map{case (k,v)=>
            <field name={k} catergory="predicate" >{v}</field>
          }
          output++predicates
        }
      </virtual-sensor>;
    vs
  }  
}