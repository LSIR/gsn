package gsn.data

import akka.actor._
import akka.actor.Status.Failure
import gsn.config._
import gsn.security.SecurityData
import javax.sql.DataSource

class SensorStore(ds:DataStore) extends Actor{

  private val sec=new SecurityData(ds)
  val confWatch=context.actorOf(Props[ConfWatcher])
  
  val sensors=new collection.mutable.HashMap[String,Sensor]
  val vsDatasources=new collection.mutable.HashMap[String,DataSource]

  override def preStart()={  
  }    
  
  def receive ={    
    case ModifiedVsConf(vs)=>
      if (vs.storage.isDefined) {
        val d=ds.datasource(vs.storage.get.url, vs.storage.get)
        vsDatasources.put(vs.name, d)        
      }
      val hasAc=sec.hasAccessControl(vs.name)
      sensors.put(vs.name,Sensor.fromConf(vs,hasAc))
    case DeletedVsConf(vs)=>
      if (sensors.contains(vs.name)){
        sensors.remove(vs.name)
        vsDatasources.remove(vs.name)
      }
    case GetAllSensorsConf =>
      sender ! AllSensorConf(sensors.values.map(s=>SensorConf(s,vsDatasources.get(s.name ))).toSeq)
    case GetSensorConf(sensorid) =>
      if (!sensors.contains(sensorid ))
        sender ! Failure(new IllegalArgumentException(s"Sensor id $sensorid is not valid."))
      else 
        sender ! SensorConf(sensors(sensorid),vsDatasources.get(sensorid))
  }
}

case class GetSensorConf(sensorid:String)
case class GetAllSensorsConf()
case class SensorConf(sensor:Sensor,ds:Option[DataSource])			
case class AllSensorConf(sensors:Seq[SensorConf])

case class GetAllSensors(latestValues:Boolean=false,timeFormat:Option[String]=None)
case class GetSensor(name:String)
case class GetSensorData(sensorid:String,fields:Seq[String],
			conditions:Seq[String], size:Option[Int],timeFormat:Option[String])
			
