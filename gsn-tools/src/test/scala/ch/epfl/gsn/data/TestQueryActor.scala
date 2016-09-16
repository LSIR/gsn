package ch.epfl.gsn.data

import akka.testkit._
import ch.epfl.gsn.data.QueryActor;
import akka.actor._
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfterAll
import scala.concurrent.Promise
import org.scalatest.Matchers
import org.scalatest.WordSpecLike

class TestQueryActor   //TestKit(ActorSystem("test") )
  // BeforeAndAfterAll
   extends TestKit(ActorSystem("test")) with ImplicitSender
with WordSpecLike with Matchers with BeforeAndAfterAll {


  override def afterAll() { system.shutdown() }
  
  "MyTrait is called when triggered" must {
    
    "get " in{
  //  val x = TestProbe()
    val p=Promise[Seq[SensorData]]
    val a= TestActorRef(new QueryActor(p))
    a ! GetAllSensors
    implicit val ec=system.dispatcher
    p.future.onComplete{f=>
      f.get.size shouldBe(3)
      
    }
    
    
  }}
}