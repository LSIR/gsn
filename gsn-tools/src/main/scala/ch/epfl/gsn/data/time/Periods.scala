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
* File: src/ch/epfl/gsn/data/time/Periods.scala
*
* @author Jean-Paul Calbimonte
*
*/
package ch.epfl.gsn.data.time

import org.joda.time.MonthDay
import org.joda.time.DateTime

object Periods {
  def addConditions(startTime:Long,period:String)={
   
    val split=period.split("/")
    val m1=MonthDay.parse("--"+split(0))
    val m2=MonthDay.parse("--"+split(1))
    val stYear=new DateTime(startTime).year.get()
    val endYear=DateTime.now.getYear
    (stYear to endYear).map{year=>
      s"""(timed > ${new DateTime(year,m1.getMonthOfYear,m1.getDayOfMonth,0,0).getMillis}
      && timed < ${new DateTime(year,m2.getMonthOfYear,m2.getDayOfMonth,0,0).getMillis})"""
    }.mkString(" || ")
  }
  
}