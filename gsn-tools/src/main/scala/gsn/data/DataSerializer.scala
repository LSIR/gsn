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
  
  private def toJson(sensor:Sensor,ts:Seq[TimeSeries],stats:SensorStats)={            
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
  private def toString(s:SensorData,props:Seq[String])={
    val output=s.ts.map(t=>t.output)
    val prop=props.map(p=>"\""+encodeBreaks(s.sensor.properties.getOrElse(p,""))+"\"").mkString(",")
    s"${s.sensor.name},$prop"+
    ",\""+output.map(f=>f.fieldName).mkString("|")+"\""+
    ",\""+output.map(f=>f.unit.code).mkString("|")+"\""+
    ",\""+output.map(f=>f.dataType.name).mkString("|")+"\""    
  }
  
  def sensorDataToCsv(data:SensorData,props:Seq[String],withLatest:Boolean=false)={
    //val valueNames="latestValues("+data.latest.map(_._1.fieldName).mkString("|")+")"
    //"#vs_name,"+props.mkString(",")+",fields,units,types,"+valueNames+System.lineSeparator
    if (withLatest)
      toString(data,props)+","+data.latest.map(_._2).mkString("|")
    else toString(data,props)
  }

  private def encodeBreaks(s:String)=s.replaceAll("(\r\n)|\r|\n", "\\\\n")

  
  
  
  def toCsv(data:SensorData,
      valueNames:Seq[String]=Seq()):StringWriter={
    val sw=new StringWriter
    def head(key:String,value:Any)=
      sw.append("# "+key+":"+value+System.lineSeparator)
    
    head("vs_name",data.sensor.name)
    data.sensor.properties.foreach(p=>head(p._1,encodeBreaks(p._2)))
    //val fields=data.sensor.fields.filter(f=>valueNames.isEmpty || valueNames.contains(f.fieldName ))
    val fields=data.ts.map(_.output)
    
    head("fields",fields.map{_.fieldName}.mkString(","))
    head("units",fields.map{_.unit.code}.mkString(","))
    head("types",fields.map{_.dataType.name}.mkString(","))
    if (!data.ts.isEmpty){
	    println("drimp "+data.ts.head.series.size+" "+fields.size)
	    val si=data.ts.head.series.size-1
	    (0 to si).foreach{i=>
	      //sw.append(data.time(i)+",")
	      val pp=(0 to fields.size-1).map{j=>
	        data.ts(j).series (i)        
	      }.mkString(",")
	      sw.append(pp+System.lineSeparator)
	      //sw.append(pp)  
	    }
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