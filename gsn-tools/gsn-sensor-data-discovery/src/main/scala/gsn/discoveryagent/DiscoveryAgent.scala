package gsn.discoveryagent

import com.typesafe.config._

import scala.Nothing

import scala.util.{Success, Failure, Try}

import akka.pattern.{ ask }
import scala.concurrent._

import scala.concurrent.duration._

import gsn.discoveryagent.util.VsXmlSerializer

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

  val obsPropertyUri = "http://purl.oclc.org/NET/ssnx/cf/cf-property#air_temperature"
  val location = (88.0, -68.0, 105.0, 90.0)
  val requestToSend = DiscoveryRequest(obsPropertyUri, Option(location))
  val intancesToQuery = List(("GSN_2","osper.epfl.ch:5150")/*, ("GSN_3","127.0.0.1:5152")*/)
     
  val requestExecutor = system.actorOf(Props[RequestExecutor], "requestExecutor")
  requestExecutor.tell(SendDiscoveryRequest(requestToSend, intancesToQuery), ActorRef.noSender)
  
  val vsCreator = system.actorOf(Props[VirtualSensorCreator], "virtualSensorCreator")
}

case object AnswerDiscoveryRequest
case class CreateNewVirtualSensor(data:List[DiscoveryAnswer])
case class SendDiscoveryRequest(request:DiscoveryRequest, targets:List[(String, String)])
case class DiscoveryRequest(obsPropertyUri: String, location:Option[(Double,Double,Double,Double)])
case class ReceiveAnswer(answer:DiscoveryAnswer)
case class DiscoveryAnswer(gsnInstanceId:String, obsPropertyUri: String, results:List[VsResult])
case class VsResult(
    gsnInstance:String,
    host:String,
    port:Integer,
    vsName:String, 
    columnName:String, 
    longitude:Option[String], 
    latitude:Option[String], 
    altitude:Option[String])

class RequestExecutor extends Actor {

  import context.dispatcher
  
  val requestTimeout = 10000 // Number of seconds a GSN instance has to reply before it is considered a failure
  
  private def futureToFutureTry[T](f: Future[T]): Future[Try[T]] = { f map {Success(_)} recover {case e => Failure(e)} } 
  
  def receive = {
            
    case SendDiscoveryRequest(request, targets) =>
     val listOfFutures = targets.map(f => 
     akka.pattern.ask(context.actorSelection("akka.tcp://discoveryagent@" + f._2 + "/user/requestReceiver"))
     .ask(request)(requestTimeout)
     .mapTo[DiscoveryAnswer])
     
     val listOfFuturesTry = listOfFutures.map(futureToFutureTry(_))
     val futureListOfTry = Future.sequence(listOfFuturesTry)
     val futureListOfSuccesses = futureListOfTry.map(_.collect{ case Success(x) => x })
     val futureListOfFailures = futureListOfTry.map(_.collect{ case Failure(x) => x })
     
     futureListOfSuccesses onComplete { 
        case Success(result) => 
          val vsCreator = context.actorOf(Props[VirtualSensorCreator])
          if (result.size > 0) vsCreator ! CreateNewVirtualSensor(result)
        case Failure(failure) => // possible scenario ?
     }
     
     futureListOfFailures.onComplete { 
       case Success(_) => // possible scenario ?
       case Failure(failures) => {
         System.err.println("DiscoveryRequest failure !") //TODO: Something else, more meaningful. How to get some details about which req failed ?
       }
     }
  }
}

class VirtualSensorCreator extends Actor {
  def receive = {
    case CreateNewVirtualSensor(data) =>
      
      //TMP
      data.flatMap { x => x.results } foreach { r => 
        println(r.vsName+" "+r.columnName+" "+r.host+" "+r.port)
      }
      
      val vsResults = data.flatMap { x => x.results }
      
      if (vsResults.size > 0) {
        
        val vsName = ""
        val columnName = "aggregated_" + data.head.obsPropertyUri
        
        // Create VSD file
        val vsdFile = "../virtual-sensors/" + vsName + ".xml" //TODO: Find a way to generate a unique name
        VsXmlSerializer.serializeToFile(vsName, columnName, vsResults, vsdFile)
        
        // Create mapping for column name <-> observed property
        data.foreach { d => 
          DiscoveryAgent.metadataMagager.addNewMapping(columnName, d.obsPropertyUri)    
        }
        
        // Add the virtual-sensor to the metadata store
        DiscoveryAgent.metadataMagager.addVirtualSensorFromXML(new File(vsdFile))
      }
  }
}

/**
 * Receive from remote DiscoveryRequestTransmitter and respond to remote DiscoveryAnswerReceiver
 */
class DiscoveryRequestReceiver extends Actor {
  
  //TODO Get config from GSN to access the port used and the hostname (if there is one ?)
  val gsnInstance = "GSN_1"
  val host = "laptop.michael" 
  val port = 22001

  def receive = {
    case DiscoveryRequest(obsPropertyUri, location) =>
      val results = 
        if (location.isDefined) {
          val x1 = location.get._1
          val y1 = location.get._2
          val x2 = location.get._3
          val y2 = location.get._4
          DiscoveryAgent.metadataMagager.getVirtualSensorsForObservedProperty(obsPropertyUri)
          .filter ({ case (_, _, long, lat, _) =>
            if (long.isDefined && lat.isDefined) 
              long.get.toDouble >= x1 && long.get.toDouble <= x2 && 
              lat.get.toDouble >= y1 && lat.get.toDouble <= y2 
            else false
          })
        } else {
          DiscoveryAgent.metadataMagager.getVirtualSensorsForObservedProperty(obsPropertyUri)
        }
      
      //Hard coded value to replace by values from GSN config file
      val vsResults = results.map({ case (vsname, columnName, longitude, latitude, altitude) => 
        new VsResult(gsnInstance, host, port, vsname, columnName, longitude, latitude, altitude)}) 
        
      sender ! DiscoveryAnswer(gsnInstance, obsPropertyUri, vsResults)
  }
}