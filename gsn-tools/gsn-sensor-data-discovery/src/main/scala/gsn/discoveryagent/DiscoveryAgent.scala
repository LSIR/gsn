package gsn.discoveryagent

import com.typesafe.config._

import scala.Nothing

import scala.util.{Success, Failure, Try}

import akka.pattern.{ ask }
import scala.concurrent._

import scala.concurrent.duration._

import gsn.data.discovery.util.XmlSerializer

import scala.concurrent.{ ExecutionContext }
import akka.actor.{ ActorRef, ActorSystem, Props, Actor }
import scala.concurrent.duration._
import gsn.data.discovery.DataManager

object DiscoveryAgent extends App {
  
  val conf = ConfigFactory.load("application.conf")
  
  // Get config from GSN to access the port used and the hostname (if there is one ?)
  
  val akkaConfig = ConfigFactory.load("remote-configuration.conf")
    
  val dataManager = new DataManager(
      conf.getString("fusekiEndpoints.properties"),
      conf.getString("fusekiEndpoints.mappings"),
      conf.getString("fusekiEndpoints.virtualSensors"),
      conf.getString("baseUri"))

  val system = ActorSystem("discoveryagent", akkaConfig)

  val requestId = 1
  val obsPropertyUri = "http://purl.oclc.org/NET/ssnx/cf/cf-property#air_temperature"
  val x1 = 88.0
  val x2 = 105.0
  val y1 = -68.0
  val y2 = 90.0
  val requestToSend = DiscoveryRequest(obsPropertyUri, x1, y1, x2, y2)
  val intancesToQuery = List(("GSN_2","127.0.0.1:5151"), ("GSN_3","127.0.0.1:5152"))
     
  val requestExecutor = system.actorOf(Props[RequestExecutor], "requestExecutor")
  requestExecutor.tell(SendDiscoveryRequest(requestToSend, intancesToQuery), ActorRef.noSender)
}

case class CreateNewVirtualSensor(data:List[DiscoveryAnswer])
case class SendDiscoveryRequest(request:DiscoveryRequest, targets:List[(String, String)])
case class DiscoveryRequest(obsPropertyUri: String, x1:Double, y1:Double, x2:Double, y2:Double)
case object AnswerDiscoveryRequest
case class ReceiveAnswer(answer:DiscoveryAnswer)
case class DiscoveryAnswer(gsnInstanceId:String, results:List[VsResult])
case class VsResult(
    host:String,
    port:Integer,
    vsName:String, 
    columnName:String, 
    longitude:Option[String], 
    latitude:Option[String], 
    altitude:Option[String])

class RequestExecutor extends Actor {

  import context.dispatcher
  
  private def futureToFutureTry[T](f: Future[T]): Future[Try[T]] = { f map {Success(_)} recover({case e => Failure(e)}) } 
  
  def receive = {
            
    case SendDiscoveryRequest(request, targets) =>
     val listOfFutures = targets.map(f => 
     akka.pattern.ask(context.actorSelection("akka.tcp://discoveryagent@" + f._2 + "/user/requestReceiver"))
     .ask(request)(20000)
     .mapTo[DiscoveryAnswer])
     
     val listOfFutureTries = listOfFutures.map(futureToFutureTry(_))
     
     val futureListOfSuccesses = listOfFutureTries.map(_.collect{case Success(x) => x})
     
//     val futureListOfFailures = listOfFutureTries.map(_.collect{ case Failure(x) => x})
     
     val futureList = Future.sequence(futureListOfSuccesses)
     
     futureList onComplete { 
        case Success(result) => 
          val vsCreator = context.actorOf(Props[VirtualSensorCreator])
          vsCreator ! CreateNewVirtualSensor(result)
        case Failure(failure) => System.err.println("DiscoveryRequest failure !") //TODO: Something else, more meaningful
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
      
      // Create VSD file
      XmlSerializer.serializeToFile(data.flatMap { x => x.results })
      
      // Create mapping for column name <-> observed property
      //TODO
  }
}

/**
 * Receive from remote DiscoveryRequestTransmitter and respond to remote DiscoveryAnswerReceiver
 */
class DiscoveryRequestReceiver extends Actor {

  def receive = {
    case DiscoveryRequest(obsPropertyUri, x1, y1, x2, y2) =>
      println("Hi ! I'm DiscoveryRequestReceiver and I've just received a request !")
      println(obsPropertyUri + " " + x1 + " " + y1 + " " + x2 + " " + y2)
      val results = DiscoveryAgent.dataManager.getVirtualSensorsForObservedProperty(obsPropertyUri)
      .filter ({ case (_, _, long, lat, _) =>  
        if (long.isDefined && lat.isDefined) 
          long.get.toDouble >= x1 && long.get.toDouble <= x2 && 
          lat.get.toDouble >= y1 && lat.get.toDouble <= y2 
        else true
      })
      .map({ case (vsname, columnName, longitude, latitude, altitude) => 
        new VsResult("osper.epfl.ch", 22001, vsname, columnName, longitude, latitude, altitude)}) //Hard coded value to replace by values from GSN config file 
      
      sender ! DiscoveryAnswer("GSN_1", results)
  }
}