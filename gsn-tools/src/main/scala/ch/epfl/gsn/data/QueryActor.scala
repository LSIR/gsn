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
* File: src/ch/epfl/gsn/data/QueryActor.scala
*
* @author Jean-Paul Calbimonte
*
*/
package ch.epfl.gsn.data

import scala.concurrent.Promise
import akka.actor._
import akka.actor.Status.Failure
import akka.event.Logging
import ch.epfl.gsn.data.time.Periods
import scala.collection.mutable.ArrayBuffer

class QueryActor(p:Promise[Seq[SensorData]]) extends Actor {
  val gsnSensor=context.actorSelection("/user/gsnSensorStore")
  val sensors=Promise[Seq[SensorInfo]]
  val sensor=Promise[SensorInfo]
  val log = Logging(context.system, this)
  import SensorDatabase._
  implicit val exe=context.system.dispatcher
  def receive= {
    case GetAllSensors(latest,timeFormat) => 
      gsnSensor ! GetAllSensorsInfo  
      sensors.future.map{ s=>
        if (latest)
          p.success(s.map(sc=>sensorInfoToData(sc)))
        else
          p.success(s.map(sc=>sensorInfoToData(sc,false) ))
        context stop self
      }
    case GetSensor(vsName,latest,timeformat) =>
      gsnSensor ! GetSensorInfo(vsName)
      sensor.future map{s=>
        if (latest)
          p success Seq(sensorInfoToData(s)) 
        else
          p success Seq(sensorInfoToData(s,false))
        context stop self
      }
    case g:GetSensorData =>
      log.debug(s"get sensor data from ${g.sensorid}")
      val conds=new ArrayBuffer[String]
      conds++=g.conditions 
      gsnSensor ! GetSensorInfo(g.sensorid )
      sensor.future.map{s=>
        g.period.map{p=>
          conds+=Periods.addConditions(s.stats.get.start.get, p)
        }
        try{
          if (g.agg.isEmpty)
            p.success(Seq(query(s,g.fields ,conds ,g.size,g.timeFormat )))
          else
            p.success(Seq(aggregationQuery(s,g.fields ,conds ,g.size,g.timeFormat,g.agg.get.aggFunction,g.agg.get.aggPeriod)))
        }                    
        catch {
          case e:Exception=>p.failure(e)
            log.error(e.getMessage)
        }                   
        context stop self
      }
    case g:GetGridData =>
      log.debug(s"get grid data from ${g.sensorid}")
      gsnSensor ! GetSensorInfo(g.sensorid )
      sensor.future.map{s=>
        try
          p.success(Seq(queryGrid(s,g.conditions ,g.size,g.timeFormat,g.boundingBox,g.aggregation,g.asTimeSeries)))          
        catch {
          case e:Exception=>p.failure(e)
            log.error(e.getMessage)
        }                   
        context stop self
      }
    case all:AllSensorInfo =>
      sensors.success(all.sensors)
    case s:SensorInfo =>
      sensor.success(s)
    case f:Failure=>
      p.failure(f.cause)
  }
  
  private def sensorInfoToData(si:SensorInfo,latest:Boolean=true)={
    val latestValues=
      if (latest) si.stats.get.latestValues
      else Seq()
    SensorData(latestValues,si.sensor,si.stats.getOrElse(EmptyStats) ) 
  }
  
  
}
