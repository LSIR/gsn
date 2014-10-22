package gsn.data



case class Sensor(name:String,
    //fields:Seq[Field],
    implements:Seq[Sensing],
    //location:Location,
    platform: Platform,
    properties:collection.Map[String,String],
    values:Seq[Array[Any]]){
  lazy val fields:Seq[Output]=implements.map(_.outputs).flatten
  lazy val location=platform.location 
} 

object Sensor{
  /*def createWithValues(s:Sensor,selectedFields:Seq[String],newValues:Seq[Array[Any]])={
    val filtered=selectedFields.map(f=>s.fields.find(_.name==f).get)
    new Sensor(s.name,filtered,s.location,s.properties,newValues)
  }*/
}   

class Platform(val name:String,val location:Location,sensors: =>Seq[Sensor])

case class Output(fieldName:String,stream:String,unit:DataUnit,dataType:DataType){
  //lazy val obsProperty=sensing.obsProperty 
}
/*
object Output{
  def apply(fieldName:String,stream:String,unit:DataUnit,dataType:DataType,obsProperty:String)={
    lazy val s:Sensing=new Sensing(obsProperty,Seq(out))
    lazy val out=new Output(fieldName,stream,unit,dataType,s)
    out
  }
    
}*/

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

//case class Field (name:String,datatype:DataType,
  //  unit:DataUnit,obsproperty:String) extends Output(name,"",unit,datatype)

case class DataUnit(name:String,code:String)

object DataUnit{
  def apply(code:String)=new DataUnit(code,code)
}
