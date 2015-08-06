package gsn.data.format

import gsn.data._
import org.slf4j.LoggerFactory
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.JsArray
import play.api.libs.json.JsNumber
import play.api.libs.json.JsString
import play.api.libs.json.JsNull
import play.api.libs.json.JsObject

object JsonSerializer extends DataSerializer{
  val logger=LoggerFactory.getLogger(JsonSerializer.getClass)
  override def ser(data:Seq[SensorData],props:Seq[String],withVals:Boolean)=
    //Json.stringify(toJson(data))    
    toJson(data)
  override def ser(data:SensorData,props:Seq[String],withVals:Boolean)=
    //Json.stringify(toJson(data))
    toJson(data)
  
  private def toJson(data:SensorData):JsValue={
    toJson(data.sensor,data.ts,data.stats)   
  }
  
  private def indices(seq: Seq[Any])={
    seq.take(dataLimit).indices
  }
  
  private def toJson(sensor:Sensor,ts:Seq[Series],stats:SensorStats)={            
    
    val values=ts.map(_.series)
    val outputs= if (ts.isEmpty) sensor.fields else ts.map(_.output) 
    val fields=outputs.map{f=>
      Json.obj("name"->f.fieldName,"type"->f.dataType.name,"unit"->f.unit.code)
    }
    logger.debug("set up ready")
    val jsValues= 
      if (values.isEmpty) Seq()
      else {
        for (i <- indices(values.head)) yield { 
          JsArray(  
            fields.indices.map {j=>valueToJson(values(j).apply(i))}
          )
        }
      }
    logger.debug("values ready")
    
    val st=Json.obj("start-datetime"->valueToJson(stats.start.getOrElse(null)),
                    "end-datetime"->valueToJson(stats.end.getOrElse(null)))
    
    val propvals=Seq("vs_name"->JsString(sensor.name),
                     "values"->JsArray(jsValues),
                     "fields"->JsArray(fields),
                     "stats"->st)++
      sensor.properties.map(a=>a._1->JsString(a._2)).toSeq
      
    val geo= 
      if (sensor.location.latitude.isDefined && sensor.location .longitude.isDefined)
        Json.obj("type"->"Point",
                 "coordinates"->Json.arr(sensor.location.longitude,
                                         sensor.location.latitude,
                                         Some(sensor.location.altitude.getOrElse(0.0))))
      else JsNull             
    
    logger.debug("properties ready")
    val total =if (!values.isEmpty) values.head.size else 0
    
    val feature=Json.obj(
        "type"->"Feature",
        "properties"->JsObject(propvals),
        "geometry"->geo,
        "total_size"->total,
        "page_size"->jsValues.size
    )    
    logger.debug("finished sensor list")

    feature
  }
  
  def toJson(sensorsData:Seq[SensorData])(implicit d:DummyImplicit):JsValue=
    Json.obj("type"->"FeatureCollection",
        "features"->JsArray(sensorsData.map(s=>toJson(s))))    
  

  private def valueToJson(any:Any):JsValue=any match{       
    case d:Double=>JsNumber(d)
    case i:Int=>JsNumber(i)
    case l:Long=>JsNumber(l)
    case s:String=>JsString(s)
    case a:Any=>JsString(a.toString) 
    case null=>JsNull
  }

}

