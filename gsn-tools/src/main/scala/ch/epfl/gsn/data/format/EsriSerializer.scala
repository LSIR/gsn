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
* File: src/ch/epfl/gsn/data/format/EsriSerializer.scala
*
* @author Jean-Paul Calbimonte
*
*/
package ch.epfl.gsn.data.format

import ch.epfl.gsn.data.DataSerializer
import org.slf4j.LoggerFactory
import ch.epfl.gsn.data.SensorData
import java.io.StringWriter

object EsriSerializer extends DataSerializer{
  private val log=LoggerFactory.getLogger(getClass)
  override def ser(data:Seq[SensorData],props:Seq[String],latest:Boolean)=
    toEsri(data.head).toString
  override def ser(data:SensorData,props:Seq[String],latest:Boolean)=
    toEsri(data).toString


  def toEsri(data:SensorData)={
    val sw=new StringWriter
    val fields=data.ts.map(_.output)
    
    if (!data.ts.isEmpty){
	    val si=data.ts.head.series.size-1
	    (0 to si).foreach{i=>
	      val pp=(0 to fields.size-1).map{j=>
	        if (fields(j).fieldName.equals("grid")){
	          val arr=data.ts(j).series(i)
	          println(arr.getClass.getCanonicalName())
	          val array=arr.asInstanceOf[Array[Array[_]]]
	          array.map(row=>row.mkString(" ")).mkString(System.lineSeparator)
	        }
	        else
  	          fields(j).fieldName+" "+data.ts(j).series(i)        
	      }.mkString(System.lineSeparator)
	      sw.append(pp+System.lineSeparator)
	    }
    }
    
    sw  
    
  }

}