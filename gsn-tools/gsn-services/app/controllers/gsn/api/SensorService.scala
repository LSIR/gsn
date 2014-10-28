package controllers.gsn.api

import play.api.mvc._
import play.api.db._
import play.api.cache._
import play.api.Play.current
import play.api.libs.concurrent.Execution
import com.typesafe.config.ConfigFactory
import models.gsn.data.GsnMetadata
import gsn.data._
import scala.concurrent._
import scala.collection.mutable.ArrayBuffer
import java.util.Date
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.format.DateTimeFormat
import collection.JavaConversions._
import org.joda.time.format.DateTimeFormatterBuilder
import gsn.xpr.XprConditions
import scala.util.Success

object SensorService extends Controller{
  lazy val conf=ConfigFactory.load
  lazy val gsnServer=conf.getString("gsn.server.url")
  lazy val metadata=new GsnMetadata(gsnServer)
  implicit val context = Execution.Implicits.defaultContext
  val parsers=conf.getStringList("gsn.api.dateTimeFormats").map{d=>
    DateTimeFormat.forPattern(d).getParser
  }.toArray
  val dtf=new DateTimeFormatterBuilder().append(null,parsers).toFormatter
  //val dtf=ISODateTimeFormat.dateTime

  def sensorsMeta:Future[Map[String,Sensor]]=
    Cache.getOrElse("sensors")(metadata.allSensors)
  
  def sensors(latestVals:Option[Boolean]) = Action.async {
    sensorsMeta.map{s=>
      if (latestVals.isDefined && latestVals.get){
        val m= s.map(ss=>latestValues(ss._2))
        Ok(DataSerializer.sensorDataToJson(m.toSeq))        
      }
      else {
        Ok(DataSerializer.toJsonString(s.values.toSeq))       
      }
    }
  }
    
  def sensor(sensorid:String,
             size:Option[Int],
             fieldStr:Option[String],
             filterStr:Option[String],
             fromStr:Option[String],
             toStr:Option[String]) = Action.async {request=>
    
    sensorsMeta.map{s=>
      if (!s.contains(sensorid))
        BadRequest("Invalid virtual sensor identifier: "+sensorid)
      else{
        val filters=new ArrayBuffer[String]
        val sensor=s(sensorid)
        val fields:Array[String]=
          if (!fieldStr.isDefined) Array() 
          else fieldStr.get.split(",")
        if (fromStr.isDefined)          
          filters+= "timed>"+dtf.parseDateTime(fromStr.get).getMillis
        if (toStr.isDefined)          
          filters+= "timed<"+dtf.parseDateTime(toStr.get).getMillis
        val conds=XprConditions.parseConditions(filterStr.toArray).recover{                 
          case e=>throw new IllegalArgumentException("illegal conditions in filter: "+e.getClass()+e.getMessage())
        }.get.map(_.toString)
         
        //.flatMap{_.map(_.toString)}
          
        Ok(DataSerializer.sensorDataToJson(Seq(query(sensor,fields,conds++filters,size))))
      }
    }
  }
  
  private def latestValues(sensor:Sensor) ={
    val vsName=sensor.name 
	val query = s"""select * from $vsName where  
	  timed = (select max(timed) from $vsName )"""
    val con=DB.getConnection()
	try {
      val stmt=con.createStatement
      val rs=stmt.executeQuery(query.toString)
      val fields=sensor.fields
      val data=fields.map{f=>new ArrayBuffer[(Any,Any)]}
      while (rs.next){
        for (i <- fields.indices) yield { 
          data(i) += ((rs.getLong("timed"),rs.getDouble(fields(i).fieldName )))
        }           
      }
      val ts=fields.indices.map{i=>
        TimeSeries(fields(i),data(i).toSeq)
      }
      SensorData(ts,sensor)      
    }
	finally con.close			
  }

  
  private def query(sensor:Sensor, fields:Seq[String],
			conditions:Seq[String], size:Option[Int]):SensorData= {
    val ordered=
      if (!fields.isEmpty)
        sensor.fields.filter(f=>fields.contains(f.fieldName)).map(_.fieldName )
      else sensor.fields.map(_.fieldName )

    val con=DB.getConnection()
    val data=ordered.map{f=>new ArrayBuffer[(Any,Any)]}

 	val query = new StringBuilder("select ")
    query.append((Seq("timed")++ordered).mkString(","))
	query.append(" from ").append(sensor.name )
	if (conditions != null && conditions.length>0) 
	  query.append(" where "+conditions.mkString(" and "))
    if (size.isDefined) 
	  query.append(" order by timed desc").append(" limit 0," + size.get);	

	try {
      val stmt=con.createStatement
      val rs=stmt.executeQuery(query.toString)
	  println(query)
      while (rs.next) {
        for (i <- ordered.indices) yield {
          data(i) += ((rs.getLong("timed"),rs.getDouble(ordered(i) )))
        }           
      }
            
      val selectedOutput=sensor.fields.filter(f=>ordered.contains(f.fieldName) ) 
      val ts=selectedOutput.indices.map{i=>
        TimeSeries(selectedOutput(i),data(i).toSeq)
      }
      SensorData(ts,sensor)
            
	} 
	finally con.close
  }
  





}