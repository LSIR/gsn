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
* File: app/controllers/gsn/api/Formats.scala
*
* @author Jean-Paul Calbimonte
*
*/
package controllers.gsn.api

case class OutputFormat(code:String) {
  val formats=Seq("csv","json","xml","shp","asc")
  if  (!formats.exists(_==code))
    throw new IllegalArgumentException(s"Invalid format: $code")
}
  
object Csv extends OutputFormat("csv")
object Json extends OutputFormat("json")	
object Xml extends OutputFormat("xml")	
object Shapefile extends OutputFormat("shp")
object EsriAscii extends OutputFormat("asc")