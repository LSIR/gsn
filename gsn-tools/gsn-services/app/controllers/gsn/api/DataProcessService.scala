package controllers.gsn.api

import play.Logger
import play.api.mvc._
import play.api.Play.current
import scala.util.Try
import scala.collection.mutable.ArrayBuffer
import gsn.xpr.XprConditions
import gsn.data._
import play.api.libs.concurrent.Akka
import akka.actor.Props
import scala.concurrent.Future
import scala.concurrent.Promise
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import gsn.data.format._
import gsn.process.WeightedMovingAverage
import gsn.process.LinearInterpolation

object DataProcessService extends Controller with GsnService{
  
  private def process(process:String,params:Array[String],data:SensorData)={
    process match {
      case "wma" => 
        val size=params(0).toInt
        val wma=new WeightedMovingAverage(size)
        val series=wma.process(data.ts(1))
        SensorData(Seq(series),data.sensor)
      case "linear-interp" =>
        val rate=params(0).toInt
        val first=data.ts.head.series.last.asInstanceOf[Long]
        val lint=new LinearInterpolation(first,rate)
        val times=data.ts.head
        val values=data.ts(1)
        val series=lint.process(new TimeSeries(values.output,values.series.reverse ,times.asLongs.toSeq.reverse ))
        SensorData(Seq(Series(times.output,series.time),series),data.sensor)
    }
  }
  
  def processData(sensorid:String,fieldid:String) = Action.async {implicit request=>
    Try{
      //to enable
      //authorizeVs(sensorid)      
      
      val size:Option[Int]=queryparam("size").map(_.toInt)
      val fromStr:Option[String]=queryparam("from")
      val toStr:Option[String]=queryparam("to")
      val timeFormat:Option[String]=queryparam("timeFormat")
      val processOp:Option[String]=queryparam("op")
      val paramsStr=queryparam("params")
        
      val params:Array[String]=
        if (!paramsStr.isDefined) Array() 
        else paramsStr.get.split(",")

      val format=param("format",OutputFormat,defaultFormat)           
      val filters=new ArrayBuffer[String]
      if (fromStr.isDefined)          
        filters+= "timed>"+dateFormatter.parseDateTime(fromStr.get).getMillis
      if (toStr.isDefined)          
        filters+= "timed<"+dateFormatter.parseDateTime(toStr.get).getMillis
       
      val p=Promise[Seq[SensorData]]               
      val q=Akka.system.actorOf(Props(new QueryActor(p)))
      
      q ! GetSensorData(sensorid,Seq(fieldid),filters,size,timeFormat)
      p.future.map{data=>       
        val rd=process(processOp.get,params,data.head)
        format match {
            case Json=>Ok(JsonSerializer.ser(rd,Seq(),false))
            case Csv=>Ok(CsvSerializer.ser(rd,Seq(),false))
            case Xml=>Ok(XmlSerializer.ser(rd, Seq(), false))
        }                             
      }.recover{
        case t=> 
          t.printStackTrace()
          BadRequest(t.getMessage)              
      }      
    }.recover{
      case t=>Future(BadRequest("Error: "+t.getMessage))
    }.get
  }

}