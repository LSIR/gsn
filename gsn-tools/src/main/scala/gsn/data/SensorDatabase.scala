package gsn.data

import javax.sql.DataSource
import scala.util._
import com.mchange.v2.c3p0.C3P0Registry
import scala.slick.jdbc.JdbcBackend.Database
import scala.collection.mutable.ArrayBuffer
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.format.DateTimeFormat
import org.slf4j.LoggerFactory
import com.mchange.v2.c3p0.DataSources
import java.sql.DriverManager
import gsn.data.format.TimeFormats._
import org.joda.time.MonthDay
import org.joda.time.DateTime

object SensorDatabase { 
  val log=LoggerFactory.getLogger(SensorDatabase.getClass)
  def latestValues(sensor:Sensor, timeFormat:Option[String]=None)
    (implicit ds:Option[String]) ={
    val vsName=sensor.name.toLowerCase 
    val fields=sensor.fields
    val fieldNames=fields.map (f=>f.fieldName ) 
              .filterNot (_.equals("grid")) ++ Seq("timed") 
	val query = s"""select ${fieldNames.mkString(",")} from $vsName where  
	  timed = (select max(timed) from $vsName limit 1) limit 1"""	  	  	  	  
	Try{
	  vsDB(ds).withSession {implicit session=>
	 
	  //val conn=vsDs(vsName)
      //val stmt=conn.createStatement
	  val stmt= session.conn.createStatement
      val rs= stmt.executeQuery(query)
      val data=fields map{f=>new ArrayBuffer[Any]}
      val time=new ArrayBuffer[Any]
      while (rs.next){
        time+=formatTime(rs.getLong("timed"))(timeFormat)
        for (i <- fields.indices) yield { 
          if (fields(i).dataType == BinaryType)
            data(i) += ("binary")
          else 
            data(i) += (rs.getObject(fields(i).fieldName ))
        }           
      }
      rs.close
      stmt.close
      //conn.close
      val ts=
        Seq(Series(timeOutput(sensor.name),time))++
        fields.indices.map{i=>
        Series(fields(i),data(i).toSeq)
      }
      log debug s"computed latest values for $vsName" 
      ts      
    }} match{
      case Failure(f)=> 
        log error s"Error ${f.getMessage}"
        f.printStackTrace(); Seq()
      case Success(d) => d
    }
  }
    
  def asSensorData(s:Sensor)=
    SensorData(s.fields.map(f=>Series(f,Seq())),s )
  
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
	query.append(" from ").append(sensor.name.toLowerCase )
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
        Seq(Series(timeOutput(sensor.name),time)) ++
        selectedOutput.indices.map{i=>
          Series(selectedOutput(i),data(i).toSeq)
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
    (implicit ds:Option[String]) ={
    //val sensor=sensorConf.sensor 
    val vsName=sensor.name.toLowerCase 
	val queryMinMax = s"select max(timed), min(timed) from $vsName "
	val queryRate = s"select timed from $vsName limit 100 "
    var min,max :Option[Long]=None
    var rate:Option[Double]=None
    //if (true)  SensorStats(rate,min,max,Seq())
//else {
	Try{
	//val conn=vsDs(vsName )
	vsDB(ds).withSession {implicit session=>	  
      val stmt=session.conn.createStatement      
      val rs= stmt.executeQuery(queryMinMax.toString)
      while (rs.next){
        min=Some(rs.getLong(2))
        max=Some(rs.getLong(1))
      }
      log debug s"Computed max_/min for $vsName"
      stmt.close
      //conn.close
    }
	vsDB(ds).withSession {implicit session=>
    val conn2=session.conn//vsDs(vsName)
	  val times=new ArrayBuffer[Long]()
	  val stmt2=conn2.createStatement
      val rs2= stmt2.executeQuery(queryRate.toString)
      var t1,t2=0L
      while (rs2.next){
        t2=rs2.getLong(1)
        if (!rs2.isFirst)
          times+= t2-t1
        t1=t2
      }          
	  if (times.size>0)
	    rate=Some(times.sum/times.size)
	  log debug s"Computed rate for $vsName"
	  stmt2.close
	  conn2.close
	}
	SensorStats(rate,min,max,latestValues(sensor,timeFormat))
    } match{
      case Failure(f)=> 
        log error s"Error ${f.getMessage}"
        f.printStackTrace(); 
      	SensorStats(rate,min,max,Seq())

        //throw new Exception(s"Error in computing the stats of $vsName" )
      case Success(d) => d
    }
//}
  }

  private def vsDs(dsName:String)={
	val sc=if (dsReg.dsss.contains(dsName))
      dsReg.dsss(dsName)
	else 
	  dsReg.dsss("gsn")
	 DriverManager.getConnection(sc.url , sc.user , sc.pass )

  }

  
  private def vsDB(dsName:Option[String])={
	if (dsName.isDefined)
	  Database.forDataSource(C3P0Registry.pooledDataSourceByName(dsName.get))	  
	else 
	  Database.forDataSource(C3P0Registry.pooledDataSourceByName("gsn"))
  }

}