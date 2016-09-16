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
* File: src/ch/epfl/gsn/data/SensorDatabase.scala
*
* @author Jean-Paul Calbimonte
* @author Julien Eberle
*
*/
package ch.epfl.gsn.data

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
import ch.epfl.gsn.data.format.TimeFormats._
import org.joda.time.MonthDay
import org.joda.time.DateTime
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.sql.ResultSet
import com.hp.hpl.jena.datatypes.RDFDatatype
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype
import com.typesafe.config.ConfigFactory

object SensorDatabase { 
  val log=LoggerFactory.getLogger(SensorDatabase.getClass)
  def latestValues(sensor:Sensor, timeFormat:Option[String]=None)
    (implicit ds:Option[String]) ={
    val timeframe=ConfigFactory.load.getInt("gsn.data.timeframe")
    val vsName=sensor.name.toLowerCase 
    val fields=sensor.fields
    val fieldNames=fields.map (f=>f.fieldName ) 
              .filterNot (_.equals("grid")) ++ Seq("timed") 
    val query = sensor.properties.get("partitionField") match {
      case Some(partitionField) => s"""SELECT ${fieldNames.mkString(",")} from $vsName where pk IN (SELECT max(pk) from $vsName where timed > (select max(timed) from $vsName) - $timeframe group by $partitionField)"""
      case _ => s"""SELECT ${fieldNames.mkString(",")} from $vsName where pk = (SELECT max(pk) from $vsName)"""
    }	  	  	  	  
	Try{
	  vsDB(ds).withSession {implicit session=>
	 
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
  
  def selectedFields(sensor:Sensor,fields:Seq[String])={
    val selFields=
      if (!fields.isEmpty)
        sensor.fields.filter(f=>fields.contains(f.fieldName)).map(_.fieldName )
      else sensor.fields.map(_.fieldName )
    selFields.filterNot(_=="timestamp")
  }  
    
  def aggregationQuery(sensorConf:SensorInfo, fields:Seq[String],
			conditions:Seq[String], size:Option[Int],timeFormat:Option[String],
			aggFunction:String,aggPeriod:Long):SensorData= {
    val sensor=sensorConf.sensor
    implicit val tf=timeFormat
        
    val selFields=selectedFields(sensor,fields)

    val data=(0 until selFields.size).map{f=>new ArrayBuffer[Any]}
    val time = new ArrayBuffer[Any]
    
 	val query = new StringBuilder("select ")
    val aggfields=selFields.map(f=>s"$aggFunction($f) as $f")
    
    query.append((Seq(s"floor(timed/$aggPeriod) as agg_interval")++aggfields).mkString(","))
	query.append(" from ").append(sensor.name.toLowerCase )
	if (conditions != null && conditions.length>0) 
	  query.append(" where "+conditions.mkString(" and "))
	query.append(" group by agg_interval")
    if (size.isDefined) 
	  query.append(" order by timed desc").append(" limit 0," + size.get);	

    try{
	  vsDB(sensorConf.ds).withSession {implicit session=>
        val stmt=session.conn.createStatement
	    log.debug("Query: "+query)
        val rs=stmt.executeQuery(query.toString)
        while (rs.next) {
          time += formatTime(rs.getLong("agg_interval")*aggPeriod)
          for (i <- selFields.indices) yield {
            data(i) += (rs.getObject(selFields(i)) )
          }           
        }
	  } 
            	
      val selectedOutput=sensor.fields.filter(f=>selFields.contains(f.fieldName) ) 
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

  
  def query(sensorConf:SensorInfo, fields:Seq[String],
			conditions:Seq[String], size:Option[Int],timeFormat:Option[String]):SensorData= {
    val sensor=sensorConf.sensor
    implicit val tf=timeFormat
        
    val selFields=selectedFields(sensor,fields)

    val data=(0 until selFields.size).map{f=>new ArrayBuffer[Any]}
    val time = new ArrayBuffer[Any]
    
 	val query = new StringBuilder("select ")
    query.append((Seq("timed")++selFields).mkString(","))
	query.append(" from ").append(sensor.name.toLowerCase )
	if (conditions != null && conditions.length>0) 
	  query.append(" where "+conditions.mkString(" and "))
    if (size.isDefined) 
	  query.append(" order by timed desc").append(" limit 0," + size.get);	

    try{
	  vsDB(sensorConf.ds).withSession {implicit session=>
        val stmt=session.conn.createStatement
        val rs=stmt.executeQuery(query.toString)
	    log.debug("Query: "+query)
        while (rs.next) {
          time += formatTime(rs.getLong("timed"))
          for (i <- selFields.indices) yield {
            data(i) += (rs.getObject(selFields(i)) )
          }           
        }
	  } 
            	
      val selectedOutput=sensor.fields.filter(f=>selFields.contains(f.fieldName) ) 
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

  
  def queryGrid(sensorConf:SensorInfo,conditions:Seq[String],size:Option[Int],
      timeFormat:Option[String],box:Option[Seq[Int]],
      aggregation:Option[String],timeSeries:Boolean=false):SensorData= {
    val sensor=sensorConf.sensor
    implicit val tf=timeFormat
        
    val fieldNames=sensor.fields.map(_.fieldName).filterNot(_=="timestamp")

    val data=(0 until fieldNames.size).map{f=>new ArrayBuffer[Any]}
    val time = new ArrayBuffer[Any]    
    
 	val query = new StringBuilder("select ")
    query.append((Seq("timed")++fieldNames).mkString(","))
	query.append(" from ").append(sensor.name.toLowerCase )
	if (conditions != null && conditions.length>0) 
	  query.append(" where "+conditions.mkString(" and "))
    if (size.isDefined) 
	  query.append(" order by timed desc").append(" limit 0," + size.get);	
    try{
	  vsDB(sensorConf.ds).withSession {implicit session=>
        val stmt=session.conn.createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY)
        stmt.setFetchSize(Integer.MIN_VALUE)
        val rs=stmt.executeQuery(query.toString)
	    log.trace(query.toString)
        while (rs.next) {
          time += formatTime(rs.getLong("timed"))
          for (i <- fieldNames.indices) yield {
            if (sensor.fields(i).dataType == BinaryType){
              val grid={
                val rawGrid=GridTools.deserialize(rs.getBytes(fieldNames(i)))
                val cropped=
                  if (!box.isDefined) rawGrid               
                  else GridTools.crop(rawGrid, GridTools.BoundingBox(box.get))
                if (timeSeries) GridTools.summarize(cropped, aggregation.get, -999) 
                else cropped
              }
              data(i) += (grid)
            }
            else 
              data(i) += (rs.getObject(fieldNames(i)) )
          }           
        }
	  } 
      val selectedOutput=sensor.fields.filter(f=>fieldNames.contains(f.fieldName)) 
      val noValueIdx=selectedOutput.zipWithIndex.
        filter(a=>a._1.fieldName.equalsIgnoreCase("nodata_value")).head._2 
	  if (aggregation.isDefined && !timeSeries){
        val ts=
          Seq(Series(timeOutput(sensor.name),Seq(time.head))) ++
          selectedOutput.indices.map{i=>
            if (selectedOutput(i).fieldName.equalsIgnoreCase("grid")){
              val noValue=data(noValueIdx).head.asInstanceOf[Double]
              val agg=GridTools.aggregate(data(i).asInstanceOf[Seq[GridTools.DoubleGrid]], aggregation.get, noValue)
              Series(selectedOutput(i),Seq(agg))
            }
            else
              Series(selectedOutput(i),Seq(data(i).head))
          }
        SensorData(ts,sensor)
	  }
	  else {
        val ts=
          Seq(Series(timeOutput(sensor.name),time)) ++
          selectedOutput.indices.map{i=>
            Series(selectedOutput(i),data(i).toSeq)
          }
        SensorData(ts,sensor)
	  }
	} catch {
	  case e:Exception=> println(s"Query error ${e.getMessage}")
	    e.printStackTrace()
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
	  rate = if (times.size == 0) Some(0)  else Some(times.sum/times.size)
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