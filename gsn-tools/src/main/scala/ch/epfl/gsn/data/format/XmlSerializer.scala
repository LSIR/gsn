/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2016, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/ch/epfl/gsn/data/format/XmlSerializer.scala
*
* @author Jean-Paul Calbimonte
*
*/
package ch.epfl.gsn.data.format

import ch.epfl.gsn.data._
import ch.epfl.gsn.data.format.TimeFormats._
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