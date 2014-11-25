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
  /*def createWithValues(s:Sensor,selectedFields:Seq[String],newValues:Seq[Array[Any]])={
    val filtered=selectedFields.map(f=>s.fields.find(_.name==f).get)
    new Sensor(s.name,filtered,s.location,s.properties,newValues)
  }*/
  def fromConf(vsConf:VsConf)={
    val output=
      Seq(Sensing("timed",Output("timed",vsConf.name,DataUnit("s"),LongType)))++
      vsConf.processing.output.map{out=>
        Sensing(out.name,Output(out.name.toLowerCase,vsConf.name,
            DataUnit(out.unit.getOrElse(null)),DataType(out.dataType) ))
      }
    /*
        if (latestValues){          
      	  java.util.Map<String, Object> se = DataStoreBis.latestValues(sensorConfig.getName());
            if (se != null){               
            	  Object[] vals=new Object[fields.size()];                	
             	  int i=1;
             	  vals[0]=se.get("time");
                //vals[0]=dateFormat.format(new Date((Long)vals[1]));
                for (DataField df: sensorConfig.getOutputStructure()){
                	  vals[i]=se.get(df.getName().toLowerCase());
                 	  i++;
                }
                values.add(vals);
            }
        }*/
    val props=vsConf.address.map{kv=>
      (kv._1.toLowerCase.trim,kv._2.trim )
    }
    def coord(p:Map[String,String],n:String)=p.get(n).map(_.toDouble)
    val location=Location(coord(props,"latitude"),coord(props,"longitude"),coord(props,"altitude"))
    val platform=new Platform(vsConf.name,location,null)
    Sensor(vsConf.name,output,platform,props)
     /*
  	  String is_public_res = "true";
    	  if (Main.getContainerConfig().isAcEnabled() && DataSource.isVSManaged(sensorConfig.getName())) 
    		  is_public_res = "false";
    	  props.put("is_public", is_public_res);          	  
        props.put("description", sensorConfig.getDescription());      
*/

  }
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
