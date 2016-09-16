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
* File: app/controllers/gsn/api/DataProcessService.scala
*
* @author Jean-Paul Calbimonte
*
*/
package controllers.gsn.api

import play.Logger
import play.api.mvc._
import play.api.Play.current
import scala.util.Try
import scala.collection.mutable.ArrayBuffer
import ch.epfl.gsn.xpr.XprConditions
import ch.epfl.gsn.data._
import play.api.libs.concurrent.Akka
import akka.actor.Props
import scala.concurrent.Future
import scala.concurrent.Promise
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import ch.epfl.gsn.data.format._
import ch.epfl.gsn.process.WeightedMovingAverage
import ch.epfl.gsn.process.LinearInterpolation

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