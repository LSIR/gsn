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
* File: src/ch/epfl/gsn/data/format/TimeFormat.scala
*
* @author Jean-Paul Calbimonte
*
*/
package ch.epfl.gsn.data.format

import org.joda.time.format.ISODateTimeFormat
import org.joda.time.format.DateTimeFormat
import scala.util.Try
import org.joda.time.format.DateTimeFormatter

object TimeFormats {
  def formatTime(t:Long)(implicit timeFormat:Option[String])=timeFormat match{
    case Some("unixTime") | None => t
    case _ => getTimeFormat(timeFormat).withZoneUTC().print(t)
  }

  def formatTime(t:Long, timeFormat:DateTimeFormatter)=
    timeFormat.print(t)

  private def getTimeFormat(tf:Option[String])={// e.g. yyyyMMdd    
    tf match {
      case Some("iso8601") => ISODateTimeFormat.dateTimeNoMillis()
      case Some("iso8601ms") => ISODateTimeFormat.dateTime()
      
      case Some(f) => 
        Try(DateTimeFormat.forPattern(f))              
        .recover{case e =>
          println("error here "+e)
          throw new IllegalArgumentException(s"Invalid time format: $f ${e.getMessage}")}.get 
      case None => ISODateTimeFormat.dateHourMinuteSecond()
    }
  }
}