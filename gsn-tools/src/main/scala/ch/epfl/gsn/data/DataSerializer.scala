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
* File: src/ch/epfl/gsn/data/DataSerializer.scala
*
* @author Jean-Paul Calbimonte
*
*/
package ch.epfl.gsn.data

import java.nio.charset.Charset
import org.slf4j.LoggerFactory
import com.typesafe.config.ConfigFactory

trait DataSerializer{
  def ser(s:Sensor,props:Seq[String]):Object=
    ser(SensorData(Seq(),s),props,false)
  def ser(data:SensorData,props:Seq[String],latest:Boolean):Object
  def ser(data:Seq[SensorData],props:Seq[String]=Seq(),latest:Boolean=false):Object
  //def ser(data:Seq[Sensor],props:Seq[String]=Seq(),latest:Boolean=false):Object
  val dataLimit=ConfigFactory.load.getInt("gsn.data.limit")
}

/*  private def valueToJson(any:Any):JsValue=any match{       
    case d:Double=>JsNumber(d)
    case f:Float=>JsNumber(f)
    case i:Int=>JsNumber(i)
    case l:Long=>JsNumber(l)
    case s:String=>JsString(s)
    case a:Any=>JsString(a.toString) 
    case null=>JsNull
  }
*/
