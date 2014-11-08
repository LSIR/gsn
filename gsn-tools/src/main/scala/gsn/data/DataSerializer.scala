package gsn.data

import play.api.libs.json.{JsNull,Json,JsString,JsValue,JsObject,JsNumber,JsArray}
import play.api.libs.json.JsArray
import java.io.StringWriter
import scala.collection.mutable.ArrayBuffer
import gsn.data.netcdf.NetCdf

object DataSerializer {
  
  def toJsonString(sensors:Seq[Sensor])=Json.stringify(toJson(sensors))
  def toJsonString(sensor:Sensor)=Json.stringify(toJson(sensor))

  def toJson(sensors:Seq[Sensor]):JsValue=
    Json.obj("type"->"FeatureCollection",
        "features"->JsArray(sensors.map(s=>toJson(s))))
  
  
  def toJson(sensorsData:Seq[SensorData])(implicit d:DummyImplicit):JsValue=
    Json.obj("type"->"FeatureCollection",
        "features"->JsArray(sensorsData.map(s=>toJson(s))))    
  
  
  def toJson(data:SensorData):JsValue={
    val values=data.ts.map{t=>
      t.series.unzip._2       
    } 
    val fields=data.ts.map(_.output.fieldName) 
    toJson(data.s,values, fields)     
  }
  
  private def valueToJson(any:Seq[Any]):JsArray=JsArray (any.map{
    case d:Double=>JsNumber(d)
    case i:Int=>JsNumber(i)
    case l:Long=>JsNumber(l)
    case s:String=>JsString(s)
    case a:Any=>JsString(a.toString) 
    case null=>JsNull
  })
  
  private def toJson(sensor:Sensor,values:Seq[Seq[Any]]=Seq(),valueNames:Seq[String]=Seq())={
        
    val fields=sensor.fields.filter(f=>valueNames.isEmpty || valueNames.contains(f.fieldName ))
    .map{f=>
      Json.obj("name"->f.fieldName,"type"->f.dataType.name,"unit"->f.unit.code)
    }
    
    val jsValues=values.map{record=>valueToJson(record)}      
    
    val propvals=Seq("vs_name"->JsString(sensor.name),
                     "values"->JsArray(jsValues),
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
      sw.append(",\""+s.fields.map(f=>f.fieldName).mkString(",")+"\"")
      sw.append(",\""+s.fields.map(f=>f.unit.code).mkString(",")+"\"")
      sw.append(",\""+s.fields.map(f=>f.dataType.name).mkString(",")+"\"")
      sw.append(System.lineSeparator)
    }
    sw
  }
  
  
  def toCsv(sensor:Sensor,
      values:Seq[Seq[Any]]=Seq(),
      valueNames:Seq[String]=Seq()):StringWriter={
    val sw=new StringWriter
    def head(key:String,value:Any)=
      sw.append("# "+key+":"+value+System.lineSeparator)
    
    head("vs_name",sensor.name)
    sensor.properties.foreach(p=>head(p._1,p._2))
    val fields=sensor.fields.filter(f=>valueNames.isEmpty || valueNames.contains(f.fieldName ))

    head("fields",fields.map{_.fieldName}.mkString(","))
    head("units",fields.map{_.unit.code}.mkString(","))
    head("types",fields.map{_.dataType.name}.mkString(","))
        
    values.map{record=>
      sw.append(record.mkString(",")+System.lineSeparator)}
    sw  
    
  }
  
  def toNetCdf(sensor:Sensor)={
    NetCdf.serialize(sensor,null)
  }
}