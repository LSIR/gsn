package gsn.discoveryagent

import com.typesafe.config._


import scala.Nothing

import scala.util.{Success, Failure, Try}

import akka.pattern.{ ask }
import scala.concurrent._

import scala.concurrent.duration._

import gsn.discoveryagent.util.VsXmlSerializer
import gsn.discoveryagent.util.DataDiscoveryRequestParser

import scala.concurrent.{ ExecutionContext }
import akka.actor.{ ActorRef, ActorSystem, Props, Actor }
import scala.concurrent.duration._
import gsn.data.discovery.MetadataManager
import java.io.File

object DiscoveryAgent extends App {
  
  val conf = ConfigFactory.load("application.conf")
  
  val akkaConfig = ConfigFactory.load("actor-configuration.conf")
    
  val metadataMagager = new MetadataManager(
      conf.getString("fusekiEndpoints.properties"),
      conf.getString("fusekiEndpoints.mappings"),
      conf.getString("fusekiEndpoints.virtualSensors"),
      conf.getString("baseUri"))

  val system = ActorSystem("discoveryagent", akkaConfig)
  
  val dataDiscoveryRequestSender = system.actorOf(Props[DataDiscoveryRequestSender], "dataDiscoverySender")
  val dataDiscoveryRequestReceiver = system.actorOf(Props[DataDiscoveryRequestReceiver], "dataDiscoveryReceiver")
  val vsCreator = system.actorOf(Props[VirtualSensorCreator], "virtualSensorCreator")
  
  // Execute Data Discovery Requests found in ddrFolder
  val ddrFolder = new File(conf.getString("ddrFolder"))
  ddrFolder.listFiles().toList filter { f => f.getName.endsWith(".xml") } foreach { file => 
    val (dataDiscoveryRequest, targets) = DataDiscoveryRequestParser.parse(file)
    dataDiscoveryRequestSender.tell(SendDataDiscoveryRequest(dataDiscoveryRequest, targets), ActorRef.noSender)
  }
}

case class Target(id:String, host:String, port:Integer)
case class Point(latitude:Double, longitude:Double)
case class Location(leftDownCorner:Point, rightUpCorner:Point) {
  // Checks if the rectangular area contains the point
  def contains(point:Point):Boolean = {
        point.latitude >= leftDownCorner.latitude && 
        point.latitude <= rightUpCorner.latitude &&
        point.longitude >= leftDownCorner.longitude && 
        point.longitude <= rightUpCorner.longitude
  }
}

case class CreateNewVirtualSensor(requestId:Integer, data:List[DataDiscoveryAnswer])
case class SendDataDiscoveryRequest(request:DataDiscoveryRequest, targets:List[Target])
case class DataDiscoveryRequest(requestId:Integer,obsPropertyUri: String, location:Option[Location])
case class ReceiveAnswer(answer:DataDiscoveryAnswer)
case class DataDiscoveryAnswer(gsnId:String, obsPropertyUri: String, results:List[VsResult])
case class VsResult(
    gsnId:String,
    host:String,
    port:Integer,
    vsName:String, 
    fieldName:String, 
    longitude:Option[String], 
    latitude:Option[String], 
    altitude:Option[String])

class DataDiscoveryRequestSender extends Actor {

  import context.dispatcher
  
  // Number of miliseconds a GSN instance has to reply before it is considered a failure
  val requestTimeout = 10000 
  
  private def futureToFutureTry[T](f: Future[T]): Future[Try[T]] = { f map {Success(_)} recover {case e => Failure(e)} } 
  
  def receive = {
            
    case SendDataDiscoveryRequest(request, targets) =>
     val listOfFutures = targets.map(target => 
     akka.pattern.ask(context.actorSelection("akka.tcp://discoveryagent@" + target.host + ":" + target.port + "/user/dataDiscoveryReceiver"))
     .ask(request)(requestTimeout)
     .mapTo[DataDiscoveryAnswer])
     
     val listOfFuturesTry = listOfFutures.map(futureToFutureTry(_))
     val futureListOfTry = Future.sequence(listOfFuturesTry)
     val futureListOfSuccesses = futureListOfTry.map(_.collect{ case Success(x) => x })
     val futureListOfFailures = futureListOfTry.map(_.collect{ case Failure(x) => x })
     
     futureListOfSuccesses onComplete { 
        case Success(result) => 
          val vsCreator = context.actorOf(Props[VirtualSensorCreator])
          if (result.size > 0) vsCreator ! CreateNewVirtualSensor(request.requestId, result)
        case Failure(failure) => //TODO: is this a possible scenario ?
     }
     
     futureListOfFailures.onComplete { 
       case Success(_) => // possible scenario ?
       case Failure(failures) => {
         //TODO: Something else, more meaningful. How to get some details about which req failed ?
         System.err.println("DiscoveryRequest failure !") 
       }
     }
  }
}

class VirtualSensorCreator extends Actor {
  def receive = {
    case CreateNewVirtualSensor(requestId, discoveryAnswers) =>
      
      val obsPropertyUri = discoveryAnswers.head.obsPropertyUri
      
      // Merge all the results from all the instances
      val vsResults = discoveryAnswers.flatMap { x => x.results }
      
      if (vsResults.size > 0) {
      
        val vsName = "remote_vs_" + requestId
        val fieldName = "aggregated_data"
        
        // Create VSD file
        val vsdFile = "../virtual-sensors/" + vsName + ".xml"
        VsXmlSerializer.serializeToFile(vsName, fieldName, vsResults, vsdFile)
        
        // Create mapping for database column name <-> observed property
        DiscoveryAgent.metadataMagager.addNewOutput(fieldName, obsPropertyUri)    
        
        // Add the virtual-sensor to the metadata store
        DiscoveryAgent.metadataMagager.addVirtualSensorFromXML(new File(vsdFile))
      }
  }
}

/**
 * Receives from remote DataDiscoveryRequestSender
 */
class DataDiscoveryRequestReceiver extends Actor {
  
  val gsnId = DiscoveryAgent.conf.getString("gsnId")
  val host = DiscoveryAgent.conf.getString("host")
  val port = Integer.valueOf(DiscoveryAgent.conf.getString("port"))

  def receive = {
    case DataDiscoveryRequest(requestId, obsPropertyUri, location) =>
      
      val results = {
        if (location.isDefined) {
          DiscoveryAgent.metadataMagager.getVirtualSensorsForObservedProperty(obsPropertyUri)
          .filter ({ case (_, _, long, lat, _) =>
            if (long.isDefined && lat.isDefined) {
              location.get.contains(new Point(lat.get.toDouble, long.get.toDouble))
            } else false
          })
        } else {
          DiscoveryAgent.metadataMagager.getVirtualSensorsForObservedProperty(obsPropertyUri)
        }
      }
      
      val vsResults = results.map({ case (vsname, columnName, longitude, latitude, altitude) => 
        new VsResult(gsnId, host, port, vsname, columnName, longitude, latitude, altitude)})
        
      sender ! DataDiscoveryAnswer(gsnId, obsPropertyUri, vsResults)
  }
}