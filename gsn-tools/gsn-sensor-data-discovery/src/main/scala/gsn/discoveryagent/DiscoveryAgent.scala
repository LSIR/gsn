import com.typesafe.config._

import akka.actor.{ ActorRef, ActorSystem, Props, Actor, Inbox }
import scala.concurrent.duration._
import gsn.data.discovery.DataManager

object DiscoveryAgent extends App {
  
  val conf = ConfigFactory.load("application.conf")
  
  val answerStore = new scala.collection.mutable.ListBuffer[DiscoveryAnswer]
  
  val dataManager = new DataManager(
      conf.getString("fusekiEndpoints.properties"),
      conf.getString("fusekiEndpoints.mappings"),
      conf.getString("fusekiEndpoints.virtualSensors"),
      conf.getString("baseUri"))

  val system = ActorSystem("discoveryagent")

  val transmitter = system.actorOf(Props[DiscoveryRequestTransmitter], "transmitter")

  val requestId = 1
  val obsPropertyUri = "http://purl.oclc.org/NET/ssnx/cf/cf-property#air_temperature"
  val x1 = 88.0
  val x2 = 105.0
  val y1 = -68.0
  val y2 = 90.0
  
  // Tell the 'greeter' to change its 'greeting' message
  println("send discovery request")
  val targetGsn1 = system.actorOf(Props[DiscoveryRequestReceiver], "targetGsn1")
  targetGsn1.tell(DiscoveryRequest(requestId, obsPropertyUri, x1, y1, x2, y2), transmitter)
  
  /*val targetGsn2 = system.actorOf(Props[DiscoveryRequestReceiver], "targetGsn2")
  targetGsn2.tell(DiscoveryRequest(requestId, obsPropertyUri, x1, y1, x2, y2), transmitter)
  
  val targetGsn3 = system.actorOf(Props[DiscoveryRequestReceiver], "targetGsn3")
  targetGsn3.tell(DiscoveryRequest(requestId, obsPropertyUri, x1, y1, x2, y2), transmitter)*/
  
}

case object SendDiscoveryRequest
case class DiscoveryRequest(requestId:Integer, obsPropertyUri: String, x1:Double, y1:Double, x2:Double, y2:Double)
case object AnswerDiscoveryRequest
case class ReceiveAnswer(answer:DiscoveryAnswer)

class DiscoveryRequestReceiver extends Actor {

  def receive = {
    case DiscoveryRequest(requestId, obsPropertyUri, x1, y1, x2, y2) =>
      println(requestId + " " + obsPropertyUri + " " + x1 + " " + y1 + " " + x2 + " " + y2)
      val results = DiscoveryAgent.dataManager.getVirtualSensorsForObservedProperty(obsPropertyUri)
      .filter { case (_, _, long, lat, _) =>  long >= x1 && long <= x2 && lat >= y1 && lat <= y2 }
      
      sender ! ReceiveAnswer(DiscoveryAnswer(requestId, "GSN_1", results.map(e => (e._1,e._2))))
  }
}

class DiscoveryRequestTransmitter extends Actor {
  def receive = {
    case ReceiveAnswer(answer) => 
      println("Request Id: " + answer.requestId)
      println("GSN instance Id: " + answer.gsnInstanceId)
      answer.results.foreach(e => println(e._1 + " " + e._2))
      DiscoveryAgent.answerStore += answer
      System.exit(0)
  }
}

case class DiscoveryAnswer(requestId:Integer, gsnInstanceId:String, results:List[(String,String)])
