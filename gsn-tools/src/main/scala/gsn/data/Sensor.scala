package gsn.data

import gsn.config.VsConf

case class Sensor(name:String,
    implements:Seq[Sensing],
    platform: Platform,
    properties:collection.Map[String,String]){
  lazy val fields:Seq[Output]=implements.map(_.outputs).flatten
  lazy val location=platform.location    
} 

object Sensor{
  def fromConf(vsConf:VsConf):Sensor=fromConf(vsConf,false)
  def fromConf(vsConf:VsConf,accessProtected:Boolean)={
    val output=
      vsConf.processing.output.map{out=>
        Sensing(out.name,Output(out.name.toLowerCase,vsConf.name,
            DataUnit(out.unit.getOrElse(null)),DataType(out.dataType) ))
      }
    val props=vsConf.address.map{kv=>
      (kv._1.toLowerCase.trim,kv._2.trim )
    } ++ 
    Map("description"->vsConf.description,
        "accessProtected"->accessProtected.toString  )
    def coord(p:Map[String,String],n:String)=p.get(n).map(_.toDouble)
    val location=Location(coord(props,"latitude"),
        coord(props,"longitude"),
        coord(props,"altitude"))
    val platform=new Platform(vsConf.name,location,null)
    Sensor(vsConf.name,output,platform,props)
  }
}   

class Platform(val name:String,val location:Location,sensors: =>Seq[Sensor])

case class Output(fieldName:String,stream:String,unit:DataUnit,dataType:DataType){
  //lazy val obsProperty=sensing.obsProperty 
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
