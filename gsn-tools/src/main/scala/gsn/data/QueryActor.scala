package gsn.data

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Promise
import scala.slick.jdbc.JdbcBackend.Database
import scala.util.Try
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.ISODateTimeFormat
import com.mchange.v2.c3p0.C3P0Registry
import akka.actor._
import javax.sql.DataSource
import akka.actor.Status.Failure

class QueryActor(p:Promise[Seq[SensorData]]) extends Actor {
  val gsnSensor=context.actorSelection("/user/gsnSensorStore")
  val sensors=Promise[Seq[SensorConf]]
  val sensor=Promise[SensorConf]
  implicit val exe=context.system.dispatcher
  def receive= {
    case GetAllSensors(latest,timeFormat) => 
      gsnSensor ! GetAllSensorsConf  
      sensors.future.map{ s=>
        if (latest)
          p.success(s.map(sc=>latestValues(sc)(timeFormat)))
        else
          p.success(s.map(sc=>asSensorData(sc.sensor) ))
        context.stop(self)
      }
    case g:GetSensorData => 
      gsnSensor ! GetSensorConf(g.sensorid )
      sensor.future.map{s=>
        p.success(Seq(query(s,g.fields ,g.conditions ,g.size,g.timeFormat )))
        context.stop(self)
      }
    case all:AllSensorConf =>
      sensors.success(all.sensors)
    case s:SensorConf =>
      sensor.success(s)
    case f:Failure=>
      p.failure(f.cause)
  }
  
     
  private def latestValues(sensorConf:SensorConf)(implicit timeFormat:Option[String]=None) ={
    val sensor=sensorConf.sensor 
    val vsName=sensor.name 
	val query = s"""select * from $vsName where  
	  timed = (select max(timed) from $vsName )"""
	vsDB(sensorConf.ds ).withSession {implicit session=>
	  
      val stmt=session.conn.createStatement
      val rs=stmt.executeQuery(query.toString)
      val fields=sensor.fields
      val data=fields.map{f=>new ArrayBuffer[Any]}
      val time=new ArrayBuffer[Any]
      while (rs.next){
        time+=formatTime(rs.getLong("timed"))
        for (i <- fields.indices) yield { 
          data(i) += (rs.getObject(fields(i).fieldName ))
        }           
      }
      val ts=
        Seq(TimeSeries(timeOutput(sensor.name),time))++
        fields.indices.map{i=>
        TimeSeries(fields(i),data(i).toSeq)
      }
      println(sensor.name )
      SensorData(ts,sensor)      
    }
  }
  
  private def asSensorData(s:Sensor)=
    SensorData(s.fields.map(f=>TimeSeries(f,Seq())),s )
  
  def query(sensorConf:SensorConf, fields:Seq[String],
			conditions:Seq[String], size:Option[Int],timeFormat:Option[String]):SensorData= {
    val sensor=sensorConf.sensor
    implicit val tf=timeFormat
    val ordered=(
      if (!fields.isEmpty)
        sensor.fields.filter(f=>fields.contains(f.fieldName))
          .map(_.fieldName )
      else sensor.fields.map(_.fieldName )
    ).filterNot(_=="timestamp")

    val data=(0 until ordered.size).map{f=>new ArrayBuffer[Any]}
    val time = new ArrayBuffer[Any]
    
 	val query = new StringBuilder("select ")
    query.append((Seq("timed")++ordered).mkString(","))
	query.append(" from ").append(sensor.name )
	if (conditions != null && conditions.length>0) 
	  query.append(" where "+conditions.mkString(" and "))
    if (size.isDefined) 
	  query.append(" order by timed desc").append(" limit 0," + size.get);	

	vsDB(sensorConf.ds).withSession {implicit session=>
      val stmt=session.conn.createStatement
      val rs=stmt.executeQuery(query.toString)
	  println(query)
      while (rs.next) {
        //data(0) += ((rs.getLong("timed"),fmt.print(rs.getLong("timed"))))
        time += formatTime(rs.getLong("timed"))
        for (i <- ordered.indices) yield {
          data(i) += (rs.getObject(ordered(i)) )
        }           
      }
            
      val selectedOutput=sensor.fields.filter(f=>ordered.contains(f.fieldName) ) 
      val ts=
        Seq(TimeSeries(timeOutput(sensor.name),time)) ++
        selectedOutput.indices.map{i=>
          TimeSeries(selectedOutput(i),data(i).toSeq)
        }
      SensorData(ts,sensor)
            
	} 
  }

   
  private def vsDB(ds:Option[DataSource])={
	if (ds.isDefined)
	  Database.forDataSource(ds.get)	  
	else 
	  Database.forDataSource(C3P0Registry.pooledDataSourceByName("gsn"))
  }
  
  private def timeOutput(sensorname:String)={
    Output("timestamp",sensorname,DataUnit("ms"),TimeType)
  }
    
  private def formatTime(t:Long)(implicit timeFormat:Option[String])=timeFormat match{
    case Some("unixTime") | None => t
    case _ => getTimeFormat(timeFormat).print(t)
  }
            
  private def getTimeFormat(tf:Option[String])={// e.g. yyyyMMdd    
    tf match {
      case Some("iso8601") => ISODateTimeFormat.dateTimeNoMillis()
      case Some("iso8601ms") => ISODateTimeFormat.dateTime()
      
      case Some(f) => 
        Try(DateTimeFormat.forPattern(f))              
        .recover{case e =>
          throw new IllegalArgumentException(s"Invalid time format: $f ${e.getMessage}")}.get 
      case None => ISODateTimeFormat.dateHourMinuteSecond()
    }
  }
}
