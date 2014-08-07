package gsn.data

import play.api.libs.json.{JsNull,Json,JsString,JsValue,JsObject,JsNumber,JsArray}
import play.api.libs.json.JsArray
import java.io.StringWriter
import scala.collection.mutable.ArrayBuffer
import gsn.data.netcdf.NetCdf

object DataSerializer {
  
  def toJsonString(sensors:Seq[Sensor])=Json.stringify(sensorsToJson(sensors))
  def toJsonString(sensor:Sensor)=Json.stringify(toJson(sensor))

  def sensorsToJson(sensors:Seq[Sensor])={
    Json.obj("type"->"FeatureCollection",
        "features"->JsArray(sensors.map(s=>toJson(s))))
  }  
  def toJson(sensor:Sensor)={
    
    val fields=sensor.fields.map{f=>
      Json.obj("name"->f.name,"type"->f.datatype.name,"unit"->f.unit.code)
    }
    
    val values=sensor.values.map{record=>
      val data:Seq[JsValue]=record.map{
        case d:Double=>JsNumber(d)
        case i:Int=>JsNumber(i)
        case l:Long=>JsNumber(l)
        case s:String=>JsString(s)
        case a:Any=>JsString(a.toString) 
        case null=>JsNull
      }.toSeq
      JsArray(data)
    }
    val propvals=Seq("vs_name"->JsString(sensor.name),
                     "values"->JsArray(values),
                     "fields"->JsArray(fields))++
      sensor.properties.map(a=>a._1->JsString(a._2)).toSeq
      
    val feature=Json.obj(
        "type"->"Feature",
        "properties"->JsObject(propvals),
        "geometry"->Json.obj("type"->"Point",
                             "coordinates"->Json.arr(sensor.location.latitude,
                                                     sensor.location.longitude,
                                                     sensor.location.altitude))
    )
    
    feature
  }

  
  def toCsv(sensors:Seq[Sensor],props:Seq[String])={
    val sw=new StringWriter
    sw.append("#vs_name,"+props.mkString(",")+",fields,units,types"+System.lineSeparator)
    sensors.foreach{s=>
      sw.append(s.name+","+props.map(p=>s.properties.getOrElse(p,"")).mkString(","))
      sw.append(",\""+s.fields.map(f=>f.name).mkString(",")+"\"")
      sw.append(",\""+s.fields.map(f=>f.unit.code).mkString(",")+"\"")
      sw.append(",\""+s.fields.map(f=>f.datatype.name).mkString(",")+"\"")
      sw.append(System.lineSeparator)
    }
    sw
  }
  
  
  def toCsv(sensor:Sensor)={
    val sw=new StringWriter
    def head(key:String,value:Any)=sw.append("# "+key+":"+value+System.lineSeparator)
    
    head("vs_name",sensor.name)
    sensor.properties.foreach(p=>head(p._1,p._2))
    head("fields",sensor.fields.map{_.name}.mkString(","))
    head("units",sensor.fields.map{_.unit.code}.mkString(","))
    head("types",sensor.fields.map{_.datatype.name}.mkString(","))
        
    val values=sensor.values.map{record=>
      sw.append(record.mkString(",")+System.lineSeparator)}
    sw  
    
  }
  
  def toNetCdf(sensor:Sensor)={
    NetCdf.serialize(sensor)
  }
}