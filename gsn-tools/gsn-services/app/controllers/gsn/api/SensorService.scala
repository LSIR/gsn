package controllers.gsn.api

import play.api.mvc._
import play.api.db._
import play.api.cache._
import play.api.Play.current
import play.api.libs.concurrent.Execution
import com.typesafe.config.ConfigFactory
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
import scala.util.Try
import scala.util.Failure
import java.io.File
import gsn.config.VsConf
import gsn.config.GsnConf
import gsn.security.SecurityData

object SensorService extends Controller{   
  lazy val conf=ConfigFactory.load
  val dateFormatter={
    val parsers=conf.getStringList("gsn.api.dateTimeFormats").map{d=>
      DateTimeFormat.forPattern(d).getParser
    }.toArray
    new DateTimeFormatterBuilder().append(null,parsers).toFormatter
  }

  val validFormats=Seq(Csv,Json)
  val defaultFormat=Json
   
  case class OutputFormat(code:String) {
    val formats=Seq("csv","json")
    if  (!formats.exists(_==code))
      throw new IllegalArgumentException("Invalid format: "+code)
  }
  
  object Csv extends OutputFormat("csv")
  object Json extends OutputFormat("json")	
  
  private val ds ={
    val gsnConf=GsnConf.load(conf.getString("gsn.config"))
    val vsConfs={
      val vsDir=new File(conf.getString("gsn.vslocation"))    
      if (!vsDir.exists) throw new Exception("config error")
      vsDir.listFiles.filter(_.getName.endsWith(".xml")).map{f=>
	    val vs=VsConf.load(f.getPath())
	    (vs.name.toLowerCase,vs )
	  }.toMap
    }
    new DataStore(gsnConf,vsConfs)
  }
  
  private val acDs=new SecurityData(ds)
  
  def queryparam(name:String)(implicit req:Request[AnyContent])={
    req.queryString.get(name).map(_.head)
  }
  def param[T](name:String,fun: String=>T,default:T)(implicit req:Request[AnyContent])=
    queryparam(name).map(fun(_)).getOrElse(default)
      
  def sensors = Action {implicit request=>
    Try{
      val format=param("format",OutputFormat,defaultFormat)    
      val latestVals=param("latestValues",_.toBoolean,false)
      val data=
        if (latestVals) ds.allSensorsLatest
        else ds.allSensors
      val out=format match{
          case Json=>JsonSerializer.ser(data,Seq(),latestVals)
          case Csv=>CsvSerializer.ser(data, Seq(), latestVals)
      }
      Ok(out)        
    }.recover{case t=>t.printStackTrace;  BadRequest(t.getMessage)    }.get
    
  }
        
  private def authorizeVs(vsname:String)(implicit request:Request[AnyContent])={
    queryparam("apikey") match {
      case Some(k) => 
        if (!acDs.authorizeVs(vsname, k))
          throw new IllegalArgumentException(s"Not authorized apikey $k for resource $vsname")
      case None => throw new IllegalArgumentException("Not provided required apikey")
    }   
  }
  
  def sensor(sensorid:String) = Action {implicit request=>
    Try{
      val vsname=sensorid.toLowerCase
      val sensor=ds.sensor(vsname)
      if (!sensor.isDefined) 
        throw new IllegalArgumentException(s"Invalid virtual vensor identifier $sensorid")              
      authorizeVs(sensorid)
    	
      val size:Option[Int]=queryparam("size").map(_.toInt)
      val fieldStr:Option[String]=queryparam("fields")
      val filterStr:Option[String]=queryparam("filter")
      val fromStr:Option[String]=queryparam("from")
      val toStr:Option[String]=queryparam("to")
        val format=param("format",OutputFormat,defaultFormat)           
        val filters=new ArrayBuffer[String]
        val fields:Array[String]=
          if (!fieldStr.isDefined) Array() 
          else fieldStr.get.split(",")
        if (fromStr.isDefined)          
          filters+= "timed>"+dateFormatter.parseDateTime(fromStr.get).getMillis
        if (toStr.isDefined)          
          filters+= "timed<"+dateFormatter.parseDateTime(toStr.get).getMillis
        val conds=XprConditions.parseConditions(filterStr.toArray).recover{                 
          case e=>throw new IllegalArgumentException("illegal conditions in filter: "+e.getMessage())
        }.get.map(_.toString)
                   
        val body=ds.query(sensor.get,fields,conds++filters,size)
        format match{
          case Json=>Ok(JsonSerializer.ser(body,Seq(),false))
          case Csv=>Ok(CsvSerializer.ser(body,Seq(),false))
        }
        
      
      }.recover{case t=>t.printStackTrace();  BadRequest(t.getMessage)}.get
  
  }
}