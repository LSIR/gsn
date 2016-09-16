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
* File: src/ch/epfl/gsn/data/Observation.scala
*
* @author Jean-Paul Calbimonte
*
*/
package ch.epfl.gsn.data

import java.util.Date

case class Observation(value:ObservationValue,time:Date,sensor:Sensor,
    obsProperty:String,feature:String) 

case class ObservationValue(value:Any,dataType:DataType)

case class Series(output:Output,series:Seq[Any]){
  def asDoubles=series.iterator.map(d =>d.asInstanceOf[Double])
  def asLongs=series.iterator.map(d =>d.asInstanceOf[Long])
}

class TimeSeries(output:Output,series:Seq[Any], val time:Seq[Long]) extends Series(output,series){
  val iterator=(asDoubles zip time.iterator)
}

class NumericalTimeSeries(output:Output,series:Seq[Double]) extends Series(output,series)

case class SensorData(ts:Seq[Series],sensor:Sensor,stats:SensorStats=EmptyStats){
  lazy val latest=
    if (ts.isEmpty || ts.head.series.isEmpty ) Seq()
    else {
      ts.map{ t=>(t.output ,t.series.last) }
    }
}

class GridData(s:Sensor,stats:SensorStats,grid:Array[Array[Double]]) extends SensorData(Seq(),s,stats){
  
}


