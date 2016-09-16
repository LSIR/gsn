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
* File: src/ch/epfl/gsn/data/format/CsvSerializer.scala
*
* @author Jean-Paul Calbimonte
*
*/
package ch.epfl.gsn.data.format

import ch.epfl.gsn.data.DataSerializer
import ch.epfl.gsn.data._
import java.io.StringWriter
import java.io.ByteArrayOutputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import java.io.FileOutputStream

object CsvSerializer extends DataSerializer{
  
  override def ser(data:Seq[SensorData],props:Seq[String],latest:Boolean)=
    toCsv(data,props,latest).toString

  override def ser(data:SensorData,props:Seq[String],latest:Boolean)=
    toCsv(data,props).toString

  def toCsv(data:Seq[SensorData],props:Seq[String],latest:Boolean)={
    val sw=new StringWriter
    //val hasVals= !data.ts.isEmpty
    val valsHead=if (latest) ",values" else ""
    sw.append("#vs_name,"+props.mkString(",")+",fields,units,types"+valsHead+System.lineSeparator)
    data.foreach{s=>
      sw.append(sensorDataToCsv(s,props,latest)+System.lineSeparator())
    }
    sw
  }
  private def toString(s:SensorData,props:Seq[String])={
    val output=s.ts.map(t=>t.output)
    val prop=props.map(p=>"\""+encodeBreaks(s.sensor.properties.getOrElse(p,""))+"\"").mkString(",")
    s"${s.sensor.name},$prop"+
    ",\""+output.map(f=>f.fieldName).mkString("|")+"\""+
    ",\""+output.map(f=>f.unit.code).mkString("|")+"\""+
    ",\""+output.map(f=>f.dataType.name).mkString("|")+"\""    
  }
  
  def sensorDataToCsv(data:SensorData,props:Seq[String],withLatest:Boolean=false)={
    //val valueNames="latestValues("+data.latest.map(_._1.fieldName).mkString("|")+")"
    //"#vs_name,"+props.mkString(",")+",fields,units,types,"+valueNames+System.lineSeparator
    if (withLatest)
      toString(data,props)+","+data.latest.map(_._2).mkString("|")
    else toString(data,props)
  }

  private def encodeBreaks(s:String)=s.replaceAll("(\r\n)|\r|\n", "\\\\n")

  
  
  
  def toCsv(data:SensorData,
      valueNames:Seq[String]=Seq()):StringWriter={
    val sw=new StringWriter
    def head(key:String,value:Any)=
      sw.append("# "+key+":"+value+System.lineSeparator)
    
    head("vs_name",data.sensor.name)
    data.sensor.properties.foreach(p=>head(p._1,encodeBreaks(p._2)))
    //val fields=data.sensor.fields.filter(f=>valueNames.isEmpty || valueNames.contains(f.fieldName ))
    val fields=data.ts.map(_.output)
    
    head("fields",fields.map{_.fieldName}.mkString(","))
    head("units",fields.map{_.unit.code}.mkString(","))
    head("types",fields.map{_.dataType.name}.mkString(","))
    if (!data.ts.isEmpty){
	    println("drimp "+data.ts.head.series.size+" "+fields.size)
	    val si=data.ts.head.series.size-1
	    (0 to si).foreach{i=>
	      //sw.append(data.time(i)+",")
	      val pp=(0 to fields.size-1).map{j=>
	        data.ts(j).series (i)        
	      }.mkString(",")
	      sw.append(pp+System.lineSeparator)
	      //sw.append(pp)  
	    }
    }
    
    sw  
    
  }
  
  
  def serZip(data:Seq[SensorData],props:Seq[String],latest:Boolean)={
    val baos=new ByteArrayOutputStream
    val zos=new ZipOutputStream(baos)
    data.foreach{d=>
      val entry=new ZipEntry(d.sensor.name+".csv")
      zos.putNextEntry(entry)
      zos.write(toCsv(d).toString.getBytes())
      zos.closeEntry
    }
    zos.finish()
    zos.close()
    baos.toByteArray()
  }
    

}