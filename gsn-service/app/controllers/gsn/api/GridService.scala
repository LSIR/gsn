package controllers.gsn.api
import play.api.mvc._
import play.api.Play.current
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.util.Try
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Promise
import play.api.libs.concurrent.Akka
import akka.actor.Props
import play.Logger
import gsn.data._
import gsn.data.format._

object GridService  extends Controller with GsnService {   
  def gridData(sensorid:String) =grid(sensorid,EsriAscii)
  def gridTimeseries(sensorid:String)=grid(sensorid,Json,true)

  
  def grid(sensorid:String, 
      defaultFormat:OutputFormat,asTimeSeries:Boolean=false) = Action.async {implicit request=>
    Try{
      val vsname=sensorid.toLowerCase
    	
      val size:Option[Int]=queryparam("size").map(_.toInt)
      val fieldStr:Option[String]=queryparam("fields")
      val fromStr:Option[String]=queryparam("from")
      val toStr:Option[String]=queryparam("to")
      val timeFormat:Option[String]=queryparam("timeFormat")
      val boxStr:Option[String]=queryparam("box")
      val agg:Option[String]=queryparam("agg")

      val format=param("format",OutputFormat,defaultFormat)           
      val filters=new ArrayBuffer[String]
      val box:Option[Seq[Int]]=
        boxStr.map(b=>b.split(",").map(_.toInt))
      val fields:Array[String]=
        if (!fieldStr.isDefined) Array() 
        else fieldStr.get.split(",")
      if (fromStr.isDefined)          
        filters+= "timed>"+dateFormatter.parseDateTime(fromStr.get).getMillis
      if (toStr.isDefined)          
        filters+= "timed<"+dateFormatter.parseDateTime(toStr.get).getMillis
                              
      val p=Promise[Seq[SensorData]]               
      val q=Akka.system.actorOf(Props(new QueryActor(p)))
      Logger.debug("request the query actor")
      q ! GetGridData(vsname,filters,size,timeFormat,box,agg,asTimeSeries)
      p.future.map{data=>        
        Logger.debug("before formatting")
                 
        format match{
            case EsriAscii=>
              val pp=EsriSerializer.ser(data.head,Seq(),false)
              Ok(pp)
            case Json=>
              val pp=JsonSerializer.ser(data.head,Seq(),false)
              Ok(pp)
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