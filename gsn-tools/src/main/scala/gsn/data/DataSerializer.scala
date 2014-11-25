package gsn.data

import play.api.libs.json.{JsNull,Json,JsString,JsValue,JsObject,JsNumber,JsArray}
import play.api.libs.json.JsArray
import java.io.StringWriter
import scala.collection.mutable.ArrayBuffer
import gsn.data.netcdf.NetCdf

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
    val values=data.ts.map{t=>
      t.series.unzip._2       
    } 
    val fields=data.ts.map(_.output.fieldName) 
    toJson(data.sensor,values, fields)     
  }
  
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
  
  def toJson(sensorsData:Seq[SensorData])(implicit d:DummyImplicit):JsValue=
    Json.obj("type"->"FeatureCollection",
        "features"->JsArray(sensorsData.map(s=>toJson(s))))    
  

  private def valueToJson(any:Seq[Any]):JsArray=JsArray (any.map{
    case d:Double=>JsNumber(d)
    case i:Int=>JsNumber(i)
    case l:Long=>JsNumber(l)
    case s:String=>JsString(s)
    case a:Any=>JsString(a.toString) 
    case null=>JsNull
  })

}

object CsvSerializer extends DataSerializer{
  
  override def ser(data:Seq[SensorData],props:Seq[String],latest:Boolean)=
    toCsv(data,props,latest).toString

  override def ser(data:SensorData,props:Seq[String],latest:Boolean)=
    toCsv(data,props).toString

  def toCsv(data:Seq[SensorData],props:Seq[String],latest:Boolean)={
    val sw=new StringWriter
    //val hasVals= !data.ts.isEmpty
    val valsHead=if (latest) ",values" else ""
    sw.append("#vs_name,"+props.mkString(",")+",fields,units,types"+valsHead+System.lineSeparator)
    data.foreach{s=>
      sw.append(sensorDataToCsv(s,props,latest)+System.lineSeparator())
    }
    sw
  }
  private def toString(s:Sensor,props:Seq[String])={
    val prop=props.map(p=>"\""+s.properties.getOrElse(p,"")+"\"").mkString(",")
    s"${s.name},$prop"+
    ",\""+s.fields.map(f=>f.fieldName).mkString("|")+"\""+
    ",\""+s.fields.map(f=>f.unit.code).mkString("|")+"\""+
    ",\""+s.fields.map(f=>f.dataType.name).mkString("|")+"\""    
  }
  
  def sensorDataToCsv(data:SensorData,props:Seq[String],withLatest:Boolean=false)={
    //val valueNames="latestValues("+data.latest.map(_._1.fieldName).mkString("|")+")"
    //"#vs_name,"+props.mkString(",")+",fields,units,types,"+valueNames+System.lineSeparator
    if (withLatest)
      toString(data.sensor,props)+","+data.latest.map(_._2 ._2 ).mkString("|")
    else toString(data.sensor,props)
  }
  
  def toCsv(data:SensorData,
      valueNames:Seq[String]=Seq()):StringWriter={
    val sw=new StringWriter
    def head(key:String,value:Any)=
      sw.append("# "+key+":"+value+System.lineSeparator)
    
    head("vs_name",data.sensor.name)
    data.sensor.properties.foreach(p=>head(p._1,p._2))
    val fields=data.sensor.fields.filter(f=>valueNames.isEmpty || valueNames.contains(f.fieldName ))

    head("fields",fields.map{_.fieldName}.mkString(","))
    head("units",fields.map{_.unit.code}.mkString(","))
    head("types",fields.map{_.dataType.name}.mkString(","))
    println("drimp "+data.ts.head.series.size+" "+fields.size)
    val si=data.ts.head.series.size-1
    (0 to si).foreach{i=>      
      val pp=(0 to fields.size-1).map{j=>
        data.ts(j).series (i)._2        
      }.mkString(",")
      sw.append(pp+System.lineSeparator)
      //sw.append(pp)
    }
    
    sw  
    
  }
}

object NetCdfSerializer extends DataSerializer{ 

  def ser(data:Seq[SensorData],props:Seq[String],withVals:Boolean=true)={
    NetCdf.serialize(null,null)
  }
  
  override def ser(data:SensorData,props:Seq[String],latest:Boolean)= ???
  
}