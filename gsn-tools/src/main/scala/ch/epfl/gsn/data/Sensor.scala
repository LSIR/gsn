/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2016, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/ch/epfl/gsn/data/Sensor.scala
*
* @author Jean-Paul Calbimonte
* @author Julien Eberle
*
*/
package ch.epfl.gsn.data

import ch.epfl.gsn.config.VsConf
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
    val _props=vsConf.address.map{kv=>
        (kv._1.toLowerCase.trim,kv._2.trim )
      } ++ 
      Map("description"->vsConf.description )
    val props = if(vsConf.processing.partitionField.isEmpty) _props else _props ++ Map("partitionField"->vsConf.processing.partitionField.get )
    def coord(p:Map[String,String],n:String)=
      try p.get(n).map(_.toDouble)
      catch {case e:Exception=> None}
    val location=Location(coord(props,"latitude"),
        coord(props,"longitude"),
        coord(props,"altitude"),
        props.get("latitude_ref"),
        props.get("longitude_ref"),
        props.get("altitude_ref")
        )
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
    altitude:Option[Double],
    latitudeRef:Option[String],
    longitudeRef:Option[String],
    altitudeRef:Option[String]
    )


object Location{
  def apply(lat:java.lang.Double,lon:java.lang.Double,alt:java.lang.Double,latref:java.lang.String,lonref:java.lang.String,altref:java.lang.String)={
    val lt=if (lat==null) None else Some(lat.doubleValue())
    val lg=if (lon==null) None else Some(lon.doubleValue())
    val al=if (alt==null) None else Some(alt.doubleValue())
    val ltr=if (latref==null) None else Some(latref)
    val lgr=if (lonref==null) None else Some(lonref)
    val alr=if (altref==null) None else Some(altref)
    new Location(lt,lg,al,ltr,lgr,alr)
  }
}

case class DataUnit(name:String,code:String)

object DataUnit{
  def apply(code:String)=new DataUnit(code,code)
}
