package gsn.data

import javax.sql.DataSource
import scala.util._
import com.mchange.v2.c3p0.C3P0Registry
import scala.slick.jdbc.JdbcBackend.Database
import scala.collection.mutable.ArrayBuffer
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.format.DateTimeFormat
import org.slf4j.LoggerFactory


object SensorDatabase { 
  val log=LoggerFactory.getLogger(SensorDatabase.getClass())
  def latestValues(sensor:Sensor, timeFormat:Option[String]=None)
    (implicit ds:Option[DataSource]) ={
    val vsName=sensor.name 
	val query = s"""select * from $vsName where  
	  timed = (select max(timed) from $vsName )"""
	Try(vsDB(ds).withSession {implicit session=>	  
      val stmt=session.conn.createStatement
      val rs= stmt.executeQuery(query.toString)
      val fields=sensor.fields
      val data=fields.map{f=>new ArrayBuffer[Any]}
      val time=new ArrayBuffer[Any]
      while (rs.next){
        time+=formatTime(rs.getLong("timed"))(timeFormat)
        for (i <- fields.indices) yield { 
          data(i) += (rs.getObject(fields(i).fieldName ))
        }           
      }
      val ts=
        Seq(TimeSeries(timeOutput(sensor.name),time))++
        fields.indices.map{i=>
        TimeSeries(fields(i),data(i).toSeq)
      }
      log debug s"computed latest values for $vsName" 
      ts      
    }) match{
      case Failure(f)=> 
        log error s"Error ${f.getMessage}"
        f.printStackTrace(); Seq()
      case Success(d) => d
    }
  }
    
  def asSensorData(s:Sensor)=
    SensorData(s.fields.map(f=>TimeSeries(f,Seq())),s )
  
  def query(sensorConf:SensorInfo, fields:Seq[String],
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

    try{
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
	  } 
            	
      val selectedOutput=sensor.fields.filter(f=>ordered.contains(f.fieldName) ) 
      val ts=
        Seq(TimeSeries(timeOutput(sensor.name),time)) ++
        selectedOutput.indices.map{i=>
          TimeSeries(selectedOutput(i),data(i).toSeq)
        }
      SensorData(ts,sensor)
            
	} catch {
	  case e:Exception=> println(s"Query error ${e.getMessage}")
	    throw e
	} 
  }   
  
  private def timeOutput(sensorname:String)={
    Output("timestamp",sensorname,DataUnit("ms"),TimeType)
  }

  def stats(sensor:Sensor,timeFormat:Option[String]=None)
    (implicit ds:Option[DataSource]) ={
    //val sensor=sensorConf.sensor 
    val vsName=sensor.name 
	val queryMinMax = s"select max(timed), min(timed) from $vsName "
	val queryRate = s"select timed from $vsName limit 100 "
    var min,max :Option[Long]=None
    var rate:Option[Double]=None
	Try{
	vsDB(ds ).withSession {implicit session=>	  
      val stmt=session.conn.createStatement      
      val rs= stmt.executeQuery(queryMinMax.toString)
      while (rs.next){
        min=Some(rs.getLong(2))
        max=Some(rs.getLong(1))
      }
      log debug s"Computed max_/min for $vsName"
    }
	vsDB(ds).withSession {implicit session=>
	  val times=new ArrayBuffer[Long]()
	  val stmt=session.conn.createStatement
      val rs= stmt.executeQuery(queryRate.toString)
      var t1,t2=0L
      while (rs.next){
        t2=rs.getLong(1)
        if (!rs.isFirst)
          times+= t2-t1
        t1=t2
      }          
	  rate = if (times.size == 0) Some(0)  else Some(times.sum/times.size)
	  log debug s"Computed rate for $vsName"
	}
	SensorStats(rate,min,max,latestValues(sensor,timeFormat))
    } match{
      case Failure(f)=> 
        log error s"Error ${f.getMessage}"
        f.printStackTrace(); 
        throw new Exception(s"Error in computing the stats of $vsName" )
      case Success(d) => d
    }
  }
  
  private def vsDB(ds:Option[DataSource])={
	if (ds.isDefined)
	  Database.forDataSource(ds.get)	  
	else 
	  Database.forDataSource(C3P0Registry.pooledDataSourceByName("gsn"))
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
          println("error here "+e)
          throw new IllegalArgumentException(s"Invalid time format: $f ${e.getMessage}")}.get 
      case None => ISODateTimeFormat.dateHourMinuteSecond()
    }
  }

}