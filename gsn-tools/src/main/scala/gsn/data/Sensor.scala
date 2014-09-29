package gsn.data

case class Sensor(name:String,
    fields:Seq[Field],
    location:Location,
    properties:collection.Map[String,String],
    values:Seq[Array[Any]]) 

object Sensor{
  def createWithValues(s:Sensor,selectedFields:Seq[String],newValues:Seq[Array[Any]])={
    val filtered=selectedFields.map(f=>s.fields.find(_.name==f).get)
    new Sensor(s.name,filtered,s.location,s.properties,newValues)
  }
}   
    
case class Location(latitude:Option[Double],
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

case class Field(name:String,datatype:DataType,
    unit:DataUnit,obsproperty:String)

case class DataUnit(name:String,code:String)

