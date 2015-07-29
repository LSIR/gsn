package gsn.data

import play.api.libs.json.{JsNull,Json,JsString,JsValue,JsObject,JsNumber,JsArray}
import play.api.libs.json.JsArray
import java.io.StringWriter
import scala.collection.mutable.ArrayBuffer
import gsn.data.netcdf.NetCdf
import java.nio.charset.Charset
import play.api.libs.json.JsNumber

trait DataSerializer{
  def ser(s:Sensor,props:Seq[String]):Object=
    ser(SensorData(Seq(),s),props,false)
  def ser(data:SensorData,props:Seq[String],latest:Boolean):Object
  def ser(data:Seq[SensorData],props:Seq[String]=Seq(),latest:Boolean=false):Object
  //def ser(data:Seq[Sensor],props:Seq[String]=Seq(),latest:Boolean=false):Object
}

object JsonSerializer extends DataSerializer{
  
  override def ser(data:Seq[SensorData],props:Seq[String],withVals:Boolean)=
    Json.stringify(toJson(data))
  override def ser(data:SensorData,props:Seq[String],withVals:Boolean)=
    Json.stringify(toJson(data))
  
  private def toJson(data:SensorData):JsValue={
    /*val values=data.ts.map{t=>
      t.series      
    } 
    val fields=data.ts.map(_.output.fieldName)*/ 
    toJson(data.sensor,data.ts,data.stats)//values, fields)     
  }
  
  private def toJson(sensor:Sensor,ts:Seq[Series],stats:SensorStats)={            
    //val fields=sensor.fields.filter(f=>valueNames.isEmpty || valueNames.contains(f.fieldName ))
    val values=ts.map(_.series)
    val outputs= if (ts.isEmpty) sensor.fields else ts.map(_.output) 
    val fields=outputs.map{f=>
      Json.obj("name"->f.fieldName,"type"->f.dataType.name,"unit"->f.unit.code)
    }
    
    val jsValues= if (values.isEmpty) Seq()
    else {
      for (i <- values.head.indices) yield { 
        JsArray(  
            fields.indices.map {j=>valueToJson(values(j).apply(i))}
        )
      }
      //values.map{record=>valueToJson(record)}      
    }
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
    val feature=Json.obj(
        "type"->"Feature",
        "properties"->JsObject(propvals),
        "geometry"->geo
    )    
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



object NetCdfSerializer extends DataSerializer{ 

  def ser(data:Seq[SensorData],props:Seq[String],withVals:Boolean=true)={
    NetCdf.serialize(null,null)
  }
  
  override def ser(data:SensorData,props:Seq[String],latest:Boolean)= ???
  
}