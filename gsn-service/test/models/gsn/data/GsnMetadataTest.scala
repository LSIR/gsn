package models.gsn.data

import com.typesafe.config.ConfigFactory
import org.scalatestplus.play._
import play.test.FakeApplication
import scala.concurrent.Await
import concurrent.duration._

class GsnMetadataTest  extends PlaySpec with OneAppPerSuite  {
   

  "gsn metadata" must{
    lazy val conf=ConfigFactory.load
    lazy val gsnServer=conf.getString("gsn.server.url")
    import concurrent.ExecutionContext.Implicits.global
    val gsn=new GsnMetadata("http://montblanc.slf.ch:22001")
    "get all sensors" in{
      println("pipocas")
      val t=Await.result(gsn.getGsnSensors,30 seconds)
      println("pipocas2 "+t.size)

    }
  }


}