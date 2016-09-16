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
* File: app/controllers/gsn/api/GsnService.scala
*
* @author Jean-Paul Calbimonte
*
*/
package controllers.gsn.api

import play.api.mvc._
import com.typesafe.config.ConfigFactory
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatterBuilder
import collection.JavaConversions._
import controllers.gsn.Global

trait GsnService {
  lazy val conf=ConfigFactory.load

  //val validFormats=Seq(Csv,Json)
  val defaultFormat=Json

  val dateFormatter={
    val parsers=conf.getStringList("gsn.api.dateTimeFormats").map{d=>
      DateTimeFormat.forPattern(d).getParser
    }.toArray
    new DateTimeFormatterBuilder().append(null,parsers).toFormatter
  }

  def queryparam(name:String)(implicit req:Request[AnyContent])=
    req.queryString.get(name).map(_.head)
  

  def param[T](name:String,fun: String=>T,default:T)(implicit req:Request[AnyContent])=
    queryparam(name).map(fun(_)).getOrElse(default)

}