package gsn.data

import akka.actor._
import akka.actor.Status.Failure
import gsn.config._
import gsn.security.SecurityData
import javax.sql.DataSource
import concurrent.duration._
import akka.event.Logging
import reactivemongo.api.MongoDriver
import reactivemongo.bson.BSONDocument
import scala.util.{Failure=>ScalaFailure}
import scala.util.Success
import reactivemongo.bson.BSON
import gsn.data.discovery.MetadataManager

object dsReg{
  val dsss=new collection.mutable.HashMap[String,StorageConf]
  
}

class SensorStore(ds:DataStore) extends Actor{
  val log = Logging(context.system, this)
  private val sec=new SecurityData(ds)
  val confWatch=context.actorOf(Props[ConfWatcher])
    
  val driver = new MongoDriver(context.system)
  val connection = driver.connection(List("localhost"))
  
  val propertiesEndpoint = GsnConf.defaultFuseki.getString("propertiesEndpoint")
  val mappingsEndpoint = GsnConf.defaultFuseki.getString("mappingsEndpoint")
  val virtualSensorsEndpoint = GsnConf.defaultFuseki.getString("virtualSensorsEndpoint")
  val baseRdfUri = GsnConf.defaultFuseki.getString("baseRdfUri")
  val metadataManager = new MetadataManager(propertiesEndpoint,
      mappingsEndpoint,virtualSensorsEndpoint, baseRdfUri)
  
  val sensors=new collection.mutable.HashMap[String,Sensor]
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
  
  private def canonicalName(name:String)=
    name.toLowerCase.replaceAll(" ", "")
  
  def receive ={    
    //vs config messages
    case ModifiedVsConf(vs)=>
      val vsname=canonicalName(vs.name)
      if (vs.storage.isDefined) {
        dsReg.dsss.put(vsname, vs.storage.get)
        val d=ds.datasource(vs.storage.get.url, vs.storage.get)
        vsDatasources.put(vsname, d.getDataSourceName)        
      }
      val hasAc=sec.hasAccessControl(vs.name)
      implicit val source=vsDatasources.get(vsname)

      val outputs = metadataManager.getOutputsForSensor(vsname)
      val outputsWithUnits = addUnitsToOutputs(vs, outputs)
      
      val s=Sensor.fromConf(vs,hasAc,None,Option(outputsWithUnits))
      val sStats= stats(s)
      //storeMongo(s)
      sensorStats put(vsname,sStats)
      sensors put(vsname,s)
      
    case DeletedVsConf(vs)=>
      val vsname=canonicalName(vs.name)
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

  def storeMongo(s:Sensor)={
    val db = connection.db("gsn-metadata")
    val collection = db.collection("sensors")
    
    import gsn.data.bson.BsonConverter._
    val doco=BSON.write(s)
    
    val future = collection.insert(doco)

    future.onComplete {
      case ScalaFailure(e) => throw e
      case Success(lastError) => {
      //println("successfully inserted document with lastError = " + lastError)
      }
    }
  }
  
  /**
   * Fetch standardized units and add them to the outputs
   */
  private def addUnitsToOutputs(vs:VsConf, outputs:Map[String, List[(String,String)]]):Map[String, List[(String,String)]] = {
    val outputsWithUnits = scala.collection.mutable.Map(outputs.toSeq: _*)
    vs.processing.output map{out=> 
      val (unitStdUri,unitStd) = metadataManager.getUnitBySymbol(out.unit.getOrElse(null))
      if (!unitStdUri.equals("") && !unitStd.equals("")) {
        val unitStdUriTuple = ("unitStdUri", unitStdUri)
        val unitStdTuple = ("unitStd",unitStd)
        if (outputsWithUnits.contains(out.name)) {
          val list = outputs.get(out.name)
          val updatedList:List[(String,String)] = list match {
            case Some(l) => l++List(unitStdUriTuple, unitStdTuple)
            case None => List(unitStdUriTuple, unitStdTuple)
          }
          outputsWithUnits.put(out.name, updatedList)
        } else {
          outputsWithUnits.put(out.name, List(unitStdUriTuple, unitStdTuple))
        }
      }
    }
    outputsWithUnits.toMap
  }
}


case class GetSensorInfo(sensorid:String)
case class GetAllSensorsInfo()
case class SensorInfo(sensor:Sensor,ds:Option[String]=None,stats:Option[SensorStats]=None)			
case class AllSensorInfo(sensors:Seq[SensorInfo])

case class GetAllSensors(latestValues:Boolean=false,timeFormat:Option[String]=None)
case class GetSensor(sensorid:String,latestValues:Boolean=false,timeFormat:Option[String]=None)
case class GetSensorData(sensorid:String,fields:Seq[String],
			conditions:Seq[String], size:Option[Int],timeFormat:Option[String])
