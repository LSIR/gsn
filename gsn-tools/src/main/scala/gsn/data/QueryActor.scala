package gsn.data

import scala.concurrent.Promise
import akka.actor._
import akka.actor.Status.Failure
import akka.event.Logging

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
      gsnSensor ! GetSensorInfo(g.sensorid )
      sensor.future.map{s=>
        if (g.size.isDefined && g.size.get== 0)
          p.success(Seq(asSensorData(s.sensor )))
        else try
          p.success(Seq(query(s,g.fields ,g.conditions ,g.size,g.timeFormat )))          
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
