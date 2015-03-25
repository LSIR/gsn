package gsn.data

import akka.actor._
import akka.actor.Status.Failure
import gsn.config._
import gsn.security.SecurityData
import javax.sql.DataSource
import concurrent.duration._
import akka.event.Logging

object dsReg{
  val dsss=new collection.mutable.HashMap[String,StorageConf]
  
}

class SensorStore(ds:DataStore) extends Actor{
  val log = Logging(context.system, this)
  private val sec=new SecurityData(ds)
  val confWatch=context.actorOf(Props[ConfWatcher])
    
  
  val sensors=new collection.mutable.HashMap[String,Sensor]
  //val vsDatasources=new collection.mutable.HashMap[String,DataSource]
  val vsDatasources=new collection.mutable.HashMap[String,String]
  val sensorStats=new collection.mutable.HashMap[String,SensorStats]
  implicit val exe=context.dispatcher
  val refresh = context.system.scheduler.schedule(0 millis, 10 minutes, self, RefreshStats)
  object RefreshStats
 dsReg.dsss .put("gsn",ds.gsn.storageConf )
  import SensorDatabase._
  override def preStart()={
  }    
  
  override def postStop()={
    refresh cancel
  }    
  
  
  def receive ={    
    //vs config messages
    case ModifiedVsConf(vs)=>
      val vsname=vs.name.toLowerCase
      if (vs.storage.isDefined) {
        dsReg.dsss.put(vsname, vs.storage.get)
        //val d=ds.datasource(vs.storage.get.url, vs.storage.get)
        //vsDatasources.put(vsname, d.getDataSourceName)        
      }
      val hasAc=sec.hasAccessControl(vs.name)
      implicit val source=vsDatasources.get(vsname)          
      val s=Sensor.fromConf(vs,hasAc,None)
      val sStats= stats(s)
      
      sensorStats put(vsname,sStats)
      sensors.put(vsname,s)
      
    case DeletedVsConf(vs)=>
      val vsname=vs.name.toLowerCase
      if (sensors.contains(vsname)){
        sensors remove vsname
        vsDatasources remove vsname
        sensorStats remove vsname
      }
  
    //sensor request messages
    case GetAllSensorsInfo =>
      sender ! AllSensorInfo(sensors.values.map{s=>
        val vsname=s.name.toLowerCase
        SensorInfo(s,vsDatasources.get(vsname),sensorStats.get(vsname))
      }.toSeq)
    case GetSensorInfo(sensorid) =>
      val vsname=sensorid.toLowerCase
      if (!sensors.contains(vsname ))
        sender ! Failure(new IllegalArgumentException(s"Sensor id $sensorid is not valid."))
      else 
        sender ! SensorInfo(sensors(vsname),
            vsDatasources.get(vsname),sensorStats.get(vsname))
     
    //stats
    case RefreshStats=>
      log.info("Refresh stats")

      sensors.foreach{case (sensorid,s)=>
        log.info(s"Stats for sensor $sensorid")
        implicit val source=vsDatasources.get(sensorid)          
        sensorStats put(sensorid,stats(s))
      }
  }
  /*
  private def computeStats(sensor:Sensor)={
    //val sensor=sensors(sensorid)
    stats(sensor)(vsDatasources.get(sensor.name ))
    //sensorStats.put(sensorid, statistics)
  }*/
}


case class GetSensorInfo(sensorid:String)
case class GetAllSensorsInfo()
case class SensorInfo(sensor:Sensor,ds:Option[String]=None,stats:Option[SensorStats]=None)			
case class AllSensorInfo(sensors:Seq[SensorInfo])

case class GetAllSensors(latestValues:Boolean=false,timeFormat:Option[String]=None)
case class GetSensor(sensorid:String,latestValues:Boolean=false,timeFormat:Option[String]=None)
case class GetSensorData(sensorid:String,fields:Seq[String],
			conditions:Seq[String], size:Option[Int],timeFormat:Option[String])
//case class GetSensorStats(sensorid:String)			
