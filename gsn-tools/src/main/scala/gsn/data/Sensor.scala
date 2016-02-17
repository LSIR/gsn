package gsn.data

import gsn.config.VsConf
import org.joda.time.DateTime

case class Sensor(name:String,
    implements:Seq[Sensing],
    platform: Platform,
    properties:collection.Map[String,String]){
  lazy val fields:Seq[Output]=implements.map(_.outputs).flatten
  lazy val location=platform.location    
} 

object Sensor{
  def fromConf(vsConf:VsConf):Sensor=fromConf(vsConf,None)
  def fromConf(vsConf:VsConf,stats:Option[SensorStats])={
    val output=
      vsConf.processing.output map{out=>
        Sensing(out.name,Output(out.name.toLowerCase,vsConf.name,
            DataUnit(out.unit.getOrElse(null)),DataType(out.dataType) ))
      }
    val props=vsConf.address.map{kv=>
        (kv._1.toLowerCase.trim,kv._2.trim )
      } ++ 
      Map("description"->vsConf.description )
    def coord(p:Map[String,String],n:String)=
      try p.get(n).map(_.toDouble)
      catch {case e:Exception=> None}
    val location=Location(coord(props,"latitude"),
        coord(props,"longitude"),
        coord(props,"altitude"))
    val platform=new Platform(vsConf.name,location)
    Sensor(vsConf.name,output,platform,props)
  }
  //def fromSensor(s:Sensor,stats:Option[SensorStats])=
    //Sensor(s.name,s.implements,s.platform,s.properties,stats)
}   

case class SensorStats(rate:Option[Double],
    start:Option[Long],end:Option[Long], latestValues:Seq[Series]){
  private val minTime=30*24*3600*1000
  val isArchive:Boolean={
    end.map{endtime=>
      val duration= (new DateTime).minus(endtime)
      duration.getMillis > minTime
    }.getOrElse(false)
  }
}

object EmptyStats extends SensorStats(None,None,None,Seq())

case class Platform(val name:String,val location:Location)

case class Output(fieldName:String,stream:String,unit:DataUnit,dataType:DataType){
}

class Sensing(val obsProperty:String,outputSeq: => Seq[Output]){
  lazy val outputs:Seq[Output]=outputSeq
}

object Sensing{
  def apply(obsProperty:String,output:Output)=
    new Sensing(obsProperty,Seq(output))
}

case class Location(
    latitude:Option[Double],
    longitude:Option[Double],
    altitude:Option[Double])


object Location{
  def apply(lat:java.lang.Double,lon:java.lang.Double,alt:java.lang.Double)={
    val lt=if (lat==null) None else Some(lat.doubleValue())
    val lg=if (lon==null) None else Some(lon.doubleValue())
    val al=if (alt==null) None else Some(alt.doubleValue())
    new Location(lt,lg,al)
  }
}

case class DataUnit(name:String,code:String)

object DataUnit{
  def apply(code:String)=new DataUnit(code,code)
}
