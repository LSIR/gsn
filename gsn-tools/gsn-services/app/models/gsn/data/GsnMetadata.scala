package models.gsn.data

import play.api.libs.ws._
import play.api.cache._
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.libs.concurrent._
import concurrent.Future
import gsn.data._
import gsn.config.VsConf
import java.io.File

class GsnMetadata(gsnServer:String) {

  import play.api.Play.current
  implicit val context = Execution.Implicits.defaultContext

  def allSensors:Future[Map[String,Sensor]]={
    println("all over again")
    getGsnSensors
  }


  
  def getGsnSensors={
    val holder: WSRequestHolder = WS.url(gsnServer+"/rest/sensors")
    val f=holder.get().map { response =>
      (response.json \ "features" ).as[JsArray].value.map{f=>
        val s=toSensor(f)
        s.name -> s
      }.toMap
    }
    f
  }
  
  private def toSensor(jsFeature:JsValue)={
    val props=jsFeature \ "properties"
    val vsName=(props \ "vs_name").as[String]
    val fields=(props \ "fields").as[JsArray].value.map{f=>
      val unit=(f\"unit").as[String]
      var fn=(f\"name").as[String]
      if (fn=="time") fn="timed"
      Sensing(null,Output(fn,vsName,
          DataUnit(unit),
          DataType((f\"type").as[String])))
    }
    val coord= (jsFeature \ "geometry" \ "coordinates")
    val location=Location(coord(0).asOpt[Double],
        coord(1).asOpt[Double],
        coord(2).asOpt[Double])
    lazy val platform=new Platform(vsName,location)
    lazy val s:Sensor= Sensor(vsName,fields,platform,Map())
    s
  }
}